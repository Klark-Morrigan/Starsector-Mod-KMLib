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
 * <p>There is no API for this: the tooltip is a private field on the map widget, and the widget is
 * not exposed. So it is read by walking the live core-UI tree to the widget and reading the field -
 * reach the core UI, take its current tab (the map when it is open), and search that subtree for a
 * component holding a live {@code StandardTooltipV2}. The field is matched by type, not by its
 * obfuscated name, so a game build that renames the field does not break the read.
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

    // How deep to search the current tab's subtree for the tooltip-bearing widget. The map widget
    // sits a few panels down; a bound keeps a pathological tree from a runaway walk.
    private static final int MAX_SEARCH_DEPTH = 8;

    // Resolved once: the tooltip class the field is matched against. Null until first resolved, or
    // when it cannot be resolved (then every read fails open).
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
            return currentTab != null && subtreeHasLiveTooltip(currentTab, MAX_SEARCH_DEPTH);
        } catch (Throwable failure) {
            warnOnce(failure);
            return false;
        }
    }

    // Whether any component in this subtree holds a non-null tooltip field. Recurses the panel's
    // children by depth, stopping at the bound so a malformed tree cannot loop the walk.
    private static boolean subtreeHasLiveTooltip(Object component, int depthRemaining)
            throws Throwable {
        if (component == null || depthRemaining < 0) {
            return false;
        }
        if (Reflection.readFieldOfType(component, resolveTooltipClass()) != null) {
            return true;
        }
        for (var child : childrenOf(component)) {
            if (subtreeHasLiveTooltip(child, depthRemaining - 1)) {
                return true;
            }
        }
        return false;
    }

    // A component's children, or none when it exposes no getChildrenCopy - most components are
    // leaves with no such method, so a failed children read ends the walk down that branch rather
    // than aborting the whole read. A genuine field-read failure still propagates to the fail-open
    // catch; only the expected "this is a leaf" case is swallowed here.
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

    // The tooltip class, resolved once and reused. A component with no getChildrenCopy is a leaf the
    // walk simply does not descend, so only the class resolution can fail here, into the caller's
    // fail-open catch.
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
