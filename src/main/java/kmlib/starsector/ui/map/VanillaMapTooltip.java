package kmlib.starsector.ui.map;

import com.fs.starfarer.api.Global;

import kmlib.reflection.Reflection;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Answers "is the vanilla map screen showing a tooltip right now?" for an overlay that must step
 * aside for it. When the cursor is over a star (or any entity) the map draws its own tooltip; an
 * overlay drawing its own box wants to suppress it then so only one shows.
 *
 * <p>There is no API for this, so it is read off the live core-UI tree. A tooltip is not a node in
 * that tree and not a typed field on the map widget - the widget that draws it is a tooltip <em>host</em>
 * (the core UI's own {@code getTooltip()} contract), which holds the currently shown tooltip and hands
 * it back from {@code getTooltip()}, clearing it to null when the tooltip hides. So the read reaches the
 * core UI, takes its current tab (the map when it is open), and searches that subtree for any host whose
 * {@code getTooltip()} returns a live {@code StandardTooltipV2}. That is the same read the core UI does
 * itself to render a child's tooltip, so it tracks exactly when a tooltip is up.
 *
 * <p>The tooltip is matched by the returned value's runtime class <em>name</em> walked up its hierarchy,
 * not by a field name, a declared field type, or {@code Class.forName} identity: the host stores it
 * behind the general tooltip interface (so a declared-type match misses it), the obfuscated field name is
 * not stable across builds, and a name walk sidesteps any classloader-identity mismatch between the mod
 * and the game class.
 *
 * <p>The whole read is best-effort: reflecting into obfuscated internals can fail on any game build,
 * and this runs every frame the map is up. On any failure it reports no tooltip - the overlay then
 * draws, so a broken read costs a possible double tooltip rather than a missing overlay - and warns
 * once per session so the failure is visible without flooding the log.
 *
 * <p>Because the reach is fragile and only observable in-engine, the read narrates itself when DEBUG
 * logging is on: each time its outcome changes it logs one line naming the current tab, how many nodes
 * it walked, every tooltip host it found and what that host's {@code getTooltip()} returned, and the
 * verdict. That is what turns "it does not suppress" from a guess into a diagnosis - which hop failed, or
 * which host was or was not reached - without flooding the log frame to frame. With DEBUG off it builds
 * none of that, so the walk stays a bare tree search.
 */
public final class VanillaMapTooltip {

    private static final Logger LOG = Global.getLogger(VanillaMapTooltip.class);

    // The map's tooltip type, matched by name up the returned value's class hierarchy. Its expandable
    // subclass is what the map actually shows, so the walk up from the runtime class finds this.
    private static final String TOOLTIP_CLASS_NAME = "com.fs.starfarer.ui.impl.StandardTooltipV2";

    // The core UI's own name for the "what tooltip is this host showing" accessor - part of the tooltip
    // contract the core UI reads to render a child's tooltip, so it survives obfuscation where a private
    // field name does not.
    private static final String GET_TOOLTIP_METHOD = "getTooltip";

    // How deep to search the current tab's subtree for a tooltip-showing host. The map's tooltip host
    // sits several panels down inside the map tab; a bound keeps a pathological tree from a runaway walk.
    private static final int MAX_SEARCH_DEPTH = 12;

    // Cap on the hosts named in one diagnostic line, so a tree with many tooltip-bearing widgets logs a
    // readable sample rather than a wall of text.
    private static final int MAX_TRACE_HOSTS = 24;

    // The last diagnostic line logged, so the probe narrates only when its outcome changes rather than
    // every frame the map is up.
    private static String lastLoggedOutcome;

    // One warning per session, so a build where the read breaks says so once rather than every frame.
    private static boolean hasWarnedThisSession;

    private VanillaMapTooltip() {
    }

    /**
     * @return whether the vanilla map screen is currently drawing a tooltip; {@code false} on any
     *         read failure, so the caller draws rather than hides on a broken read
     */
    public static boolean isShowing() {
        try {
            var sector = Global.getSector();
            if (sector == null || sector.getCampaignUI() == null) {
                return reportOutcome(false, "no campaign UI", null);
            }
            var core = Reflection.invokeNoArg(sector.getCampaignUI(), "getCore");
            if (core == null) {
                return reportOutcome(
                        false, "getCore null on " + sector.getCampaignUI().getClass().getName(), null);
            }
            var currentTab = Reflection.invokeNoArg(core, "getCurrentTab");
            if (currentTab == null) {
                return reportOutcome(
                        false, "getCurrentTab null on " + core.getClass().getName(), null);
            }
            // Build the diagnostic trace only when DEBUG is on, so a normal frame is a bare tree walk
            // with no per-node string work.
            var trace = LOG.isDebugEnabled() ? new WalkTrace(currentTab.getClass().getName()) : null;
            var tooltip = findShownTooltip(currentTab, MAX_SEARCH_DEPTH, trace);
            return reportOutcome(tooltip != null, null, trace);
        } catch (Throwable failure) {
            warnOnce(failure);
            return false;
        }
    }

    // The first live vanilla tooltip in this subtree, or null when none is up. Recurses the panel's
    // children by depth, stopping at the bound so a malformed tree cannot loop the walk. Records every
    // host it passes into the trace (when one is given, i.e. DEBUG is on) so a walk that finds nothing
    // still says what it saw.
    private static Object findShownTooltip(Object component, int depthRemaining, WalkTrace trace)
            throws Throwable {
        if (component == null || depthRemaining < 0) {
            return null;
        }
        if (trace != null) {
            trace.nodesVisited++;
        }
        var read = readTooltipOf(component);
        if (read.isHost()) {
            if (trace != null) {
                trace.recordHost(component, read.tooltip());
            }
            if (read.tooltip() != null && isStandardTooltip(read.tooltip())) {
                return read.tooltip();
            }
        }
        for (var child : childrenOf(component)) {
            var found = findShownTooltip(child, depthRemaining - 1, trace);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    // What a component's getTooltip reports: whether it is a tooltip host at all (has the method) and,
    // if so, the tooltip it currently shows (null when it shows none). Kept apart so the walk can tell
    // "not a host" (the common leaf) from "a host showing nothing", which the diagnostics need to
    // distinguish a tree that never reaches the map's host from one where the host holds no tooltip.
    private static TooltipRead readTooltipOf(Object component) {
        try {
            return new TooltipRead(true, Reflection.invokeNoArg(component, GET_TOOLTIP_METHOD));
        } catch (NoSuchMethodException notAHost) {
            return TooltipRead.NOT_A_HOST;
        } catch (Throwable readFailed) {
            // A host whose getTooltip threw: still a host, but showing nothing we can read. Rare; the
            // diagnostics record it via the null tooltip so a systemic read fault is visible.
            return TooltipRead.HOST_READ_FAILED;
        }
    }

    // Whether the tooltip's runtime class is, or descends from, the vanilla tooltip class - matched by
    // name up the hierarchy rather than by Class identity, so a classloader mismatch between the mod and
    // the game class cannot make a real tooltip read as foreign.
    private static boolean isStandardTooltip(Object tooltip) {
        for (var clazz = tooltip.getClass(); clazz != null && clazz != Object.class;
                clazz = clazz.getSuperclass()) {
            if (clazz.getName().equals(TOOLTIP_CLASS_NAME)) {
                return true;
            }
        }
        return false;
    }

    // A component's children, or none when it exposes no getChildrenCopy - most components are
    // leaves with no such method, so a failed children read ends the walk down that branch rather
    // than aborting the whole read. A genuine failure still surfaces through the fail-open catch;
    // only the expected "this is a leaf" case is swallowed here.
    private static List<?> childrenOf(Object component) {
        try {
            if (Reflection.invokeNoArg(component, "getChildrenCopy") instanceof List<?> children) {
                return children;
            }
        } catch (Throwable notAParent) {
            // No getChildrenCopy: a leaf. Descend no further down this branch.
        }
        return List.of();
    }

    // Logs the probe's outcome, at DEBUG, the first time it reaches a given state and on every change
    // after, then returns the verdict so it reads as one line at the call site. A stable outcome logs
    // once; a tooltip appearing or disappearing logs the transition, so the log shows what the probe saw
    // without a per-frame flood. With DEBUG off it does nothing but return the verdict.
    private static boolean reportOutcome(boolean verdict, String reachFailure, WalkTrace trace) {
        if (!LOG.isDebugEnabled()) {
            return verdict;
        }
        var outcome = reachFailure != null
                ? "verdict=" + verdict + " (" + reachFailure + ")"
                : "verdict=" + verdict + " " + (trace == null ? "" : trace.describe());
        if (!outcome.equals(lastLoggedOutcome)) {
            lastLoggedOutcome = outcome;
            LOG.debug("Vanilla map-tooltip probe: " + outcome);
        }
        return verdict;
    }

    private static void warnOnce(Throwable failure) {
        if (hasWarnedThisSession) {
            return;
        }
        hasWarnedThisSession = true;
        LOG.warn("Could not read the vanilla map tooltip state by reflection; the overlay tooltip "
                + "will not suppress for it. This is safe but means both may show over a star icon.",
                failure);
    }

    // What one component's getTooltip read yielded: whether it is a host, and the tooltip it shows.
    private record TooltipRead(boolean isHost, Object tooltip) {
        private static final TooltipRead NOT_A_HOST = new TooltipRead(false, null);
        private static final TooltipRead HOST_READ_FAILED = new TooltipRead(true, null);
    }

    // Accumulates what one walk saw - the current tab, how many nodes it visited, and each tooltip host
    // with the tooltip it showed - so a walk that finds no tooltip can still say why: whether it reached
    // the map's host at all, and what that host held.
    private static final class WalkTrace {
        private final String tabClassName;
        private final List<String> hostFindings = new ArrayList<>();
        private int nodesVisited;
        private int hostsFound;

        private WalkTrace(String tabClassName) {
            this.tabClassName = tabClassName;
        }

        // Notes one tooltip host and the tooltip it is showing (or "none"), up to the trace cap so a
        // busy tree logs a readable sample.
        private void recordHost(Object host, Object tooltip) {
            hostsFound++;
            if (hostFindings.size() < MAX_TRACE_HOSTS) {
                var shown = tooltip == null
                        ? "none"
                        : tooltip.getClass().getName();
                hostFindings.add(host.getClass().getName() + "=>" + shown);
            }
        }

        private String describe() {
            return "tab=" + tabClassName + " visited=" + nodesVisited + " hosts=" + hostsFound
                    + " " + hostFindings;
        }
    }
}
