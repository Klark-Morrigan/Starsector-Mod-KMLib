package kmlib.starsector.ui.widgets.lists;

import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.ReselectBehaviour;
import kmlib.starsector.ui.controls.TriangleDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A picker's sort selector: a vertical radio table with one row per mode of the calling mod's
 * {@link ListSortMode} set, the lit row the mode the list is currently ranked by, and each row's
 * trailing slot showing the direction that mode would sort in. Picking a row reports the resulting
 * sort back to the caller, which persists it however it keeps such state; the caller's next
 * per-frame body build then reorders its list under it, so the selector and the list always agree on
 * the active metric and direction.
 *
 * <p>A sort is always active (the list is always ordered somehow), so this radio never clears to
 * nothing. Instead it re-fires on a re-pick ({@link ReselectBehaviour#REFIRE}): clicking the
 * already-lit mode flips its direction, while clicking a different mode switches to it at that
 * mode's default direction. Each row's direction is drawn as a small filled triangle (up for
 * ascending, down for descending) rather than a letter, since a sidebar body font renders no
 * up/down glyph.
 *
 * <p>The rows carry no icon - the direction table's icon column is all null - so the selector
 * reuses the picker list's three-column table geometry (a would-be icon column, the mode name, the
 * trailing direction triangle) and reads as a left-aligned list of modes with their directions
 * flush right. The rows are drawn in the caller's mode order, so the row index a click reports
 * maps straight back to a mode by position.
 */
public final class SortSelectorControl {

    private SortSelectorControl() {
    }

    /**
     * Builds the sort selector for the active mode and direction: a vertical radio lit on that
     * mode's row, each row labelled with its mode's own text and trailed by a direction triangle -
     * the active mode's current direction on the lit row, each other mode's default direction on
     * its own row, so every row previews the order picking it would give.
     *
     * <p>{@code activeSort} is both what the selector lights and what a re-pick flips from, so it
     * must be the caller's live read of its stored sort rather than a value held across frames: a
     * stale one would light the wrong row and flip away from a direction the store no longer holds.
     *
     * @param <T>          the list item type the modes rank
     * @param activeSort   the sort the list is currently ranked by - the mode this lights and the
     *                     direction that mode is ranking in
     * @param sortModes    the calling mod's sort vocabulary: the modes in the order the rows stack
     *                     top to bottom, and the fallback the rest of the pick resolves against
     * @param onSortPicked told the sort a click lands on, for the caller to persist
     * @return the vertical, re-firing sort-selector radio table
     */
    public static <T> ControlSpec.VerticalTable buildSelector(
            ListSort<T> activeSort,
            ListSortModes<T> sortModes,
            Consumer<ListSort<T>> onSortPicked) {

        var modes = sortModes.modes();
        var labels = new ArrayList<String>(modes.size());
        var directions = new ArrayList<TriangleDirection>(modes.size());

        for (var mode : modes) {
            labels.add(mode.resolveLabelText());

            // The lit mode shows its live direction; every other row previews its own default, so a
            // row reads as "pick me and the list sorts this way".
            var rowDirection = mode.equals(activeSort.mode())
                ? activeSort.direction()
                : mode.defaultDirection();

            directions.add(resolveTriangleDirection(rowDirection));
        }
        return ControlSpec.VerticalTable.directionTable(
            labels,
            directions,
            modes.indexOf(activeSort.mode()),
            cellIndex -> applySelection(activeSort, modes, onSortPicked, cellIndex),
            ReselectBehaviour.REFIRE);
    }

    // The triangle that previews a sort direction: ascending points up, descending down. The
    // selector draws this shape in each row's trailing slot in place of a direction word, since the
    // body font renders no up/down glyph.
    private static TriangleDirection resolveTriangleDirection(SortDirection direction) {
        return direction == SortDirection.ASCENDING
            ? TriangleDirection.UP
            : TriangleDirection.DOWN;
    }

    // Applies a click on a sort row and reports the whole resulting sort, so the caller writes one
    // consistent pair rather than reconstructing which half moved. Re-picking the lit mode flips its
    // direction; picking a different mode switches to it at that mode's default direction. Any index
    // outside the mode rows is ignored, so a stray hit changes nothing.
    private static <T> void applySelection(
            ListSort<T> activeSort,
            List<? extends ListSortMode<T>> modes,
            Consumer<ListSort<T>> onSortPicked,
            int cellIndex) {

        if (cellIndex < 0 || cellIndex >= modes.size()) {
            return;
        }

        ListSortMode<T> clickedMode = modes.get(cellIndex);
        var direction = clickedMode.equals(activeSort.mode())
            ? activeSort.direction().opposite()
            : clickedMode.defaultDirection();

        onSortPicked.accept(new ListSort<>(clickedMode, direction));
    }
}
