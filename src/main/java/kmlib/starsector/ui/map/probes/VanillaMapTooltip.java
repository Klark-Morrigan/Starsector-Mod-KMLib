package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.Global;

import kmlib.starsector.ui.coreui.CoreUiTree;

import org.apache.log4j.Logger;
import org.magiclib.ReflectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Answers "which tooltip is the vanilla map screen showing right now?" for an overlay that must
 * step aside for it. When the cursor is over a star (or any entity) the map draws its own tooltip;
 * an overlay drawing its own box wants to suppress it then so only one shows.
 *
 * <p>The found component is handed back rather than reduced to a yes/no, because stepping aside is
 * not the only way to cohabit with a tooltip: a caller that draws over the tooltip instead needs the
 * component itself to act on. The probe stays a pure locator - it finds, it never draws.
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
 * <p>The whole read is best-effort, since reflecting into obfuscated internals can fail on any game
 * build. On any failure it reports no tooltip - the overlay then draws, so a broken read costs a
 * possible double tooltip rather than a missing overlay - and warns once per session so the failure is
 * visible without flooding the log. A missing MagicLib is not among those failures - it is a
 * declared {@code mod_info.json} dependency.
 *
 * <p>Because the reach is fragile and only observable in-engine, the read narrates itself at DEBUG:
 * each time its outcome changes it logs one line naming the current tab, how many nodes it walked,
 * the tooltips it saw shown, and the verdict. That is what turns "it does not suppress" from a guess
 * into a diagnosis - which hop failed, or whether the map's host was reached - without flooding the
 * log frame to frame. With DEBUG off it builds none of that, so the walk stays a bare tree search.
 * The level is this library's own, so a consuming mod's verbosity setting does not reach it; the
 * warning below is the part that survives the default level, and it is the one that matters.
 *
 * <p>An instance rather than a static holder so a caller can stand a stub in its place: whether the
 * map is drawing its own tooltip decides whether that caller draws at all, which is behaviour worth
 * pinning without a live game.
 */
public final class VanillaMapTooltip {

    private static final Logger LOG = Global.getLogger(VanillaMapTooltip.class);

    // The map's tooltip type, matched by name up the returned value's class hierarchy. Its expandable
    // subclass is what the map actually shows, so the walk up from the runtime class finds this.
    private static final String TOOLTIP_CLASS_NAME = "com.fs.starfarer.ui.impl.StandardTooltipV2";

    // The accessors this probe names for itself: a host's current tooltip, and (to tell a shown tooltip
    // from a merely-configured one) the tooltip's fade state. The hops down to the tab and a component's
    // children are {@link CoreUiTree}'s. All are part of the core UI's contract, so they survive
    // obfuscation.
    private static final String GET_TOOLTIP_METHOD = "getTooltip";
    private static final String GET_FADER_METHOD = "getFader";
    private static final String IS_FADED_OUT_METHOD = "isFadedOut";

    // Says once per session that this read broke, rather than every frame. Per instance rather than
    // per class, since each consumer holds its own probe and a shared flag would let one consumer's
    // broken read silence the news of another's.
    private final SessionWarning warning = new SessionWarning(LOG);

    // The last diagnostic line logged, so the probe narrates only when its outcome changes rather
    // than every frame the map is up.
    private String lastLoggedOutcome;

    /**
     * @return the core-UI component the vanilla map screen is currently showing as a tooltip, or
     *         {@code null} when none is up; {@code null} on any read failure too, so the caller
     *         falls back to its own drawing rather than acting on a broken read
     */
    public Object findShownTooltip() {
        try {
            var currentTab = CoreUiTree.resolveCurrentTab();
            if (currentTab == null) {
                reportOutcome(false, "no current tab", null);
                return null;
            }
            // Build the diagnostic trace only when DEBUG is on, so a normal frame is a bare tree walk
            // with no per-node string work.
            var trace = LOG.isDebugEnabled() ? new WalkTrace(currentTab.getClass().getName()) : null;
            var tooltip = searchSubtreeForShownTooltip(currentTab, ProbeLimits.MAX_SEARCH_DEPTH, trace);
            reportOutcome(tooltip != null, null, trace);
            return tooltip;
        } catch (Throwable failure) {
            warnOnce(failure);
            return null;
        }
    }

    /**
     * @return whether the vanilla map screen is currently drawing a tooltip; {@code false} on any
     *         read failure, so the caller draws rather than hides on a broken read
     */
    public boolean isShowing() {
        return findShownTooltip() != null;
    }

    // The first live vanilla tooltip in this subtree, or null when none is up. Recurses the panel's
    // children by depth, stopping at the bound so a malformed tree cannot loop the walk. Records every
    // shown tooltip into the trace (when one is given, i.e. DEBUG is on) so a walk that finds no vanilla
    // tooltip still says what it saw.
    private static Object searchSubtreeForShownTooltip(Object component, int depthRemaining,
            WalkTrace trace) {
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
        for (var child : CoreUiTree.readChildrenOf(component)) {
            var found = searchSubtreeForShownTooltip(child, depthRemaining - 1, trace);
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
            return CoreUiTree.invokeNoArg(component, GET_TOOLTIP_METHOD);
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
            var fader = CoreUiTree.invokeNoArg(tooltip, GET_FADER_METHOD);
            return fader != null
                && CoreUiTree.invokeNoArg(fader, IS_FADED_OUT_METHOD) instanceof Boolean fadedOut
                && !fadedOut;
        } catch (Throwable cannotReadFader) {
            return false;
        }
    }

    // Logs the probe's outcome, at DEBUG, the first time it reaches a given state and on every change
    // after. A stable outcome logs once; a tooltip appearing or disappearing logs the transition, so
    // the log shows what the probe saw without a per-frame flood. With DEBUG off it does nothing.
    private void reportOutcome(boolean verdict, String reachFailure, WalkTrace trace) {
        if (!LOG.isDebugEnabled()) {
            return;
        }
        var outcome = reachFailure != null
            ? "verdict=" + verdict + " (" + reachFailure + ")"
            : "verdict=" + verdict + " " + (trace == null ? "" : trace.describe());

        if (!outcome.equals(lastLoggedOutcome)) {
            lastLoggedOutcome = outcome;
            LOG.debug("Vanilla map-tooltip probe: " + outcome);
        }
    }

    private void warnOnce(Throwable failure) {
        warning.warnOnce(
            "Could not read the vanilla map tooltip state by reflection; "
                + "the overlay tooltip will not suppress for it. "
                + "This is safe but means both may show over a star icon.",
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
            if (shownTooltips.size() < ProbeLimits.MAX_DESCRIBED_ITEMS) {
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
