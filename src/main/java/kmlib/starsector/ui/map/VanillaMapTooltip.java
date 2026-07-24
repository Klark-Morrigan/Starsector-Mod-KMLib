package kmlib.starsector.ui.map;

import com.fs.starfarer.api.Global;

import kmlib.reflection.Reflection;

import org.apache.log4j.Logger;

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
 * <p>The tooltip is matched by the returned value's runtime type, not by a field name or a field's
 * declared type: the host stores it behind the general tooltip interface, so a declared-type match would
 * miss it, and the obfuscated field name is not stable across builds. {@code getTooltip} is part of the
 * core UI's own tooltip contract, so it survives obfuscation where a private field does not.
 *
 * <p>The whole read is best-effort: reflecting into obfuscated internals can fail on any game build,
 * and this runs every frame the map is up. On any failure it reports no tooltip - the overlay then
 * draws, so a broken read costs a possible double tooltip rather than a missing overlay - and warns
 * once per session so the failure is visible without flooding the log.
 */
public final class VanillaMapTooltip {

    private static final Logger LOG = Global.getLogger(VanillaMapTooltip.class);

    // The map's tooltip type. Referenced by name, not as a class literal, so this class stays
    // loadable where the tooltip class (with obfuscated inner members) may not.
    private static final String TOOLTIP_CLASS_NAME = "com.fs.starfarer.ui.impl.StandardTooltipV2";

    // The core UI's own name for the "what tooltip is this host showing" accessor - part of the
    // tooltip contract the core UI reads to render a child's tooltip, so it survives obfuscation.
    private static final String GET_TOOLTIP_METHOD = "getTooltip";

    // How deep to search the current tab's subtree for a tooltip-showing host. The map's tooltip host
    // sits several panels down inside the map tab; a bound keeps a pathological tree from a runaway walk.
    private static final int MAX_SEARCH_DEPTH = 12;

    // Resolved once: the tooltip class the returned value is matched against. Null until first resolved,
    // or when it cannot be resolved (then every read fails open).
    private static Class<?> tooltipClass;

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
                return false;
            }
            var core = Reflection.invokeNoArg(sector.getCampaignUI(), "getCore");
            if (core == null) {
                return false;
            }
            var currentTab = Reflection.invokeNoArg(core, "getCurrentTab");
            return currentTab != null && subtreeShowsTooltip(currentTab, MAX_SEARCH_DEPTH);
        } catch (Throwable failure) {
            warnOnce(failure);
            return false;
        }
    }

    // Whether any host in this subtree is currently showing a tooltip. Recurses the panel's children by
    // depth, stopping at the bound so a malformed tree cannot loop the walk.
    private static boolean subtreeShowsTooltip(Object component, int depthRemaining) throws Throwable {
        if (component == null || depthRemaining < 0) {
            return false;
        }
        var tooltip = tooltipShownBy(component);
        if (tooltip != null && resolveTooltipClass().isInstance(tooltip)) {
            return true;
        }
        for (var child : childrenOf(component)) {
            if (subtreeShowsTooltip(child, depthRemaining - 1)) {
                return true;
            }
        }
        return false;
    }

    // The tooltip a component is currently showing, via the same getTooltip the core UI reads; null for
    // a component that is not a tooltip host (no such method - the common case) or that shows none now.
    // A host clears this to null when its tooltip hides, so a non-null value means one is up.
    private static Object tooltipShownBy(Object component) {
        try {
            return Reflection.invokeNoArg(component, GET_TOOLTIP_METHOD);
        } catch (Throwable notATooltipHost) {
            // Most components expose no getTooltip: not a host, so it shows no tooltip to step aside for.
            return null;
        }
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

    // The tooltip class, resolved once and reused, used to match the returned tooltip's runtime type.
    // Only its resolution can fail here, into the caller's fail-open catch.
    private static Class<?> resolveTooltipClass() throws ClassNotFoundException {
        if (tooltipClass == null) {
            tooltipClass = Class.forName(TOOLTIP_CLASS_NAME);
        }
        return tooltipClass;
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
}
