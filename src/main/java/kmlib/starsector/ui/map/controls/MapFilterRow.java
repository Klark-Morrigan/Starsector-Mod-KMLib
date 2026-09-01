package kmlib.starsector.ui.map.controls;

import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.coreui.CoreUiTree;
import kmlib.starsector.ui.layout.VanillaPositions;

/**
 * One resolved instance of the row of toggles the game furnishes its map screens from - the strip
 * along the bottom of the {@code M} screen carrying Starscape, Fuel range and the rest, and the
 * shorter band the intel screen's map visor carries. A control appended to that row is appended to
 * one of these.
 *
 * <p>Held rather than fetched afresh at each use, because the widget is what an appended control is
 * attached to: two reads a frame apart can hand back different rows, and a control appended against
 * one of them while the other is on screen is appended to nothing the player can see.
 *
 * <p>The widget stays wrapped rather than being handed out beyond this package. Its class is
 * obfuscated, so nothing outside could name it in a signature anyway - but the wrapper is what says
 * that writing into the row is this package's business, and that a caller holding one is holding an
 * attachment point rather than a widget to reach into.
 *
 * <p>What such a caller can ask is which row it has, and it asks by identity: the row is rebuilt
 * inside the map widget's constructor on every open, so a held one goes stale each time the player
 * closes and reopens the screen. Nothing about the reference itself says so, and a control still
 * attached to the previous row draws nowhere while the new row stands bare.
 *
 * <p>Beside that it answers where it is and where the last thing standing on it is, which is what a
 * control appended to it is sized by and fitted into. Those are reads of the row rather than
 * decisions about the control, which is why they are answered here and what to do with them is not:
 * a row is a shape on screen whichever screen it belongs to, and how a button is laid out on one is
 * a separate question with its own reasoning.
 */
public final class MapFilterRow {

    // The live widget. Typed as Object because the class is obfuscated and its name churns between
    // game builds, which is also why nothing here reaches into it by name.
    private final Object rowWidget;

    /**
     * Wraps a widget the resolution beside this one has already recognised as a row, which is why
     * this is not reachable from outside the package: a wrapper built over some other widget would
     * be an attachment point to a row that does not exist.
     *
     * @param rowWidget the live row widget
     */
    MapFilterRow(Object rowWidget) {
        this.rowWidget = rowWidget;
    }

    /**
     * Whether this and another handle name the same live row.
     *
     * <p>Identity rather than equality: two handles describe the same row only by wrapping the same
     * object, and what an obfuscated widget's own {@code equals} compares is the game's business.
     *
     * @param otherRow the row to compare against, or null when the caller has none - which answers
     *                 false, since no row is not the same row as this one
     * @return whether both wrap the one widget
     */
    public boolean isSameRowAs(MapFilterRow otherRow) {

        return otherRow != null && otherRow.rowWidget == rowWidget;
    }

    /**
     * The widget itself, for the writes in this package that have to reach into it.
     *
     * @return the live row widget
     */
    Object getRowWidget() {

        return rowWidget;
    }

    /**
     * Where the layout put the row, which is what a control appended to it is sized and fitted
     * against.
     *
     * <p>Read live at each ask rather than kept. The row is laid out against the screen it is on, so
     * a resized window moves it, and a box read at the moment the screen opened would be measured
     * against a window that is no longer there.
     *
     * @return the row's box in UI units, or null when the row is not a laid-out component at all or
     *         the layout never placed it - either way there is nothing to measure against
     */
    Rectangle readBox() {

        return readBoxOf(rowWidget);
    }

    /**
     * Where the layout put the last button already standing on the row.
     *
     * <p>The last rather than the widest, because the row lays its children left to right off the
     * one before: the last child added is the one furthest right, so its far edge is where the row
     * has been filled to. Its width is also what a button appended after it has to match to look
     * like a sibling rather than a visitor.
     *
     * @return that button's box in UI units, or null when the row holds nothing, or holds something
     *         the layout never placed
     */
    Rectangle readLastButtonBox() {

        var buttons = CoreUiTree.readChildrenOf(rowWidget);

        return buttons.isEmpty()
            ? null
            : readBoxOf(buttons.get(buttons.size() - 1));
    }

    // A widget's laid-out box, or null where there is none to read. The published component
    // interface answers it, so this is the game's own account of where something is rather than
    // anything inferred from the walk that reached it - and a shape not answering it at all is a
    // shape this cannot fit a control against, which is the same answer as one never placed.
    private static Rectangle readBoxOf(Object widget) {

        if (!(widget instanceof UIComponentAPI component)) {
            return null;
        }

        var position = component.getPosition();

        return position == null
            ? null
            : VanillaPositions.toRectangle(position);
    }
}
