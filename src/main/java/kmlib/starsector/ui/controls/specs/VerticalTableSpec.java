package kmlib.starsector.ui.controls.specs;

import kmlib.starsector.ui.widgets.LabelledRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A vertical stack of option rows, exactly one lit, each row a {@link LabelledRow} - what it leads
 * with, what its label says, and what it trails with. It is the general stacked selector: a
 * label-only list, an icon picker whose rows show a crest and a ranking value, and a direction list
 * whose rows trail a triangle are all this variant with different slots filled.
 *
 * <p>One list of rows rather than a list per column: a row is a thing that holds its own parts, so a
 * host that fills three of them writes three rows and not three lists it must keep the same length
 * and the same order. Which kinds of thing a flank can hold is {@link RowSlot}'s sealed set, so a
 * new kind of leading or trailing element is a new member of that set rather than another list here.
 *
 * <p>{@code rowGeometry} states whether the rows lay out as a table of columns or as uniform cells;
 * {@code reselect} refines what a click on the lit option does; {@code columnCount} spreads the rows
 * across columns (filling each top to bottom before the next). What scrolls is stated a level up, by
 * the {@link ScrollingSectionSpec} a host puts a run inside, so a long list and the heading above it
 * travel together rather than the list alone being scrollable.
 *
 * @param labelledRows  the option rows, top to bottom, in segment order
 * @param rowGeometry   how each row lays its content out - a table of columns, or uniform cells
 * @param selectedIndex the lit option's index, or {@link #NO_SELECTION} when nothing is picked
 * @param action        what a click on an option does, keyed by the option index
 * @param hoverReport   where the row under the pointer is reported, {@link ControlHoverReport#NONE}
 *                      for a list whose host answers clicks alone
 * @param reselect      what a click on the lit option does (deselect, re-fire, or inert)
 * @param columnCount   how many columns to spread the options across ({@link #SINGLE_COLUMN} for
 *                      one column)
 */
public record VerticalTableSpec(
    List<LabelledRow> labelledRows,
    RowGeometry rowGeometry,
    int selectedIndex,
    ControlAction action,
    ControlHoverReport hoverReport,
    ReselectBehaviour reselect,
    int columnCount) implements InteractiveSpec {

    // What a table holds before a host refines it: every option inert on a re-pick (the standard
    // always-one-lit list), stacked in a single column, and reporting nothing back as the pointer
    // crosses its rows. Each is a refinement below, so a host states only the ones its list actually
    // wants.
    private static final ReselectBehaviour DEFAULT_RESELECT = ReselectBehaviour.INERT;

    /**
     * Copies the rows defensively and rejects a missing geometry, a missing hover report, or a column
     * count the layout cannot lay out, so a mis-built table fails at construction rather than at paint
     * time. What each row itself must hold is {@link LabelledRow}'s own rule, checked where that row is
     * built.
     */
    public VerticalTableSpec {

        labelledRows = List.copyOf(labelledRows);
        Objects.requireNonNull(rowGeometry, "rowGeometry");

        // A null here would pass every layout and every draw and then throw from inside the frame that
        // first put the pointer on a row, which is nowhere near the host that built the list.
        Objects.requireNonNull(hoverReport, "hoverReport");

        if (columnCount < SINGLE_COLUMN) {

            throw new IllegalArgumentException("columnCount must be at least "
                + SINGLE_COLUMN
                + ", was "
                + columnCount);
        }
    }

    /**
     * Builds a table whose rows lay out as columns: what each row leads with at its left, the label
     * past it, and what it trails with flush right, all lining up down the stack. The picker's
     * crest-name-value list and the sort selector's name-and-triangle list are both this shape, since
     * the shape is a fact about the layout rather than about which slots the rows happen to fill.
     *
     * @param labelledRows  the option rows, top to bottom
     * @param selectedIndex the lit option's index, or {@link #NO_SELECTION}
     * @param action        what a click on an option does, keyed by the option index
     * @return the column table, in a single inert column
     */
    public static VerticalTableSpec createColumnTable(
            List<LabelledRow> labelledRows,
            int selectedIndex,
            ControlAction action) {

        return new VerticalTableSpec(
            labelledRows,
            RowGeometry.COLUMNS,
            selectedIndex,
            action,
            ControlHoverReport.NONE,
            DEFAULT_RESELECT,
            SINGLE_COLUMN);
    }

    /**
     * Builds a table whose rows lay out as uniform cells, each label centred in a cell as wide as the
     * widest of them - a horizontal radio's option row, stacked. For a set of choices that reads as
     * buttons rather than as a table of entries; a row's flanking slots have no column to sit in
     * here, so such a list carries labels alone.
     *
     * @param labelledRows  the option rows, top to bottom
     * @param selectedIndex the lit option's index, or {@link #NO_SELECTION}
     * @param action        what a click on an option does, keyed by the option index
     * @return the uniform-cell list, in a single inert column
     */
    public static VerticalTableSpec createSegmentedList(
            List<LabelledRow> labelledRows,
            int selectedIndex,
            ControlAction action) {

        return new VerticalTableSpec(
            labelledRows,
            RowGeometry.UNIFORM_SEGMENTS,
            selectedIndex,
            action,
            ControlHoverReport.NONE,
            DEFAULT_RESELECT,
            SINGLE_COLUMN);
    }

    // Each row's label as the one line it reads as, so a strip that snaps a control to its text
    // charges a row once for the whole label rather than once per run it was authored in.
    //
    // A uniform-cell list is the one shape sized from these lines rather than from its rows' runs,
    // since its cells size through the shared horizontal-segment rule, which takes labels as plain
    // text. Such a list therefore reserves no width for a row's image runs - the same reason it
    // reserves none for a row's flanking slots, which have no column to sit in there either.
    @Override
    public List<String> labels() {

        var rowLabels = new ArrayList<String>(labelledRows.size());

        for (var labelledRow : labelledRows) {
            rowLabels.add(labelledRow.resolveLabelText());
        }
        return List.copyOf(rowLabels);
    }

    /**
     * Returns a copy of this table handling a re-pick of its lit option the given way - {@link
     * ReselectBehaviour#DESELECT} for a spotlight list that clears when its lit row is picked again,
     * {@link ReselectBehaviour#REFIRE} for a selector that acts on every pick.
     *
     * @param reselect what a click on the lit option does
     * @return an otherwise-identical table handling a re-pick that way
     */
    public VerticalTableSpec handlesReselect(ReselectBehaviour reselect) {
        return rebuildAsLaidOut(hoverReport, reselect, columnCount);
    }

    /**
     * Returns a copy of this table reporting the row under the pointer to {@code hoverReport} - for a
     * host that answers a hover as well as a click, previewing what picking that row would do.
     *
     * <p>A refinement rather than a factory parameter for the reason the three below it are: a list is
     * built the same way whether or not anything is listening, and a host that takes no report writes
     * nothing about one.
     *
     * @param hoverReport where the row under the pointer is reported
     * @return an otherwise-identical table reporting its hovered row there
     */
    public VerticalTableSpec reportsHoverTo(ControlHoverReport hoverReport) {
        return rebuildAsLaidOut(hoverReport, reselect, columnCount);
    }

    /**
     * Returns a copy of this table folded across {@code columnCount} columns, its rows filling each
     * column top to bottom before the next - so a long list reads as a grid rather than running off
     * the bottom of its strip.
     *
     * @param columnCount how many columns to spread the options across
     * @return an otherwise-identical table folded across that many columns
     */
    public VerticalTableSpec spreadsAcross(int columnCount) {
        return rebuildAsLaidOut(hoverReport, reselect, columnCount);
    }

    @Override
    public boolean isSegmented() {
        return true;
    }

    @Override
    public ReselectBehaviour reselectBehaviour() {
        return reselect();
    }

    // Rebuilds the table around how it is laid out and driven, carrying what it holds - its rows,
    // their geometry, the lit row, and the click action - over untouched. The three refinements
    // share it rather than each restating all seven components, one of which would eventually be
    // restated wrongly: the lit row and the column count are both counts, so a rebuild that crossed
    // them would compile clean and light the wrong row.
    private VerticalTableSpec rebuildAsLaidOut(
            ControlHoverReport hoverReport,
            ReselectBehaviour reselect,
            int columnCount) {

        return new VerticalTableSpec(
            labelledRows,
            rowGeometry,
            selectedIndex,
            action,
            hoverReport,
            reselect,
            columnCount);
    }
}
