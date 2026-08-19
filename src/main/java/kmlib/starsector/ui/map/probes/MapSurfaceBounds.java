package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.Global;

import kmlib.logging.SessionWarning;
import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.coreui.CoreUiTree;
import kmlib.starsector.ui.screen.VanillaScreen;

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
 * <p>Chrome drawn over the surface is narrowed to what it actually draws, since there and only
 * there does its box stand between the player and map they can see. A strip laid across the map to
 * hold buttons spans the map's whole width while the player only ever aims at the buttons, so such
 * a piece stands for the boxes of its own drawn children wherever it has any. Chrome laid beside
 * the surface keeps its own box, whatever it holds: the surface's bounds already exclude it, so its
 * exact footprint was never what suppressed there.
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

    // Says once per session that this rule no longer fits. One warning covers both ways it can
    // fail, so the first of them silences the other - which costs nothing since either one means
    // the same thing, that the surface is unidentifiable.
    private static final SessionWarning WARNING = new SessionWarning(LOG);

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
     * frame. A measure reads the tab's children and then the children of each drawn one - two
     * levels, not a subtree walk - and only a measure that found a surface is kept, so an
     * unanswerable frame is retried rather than remembered.
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
            // The axes rather than the whole box: the memo's guard compares the screen it was
            // measured on against this one, and a screen's origin never moves to compare.
            var screenWidth = VanillaScreen.resolveUiWidth();
            var screenHeight = VanillaScreen.resolveUiHeight();
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
     * @param tabBox        the tab's own box, or null when the tab was never positioned
     * @param drawnChildren the tab's drawn direct children, each with the boxes it draws in, in any
     *                      order
     * @return the surface and the chrome to exclude from it, or null when the tab has no area or
     *         nothing covers enough of it
     */
    static MapSurfaceArea selectSurfaceArea(
            Rectangle tabBox,
            List<DrawnChildBoxes> drawnChildren) {

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
        for (var index = 0; index < drawnChildren.size(); index++) {
            var boxWithinTab = drawnChildren.get(index).box().intersectWith(tabBox);
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
            collectChromeBoxesAround(drawnChildren, surfaceBox, surfaceIndex));
    }

    /**
     * The drawn ones among a component's children, each with the boxes of its own drawn children.
     *
     * <p>Children that are not components, are faded to nothing, or were never positioned are left
     * out at both levels: none of them is something the player can see, so none can be the surface
     * and none is drawn over it. Shared with the widget trace so a diagnostic reporting which child
     * this rule picks feeds it the same candidates the live read does.
     *
     * @param children the children to measure, as read off the tree
     * @return each drawn child's boxes, in the order given
     */
    static List<DrawnChildBoxes> collectDrawnBoxesOf(List<?> children) {
        var drawnChildren = new ArrayList<DrawnChildBoxes>();
        for (var child : children) {
            var box = DrawnWidgets.resolveDrawnBoxOf(child);
            if (box != null) {
                drawnChildren.add(new DrawnChildBoxes(box, collectDrawnChildBoxesOf(child)));
            }
        }
        return drawnChildren;
    }

    // Every drawn child but the one accepted as the surface, each narrowed to where it draws. Taken
    // as the tab listed them rather than confined to the tab the way the surface is: confining would
    // change nothing a caller can observe, since the only points ever tested against chrome are ones
    // already inside the surface, and the surface is inside the tab.
    private static List<Rectangle> collectChromeBoxesAround(
            List<DrawnChildBoxes> drawnChildren,
            Rectangle surfaceBox,
            int surfaceIndex) {

        var chromeBoxes = new ArrayList<Rectangle>();
        for (var index = 0; index < drawnChildren.size(); index++) {
            if (index != surfaceIndex) {
                chromeBoxes.addAll(
                    resolveChromeFootprintOf(drawnChildren.get(index), surfaceBox));
            }
        }
        return chromeBoxes;
    }

    // One level down from a component, sifted the same way its own level was.
    private static List<Rectangle> collectDrawnChildBoxesOf(Object component) {
        var childBoxes = new ArrayList<Rectangle>();
        for (var child : CoreUiTree.readChildrenOf(component)) {
            var box = DrawnWidgets.resolveDrawnBoxOf(child);
            if (box != null) {
                childBoxes.add(box);
            }
        }
        return childBoxes;
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

    // Whether two boxes share any area at all. A shared edge alone does not count: an intersection
    // of zero extent is what a non-overlap yields, and a chrome piece merely abutting the surface is
    // beside it, which is the case that keeps its own box.
    private static boolean isOverlapping(Rectangle box, Rectangle other) {
        return computeAreaOf(box.intersectWith(other)) > 0f;
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

    // Where a chrome piece suppresses, which is not always the box it was laid out in.
    //
    // A piece drawn over the surface is the only kind whose box can hide map the player can see, and
    // there what suppresses is what it draws - a strip laid out to hold buttons is as wide as the
    // map, while the buttons are all there is to aim at. A piece beside the surface keeps its own
    // box, since the surface's own bounds already exclude every point in it; that is also what makes
    // this a no-op on a tab whose chrome is laid out beside its map, by construction rather than by
    // inspection.
    //
    // What this gives up: a chrome piece that both holds children and paints its own backing is
    // narrowed to the children, so a cell would light under the backing. Cosmetic and self-evident
    // in play, against a leak that is neither - the same side of the trade as failing open.
    private static List<Rectangle> resolveChromeFootprintOf(
            DrawnChildBoxes chromePiece,
            Rectangle surfaceBox) {

        if (chromePiece.drawnChildBoxes().isEmpty()
                || !isOverlapping(chromePiece.box(), surfaceBox)) {

            return List.of(chromePiece.box());
        }
        return chromePiece.drawnChildBoxes();
    }

    // On this library's own logger rather than the caller's: a rule that stopped matching the game
    // is the library's news to report.
    private static void warnOnce(String reason, Throwable failure) {
        WARNING.warnOnce(
            "Could not identify the map surface: " + reason
                + ". Overlays that stand aside for the map's chrome cannot do so this session.",
            failure);
    }
}
