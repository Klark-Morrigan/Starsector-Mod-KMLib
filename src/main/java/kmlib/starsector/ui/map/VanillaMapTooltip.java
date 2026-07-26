package kmlib.starsector.ui.map;

import com.fs.starfarer.api.Global;

import org.apache.log4j.Logger;
import org.magiclib.ReflectionUtils;

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
 * <p>The private core-UI methods and the private tooltip host are reached through MagicLib's {@link
 * ReflectionUtils}, the ecosystem's proven bypass of the game's script-classloader reflection ban (it
 * drives {@code java.lang.reflect} through method handles so no reflect type is named in mod code). The
 * tooltip is matched by the returned value's runtime class <em>name</em> walked up its hierarchy, not by
 * a field name or {@code Class} identity, so an obfuscated rename or a classloader mismatch cannot make a
 * real tooltip read as foreign.
 *
 * <p>The whole read is best-effort: reflecting into obfuscated internals can fail on any game build, and
 * on an install without MagicLib the reflection class is absent entirely. On any failure it reports no
 * tooltip - the overlay then draws, so a broken read costs a possible double tooltip rather than a
 * missing overlay - and warns once per session so the failure is visible without flooding the log.
 *
 * <p>Because the reach is fragile and only observable in-engine, the read narrates itself when DEBUG
 * logging is on: each time its outcome changes it logs one line naming the current tab, how many nodes
 * it walked, the tooltips it saw shown, and the verdict. That is what turns "it does not suppress" from a
 * guess into a diagnosis - which hop failed, or whether the map's host was reached - without flooding the
 * log frame to frame. With DEBUG off it builds none of that, so the walk stays a bare tree search.
 */
public final class VanillaMapTooltip {

    private static final Logger LOG = Global.getLogger(VanillaMapTooltip.class);

    // The map's tooltip type, matched by name up the returned value's class hierarchy. Its expandable
    // subclass is what the map actually shows, so the walk up from the runtime class finds this.
    private static final String TOOLTIP_CLASS_NAME = "com.fs.starfarer.ui.impl.StandardTooltipV2";

    // The core UI's own accessors, driven by name through ReflectionUtils: the core and its current tab
    // (the reach to the map subtree), a host's current tooltip, a panel's children, and (to tell a shown
    // tooltip from a merely-configured one) the tooltip's fade state. All are part of the core UI's
    // contract, so they survive obfuscation.
    private static final String GET_CORE_METHOD = "getCore";
    private static final String GET_CURRENT_TAB_METHOD = "getCurrentTab";
    private static final String GET_TOOLTIP_METHOD = "getTooltip";
    private static final String GET_CHILDREN_METHOD = "getChildrenCopy";
    private static final String GET_FADER_METHOD = "getFader";
    private static final String IS_FADED_OUT_METHOD = "isFadedOut";

    // ReflectionUtils.invoke resolves a public method (declared=false) matching these argument types -
    // none, for the no-arg reads here.
    private static final Object[] NO_ARGS = new Object[0];
    private static final boolean PUBLIC_METHOD = false;

    // How deep to search the current tab's subtree for a tooltip-showing host. The map's tooltip host
    // sits several panels down inside the map tab; a bound keeps a pathological tree from a runaway walk.
    private static final int MAX_SEARCH_DEPTH = 12;

    // Cap on the tooltips named in one diagnostic line, so a tree with many shown tooltips logs a
    // readable sample rather than a wall of text.
    private static final int MAX_TRACE_TOOLTIPS = 24;

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
            var core = invokeNoArg(sector.getCampaignUI(), GET_CORE_METHOD);
            if (core == null) {
                return reportOutcome(
                        false, "getCore null on " + sector.getCampaignUI().getClass().getName(), null);
            }
            var currentTab = invokeNoArg(core, GET_CURRENT_TAB_METHOD);
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

    // Invokes a public no-arg method by name on {@code instance} through MagicLib's reflection bypass,
    // returning its result. {@code invoke} is an instance method on the ReflectionUtils singleton (only
    // set/get are static), and resolves a public method matching the argument types - none here. Throws
    // when the method is absent or the call fails; a caller that expects an absent method (a leaf node)
    // catches, while the reach hops let it reach the fail-open catch.
    private static Object invokeNoArg(Object instance, String methodName) {
        return ReflectionUtils.INSTANCE.invoke(methodName, instance, NO_ARGS, PUBLIC_METHOD);
    }

    // The first live vanilla tooltip in this subtree, or null when none is up. Recurses the panel's
    // children by depth, stopping at the bound so a malformed tree cannot loop the walk. Records every
    // shown tooltip into the trace (when one is given, i.e. DEBUG is on) so a walk that finds no vanilla
    // tooltip still says what it saw.
    private static Object findShownTooltip(Object component, int depthRemaining, WalkTrace trace) {
        if (component == null || depthRemaining < 0) {
            return null;
        }
        if (trace != null) {
            trace.nodesVisited++;
        }
        var tooltip = tooltipShownBy(component);
        if (tooltip != null && isStandardTooltip(tooltip)) {
            // A widget can hold a configured tooltip whose fader sits idle at zero (never hovered), which
            // must not suppress our overlay - only a tooltip actually faded in should. So gate on the
            // fader, the same read vanilla does before it renders a child's tooltip.
            var visible = isTooltipVisible(tooltip);
            if (trace != null) {
                trace.recordShownTooltip(tooltip, visible);
            }
            if (visible) {
                return tooltip;
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

    // The tooltip a component is currently showing, via the same getTooltip the core UI reads; null for
    // a component that is not a tooltip host (no such method - the common leaf) or that shows none now. A
    // host clears this to null when its tooltip hides, so a non-null value means one is up.
    private static Object tooltipShownBy(Object component) {
        try {
            return invokeNoArg(component, GET_TOOLTIP_METHOD);
        } catch (Throwable notATooltipHost) {
            // Most components expose no getTooltip: not a host, so it shows no tooltip to step aside for.
            return null;
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

    // Whether the tooltip is actually on screen rather than merely configured on a widget: its fader is
    // not faded out. A tooltip a widget holds but has never shown sits idle at zero brightness, which
    // reads as faded out and so does not suppress our overlay. Any unreadable fader returns false, so an
    // uncertain read leaves our overlay drawing (fail-open) rather than hiding it on a guess.
    private static boolean isTooltipVisible(Object tooltip) {
        try {
            var fader = invokeNoArg(tooltip, GET_FADER_METHOD);
            return fader != null
                    && invokeNoArg(fader, IS_FADED_OUT_METHOD) instanceof Boolean fadedOut
                    && !fadedOut;
        } catch (Throwable cannotReadFader) {
            return false;
        }
    }

    // A component's children, or none when it exposes no getChildrenCopy - most components are leaves
    // with no such method, so a failed children read ends the walk down that branch rather than aborting
    // the whole read. A genuine failure still surfaces through the fail-open catch; only the expected
    // "this is a leaf" case is swallowed here.
    private static List<?> childrenOf(Object component) {
        try {
            if (invokeNoArg(component, GET_CHILDREN_METHOD) instanceof List<?> children) {
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

    // Accumulates what one walk saw - the current tab, how many nodes it visited, and the tooltips it
    // found shown - so a walk that finds no vanilla tooltip can still say whether it reached the map's
    // host at all and what it held.
    private static final class WalkTrace {
        private final String tabClassName;
        private final List<String> shownTooltips = new ArrayList<>();
        private int nodesVisited;

        private WalkTrace(String tabClassName) {
            this.tabClassName = tabClassName;
        }

        // Notes one found tooltip's class and whether it read as visible, up to the trace cap so a busy
        // tree logs a readable sample. The visibility is what separates "found a tooltip but it was
        // faded out" from "found a shown one", the distinction a wrong suppression is diagnosed against.
        private void recordShownTooltip(Object tooltip, boolean visible) {
            if (shownTooltips.size() < MAX_TRACE_TOOLTIPS) {
                shownTooltips.add(tooltip.getClass().getName() + "(visible=" + visible + ")");
            }
        }

        private String describe() {
            return "tab=" + tabClassName
                    + " visited=" + nodesVisited
                    + " shownTooltips=" + shownTooltips;
        }
    }
}
