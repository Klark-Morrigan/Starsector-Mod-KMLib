package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.coreui.CoreUiTree;
import kmlib.starsector.ui.input.UiCursor;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Describes the vanilla widgets the cursor is currently inside on the map screen, so an overlay that
 * must stand aside for the map's own chrome can be written against what is actually there.
 *
 * <p>An overlay hovering the map has no way to ask "is the cursor over vanilla UI?" - the game
 * publishes no such answer, and the map surface spans nearly the whole tab with the chrome laid in
 * strips over it, so the question is really "which of the tab's widgets is the cursor in, and which
 * of those is the map". That is a fact about one game build's widget tree, not something derivable,
 * so this reports it: hover the chrome and read off what contains the cursor, hover open space and
 * read off what does not.
 *
 * <p>Reports depth and parent alongside each box, because the boxes alone do not say how the tree is
 * shaped, and the shape is what a rule for telling map from chrome has to be built on. Two widgets
 * can both contain the cursor because one encloses the other or because they merely overlap as
 * siblings, and those want opposite rules; only the parentage tells them apart.
 *
 * <p>Reports the tab's whole set of direct children too, with the box each occupies, and what
 * {@link MapSurfaceBounds} picks out of them. A rule that chooses among siblings by size can only be
 * held to account against the siblings it chose from, and the under-cursor view never shows the ones
 * the cursor did not happen to visit.
 *
 * <p>Those children are the <em>map</em> tab's, named separately from the tab that is up, because
 * the two are not the same widget on every screen - the intel screen hosts its map below a tab that
 * is not one. Rooting them where {@link ShownMapTab} roots the rule is what keeps the line an
 * account of what the rule is doing; describing one screen's children beside another screen's
 * suppression would be wrong in a way only visible in play. The under-cursor walk stays rooted at
 * the tab that is up, since what the cursor is inside is the raw reading this exists to take.
 *
 * <p>The walk is unpublished API and the hit-test is not. Reaching the tab's components needs
 * {@link CoreUiTree}'s by-name reach, but every component then answers {@code getPosition} and
 * {@code getOpacity} as {@link UIComponentAPI}, so what is reported is read through the published
 * interface rather than guessed at.
 *
 * <p>Describes rather than logs. A logger is named after its class, so a line written here would sit
 * outside every mod's logger subtree and answer to no mod's verbosity setting; handing the
 * description back instead lets the caller log it as its own, under its own switch. That also leaves
 * the caller to decide how often to say it - the answer changes only when the cursor crosses a
 * widget edge, and a caller in a render pass will want to notice that rather than repeat itself
 * sixty times a second.
 */
public final class MapTabWidgetTrace {

    private static final Logger LOG = Global.getLogger(MapTabWidgetTrace.class);

    // How deep to walk the tab's subtree. The chrome sits a few panels down inside the tab; a bound
    // keeps a pathological tree from a runaway walk.
    private static final int MAX_SEARCH_DEPTH = 12;

    // Cap on the widgets named in one description, so a deeply nested hit stays readable.
    private static final int MAX_TRACE_WIDGETS = 24;

    // One warning per session, so a build where the reach breaks says so once rather than per frame.
    private static boolean hasWarnedThisSession;

    private MapTabWidgetTrace() {
    }

    /**
     * The current tab, the map tab on screen with the box each of its direct children occupies,
     * what {@link MapSurfaceBounds} picks out of them as the map surface and as the chrome to
     * exclude from it, and the widgets whose drawn box contains the cursor - the last of those
     * outermost first, so the innermost is named last, each with its depth, class, box, opacity and
     * parent.
     *
     * <p>Call from a map render path. Reads the cursor in UI coordinates, the space
     * {@code getPosition} reports in, so the boxes described are the boxes the player sees.
     *
     * <p>Names every direct child, not only the ones under the cursor, because that is the list the
     * surface rule chooses from and a cursor-filtered view of it cannot show what the rule passed
     * over. A child the cursor never visits is invisible to the under-cursor walk while still being
     * a candidate, so a rule that picks by size can only be checked against the whole list. The
     * chosen surface and the chrome boxes are reported beside it, off the same candidates the live
     * read uses, so the line shows the choice rather than leaving it to be re-derived from the boxes
     * by eye - and the chrome boxes are not always in the children list, since a chrome piece drawn
     * over the surface stands for the boxes of what it draws a level further down.
     *
     * <p>Costs a tree walk and builds a string, so a caller in a render pass should ask only while
     * it intends to report the answer. The cursor position is deliberately left out: a caller
     * reporting only when the answer changes would otherwise log on every pixel of mouse movement,
     * and each widget's box is in the line anyway.
     *
     * @return a one-line description for a log, or null when there is no tab to walk or the reach
     *         into the widget tree failed - neither of which the caller can act on differently
     */
    public static String describeWidgetsUnderCursor() {
        try {
            var currentTab = CoreUiTree.resolveCurrentTab();
            if (currentTab == null) {
                return null;
            }
            var widgetsUnderCursor = new ArrayList<String>();
            collectWidgetsContaining(
                currentTab,
                null,
                0,
                UiCursor.getUiX(),
                UiCursor.getUiY(),
                widgetsUnderCursor);

            var mapTab = ShownMapTab.resolveShownMapTab();
            var mapTabChildren = mapTab == null ? List.of() : CoreUiTree.readChildrenOf(mapTab);
            return "tab=" + describeTab(currentTab)
                + " mapTab=" + (mapTab == null ? "none" : describeTab(mapTab))
                + " surface=" + describeSurfacePickedFrom(mapTab, mapTabChildren)
                + " children=" + describeDirectChildren(mapTabChildren)
                + " under=" + widgetsUnderCursor;
        } catch (Throwable failure) {
            // Swallowed rather than raised: this is a diagnostic, and one that cannot read the tree
            // must not take down the render pass its caller is in the middle of.
            warnOnce(failure);
            return null;
        }
    }

    /**
     * Whether a component counts as being under the cursor: it is drawn at all, and its box
     * contains the point.
     *
     * <p>The opacity gate is what keeps a faded-out panel from reading as chrome. A tab the player
     * has switched away from keeps its box and its place in the tree while fading to nothing, so
     * box containment alone would report widgets that are not on screen - and an overlay built on
     * that would stand aside for chrome nobody can see.
     *
     * @param box     the component's drawn box in UI coordinates
     * @param opacity the component's own opacity
     * @param cursorX the cursor's x in UI coordinates
     * @param cursorY the cursor's y in UI coordinates
     * @return whether the component is drawn and contains the cursor
     */
    static boolean isWidgetUnderCursor(
            Rectangle box,
            float opacity,
            float cursorX,
            float cursorY) {

        return box != null
            && opacity >= DrawnWidgets.MIN_VISIBLE_OPACITY
            && box.containsPoint(cursorX, cursorY);
    }

    // Walks the subtree depth-first, appending each drawn component that contains the cursor. Order
    // is outermost-first, so the last entry is the innermost widget the cursor is in. Carries the
    // parent and the depth down rather than deriving them afterwards, since a flat list of hits
    // cannot say which of them enclose each other.
    private static void collectWidgetsContaining(
            Object component,
            Object parent,
            int depth,
            float cursorX,
            float cursorY,
            List<String> widgetsUnderCursor) {

        if (component == null || depth > MAX_SEARCH_DEPTH) {
            return;
        }
        if (component instanceof UIComponentAPI widget) {
            var box = DrawnWidgets.resolveBoxOf(widget);
            if (isWidgetUnderCursor(box, widget.getOpacity(), cursorX, cursorY)
                    && widgetsUnderCursor.size() < MAX_TRACE_WIDGETS) {

                widgetsUnderCursor.add(describeWidget(widget, box, depth, parent));
            }
        }
        for (var child : CoreUiTree.readChildrenOf(component)) {
            collectWidgetsContaining(
                child, component, depth + 1, cursorX, cursorY, widgetsUnderCursor);
        }
    }

    private static String describeWidget(
            UIComponentAPI widget,
            Rectangle box,
            int depth,
            Object parent) {

        return "d" + depth
            + " " + widget.getClass().getName()
            + "[" + describeBox(box)
            + " opacity=" + widget.getOpacity()
            + " parent=" + (parent == null ? "none" : parent.getClass().getName())
            + "]";
    }

    private static String describeTab(Object currentTab) {
        var tabBox = currentTab instanceof UIComponentAPI tab
            ? DrawnWidgets.resolveBoxOf(tab)
            : null;
        return currentTab.getClass().getName()
            + "[" + (tabBox == null ? "unpositioned" : describeBox(tabBox)) + "]";
    }

    // Every direct child with the box it occupies, drawn or not - a child faded out or never
    // positioned is named too, since a rule that skipped it is only checkable against a list that
    // says it was there to skip.
    private static List<String> describeDirectChildren(List<?> children) {
        return describeUpToCap(children, MapTabWidgetTrace::describeDirectChild);
    }

    private static String describeDirectChild(Object child) {
        if (!(child instanceof UIComponentAPI widget)) {
            // Not a component, so it has neither box nor opacity to report - and cannot be the
            // surface. Named anyway so the list is the tab's real children rather than a filtered
            // view of them.
            return child.getClass().getName() + "[not a component]";
        }
        var box = DrawnWidgets.resolveBoxOf(widget);
        return widget.getClass().getName()
            + "[" + (box == null ? "unpositioned" : describeBox(box))
            + " opacity=" + widget.getOpacity()
            + "]";
    }

    // What the surface rule makes of the map tab's children. Re-derived here rather than read back
    // from the live memo: the point of the line is what the rule says about the tree as it stands,
    // which a remembered answer could no longer be.
    //
    // Reports every chrome box rather than counting them, unlike the surface's own single box. The
    // children list this sits next to no longer accounts for them: a chrome piece drawn over the
    // surface stands for the boxes of what it draws, which are a level below anything that list
    // names, so the boxes the cursor is actually excluded from appear nowhere else in the line.
    private static String describeSurfacePickedFrom(UIComponentAPI mapTab, List<?> children) {
        if (mapTab == null) {
            return "none";
        }
        var surfaceArea = MapSurfaceBounds.selectSurfaceArea(
            DrawnWidgets.resolveBoxOf(mapTab),
            MapSurfaceBounds.collectDrawnBoxesOf(children));

        return surfaceArea == null
            ? "none"
            : "[" + describeBox(surfaceArea.box())
                + " chrome=" + describeUpToCap(
                    surfaceArea.chromeBoxes(), MapTabWidgetTrace::describeBox) + "]";
    }

    // Every list in the line is capped the same way, so one pathological tab cannot push the rest of
    // the description past where a log reader will follow it.
    private static <T> List<String> describeUpToCap(
            List<T> items,
            Function<? super T, String> describeItem) {

        var describedItems = new ArrayList<String>();
        for (var item : items) {
            if (describedItems.size() >= MAX_TRACE_WIDGETS) {
                break;
            }
            describedItems.add(describeItem.apply(item));
        }
        return describedItems;
    }

    // Rounded to whole units: these are read off a log by eye against the game's own pixel grid, and
    // a fractional layout coordinate is noise at that resolution.
    private static String describeBox(Rectangle box) {
        return "x=" + Math.round(box.x())
            + " y=" + Math.round(box.y())
            + " w=" + Math.round(box.width())
            + " h=" + Math.round(box.height());
    }

    // Warns on this library's own logger rather than the caller's, since a reach that broke is the
    // library's news to report. WARN survives the default level, so unlike a DEBUG line it is
    // actually seen.
    private static void warnOnce(Throwable failure) {
        if (hasWarnedThisSession) {
            return;
        }
        hasWarnedThisSession = true;
        LOG.warn("Could not walk the map tab's widget tree by reflection; "
            + "the widget trace will describe nothing this session.", failure);
    }
}
