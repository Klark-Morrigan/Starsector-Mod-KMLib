package kmlib.starsector.ui.controls;

import kmlib.starsector.ui.text.LabelRun;
import kmlib.starsector.ui.text.LabelRuns;
import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.widgets.LabelledRow;
import kmlib.text.KmlibStrings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A host's description of one body control: which widget it is and the state that widget draws in.
 * The layout measures its labels to snap it to its text and the renderer paints it in that state,
 * while the host that supplies the spec owns what the control means and does - so any host composes a
 * control strip without the layout or renderer learning the control's meaning.
 *
 * <p>The shape is a sealed hierarchy rather than one flag-laden record: each variant carries only the
 * state its own widget draws, so a component that refines a stacked list ({@code rowGeometry}, {@code
 * columnCount}) simply does not exist on a checkbox or a tabs row. That makes the illegal
 * combinations unrepresentable - there are no runtime guards rejecting, say, an icon list on a
 * divider, because a divider has no such component to set - and lets the layout, renderer, and input
 * listener dispatch by pattern-matching the variant instead of re-branching on a kind tag.
 *
 * <p>State rides along as data because the host, not the renderer, knows how to read it: the host
 * reads its live value when it builds the spec (specs are rebuilt each frame), so the carried state
 * is current. Every clickable control reduces to "which of my cells is lit" - {@link #NO_SELECTION}
 * for none - which {@link Interactive} carries; the two chrome variants ({@link Label}, {@link
 * Divider}) are drawn but never clicked and so carry neither a lit cell nor an action.
 */
public sealed interface ControlSpec {
    /** {@code selectedIndex} value meaning the control is off - no cell is lit. */
    int NO_SELECTION = -1;

    /** The column count of an ordinary single-column list. */
    int SINGLE_COLUMN = 1;

    /** The single cell of a checkbox or toggle: its whole row is one hit target, lit at index 0. */
    int SINGLE_CELL = 0;

    /** Trailing-caption value meaning the control draws no caption after its row. */
    String NO_TRAILING_CAPTION = "";

    /**
     * The control's own label(s) as the lines they read as: one for a checkbox, toggle, or caption, one
     * per option for a radio, table, or tab (in segment order), and none for a divider. A label authored
     * as several runs is charged one line here, so a control snapped to its text is snapped to what the
     * label says rather than to how many colours it says it in.
     *
     * <p>Only the words survive: a label's image runs have no text form and so appear in none of these
     * lines. A control sized from them alone therefore reserves no room for its images, which is why the
     * label-bearing controls are sized through their runs instead - see {@link VerticalTable#labels()}
     * for the one shape that is not.
     *
     * @return the control's labels, in draw order
     */
    List<String> labels();

    /**
     * A clickable, stateful control - a checkbox, toggle, radio, table, or tabs row - as opposed to
     * the {@link Label} and {@link Divider} chrome that is drawn but never clicked. It carries the lit
     * cell the renderer draws and the action the input listener fires; the host attaches both when it
     * builds the spec, so the framework acts on a click without learning what the control means.
     */
    sealed interface Interactive extends ControlSpec {
        /**
         * The index of the control's lit cell - the active radio segment, or {@link #SINGLE_CELL} for
         * a checked checkbox or a lit toggle - or {@link #NO_SELECTION} when nothing is lit.
         *
         * @return the lit cell's index, or {@link #NO_SELECTION}
         */
        int selectedIndex();

        /**
         * What a click on the control does, keyed by which cell was hit; the host supplies it and the
         * input listener invokes it, while the layout and renderer ignore it.
         *
         * @return the click action
         */
        ControlAction action();

        /**
         * Whether the control's cells are separately hit targets rather than the whole row being one.
         *
         * <p>Answered by the control rather than worked out about it from outside, because it is one
         * rule with two readers: the hit-test that resolves a cell, and the narrowing that decides
         * whether pressing one acts. Stated on either side, a variant would be hit as a row of
         * segments and pressed as a whole row, or the other way about.
         *
         * @return whether the cells are separate hit targets
         */
        boolean isSegmented();

        /**
         * What a re-pick of the already-lit cell does.
         *
         * @return the re-pick behaviour, {@link ReselectBehaviour#INERT} for a control holding none
         */
        ReselectBehaviour reselectBehaviour();

        /**
         * Whether the control is lit - any cell selected. A single-cell checkbox or toggle reads on
         * from this; a radio uses it to tell a lit list from a fully-cleared one.
         *
         * @return true when a cell is lit
         */
        default boolean isLit() {
            return selectedIndex() != NO_SELECTION;
        }

        /**
         * Where the control reports which of its cells the pointer is on, for a host that answers a hover
         * as well as a click. The input listener calls it as the hovered cell changes; the layout and the
         * renderer ignore it, exactly as they ignore the action beside it.
         *
         * <p>Defaulted rather than carried by every variant, because a report is only worth wiring where a
         * control's cells stand for things the host holds: a stacked list's rows name items it can act on,
         * while a checkbox's single cell says no more than its own lit state already does. A variant whose
         * host comes to want one carries a component overriding this, and nothing that drives the report
         * changes - the frame reads it here and never branches on a variant.
         *
         * @return where the hovered cell is reported, {@link ControlHoverReport#NONE} for a control that
         *         reports none
         */
        default ControlHoverReport hoverReport() {
            return ControlHoverReport.NONE;
        }
    }

    /**
     * A tick box with a trailing label: a single-cell control lit at {@link #SINGLE_CELL} when on and
     * {@link #NO_SELECTION} when off, its whole row one hit target. The renderer draws the tick box at
     * the row's left and the label beside it.
     *
     * @param labelRuns     the trailing label's runs in reading order - text in the colour it draws in
     *                      before any opacity fade the host applies, or an image squared off the row;
     *                      never empty
     * @param selectedIndex {@link #SINGLE_CELL} when ticked, {@link #NO_SELECTION} when off
     * @param action        what a click on the row does
     */
    record Checkbox(
            List<LabelRun> labelRuns,
            int selectedIndex,
            ControlAction action) implements Interactive {

        /** Holds the label to the floor {@link LabelRuns} holds every label to. */
        public Checkbox {
            labelRuns = LabelRuns.copyRuns(labelRuns);
        }

        /**
         * Builds a checkbox in its current lit state, mapping on/off to the single-cell {@code
         * selectedIndex} in one place so no host re-derives the "cell 0 lit or nothing" convention. Its
         * label is the one run given; a host calling part of it out layers a second on with
         * {@link #continuesWith}.
         *
         * @param labelRun the trailing label's one run - ordinarily a {@link TextSpan}, the text and the
         *                 colour it draws in
         * @param isOn     whether the box is ticked
         * @param action   what a click on the row does
         * @return the checkbox spec
         */
        public static Checkbox lit(LabelRun labelRun, boolean isOn, ControlAction action) {
            return new Checkbox(
                List.of(labelRun),
                isOn ? SINGLE_CELL : NO_SELECTION,
                action);
        }

        /**
         * Returns a copy whose label runs on into {@code labelRun} - the next stretch of the same
         * sentence, a stretch of text picked out in its own colour or an image set among the words,
         * while what came before it stays as it was.
         *
         * @param labelRun the run continuing the label
         * @return an otherwise-identical checkbox whose label carries that run last
         */
        public Checkbox continuesWith(LabelRun labelRun) {
            return new Checkbox(
                LabelRuns.appendRun(labelRuns, labelRun),
                selectedIndex,
                action);
        }

        @Override
        public List<String> labels() {
            return List.of(LabelRuns.resolveLineText(labelRuns));
        }

        @Override
        public boolean isSegmented() {
            return false;
        }

        @Override
        public ReselectBehaviour reselectBehaviour() {
            return ReselectBehaviour.INERT;
        }
    }

    /**
     * A single framed button that washes when on: a single-cell control lit at {@link #SINGLE_CELL}
     * when on and {@link #NO_SELECTION} when off, the button itself the hit target. It reads the same
     * on/off state as a {@link Checkbox} but draws as a lit push-button with its label centred inside,
     * rather than a tick box with an adjacent label.
     *
     * @param labelRuns     the centred label's runs in reading order - text in the colour it draws in
     *                      before any opacity fade the host applies, or an image squared off the row;
     *                      never empty
     * @param selectedIndex {@link #SINGLE_CELL} when on, {@link #NO_SELECTION} when off
     * @param action        what a click on the button does
     */
    record Toggle(
            List<LabelRun> labelRuns,
            int selectedIndex,
            ControlAction action) implements Interactive {

        /** Holds the label to the floor {@link LabelRuns} holds every label to. */
        public Toggle {
            labelRuns = LabelRuns.copyRuns(labelRuns);
        }

        /**
         * Builds a toggle in its current lit state, mapping on/off to the single-cell {@code
         * selectedIndex} as {@link Checkbox#lit} does for a tick box.
         *
         * @param labelRun the centred label's one run - ordinarily a {@link TextSpan}, the text and the
         *                 colour it draws in
         * @param isOn     whether the button is lit
         * @param action   what a click on the button does
         * @return the toggle spec
         */
        public static Toggle lit(LabelRun labelRun, boolean isOn, ControlAction action) {
            return new Toggle(
                List.of(labelRun),
                isOn ? SINGLE_CELL : NO_SELECTION,
                action);
        }

        /**
         * Returns a copy whose label runs on into {@code labelRun} - the next stretch of the same
         * sentence, a stretch of text picked out in its own colour or an image set among the words,
         * while what came before it stays as it was.
         *
         * @param labelRun the run continuing the label
         * @return an otherwise-identical toggle whose label carries that run last
         */
        public Toggle continuesWith(LabelRun labelRun) {
            return new Toggle(
                LabelRuns.appendRun(labelRuns, labelRun),
                selectedIndex,
                action);
        }

        @Override
        public List<String> labels() {
            return List.of(LabelRuns.resolveLineText(labelRuns));
        }

        @Override
        public boolean isSegmented() {
            return false;
        }

        @Override
        public ReselectBehaviour reselectBehaviour() {
            return ReselectBehaviour.INERT;
        }
    }

    /**
     * A text-only caption row, drawn but never clicked - it heads a run of controls with a title. It
     * is not {@link Interactive}: a caption carries no lit cell and no action.
     *
     * <p>Its one component is named for what it holds, as the two controls above name theirs: a caption
     * is a label like any other control's, and calling it {@code text} here and {@code label} there
     * would make one concept read as two.
     *
     * @param labelRuns the caption's runs in reading order - text in the colour it draws in before any
     *                  opacity fade the host applies, or an image squared off the row; never empty
     */
    record Label(
        List<LabelRun> labelRuns) implements ControlSpec {

        /** Holds the label to the floor {@link LabelRuns} holds every label to. */
        public Label {
            labelRuns = LabelRuns.copyRuns(labelRuns);
        }

        /**
         * Builds a caption of one run - what a caption that reads in a single colour is.
         *
         * @param labelRun the caption's one run - ordinarily a {@link TextSpan}, the text and the colour
         *                 it draws in
         * @return the caption spec
         */
        public static Label createLabel(LabelRun labelRun) {
            return new Label(List.of(labelRun));
        }

        /**
         * Returns a copy whose caption runs on into {@code labelRun} - the next stretch of the same
         * sentence, a stretch of text picked out in its own colour or an image set among the words,
         * while what came before it stays as it was.
         *
         * @param labelRun the run continuing the caption
         * @return an otherwise-identical caption carrying that run last
         */
        public Label continuesWith(LabelRun labelRun) {
            return new Label(LabelRuns.appendRun(labelRuns, labelRun));
        }

        @Override
        public List<String> labels() {
            return List.of(LabelRuns.resolveLineText(labelRuns));
        }
    }

    /**
     * A horizontal rule spanning the strip's inner width, drawn but never clicked - it parts one run
     * of controls from the next. It is not {@link Interactive} and carries no label; the layout spans
     * it to the content width and the renderer draws the rule.
     */
    record Divider() implements ControlSpec {

        @Override
        public List<String> labels() {
            return List.of();
        }
    }

    /**
     * A set of mutually exclusive option cells, exactly one lit - laid across a row or stacked into a
     * column. A reader that acts on any radio names this rather than the two variants: the hit-test that
     * splits one into cells and the activation that reads its re-pick rule both do, so neither branches on
     * how the cells are arranged.
     *
     * <p>Two variants rather than one record carrying a direction, on the same rule the hierarchy above
     * follows: the state each draws differs. Cells laid across a row size to their labels and may carry a
     * caption past the last one; stacked cells are one column wide, so neither component exists to be set
     * wrongly on them.
     */
    sealed interface Radio extends Interactive permits HorizontalRadio, VerticalRadio {
        /**
         * What a re-pick of the lit cell does - inert for a set that always holds one once picked,
         * deselect for a clearable one.
         *
         * @return the re-pick behaviour
         */
        ReselectBehaviour reselect();

        /**
         * A radio's cells are separately hit, whichever way the set is arranged.
         *
         * @return {@code true}
         */
        @Override
        default boolean isSegmented() {
            return true;
        }

        /**
         * Read off this interface rather than off each alignment, so a stacked set answers a re-pick
         * exactly as a laid-across one does - the behaviour is the control's, not its arrangement's.
         *
         * @return the re-pick behaviour the set carries
         */
        @Override
        default ReselectBehaviour reselectBehaviour() {
            return reselect();
        }
    }

    /**
     * A row of mutually exclusive option segments laid side by side. Its {@link SegmentSizing} picks how
     * the segments size: {@link SegmentSizing#UNIFORM} gives every segment the widest label's width (even
     * cells, the default an option pair reads as), {@link SegmentSizing#SNAPPED} gives each its own
     * label's width (a ragged row that would waste space as even cells). Its {@link ReselectBehaviour}
     * sets what a re-pick of the lit segment does: an option pair is {@link ReselectBehaviour#INERT}
     * (always one lit, the standard row), while a {@link ReselectBehaviour#DESELECT} row clears to
     * nothing on a re-pick - so a horizontal radio can read as the on/off selector a {@link
     * VerticalTable} did, only laid across one row.
     *
     * @param labels        the option labels, left to right, in segment order
     * @param selectedIndex the lit option's index, or {@link #NO_SELECTION} when nothing is picked
     * @param action        what a click on an option does, keyed by the option index
     * @param trailingLabel a caption drawn after the row, or {@link #NO_TRAILING_CAPTION} for none
     * @param segmentSizing how the segments size (uniform cells, or each snapped to its own label)
     * @param reselect      what a re-pick of the lit segment does (inert for an option pair, deselect for
     *                      a clearable selector)
     */
    record HorizontalRadio(
            List<String> labels,
            int selectedIndex,
            ControlAction action,
            String trailingLabel,
            SegmentSizing segmentSizing,
            ReselectBehaviour reselect) implements Radio {

        /** Copies the label list defensively, so a later edit to a caller's list cannot mutate the spec. */
        public HorizontalRadio {
            labels = List.copyOf(labels);
        }

        /**
         * Builds the plain option row a host reaches for by default: even cells, no trailing caption, and
         * a re-pick of the lit segment inert, so the row always holds one option once one is picked.
         *
         * <p>The caption, the segment sizing, and the re-pick behaviour are three independent refinements
         * a host layers on with {@link #showsCaption}, {@link #sizesSegments}, and {@link
         * #handlesReselect} - so any combination of the three is reachable, rather than only the
         * combinations a fixed set of factories happened to name.
         *
         * @param labels        the option labels, left to right, in segment order
         * @param selectedIndex the lit option's index, or {@link #NO_SELECTION} when nothing is picked
         * @param action        what a click on an option does, keyed by the option index
         * @return the plain horizontal radio spec
         */
        public static HorizontalRadio of(
                List<String> labels,
                int selectedIndex,
                ControlAction action) {

            return new HorizontalRadio(
                labels,
                selectedIndex,
                action,
                NO_TRAILING_CAPTION,
                SegmentSizing.UNIFORM,
                ReselectBehaviour.INERT);
        }

        /**
         * Whether the row draws a trailing caption - any text past its segments. One rule read by both
         * the layout that reserves the caption's footprint and the renderer that draws it, so the two
         * cannot disagree on which rows carry one. Blank-but-present text reads as no caption, so a host
         * that assembles a caption from parts and comes up empty gets the uncaptioned row it should.
         *
         * @return true when the row carries a trailing caption
         */
        public boolean hasTrailingCaption() {
            return KmlibStrings.hasText(trailingLabel);
        }

        /**
         * Returns a copy of this radio captioned with {@code trailingLabel}, drawn after the row - for a
         * row whose segment labels alone do not say what the options choose between.
         *
         * @param trailingLabel the caption drawn after the row
         * @return an otherwise-identical radio carrying that caption
         */
        public HorizontalRadio showsCaption(String trailingLabel) {
            return rebuildAsLaidOut(trailingLabel, segmentSizing, reselect);
        }

        /**
         * Returns a copy of this radio sizing its segments the given way - {@link SegmentSizing#SNAPPED}
         * for a ragged row whose labels differ enough in width that even cells would waste space.
         *
         * @param segmentSizing how the segments size (uniform cells, or each snapped to its own label)
         * @return an otherwise-identical radio sized that way
         */
        public HorizontalRadio sizesSegments(SegmentSizing segmentSizing) {
            return rebuildAsLaidOut(trailingLabel, segmentSizing, reselect);
        }

        /**
         * Returns a copy of this radio handling a re-pick of its lit segment the given way - {@link
         * ReselectBehaviour#DESELECT} for a clearable selector, where a re-click of the active option
         * turns the row off rather than leaving it lit.
         *
         * @param reselect what a re-pick of the lit segment does
         * @return an otherwise-identical radio handling a re-pick that way
         */
        public HorizontalRadio handlesReselect(ReselectBehaviour reselect) {
            return rebuildAsLaidOut(trailingLabel, segmentSizing, reselect);
        }

        // Rebuilds the row around how it is laid out and driven, carrying what it holds - its labels,
        // the lit segment, and the click action - over untouched. The three refinements share it rather
        // than each restating all six components, one of which would eventually be restated wrongly.
        private HorizontalRadio rebuildAsLaidOut(
                String trailingLabel,
                SegmentSizing segmentSizing,
                ReselectBehaviour reselect) {

            return new HorizontalRadio(
                labels,
                selectedIndex,
                action,
                trailingLabel,
                segmentSizing,
                reselect);
        }
    }

    /**
     * A column of mutually exclusive option cells stacked top to bottom, exactly one lit. The shape a set
     * of more than two or three options reads as: the same options laid across one row letter too narrow
     * to tell apart once the row is snapped to a body's width.
     *
     * <p>Distinct from {@link VerticalTable}, which stacks rows holding their own parts - a crest, a
     * label, a trailing value. This is the plain stack: one label per cell and nothing else, which is
     * what an option list that happens to be long wants.
     *
     * @param labels        the option labels, top to bottom, in cell order
     * @param selectedIndex the lit option's index, or {@link #NO_SELECTION} when nothing is picked
     * @param action        what a click on an option does, keyed by the option index
     * @param reselect      what a re-pick of the lit cell does (inert for a set that always holds one,
     *                      deselect for a clearable one)
     */
    record VerticalRadio(
            List<String> labels,
            int selectedIndex,
            ControlAction action,
            ReselectBehaviour reselect) implements Radio {

        /** Copies the label list defensively, so a later edit to a caller's list cannot mutate the spec. */
        public VerticalRadio {
            labels = List.copyOf(labels);
        }

        /**
         * Builds the plain stacked option column a host reaches for by default: a re-pick of the lit cell
         * inert, so the column holds one option once one is picked.
         *
         * @param labels        the option labels, top to bottom, in cell order
         * @param selectedIndex the lit option's index, or {@link #NO_SELECTION} when nothing is picked
         * @param action        what a click on an option does, keyed by the option index
         * @return the plain stacked radio spec
         */
        public static VerticalRadio of(
                List<String> labels,
                int selectedIndex,
                ControlAction action) {

            return new VerticalRadio(
                labels,
                selectedIndex,
                action,
                ReselectBehaviour.INERT);
        }

        /**
         * Returns a copy of this column handling a re-pick of its lit cell the given way - {@link
         * ReselectBehaviour#DESELECT} for a clearable selector, where a re-click of the active option
         * clears the column rather than leaving it lit.
         *
         * @param reselect what a re-pick of the lit cell does
         * @return an otherwise-identical column handling a re-pick that way
         */
        public VerticalRadio handlesReselect(ReselectBehaviour reselect) {
            return new VerticalRadio(
                labels,
                selectedIndex,
                action,
                reselect);
        }
    }

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
     * the {@link ScrollingSection} a host puts a run inside, so a long list and the heading above it
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
    record VerticalTable(
            List<LabelledRow> labelledRows,
            RowGeometry rowGeometry,
            int selectedIndex,
            ControlAction action,
            ControlHoverReport hoverReport,
            ReselectBehaviour reselect,
            int columnCount) implements Interactive {

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
        public VerticalTable {
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
        public static VerticalTable createColumnTable(
                List<LabelledRow> labelledRows,
                int selectedIndex,
                ControlAction action) {

            return new VerticalTable(
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
        public static VerticalTable createSegmentedList(
                List<LabelledRow> labelledRows,
                int selectedIndex,
                ControlAction action) {

            return new VerticalTable(
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
        public VerticalTable handlesReselect(ReselectBehaviour reselect) {
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
        public VerticalTable reportsHoverTo(ControlHoverReport hoverReport) {
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
        public VerticalTable spreadsAcross(int columnCount) {
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
        private VerticalTable rebuildAsLaidOut(
                ControlHoverReport hoverReport,
                ReselectBehaviour reselect,
                int columnCount) {

            return new VerticalTable(
                labelledRows,
                rowGeometry,
                selectedIndex,
                action,
                hoverReport,
                reselect,
                columnCount);
        }
    }

    /**
     * A row of vanilla-styled tabs, exactly one lit - the panel's navigation, a control like any other
     * rather than special panel chrome. Each tab snaps to its own label-plus-shortcut width and is its
     * own hit target; a click fires the action with the tab's index. The {@code shortcuts} run parallel
     * to {@code labels}, so a shorter or empty list (or a null entry) leaves the trailing tabs hint-less.
     *
     * @param labels        the tab labels, left to right (an unlabelled tab passes an empty string)
     * @param shortcuts     the per-tab shortcut hints, aligned to {@code labels} (a null entry is a tab
     *                      with no hint); empty for a strip drawn without hints
     * @param selectedIndex the lit tab's index, or {@link #NO_SELECTION} when none is lit
     * @param action        what a click on a tab does, keyed by the tab index
     */
    record Tabs(
            List<String> labels,
            List<String> shortcuts,
            int selectedIndex,
            ControlAction action) implements Interactive {

        /** Copies the label and shortcut lists defensively; a null shortcut entry is a real "no hint". */
        public Tabs {
            labels = List.copyOf(labels);
            shortcuts = Collections.unmodifiableList(new ArrayList<>(shortcuts));
        }

        /**
         * The shortcut hint drawn at tab {@code index}, or "" when the tab has none - a shorter or empty
         * list, or a null entry. Returning "" rather than null lets the layout and renderer treat "no
         * hint" as a zero-width text without a null check at each site.
         *
         * @param index the tab index
         * @return the tab's shortcut hint, or "" when it has none
         */
        public String shortcutAt(int index) {
            if (index >= shortcuts.size() || shortcuts.get(index) == null) {
                return "";
            }
            return shortcuts.get(index);
        }

        @Override
        public boolean isSegmented() {
            return true;
        }

        @Override
        public ReselectBehaviour reselectBehaviour() {
            return ReselectBehaviour.INERT;
        }
    }

    /**
     * Two columns of controls laid side by side in one strip row - a left column and a right column,
     * each its own top-to-bottom run of controls. It exists because the strip otherwise stacks one
     * full-width row per control; this pairs two runs across one row so a selector can sit beside a
     * related block rather than above it. The layout sizes the group to its columns (as wide as the
     * left column, a gap, then the right, and as tall as the taller column) and places each column in
     * its own half, then flattens the group to its children's laid-out controls - so it is not drawn or
     * clicked as a unit and the renderer and input listener only ever see the ordinary controls inside
     * it, never this container.
     *
     * <p>It is not {@link Interactive} and carries no labels of its own - each child carries its own -
     * so it is measured through its two columns rather than a label. Its children are ordinary controls,
     * so a column holds no further group; nesting is neither needed nor laid out.
     *
     * @param leftColumn  the controls filling the left column, top to bottom
     * @param rightColumn the controls filling the right column, top to bottom
     */
    record SideBySide(
            List<ControlSpec> leftColumn,
            List<ControlSpec> rightColumn) implements ControlSpec {

        /** Copies both columns defensively, so a later edit to a caller's list cannot mutate the spec. */
        public SideBySide {
            leftColumn = List.copyOf(leftColumn);
            rightColumn = List.copyOf(rightColumn);
        }

        @Override
        public List<String> labels() {
            return List.of();
        }
    }

    /**
     * The one run of a strip that scrolls: its controls keep their natural height and slide within
     * whatever room is left once everything outside the section is pinned, so a long list stays reachable
     * inside a bounded box while the controls around it never move. A strip with no section is never
     * capped - it stands at its natural height.
     *
     * <p>A group rather than a flag on whichever control happens to be long, because what a body wants to
     * scroll is a run: a heading, the list under it and the row beside that travel together, and a flag
     * on one member could only ever scroll that member. At most one section per strip - two would each
     * need the leftover height the other is claiming, so the capped layout takes the first and a body
     * stating two has asked for a layout nothing can satisfy.
     *
     * <p>It draws no chrome of its own and is flattened to its children's laid-out controls exactly as
     * {@link SideBySide} is, so the renderer and the input listener only ever see ordinary controls -
     * each marked as scrolled, which is what the clip and the viewport-limited hit-test read. Moving a
     * run into a section therefore changes where it may go and nothing about how it reads.
     *
     * @param controls the controls inside the scrolling run, top to bottom
     */
    record ScrollingSection(List<ControlSpec> controls) implements ControlSpec {

        /** Copies the run defensively, so a later edit to a caller's list cannot mutate the spec. */
        public ScrollingSection {
            controls = List.copyOf(controls);
        }

        @Override
        public List<String> labels() {
            return List.of();
        }
    }
}
