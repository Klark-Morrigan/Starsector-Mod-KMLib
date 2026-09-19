package kmlib.starsector.ui.controls;

import kmlib.starsector.ui.controls.specs.ControlAction;
import kmlib.starsector.ui.controls.specs.ControlSpec;
import kmlib.starsector.ui.controls.specs.ReselectBehaviour;
import kmlib.starsector.ui.controls.specs.VerticalTableSpec;
import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.widgets.LabelledRow;
import kmlib.starsector.ui.widgets.RowSlot;
import kmlib.starsector.ui.widgets.TriangleDirection;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Test-only builders for the {@link VerticalTableSpec} shapes the tests exercise, and for the
 * rows behind them. A test states a stack of rows as the columns it is checking - the labels, the crests,
 * the values - and these zip those into the rows a host writes one at a time, so a fixture stays a few
 * short lists while the spec under test still holds whole rows.
 *
 * <p>The rows carry a literal colour rather than the engine's live text tone, since nothing about a
 * layout or a hit test turns on which colour a row draws in and resolving the live palette would put the
 * game's settings in the way of every one of these fixtures.
 */
public final class VerticalTableSpecs {

    /** The tone every fixture row's text carries - a literal, so no fixture needs the live palette. */
    public static final Color ROW_TEXT_COLOUR = Color.WHITE;

    private VerticalTableSpecs() {
    }

    /**
     * Builds a single-column column table with leading crests and no trailing column, deselectable on a
     * re-pick - the picker list's own shape.
     *
     * @param labels        the row labels, top to bottom
     * @param iconPaths     the per-row crest paths, aligned to {@code labels} (a null or absent entry
     *                      leads with nothing)
     * @param selectedIndex the lit row's index, or {@link ControlSpec#NO_SELECTION}
     * @param action        what a click on a row does, keyed by the row index
     * @return the single-column icon-list spec
     */
    public static VerticalTableSpec buildIconList(
            List<String> labels,
            List<String> iconPaths,
            int selectedIndex,
            ControlAction action) {

        return buildIconList(
            labels,
            iconPaths,
            List.of(),
            selectedIndex,
            action);
    }

    /**
     * Builds a single-column column table (crest, name, value) with a right-aligned value per row,
     * deselectable on a re-pick.
     *
     * @param labels         the row labels, top to bottom
     * @param iconPaths      the per-row crest paths, aligned to {@code labels}
     * @param trailingLabels the per-row values, aligned to {@code labels} (a null or absent entry trails
     *                       with nothing)
     * @param selectedIndex  the lit row's index, or {@link ControlSpec#NO_SELECTION}
     * @param action         what a click on a row does, keyed by the row index
     * @return the single-column icon-table spec
     */
    public static VerticalTableSpec buildIconList(
            List<String> labels,
            List<String> iconPaths,
            List<String> trailingLabels,
            int selectedIndex,
            ControlAction action) {

        return buildIconList(
            labels,
            iconPaths,
            trailingLabels,
            selectedIndex,
            action,
            ControlSpec.SINGLE_COLUMN);
    }

    /**
     * Builds a column table (crest, name, value) folded across {@code columnCount} columns, deselectable
     * on a re-pick.
     *
     * @param labels         the row labels, top to bottom
     * @param iconPaths      the per-row crest paths, aligned to {@code labels}
     * @param trailingLabels the per-row values, aligned to {@code labels}
     * @param selectedIndex  the lit row's index, or {@link ControlSpec#NO_SELECTION}
     * @param action         what a click on a row does, keyed by the row index
     * @param columnCount    how many columns to fold the rows across
     * @return the folded icon-table spec
     */
    public static VerticalTableSpec buildIconList(
            List<String> labels,
            List<String> iconPaths,
            List<String> trailingLabels,
            int selectedIndex,
            ControlAction action,
            int columnCount) {

        return VerticalTableSpec
            .createColumnTable(
                buildRows(labels, iconPaths, trailingLabels),
                selectedIndex,
                action)
            .handlesReselect(ReselectBehaviour.DESELECT)
            .spreadsAcross(columnCount);
    }

    /**
     * Builds a single-column column table whose rows trail a direction triangle - the sort selector's
     * shape.
     *
     * @param labels              the row labels, top to bottom
     * @param triangleDirections  the per-row triangle directions, aligned to {@code labels}
     * @param selectedIndex       the lit row's index, or {@link ControlSpec#NO_SELECTION}
     * @param action              what a click on a row does, keyed by the row index
     * @param reselect            what a click on the lit row does
     * @return the direction-table spec
     */
    public static VerticalTableSpec buildDirectionTable(
            List<String> labels,
            List<TriangleDirection> triangleDirections,
            int selectedIndex,
            ControlAction action,
            ReselectBehaviour reselect) {

        var labelledRows = new ArrayList<LabelledRow>(labels.size());
        for (var index = 0; index < labels.size(); index++) {
            labelledRows.add(buildRow(labels.get(index))
                .trailsWith(new RowSlot.Triangle(triangleDirections.get(index))));
        }
        return VerticalTableSpec
            .createColumnTable(labelledRows, selectedIndex, action)
            .handlesReselect(reselect);
    }

    /**
     * Builds a single-column list of label-only rows laid as uniform cells, with a caller-chosen re-pick
     * behaviour.
     *
     * @param labels        the row labels, top to bottom
     * @param selectedIndex the lit row's index, or {@link ControlSpec#NO_SELECTION}
     * @param action        what a click on a row does, keyed by the row index
     * @param reselect      what a click on the lit row does
     * @return the label-only uniform-cell list spec
     */
    public static VerticalTableSpec buildSegmentedList(
            List<String> labels,
            int selectedIndex,
            ControlAction action,
            ReselectBehaviour reselect) {

        return VerticalTableSpec
            .createSegmentedList(buildRows(labels), selectedIndex, action)
            .handlesReselect(reselect);
    }

    /**
     * Zips a fixture's columns into rows: the label at each index, the crest at that index when the icon
     * list reaches it and holds a path, and the value at that index likewise. A shorter list, or a null
     * entry in one, leaves that flank of the row unfilled.
     *
     * @param labels         the row labels, top to bottom
     * @param iconPaths      the per-row crest paths, aligned to {@code labels}
     * @param trailingLabels the per-row values, aligned to {@code labels}
     * @return one row per label
     */
    public static List<LabelledRow> buildRows(
            List<String> labels,
            List<String> iconPaths,
            List<String> trailingLabels) {

        var labelledRows = new ArrayList<LabelledRow>(labels.size());
        for (var index = 0; index < labels.size(); index++) {
            labelledRows.add(buildRow(labels.get(index))
                .leadsWith(resolveLeadingRowSlot(iconPaths, index))
                .trailsWith(resolveTrailingRowSlot(trailingLabels, index)));
        }
        return labelledRows;
    }

    /**
     * Builds label-only rows, one per label - the plainest stack a fixture can state.
     *
     * @param labels the row labels, top to bottom
     * @return one label-only row per label
     */
    public static List<LabelledRow> buildRows(List<String> labels) {
        return buildRows(labels, List.of(), List.of());
    }

    /**
     * Builds one label-only row in the fixture tone, for a test assembling its own flanks.
     *
     * @param label the row's label
     * @return the label-only row
     */
    public static LabelledRow buildRow(String label) {
        return LabelledRow.createRow(new TextSpan(label, ROW_TEXT_COLOUR));
    }

    // The crest a fixture's icon column names at this index, or nothing when the column runs short or
    // holds a null there - the two ways a fixture spells "this row leads with nothing".
    private static RowSlot resolveLeadingRowSlot(List<String> iconPaths, int index) {
        if (index >= iconPaths.size() || iconPaths.get(index) == null) {
            return RowSlot.EMPTY;
        }
        return new RowSlot.Image(iconPaths.get(index));
    }

    // The value a fixture's value column names at this index, or nothing when the column runs short or
    // holds a null there.
    private static RowSlot resolveTrailingRowSlot(List<String> trailingLabels, int index) {
        if (index >= trailingLabels.size() || trailingLabels.get(index) == null) {
            return RowSlot.EMPTY;
        }
        return new RowSlot.Text(new TextSpan(trailingLabels.get(index), ROW_TEXT_COLOUR));
    }
}
