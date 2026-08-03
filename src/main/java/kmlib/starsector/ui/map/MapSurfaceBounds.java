package kmlib.starsector.ui.map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.coreui.CoreUiTree;

import org.apache.log4j.Logger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Where a map tab shows its map, so an overlay hovering the map can tell the map apart from the
 * chrome the tab draws beside and over it.
 *
 * <p>The game publishes no "is the cursor over UI" answer, and the question cannot be asked the
 * obvious way round: the map tab's chrome is several small widgets whose count and identity are a
 * fact about one build, while the map surface is a single widget that nearly fills the tab. So the
 * surface is what is identified, and the chrome is whatever else the tab draws at the same level.
 *
 * <p>Identifies the surface by shape rather than by name. The obfuscator reshuffles the tab's class
 * names between game builds, so naming the surface's class would break on the next one, and because
 * a caller here has to fail open a break would restore the un-suppressed behaviour silently. Two
 * structural facts single it out instead, both read off the live tree:
 *
 * <ul>
 *   <li><b>Depth.</b> Only the tab's direct children are considered. The panned map content is
 *       larger than the screen and contains the cursor even when the cursor is on a tab strip, so a
 *       rule that could reach it would suppress nothing - but it hangs below the surface rather than
 *       beside it, so a single-level scan cannot reach it.</li>
 *   <li><b>Coverage.</b> Among those children the surface is the one that nearly fills the tab,
 *       while every chrome piece is a thin strip or a small control.</li>
 * </ul>
 *
 * <p>The rest of those children come back as the surface's chrome, because a complement of the
 * surface does not cover every host. Where the surface is inset the chrome sits beside it and is
 * outside it anyway; where the surface fills its tab exactly the chrome is drawn over it, and only
 * naming the siblings excludes it. See {@link MapSurfaceArea}.
 *
 * <p>Reports nothing rather than guessing when no child fits, and says so once at WARN. A build that
 * reshapes the tab past this rule is exactly the case that would otherwise regress in silence, so
 * the absence is made loud even though the rule names no class.
 *
 * <p>Answers only about a map tab, on whichever screen shows one - see {@link ShownMapTab}. That
 * scoping is also what keeps the warning honest: a rule about how a map tab is laid out has nothing
 * to say about a tab that is not one, and would otherwise report itself broken over every screen
 * whose tab it was never meant to fit.
 */
public final class MapSurfaceBounds {

    private static final Logger LOG = Global.getLogger(MapSurfaceBounds.class);

    // How much of the tab the surface must cover to be accepted as the surface. Set well clear of
    // both sides of the gap it has to split: the surface covers about 96% of the tab, while the
    // widest chrome piece - the full-width tab strip - covers about 1%. Anything near either figure
    // would be a rule tuned to one build's pixel sizes rather than to the shape of the tree.
    private static final float MIN_SURFACE_SHARE_OF_TAB = 0.5f;

    // One warning per session, so a build this rule no longer fits says so once rather than per
    // frame. The first failure to warn wins; a session that hits both kinds has the second silenced,
    // which costs nothing since either one means the same thing - the surface is unidentifiable.
    private static boolean hasWarnedThisSession;

    // The last surface measured, and what it was measured against. Kept because a caller in a render
    // pass asks per frame while the answer moves only when the layout does, and re-reading it is not
    // free: the core's children accessor hands back a fresh copy of the list every call, so a
    // per-frame measure is a per-frame allocation in the middle of a frame.
    //
    // Held weakly, and only against the tab it came from. A tab lays its children out once and keeps
    // them, so the same tab instance on the same screen has the same surface - while a different
    // instance is a tab rebuilt or a different screen entirely, whose layout this says nothing
    // about. That is also what makes one memo slot enough for two screens: switching between them
    // hands a different widget in and re-measures. Weakly because a strong static reference would
    // pin one save's whole tab subtree past the load that replaced it.
    private static WeakReference<Object> measuredTab = new WeakReference<>(null);
    private static MapSurfaceArea memoisedSurfaceArea;

    // The screen the memo was measured on. The game fixes its resolution at launch, so this is
    // expected never to move - it is here so that the memo rests on a checked fact rather than on
    // that expectation, since a stale surface would silently misplace every suppression built on it.
    private static float measuredScreenWidth;
    private static float measuredScreenHeight;

    private MapSurfaceBounds() {
    }

    /**
     * Where the map tab on screen shows its map.
     *
     * <p>Measured once per tab and screen and reused after, so a caller in a render pass can ask per
     * frame. A measure is one children read and a box read per child - a single level, not a subtree
     * walk - and only a measure that found a surface is kept, so an unanswerable frame is retried
     * rather than remembered.
     *
     * @return the surface and the chrome drawn with it, or null when no map tab is on screen, the
     *         reach into the widget tree failed, or no child of the tab looks like a surface; a
     *         caller decides what an unanswerable question means for it
     */
    public static MapSurfaceArea resolveSurfaceArea() {
        try {
            // Null off the map screens, and quietly so: a screen showing no map is the ordinary
            // state, not a rule that stopped fitting, so nothing below it is asked or warned about.
            var mapTab = ShownMapTab.resolveShownMapTab();
            if (mapTab == null) {
                return null;
            }
            var screenWidth = Global.getSettings().getScreenWidth();
            var screenHeight = Global.getSettings().getScreenHeight();
            if (isMemoisedFor(mapTab, screenWidth, screenHeight)) {
                return memoisedSurfaceArea;
            }
            var surfaceArea = selectSurfaceArea(
                DrawnWidgets.resolveBoxOf(mapTab),
                collectDrawnBoxesOf(CoreUiTree.readChildrenOf(mapTab)));

            if (surfaceArea == null) {
                warnOnce("no child of the map tab covers enough of it to be the map surface", null);
                return null;
            }
            memoise(mapTab, screenWidth, screenHeight, surfaceArea);
            return surfaceArea;
        } catch (Throwable failure) {
            // Swallowed rather than raised: a caller is in the middle of a render pass, and a read
            // that cannot answer must not take down the frame it was meant to refine.
            warnOnce("the map tab's widget tree could not be walked by reflection", failure);
            return null;
        }
    }

    /**
     * Splits a tab's direct children into the surface and the chrome: the one covering the most of
     * the tab is the surface, every other drawn child is chrome, and every candidate is confined to
     * the tab's own box first.
     *
     * <p>The confining is what keeps a child that overflows the tab from being accepted as wider
     * than the tab is - the surface box never reaches past the tab, so a cursor outside the tab is
     * never inside the surface.
     *
     * @param tabBox          the tab's own box, or null when the tab was never positioned
     * @param drawnChildBoxes the boxes of the tab's drawn direct children, in any order
     * @return the surface and its sibling chrome, or null when the tab has no area or nothing
     *         covers enough of it
     */
    static MapSurfaceArea selectSurfaceArea(Rectangle tabBox, List<Rectangle> drawnChildBoxes) {
        if (tabBox == null) {
            return null;
        }
        var tabArea = computeAreaOf(tabBox);
        if (tabArea <= 0f) {
            return null;
        }
        var surfaceIndex = -1;
        var largestArea = 0f;
        Rectangle surfaceBox = null;
        for (var index = 0; index < drawnChildBoxes.size(); index++) {
            var boxWithinTab = drawnChildBoxes.get(index).intersectWith(tabBox);
            var area = computeAreaOf(boxWithinTab);
            if (area > largestArea) {
                largestArea = area;
                surfaceIndex = index;
                surfaceBox = boxWithinTab;
            }
        }
        if (largestArea < tabArea * MIN_SURFACE_SHARE_OF_TAB) {
            return null;
        }
        return new MapSurfaceArea(
            surfaceBox,
            collectChromeBoxesAround(drawnChildBoxes, surfaceIndex));
    }

    /**
     * The drawn ones among a component's children, as boxes.
     *
     * <p>Children that are not components, are faded to nothing, or were never positioned are left
     * out: none of them is something the player can see, so none can be the surface. Shared with the
     * widget trace so a diagnostic reporting which child this rule picks feeds it the same
     * candidates the live read does.
     *
     * @param children the children to measure, as read off the tree
     * @return their boxes, in the order given
     */
    static List<Rectangle> collectDrawnBoxesOf(List<?> children) {
        var childBoxes = new ArrayList<Rectangle>();
        for (var child : children) {
            if (child instanceof UIComponentAPI widget && DrawnWidgets.isWidgetDrawn(widget)) {
                var box = DrawnWidgets.resolveBoxOf(widget);
                if (box != null) {
                    childBoxes.add(box);
                }
            }
        }
        return childBoxes;
    }

    // Every drawn child but the one accepted as the surface. Taken as the tab listed them rather
    // than confined to the tab the way the surface is: confining would change nothing a caller can
    // observe, since the only points ever tested against chrome are ones already inside the surface,
    // and the surface is inside the tab.
    private static List<Rectangle> collectChromeBoxesAround(
            List<Rectangle> drawnChildBoxes,
            int surfaceIndex) {

        var chromeBoxes = new ArrayList<Rectangle>();
        for (var index = 0; index < drawnChildBoxes.size(); index++) {
            if (index != surfaceIndex) {
                chromeBoxes.add(drawnChildBoxes.get(index));
            }
        }
        return chromeBoxes;
    }

    private static float computeAreaOf(Rectangle box) {
        return box.width() * box.height();
    }

    // Whether the memo was measured from this very tab on this very screen. Identity rather than
    // equality: two tabs are the same layout only by being the same object, and a widget's equals is
    // the obfuscated class's business.
    private static boolean isMemoisedFor(Object mapTab, float screenWidth, float screenHeight) {
        return memoisedSurfaceArea != null
            && measuredTab.get() == mapTab
            && measuredScreenWidth == screenWidth
            && measuredScreenHeight == screenHeight;
    }

    // Records a measure for reuse. Only ever called with a surface that was found, so a frame that
    // could not answer is retried on the next one rather than pinning its own failure.
    private static void memoise(
            Object mapTab,
            float screenWidth,
            float screenHeight,
            MapSurfaceArea surfaceArea) {

        measuredTab = new WeakReference<>(mapTab);
        measuredScreenWidth = screenWidth;
        measuredScreenHeight = screenHeight;
        memoisedSurfaceArea = surfaceArea;
    }

    // WARN rather than DEBUG, and on this library's own logger: a rule that stopped matching the
    // game is the library's news, and it has to survive the default log level to be the warning it
    // was meant to be.
    private static void warnOnce(String reason, Throwable failure) {
        if (hasWarnedThisSession) {
            return;
        }
        hasWarnedThisSession = true;
        var message = "Could not identify the map surface: " + reason
            + ". Overlays that stand aside for the map's chrome cannot do so this session.";
        if (failure == null) {
            LOG.warn(message);
        } else {
            LOG.warn(message, failure);
        }
    }
}
