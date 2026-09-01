package kmlib.starsector.ui.widgets.lists;

import kmlib.starsector.ui.colour.StarsectorUiColour;
import kmlib.starsector.ui.controls.ControlHoverReport;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.ReselectBehaviour;
import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.widgets.LabelledRow;
import kmlib.starsector.ui.widgets.RowSlot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A spotlight picker's controls, top to bottom: a rule heading the block, the columns selector, a
 * row pairing the sort selector beside whatever the caller pairs with it, then the vertical
 * icon-radio list of selectable items. It turns a consumer's {@link SelectableListItem} options
 * into one clickable list where picking a row spotlights that item and re-picking the lit row
 * clears the spotlight. Which items the list holds is decided before this is called, so it names
 * nothing about what is being picked.
 *
 * <p>The composition is why this is a control rather than a note in a README: the pieces it
 * arranges - a divider, two selectors, a scrolling table - are each reachable on their own, but the
 * three rules that make the arrangement behave are only obvious after getting them wrong. Re-picking
 * the lit row clears rather than re-selects, the lit index is resolved against the *ranked* order
 * rather than the caller's, and an index outside the rows is ignored rather than trusted.
 *
 * <p>The rule parts whatever sits above from the picker below, marking the section a caption
 * otherwise would. The columns selector rides directly under it, so how many columns the list wraps
 * across is chosen for the block as a whole. Below it a paired row sets the sort beside the
 * caller's own trailing controls: the sort selector on the left picks the metric the list ranks by
 * (the picker sorts the items by that metric and labels each row with its value), while the right
 * half is the caller's - a consumer with nothing to pair passes none and the row draws as the sort
 * selector alone.
 *
 * <p>Every row's tone is resolved here rather than carried in by the item, so an item states only
 * whether it reads back ({@link SelectableListItem#isDimmed}) and this decides how far back that is.
 * A receded row draws its words and its crest back together - the words in the engine's gray, the
 * crest darkened by a tint - since receding one and not the other reads as a rendering slip.
 *
 * <p>Nothing here reaches a save. The three live values arrive as parameters, and the three picks -
 * with the row the pointer is on beside them - are reported through {@link ListPickerStore}, which
 * is the division the whole family rests on.
 */
public final class ListPickerControl {

    private ListPickerControl() {
    }

    /**
     * Builds the picker block for one list of selectable items, top to bottom: the section rule,
     * the columns selector, a row pairing the sort selector beside the caller's trailing controls,
     * then the icon-radio list ranked by the sort mode (its lit row the spotlighted item, or none
     * when the selected id is not among these items). Returns an empty list when there are no
     * selectable items, so a caller with nothing to spotlight contributes no picker rather than an
     * empty list widget.
     *
     * @param <T>                 the caller's own item type, ranked by its own comparators
     *                            throughout
     * @param items               the selectable items; order here is immaterial since the sort mode
     *                            reorders them for display
     * @param selectedItemId      the currently spotlighted item's id, or null when nothing is
     *                            spotlighted
     * @param sort                how the list is ranked - the metric, its direction, and the
     *                            vocabulary the sort selector draws its rows from; the metric also
     *                            picks each row's trailing value
     * @param columns             how many columns the item list wraps its rows across, which the
     *                            columns selector lights and the list lays out under
     * @param columnsCaptionText  the caption drawn beside the columns segments, resolved by the
     *                            caller against its own strings
     * @param trailingControls    the controls filling the right half of the sort row; empty leaves
     *                            the sort selector alone on the row
     * @param pickerStore         told each of the three picks, for the caller to persist, and told
     *                            which item the pointer is on as it crosses the rows
     * @return the picker controls, top to bottom; empty when {@code items} is empty
     */
    public static <T extends SelectableListItem> List<ControlSpec> buildPicker(
            List<T> items,
            String selectedItemId,
            ListSort<T> sort,
            ListColumns columns,
            String columnsCaptionText,
            List<ControlSpec> trailingControls,
            ListPickerStore pickerStore) {

        if (items.isEmpty()) {
            return List.of();
        }

        // Rank a copy under the active mode and direction, leaving the caller's (possibly cached)
        // list untouched, so the rows draw in the chosen order and the lit index below is resolved
        // against that same order.
        var rankedItems = new ArrayList<>(items);
        rankedItems.sort(sort.comparator());

        var selectedIndex = resolveSelectedIndex(rankedItems, selectedItemId);
        var controls = new ArrayList<ControlSpec>();

        // A rule heads the block, parting whatever sits above from the picker below - the section
        // break a caption would otherwise mark, now carrying no text.
        controls.add(new ControlSpec.Divider());

        // The columns selector rides directly under the rule, so the column count is chosen for the
        // block as a whole; the list below then wraps its rows across that many columns.
        controls.add(ColumnsSelectorControl.buildSelector(
            columns,
            columnsCaptionText,
            pickerStore::storeColumnsPick));

        // The sort selector and the caller's trailing controls share one row, the sort on the left
        // picking the metric the list ranks by. Pairing them keeps the picker compact; what sits
        // beside the sort is the caller's decision, so this composes the row and the caller fills
        // its right half.
        controls.add(new ControlSpec.SideBySide(
            List.of(SortSelectorControl.buildSelector(
                sort,
                pickerStore::storeSortPick)),
            trailingControls));

        // The item list is the block's one scrolling cluster: when the picker plus the controls
        // around it would run their box past its bottom margin, the list gives up the difference and
        // scrolls while everything around it stays pinned. asScrolling marks the list; the capped
        // layout, renderer, and input listener all read that one flag.
        controls.add(
            ControlSpec.VerticalTable
                .createColumnTable(
                    buildItemRows(rankedItems, sort.mode()),
                    selectedIndex,
                    cellIndex -> pickItem(pickerStore, rankedItems, selectedIndex, cellIndex))
                .handlesReselect(ReselectBehaviour.DESELECT)

                // The row under the pointer is reported beside the click and resolved the same way,
                // so a host can show what picking a row would do before it is picked. A host that
                // previews nothing takes the report and drops it; nothing here draws differently
                // either way.
                .reportsHoverTo(hoveredCell ->
                    reportHoveredItem(pickerStore, rankedItems, hoveredCell))
                .spreadsAcross(columns.columnCount())
                .asScrolling());

        return List.copyOf(controls);
    }

    // One row per item, in ranked order: the item's crest leading it, its name, and the active sort
    // metric's value for that item trailing it. Built as whole rows rather than as a column each, so
    // an item's three parts are written together and cannot fall out of step with one another.
    //
    // An item with no resolved name draws as an unlabelled row rather than a null the width measurer
    // would choke on; an item with no crest leads with nothing; and every row carries the metric's
    // value (a zero metric shows "0" rather than dropping the column), which for a mode with no value
    // to show is an unfilled slot throughout and the rows read as a plain list.
    //
    // A row the caller marked as receding draws all three parts back at once - its value too, even
    // where the mode picked colours of its own - since a greyed name beside a full-strength crest or
    // number reads as a rendering slip rather than as a state. Both tones are resolved here rather
    // than asked of the item or of the mode, so what "receding" looks like is one decision the whole
    // family shares and a consumer states only which of its rows are in that state.
    private static <T extends SelectableListItem> List<LabelledRow> buildItemRows(
            List<T> items,
            ListSortMode<T> sortMode) {

        var textColour = StarsectorUiColour.VANILLA_TEXT.resolve();
        var recededTextColour = StarsectorUiColour.VANILLA_GRAY.resolve();

        // What a receding row's crest is multiplied by, resolved once for the stack beside the tones
        // its words take. A flat neutral, halving every channel while leaving the hue that identifies
        // the badge readable - and deliberately not the tone the words recede to, which is the
        // engine's own gray and carries an alpha under 255 (textGrayColor is [175,175,175,180] in the
        // stock file). A sprite tint's alpha multiplies into the draw, so that one would fade the
        // crest as well as darken it, and a badge with the map showing through reads as half-drawn
        // rather than as receded. The frozen literal is opaque and so darkens only.
        var recededCrestTint = StarsectorUiColour.DIM_GRAY.resolve();
        var itemRows = new ArrayList<LabelledRow>(items.size());

        for (var item : items) {

            var displayName = item.displayName() == null ? "" : item.displayName();
            var crestSpritePath = item.crestSpritePath();

            // Asked once and spent on all three parts: a consumer may work this out rather than hold
            // it, so a second read is both a second computation and a chance for one row to draw its
            // words and its crest under different answers.
            var isReceding = item.isDimmed();
            var rowColour = isReceding ? recededTextColour : textColour;
            var crestTint = isReceding ? recededCrestTint : null;

            itemRows.add(LabelledRow
                .createRow(new TextSpan(displayName, rowColour))
                .leadsWith(crestSpritePath == null
                    ? RowSlot.EMPTY
                    : new RowSlot.Image(crestSpritePath, crestTint))
                .trailsWith(buildTrailingRowSlot(sortMode, item, rowColour, isReceding)));
        }
        return itemRows;
    }

    // The active mode's value for one item as the slot that draws it: nothing at all where the mode
    // shows no value, one run where it shows a plain one, and several runs where the value is picked
    // out in shades of its own. Which of the three is decided here rather than by the mode, so a mode
    // states only what its value reads as and every stack of rows spells "no value" the one way.
    //
    // A mode that answered no runs and one whose runs all came out blank are the same absence, so both
    // take the empty slot rather than one of them arriving as a run with nothing in it - a consumer
    // assembling a value from parts cannot tell which of the two it produced. Which runs read as blank
    // is TextSpan's rule, not a second one here.
    private static <T extends SelectableListItem> RowSlot buildTrailingRowSlot(
            ListSortMode<T> sortMode,
            T item,
            Color rowColour,
            boolean isReceding) {

        var valueRuns = sortMode.resolveTrailingRuns(item, rowColour);
        if (valueRuns.stream().noneMatch(TextSpan::hasContent)) {
            return RowSlot.EMPTY;
        }

        var drawnRuns = isReceding ? recedeRuns(valueRuns, rowColour) : valueRuns;
        return drawnRuns.size() == 1
            ? new RowSlot.Text(drawnRuns.get(0))
            : new RowSlot.TextRuns(drawnRuns);
    }

    // Reports the clicked item as the spotlighted one, or reports a clear when the click landed on
    // the already-lit row. The list is deselectable, so a press on the lit option reaches here with
    // its own index; re-picking it means "stop spotlighting". Any index outside the item list is
    // ignored, so a stray hit changes nothing.
    private static void pickItem(
            ListPickerStore pickerStore,
            List<? extends SelectableListItem> items,
            int selectedIndex,
            int cellIndex) {

        if (!ListOptions.isOptionAt(items, cellIndex)) {
            return;
        }
        if (cellIndex == selectedIndex) {
            pickerStore.clearItemPick();
        } else {
            pickerStore.storeItemPick(items.get(cellIndex).itemId());
        }
    }

    // Every run of a value in the receded tone. Each run's own spacing is carried over, so a value
    // whose runs butt against one another still reads as one word once it has receded.
    private static List<TextSpan> recedeRuns(List<TextSpan> valueRuns, Color recededColour) {
        return valueRuns
            .stream()
            .map(run -> new TextSpan(run.text(), recededColour, run.isJoinedToPreviousRun()))
            .toList();
    }

    // Reports the item the pointer is on, resolved against the ranked list exactly as the click is,
    // so what a hover previews and what a press would spotlight are the same item under any sort.
    //
    // A pointer on no row - it left the list, or the reading landed on a cell no item stands in -
    // reports the leave rather than being dropped, which is where this parts from pickItem. A stray
    // click must leave the spotlight standing, while a hover on nothing is itself the answer that no
    // item is under the pointer: swallowed, it would leave a host previewing the last row the pointer
    // crossed while the pointer is somewhere else entirely.
    private static void reportHoveredItem(
            ListPickerStore pickerStore,
            List<? extends SelectableListItem> items,
            Integer hoveredCell) {

        if (hoveredCell == ControlHoverReport.NO_CELL_HOVERED
                || !ListOptions.isOptionAt(items, hoveredCell)) {

            pickerStore.clearItemHover();
            return;
        }
        pickerStore.reportItemHover(items.get(hoveredCell).itemId());
    }

    // The lit row: the index of the item whose id is selected, or no selection when that id is
    // absent (nothing spotlighted) or names an item no longer in the list (a stale id the caller's
    // own heal has not yet cleared). An unlit list still shows every option, so the player can pick
    // one.
    private static int resolveSelectedIndex(
            List<? extends SelectableListItem> items,
            String selectedItemId) {

        if (selectedItemId == null) {
            return ControlSpec.NO_SELECTION;
        }
        for (var index = 0; index < items.size(); index++) {
            if (selectedItemId.equals(items.get(index).itemId())) {
                return index;
            }
        }
        return ControlSpec.NO_SELECTION;
    }
}
