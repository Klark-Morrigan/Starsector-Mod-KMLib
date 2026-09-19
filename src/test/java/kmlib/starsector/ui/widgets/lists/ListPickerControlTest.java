package kmlib.starsector.ui.widgets.lists;

import com.fs.starfarer.api.util.Misc;

import kmlib.starsector.ui.controls.specs.ControlHoverReport;
import kmlib.starsector.ui.controls.specs.ControlSpec;
import kmlib.starsector.ui.controls.specs.DividerSpec;
import kmlib.starsector.ui.controls.specs.HorizontalRadioSpec;
import kmlib.starsector.ui.controls.specs.LabelSpec;
import kmlib.starsector.ui.controls.specs.ReselectBehaviour;
import kmlib.starsector.ui.controls.specs.ScrollingSectionSpec;
import kmlib.starsector.ui.controls.specs.SideBySideSpec;
import kmlib.starsector.ui.controls.specs.VerticalTableSpec;
import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.widgets.LabelledRow;
import kmlib.starsector.ui.widgets.RowSlot;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;
import kmlib.testfixtures.starsector.ui.widgets.lists.Anomaly;
import kmlib.testfixtures.starsector.ui.widgets.lists.AnomalySortMode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static kmlib.testfixtures.starsector.ui.widgets.lists.ListPickerBlockReads.readColumnsSelector;
import static kmlib.testfixtures.starsector.ui.widgets.lists.ListPickerBlockReads.readItemList;
import static kmlib.testfixtures.starsector.ui.widgets.lists.ListPickerBlockReads.readSortRow;
import static kmlib.testfixtures.starsector.ui.widgets.lists.ListPickerBlockReads.readSortSelector;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the picker's controls, top to bottom: a section rule, the columns selector, a row pairing the
 * sort selector beside the caller's own trailing controls, and the vertical icon-radio list of
 * selectable items ranked by the active sort mode. Also pins the click wiring: an unlit option
 * reports its item, the lit option reports a clear, and both selectors report through the same
 * store. Beside the click, the hover wiring: the row under the pointer reports its item to that
 * same store, and a pointer on no row reports the leave.
 *
 * <p>Run throughout over the {@link Anomaly} item and {@link AnomalySortMode} vocabulary declared
 * outside this package's production surface - the proof that the picker draws, ranks, and reports a
 * caller's own type without opening it. Every pick is collected through a recording store, so this
 * pins the picker's shape and wiring with no save in reach.
 */
final class ListPickerControlTest {

    // The sort vocabulary the picker ranks and lays its selector rows out from.
    private static final List<AnomalySortMode> MODES = List.of(AnomalySortMode.values());
    private static final ListSortModes<Anomaly> SORT_MODES =
        new ListSortModes<>(MODES, AnomalySortMode.ALPHA);

    // The two items the picker lists in every test: a crested one that is harsher and wider and a
    // crestless one, so the null-crest path is exercised. Their IDs differ from their labels, so a
    // row lit by ID cannot be passing by matching a label.
    private static final Anomaly STORM = new Anomaly("storm_1", "Storm", "crest_storm", 9, 8);
    private static final Anomaly DRIFT = new Anomaly("drift_1", "Drift", null, 2, 3);

    private static final List<Anomaly> ANOMALIES = List.of(STORM, DRIFT);

    // The caption the caller resolved and handed over for the columns segments.
    private static final String CAPTION = "Columns";

    // The trailing controls the caller pairs with the sort selector; a plain label stands in for
    // whatever a consumer actually pairs there, since the picker only places what it is handed. Its
    // tone is arbitrary - nothing under test reads what colour a placed control draws in.
    private static final ControlSpec TRAILING_MARKER =
        LabelSpec.createLabel(new TextSpan("trailing", Color.WHITE));

    private static final List<ControlSpec> TRAILING = List.of(TRAILING_MARKER);

    // The block is a fixed four rows: the section rule, the columns selector, the paired sort row,
    // then the list.
    private static final int DIVIDER = 0;
    private static final int COLUMNS_SELECTOR = 1;

    // The engine tones the picker's rows carry, stood in for so the rows can be built without the live
    // palette in reach. Two distinguishable stand-ins, since what the tone cases assert is which of the
    // two a row took.
    private static final Color TEXT = Color.LIGHT_GRAY;
    private static final Color GRAY = Color.DARK_GRAY;

    // What a receded row's crest is multiplied by, spelled as the literal shade rather than read back
    // off the palette the picker resolves it from - an expectation computed the way the code computes
    // it would hold however the shade drifted. Opaque on purpose: a tint's alpha multiplies into the
    // draw, so a translucent one would fade the crest instead of darkening it.
    private static final Color CREST_TINT = new Color(130, 130, 130);

    // Collects whatever the picker reports, so a test reads what the caller would have been asked to
    // persist.
    private final ListPickerStoreFake pickerStoreFake = new ListPickerStoreFake();

    private MockedStatic<Misc> miscMock;

    @BeforeEach
    void installColours() {
        // Settings first, then the Misc statics: Misc's class initialiser reads the settings, so
        // mocking it against an uninstalled settings proxy would fail on class load.
        StarsectorSettingsFake.installSettings();

        miscMock = Mockito.mockStatic(Misc.class);
        miscMock
            .when(Misc::getTextColor)
            .thenReturn(TEXT);
        miscMock
            .when(Misc::getGrayColor)
            .thenReturn(GRAY);
    }

    @AfterEach
    void clearColours() {
        miscMock.close();
        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class BuildPicker {

        @Test
        void buildPickerReturnsNothingWhenNoItemsAreSelectable() {
            // A caller with nothing to spotlight contributes no picker at all, so its body carries
            // no empty list widget.
            assertThat(build(List.of(), null, AnomalySortMode.ALPHA))
                .isEmpty();
        }

        @Test
        void buildPickerHeadsWithADivider() {
            // The section rule heads the block with no text, parting whatever sits above from the
            // picker below.
            var divider = build(ANOMALIES, null, AnomalySortMode.ALPHA)
                .get(DIVIDER);

            assertThat(divider)
                .isInstanceOf(DividerSpec.class);
        }

        @Test
        void buildPickerPlacesTheColumnsSelectorUnderTheDivider() {
            // The columns selector rides directly under the rule, so the column count is chosen for
            // the block as a whole: a two-segment horizontal radio under the caller's caption.
            var columnsSelector = build(ANOMALIES, null, AnomalySortMode.ALPHA)
                .get(COLUMNS_SELECTOR);

            assertThat(columnsSelector)
                .isInstanceOf(HorizontalRadioSpec.class);
            assertThat(columnsSelector.labels())
                .hasSize(2);
            assertThat(((HorizontalRadioSpec) columnsSelector).trailingLabel())
                .isEqualTo(CAPTION);
        }

        @Test
        void buildPickerPairsTheSortSelectorBesideTheCallersTrailingControls() {
            // The sort selector holds the row's left half (a vertical, re-firing radio lit on the
            // active mode's row) and whatever the caller handed over fills the right, so the metric
            // reads side by side with the caller's own knobs above the list.
            var pair = readSortRow(build(ANOMALIES, null, AnomalySortMode.SEVERITY));
            var sortSelector = (VerticalTableSpec) pair
                .leftColumn()
                .get(0);

            assertThat(pair.leftColumn())
                .hasSize(1);

            // A sort is always active, so a re-pick re-fires to flip; the lit row is the active
            // mode.
            assertThat(sortSelector.reselect())
                .isEqualTo(ReselectBehaviour.REFIRE);
            assertThat(sortSelector.selectedIndex())
                .isEqualTo(MODES.indexOf(AnomalySortMode.SEVERITY));

            // The right half is the caller's, placed verbatim.
            assertThat(pair.rightColumn())
                .containsExactly(TRAILING_MARKER);
        }

        @Test
        void buildPickerDrawsTheSortSelectorAloneWhenNothingIsPairedWithIt() {
            // A caller with nothing to pair passes no trailing controls, which leaves the sort
            // selector alone on its row rather than forcing a stand-in widget into the right half.
            var pair = readSortRow(buildPicker(
                ANOMALIES,
                null,
                sortOf(AnomalySortMode.ALPHA),
                ListColumns.ONE,
                List.of()));

            assertThat(pair.leftColumn())
                .hasSize(1);
            assertThat(pair.rightColumn())
                .isEmpty();
        }

        @Test
        void buildPickerIsAFixedFourRowBlock() {
            // The rule, the columns selector, the paired sort row, and the scrolling section the
            // list sits in: four rows in that order, fixed regardless of whether anything is
            // spotlighted. Asserted by type rather than by count alone, because the order is what
            // every suite reaching into a built block reads through ListPickerBlockReads - so a row
            // inserted or moved fails here, where it names what changed, rather than in a consumer's
            // suite as a cast against a control it never asked for.
            assertThat(build(ANOMALIES, null, AnomalySortMode.ALPHA))
                .hasExactlyElementsOfTypes(
                    DividerSpec.class,
                    HorizontalRadioSpec.class,
                    SideBySideSpec.class,
                    ScrollingSectionSpec.class);
            assertThat(build(ANOMALIES, "storm_1", AnomalySortMode.ALPHA))
                .hasSize(4);
        }

        @Test
        void buildPickerBuildsAVerticalDeselectableIconListOfTheItems() {

            var picker = readItemList(build(ANOMALIES, null, AnomalySortMode.ALPHA));

            // A vertical table by type; re-picking the lit row clears the spotlight (DESELECT).
            assertThat(picker.reselect())
                .isEqualTo(ReselectBehaviour.DESELECT);

            // Each row carries its own name and its own crest, so a crestless item leads with the
            // empty slot rather than dropping out of a parallel column. Alpha-sorted, so Drift
            // leads.
            assertThat(picker.labels())
                .containsExactly("Drift", "Storm");
            assertThat(readLeadingRowSlots(picker))
                .containsExactly(RowSlot.EMPTY, new RowSlot.Image("crest_storm"));
        }

        @Test
        void buildPickerRanksTheListByTheSortMode() {
            // The list is ordered by the chosen metric, and the trailing value is that metric, so the
            // rows read as a table sorted by the number shown.
            //
            // A third item that trails on severity but leads on radius, so a radius sort visibly
            // reorders the list rather than just relabelling it.
            var squall = new Anomaly("squall_1", "Squall", "crest_squall", 1, 12);
            var picker = readItemList(build(
                List.of(STORM, DRIFT, squall),
                null,
                AnomalySortMode.RADIUS));

            assertThat(picker.labels())
                .containsExactly("Squall", "Storm", "Drift");
            assertThat(readTrailingRowSlots(picker))
                .containsExactly(readValueRowSlot("12"), readValueRowSlot("8"), readValueRowSlot("3"));
        }

        @Test
        void buildPickerRanksTheListInTheGivenDirection() {
            // The direction flows through to the ordering: radius ascending reverses the default
            // descending list. The trailing values still read the radius, only their order flips.
            var picker = readItemList(buildPicker(
                ANOMALIES,
                null,
                new ListSort<>(AnomalySortMode.RADIUS, SortDirection.ASCENDING, SORT_MODES),
                ListColumns.ONE,
                TRAILING));

            assertThat(picker.labels())
                .containsExactly("Drift", "Storm");
            assertThat(readTrailingRowSlots(picker))
                .containsExactly(readValueRowSlot("3"), readValueRowSlot("8"));
        }

        @Test
        void buildPickerLeavesTheValueSlotUnfilledUnderAModeThatShowsNone() {
            // A mode that answers no runs leaves every row's value column unfilled, so the list reads
            // as a plain one rather than a ranked table - and the absence is the empty slot rather
            // than a blank run, so nothing downstream has to read an empty string as "nothing here".
            var picker = readItemList(build(ANOMALIES, null, AnomalySortMode.ALPHA));

            assertThat(readTrailingRowSlots(picker))
                .containsExactly(RowSlot.EMPTY, RowSlot.EMPTY);
        }

        @Test
        void buildPickerLeavesTheValueSlotUnfilledWhenTheModesRunsAllCameOutBlank() {
            // The other way a mode says nothing: it answers a run, and the run turns out to carry no
            // text. A consumer assembling a value from parts cannot tell that case from declaring no
            // value at all, so both reach the row as the one empty slot rather than as two spellings
            // of the same absence.
            var picker = readItemList(build(ANOMALIES, null, AnomalySortMode.SEVERITY));

            assertThat(readTrailingRowSlots(picker))
                .containsExactly(RowSlot.EMPTY, RowSlot.EMPTY);
        }

        @Test
        void buildPickerDrawsAMultiRunValueInTheModesOwnColours() {
            // A mode whose value is picked out in shades of its own reaches the row as several runs,
            // each keeping the colour the mode gave it, while the run it left to the row takes the
            // row's tone - so one value can mix the two.
            var picker = readItemList(build(List.of(STORM), null, AnomalySortMode.SPREAD));

            assertThat(picker.labelledRows().get(0).trailingRowSlot())
                .isEqualTo(new RowSlot.TextRuns(List.of(
                    new TextSpan("9", AnomalySortMode.LOW_END_COLOUR),
                    new TextSpan(AnomalySortMode.RANGE_SEPARATOR, TEXT).joinsPreviousRun(),
                    new TextSpan("8", AnomalySortMode.HIGH_END_COLOUR).joinsPreviousRun())));
        }

        @Test
        void buildPickerRecedesAMultiRunValueOverTheModesOwnColoursKeepingItsSpacing() {
            // The receded tone wins over a mode's own shades: a row that reads back cannot keep a
            // full-strength value beside its greyed name, which is the same rule its crest follows.
            // Each run's own spacing survives the re-colour, so a range that read as one word still
            // does once greyed rather than falling apart into word-spaced numbers.
            var lapsed = new Anomaly("lapsed_1", "Lapsed", "crest_lapsed", 4, 6, true);
            var picker = readItemList(build(List.of(lapsed), null, AnomalySortMode.SPREAD));

            assertThat(picker.labelledRows().get(0).trailingRowSlot())
                .isEqualTo(new RowSlot.TextRuns(List.of(
                    new TextSpan("4", GRAY),
                    new TextSpan(AnomalySortMode.RANGE_SEPARATOR, GRAY).joinsPreviousRun(),
                    new TextSpan("6", GRAY).joinsPreviousRun())));
        }

        @Test
        void buildPickerDrawsAnOrdinaryRowAtFullStrength() {
            // The baseline the receded case is read against: an item that says nothing about reading
            // back takes the plain tone throughout and its crest draws in the colours it was authored
            // in, so nothing is tinted on speculation.
            // Ranked by the one fixture mode that writes a number onto its rows, so the trailing
            // value carries a tone to read rather than the blank the other modes leave.
            var picker = readItemList(build(List.of(STORM), null, AnomalySortMode.RADIUS));
            var row = picker.labelledRows().get(0);

            assertThat(row.labelRuns())
                .containsExactly(new TextSpan("Storm", TEXT));
            assertThat(row.trailingRowSlot())
                .isEqualTo(new RowSlot.Text(new TextSpan("8", TEXT)));
            assertThat(row.leadingRowSlot())
                .isEqualTo(new RowSlot.Image("crest_storm", null));
        }

        @Test
        void buildPickerRecedesADimmedRowsWordsAndCrestTogether() {
            // A row the caller marked as reading back recedes as a whole: its name and its value take
            // the engine's gray and its crest is multiplied by the flat tint, so the row cannot draw
            // greyed words beside a full-strength badge - which reads as a rendering slip rather than
            // as a state. The tint is a separate shade from the words' tone on purpose; see the
            // constant.
            var lapsed = new Anomaly("lapsed_1", "Lapsed", "crest_lapsed", 0, 0, true);
            var picker = readItemList(build(List.of(lapsed), null, AnomalySortMode.RADIUS));
            var row = picker.labelledRows().get(0);

            assertThat(row.labelRuns())
                .containsExactly(new TextSpan("Lapsed", GRAY));
            assertThat(row.trailingRowSlot())
                .isEqualTo(new RowSlot.Text(new TextSpan("0", GRAY)));
            assertThat(row.leadingRowSlot())
                .isEqualTo(new RowSlot.Image("crest_lapsed", CREST_TINT));
        }

        @Test
        void buildPickerRecedesOnlyTheRowsThatSayTheyDo() {
            // The state is per row rather than per list, so a receded row and a full-strength one sit
            // in the same list without either taking the other's tone.
            var lapsed = new Anomaly("lapsed_1", "Lapsed", "crest_lapsed", 0, 0, true);
            var picker = readItemList(build(
                List.of(STORM, lapsed),
                null,
                AnomalySortMode.RADIUS));

            assertThat(picker.labels())
                .containsExactly("Storm", "Lapsed");
            assertThat(readLeadingRowSlots(picker))
                .containsExactly(
                    new RowSlot.Image("crest_storm", null),
                    new RowSlot.Image("crest_lapsed", CREST_TINT));
        }

        @Test
        void buildPickerLabelsANamelessItemAsAnEmptyRow() {
            // An item whose name did not resolve draws as an unlabelled row, not a null the width
            // measurer would choke on.
            var nameless = new Anomaly("ghost_1", null, null, 4, 4);
            var picker = readItemList(build(List.of(nameless), null, AnomalySortMode.SEVERITY));

            assertThat(picker.labels())
                .containsExactly("");
        }

        @Test
        void buildPickerLightsTheSpotlightedItemsRow() {
            // The lit row is resolved by the selected ID, not by the label the row draws, so an ID
            // that matches no label still lights its own item's row wherever the ranking put it.
            var picker = readItemList(build(ANOMALIES, "storm_1", AnomalySortMode.ALPHA));

            // Alpha-sorted, Storm is the second row.
            assertThat(picker.selectedIndex())
                .isEqualTo(1);
        }

        @Test
        void buildPickerLightsNoRowWhenTheSelectedItemIsNotInTheList() {
            // A stale selected ID (the caller's heal has not run, or the item lapsed mid-session)
            // lights nothing, so the list still shows every real option to pick from.
            var picker = readItemList(build(ANOMALIES, "vanished", AnomalySortMode.ALPHA));

            assertThat(picker.selectedIndex())
                .isEqualTo(ControlSpec.NO_SELECTION);
        }

        @Test
        void buildPickerLaysTheListAcrossTheChosenColumnCount() {
            // The chosen column count reaches the list widget's geometry: a two-column choice builds
            // a two-column list, a single-column choice a one-column list, so the layout wraps the
            // rows exactly as the selector says.
            var oneColumn = readItemList(buildPicker(
                ANOMALIES,
                null,
                sortOf(AnomalySortMode.ALPHA),
                ListColumns.ONE,
                TRAILING));

            var twoColumn = readItemList(buildPicker(
                ANOMALIES,
                null,
                sortOf(AnomalySortMode.ALPHA),
                ListColumns.TWO,
                TRAILING));

            assertThat(oneColumn.columnCount())
                .isEqualTo(1);
            assertThat(twoColumn.columnCount())
                .isEqualTo(2);
        }

        @Test
        void buildPickerReportsAColumnsSegmentPickToTheStore() {
            // The columns selector reports its pick rather than storing one, and the picker passes
            // that report straight through - a wiring that silently came undone would leave the
            // segment lighting up and the caller never told.
            var columnsSelector =
                readColumnsSelector(build(ANOMALIES, null, AnomalySortMode.ALPHA));

            columnsSelector.action().activateCell(
                List.of(ListColumns.values()).indexOf(ListColumns.TWO));

            assertThat(pickerStoreFake.pickedColumns)
                .containsExactly(ListColumns.TWO);
        }

        @Test
        void buildPickerReportsASortRowPickToTheStore() {
            // The same pass-through for the other selector: a click on an unlit mode's row reports
            // that mode in its own default direction.
            var sortSelector = readSortSelector(build(ANOMALIES, null, AnomalySortMode.ALPHA));

            sortSelector.action().activateCell(MODES.indexOf(AnomalySortMode.SEVERITY));

            assertThat(pickerStoreFake.pickedSorts)
                .containsExactly(new ListSort<>(
                    AnomalySortMode.SEVERITY,
                    AnomalySortMode.SEVERITY.defaultDirection(),
                    SORT_MODES));
        }
    }

    @Nested
    class PickItem {

        @Test
        void clickingAnUnlitOptionReportsThatItem() {

            var picker = readItemList(build(ANOMALIES, null, AnomalySortMode.ALPHA));

            picker.action().activateCell(0);

            // The item's own ID is what is reported, not its label or its row index.
            assertThat(pickerStoreFake.pickedItemIds)
                .containsExactly("drift_1");
            assertThat(pickerStoreFake.pickClearCount)
                .isZero();
        }

        @Test
        void clickingTheLitOptionReportsAClear() {
            // The list is deselectable, so a press on the spotlighted row reaches the action with its
            // own index; re-picking it stops the spotlight rather than re-selecting it.
            var picker = readItemList(build(ANOMALIES, "drift_1", AnomalySortMode.ALPHA));

            picker.action().activateCell(0);

            assertThat(pickerStoreFake.pickClearCount)
                .isEqualTo(1);
            assertThat(pickerStoreFake.pickedItemIds)
                .isEmpty();
        }

        @Test
        void clickingAnotherOptionWhileSpotlightingReportsTheNewItem() {

            var picker = readItemList(build(ANOMALIES, "drift_1", AnomalySortMode.ALPHA));

            picker.action().activateCell(1);

            assertThat(pickerStoreFake.pickedItemIds)
                .containsExactly("storm_1");
        }

        @Test
        void clickingOutsideTheListReportsNothing() {
            // A stray hit - an index past the rows, or a negative one - neither spotlights nor
            // clears, so a click that lands outside the options leaves the caller's state as it was.
            var picker = readItemList(build(ANOMALIES, null, AnomalySortMode.ALPHA));

            picker.action().activateCell(ANOMALIES.size());
            picker.action().activateCell(-1);

            assertThat(pickerStoreFake.pickedItemIds)
                .isEmpty();
            assertThat(pickerStoreFake.pickClearCount)
                .isZero();
        }
    }

    @Nested
    class ReportHoveredItem {

        @Test
        void hoveringARowReportsThatItemsId() {
            // Which item a cell names is read off the ranked order, not off the list the caller handed
            // over, so what a hover previews is what a click on that row would spotlight. Sorted by
            // radius, the third item leads and the caller's own first item falls behind it.
            var squall = new Anomaly("squall_1", "Squall", "crest_squall", 1, 12);
            var picker = readItemList(build(
                List.of(STORM, DRIFT, squall),
                null,
                AnomalySortMode.RADIUS));

            picker.hoverReport().reportHoveredCell(0);

            assertThat(pickerStoreFake.hoveredItemIds)
                .containsExactly("squall_1");
            assertThat(pickerStoreFake.hoverClearCount)
                .isZero();

            // The two channels are separate: crossing a row previews it and picks nothing, so a
            // pointer sweeping the list cannot move what is spotlighted.
            assertThat(pickerStoreFake.pickedItemIds)
                .isEmpty();
            assertThat(pickerStoreFake.pickClearCount)
                .isZero();
        }

        @Test
        void hoveringTheSpotlightedRowReportsItLikeAnyOther() {
            // A hover reads the row under the pointer and nothing about what is lit, so the
            // spotlighted row reports its own item rather than the clear a click on it reports.
            var picker = readItemList(build(ANOMALIES, "drift_1", AnomalySortMode.ALPHA));

            picker.hoverReport().reportHoveredCell(0);

            assertThat(pickerStoreFake.hoveredItemIds)
                .containsExactly("drift_1");
            assertThat(pickerStoreFake.hoverClearCount)
                .isZero();
        }

        @Test
        void leavingTheListReportsNoItem() {
            // The leave is the one thing a stream of readings never says out loud, so it reaches the
            // store as a report of its own: a host previewing the hovered item stops previewing one
            // rather than holding the last row the pointer crossed.
            var picker = readItemList(build(ANOMALIES, null, AnomalySortMode.ALPHA));

            picker.hoverReport().reportHoveredCell(0);
            picker.hoverReport().reportHoveredCell(ControlHoverReport.NO_CELL_HOVERED);

            assertThat(pickerStoreFake.hoveredItemIds)
                .containsExactly("drift_1");
            assertThat(pickerStoreFake.hoverClearCount)
                .isEqualTo(1);
        }

        @Test
        void hoveringOutsideTheListReportsNoItem() {
            // A reading on a cell no item stands in - past the rows, or a negative one - reports the
            // leave rather than being dropped, which is where the hover parts from the click beside
            // it: a stray click leaves the spotlight standing, while a hover on nothing is itself the
            // answer that no item is under the pointer.
            var picker = readItemList(build(ANOMALIES, null, AnomalySortMode.ALPHA));

            picker.hoverReport().reportHoveredCell(ANOMALIES.size());
            picker.hoverReport().reportHoveredCell(-1);

            assertThat(pickerStoreFake.hoveredItemIds)
                .isEmpty();
            assertThat(pickerStoreFake.hoverClearCount)
                .isEqualTo(2);
        }
    }

    // A mode in its own natural direction - the state a caller that has never flipped the sort reads.
    private static ListSort<Anomaly> sortOf(AnomalySortMode mode) {
        return new ListSort<>(mode, mode.defaultDirection(), SORT_MODES);
    }

    // What each row leads with, top to bottom - an item's crest, or the empty slot for an item with
    // none.
    private static List<RowSlot> readLeadingRowSlots(VerticalTableSpec picker) {
        return picker.labelledRows().stream()
            .map(LabelledRow::leadingRowSlot)
            .toList();
    }

    // What each row trails with, top to bottom - the sort metric's value for that item.
    private static List<RowSlot> readTrailingRowSlots(VerticalTableSpec picker) {
        return picker.labelledRows().stream()
            .map(LabelledRow::trailingRowSlot)
            .toList();
    }

    // The trailing slot a row carrying this value holds, in the tone the picker resolves - what an
    // assertion spells out to say "this row's value column reads that".
    private static RowSlot readValueRowSlot(String value) {
        return new RowSlot.Text(new TextSpan(value, TEXT));
    }

    // The one call into the picker every test in the suite goes through, so the sort vocabulary, the
    // caption, and the recording store are named once and a test spells out only the input it is
    // actually varying.
    private List<ControlSpec> buildPicker(
            List<Anomaly> anomalies,
            String selectedItemId,
            ListSort<Anomaly> sort,
            ListColumns columns,
            List<ControlSpec> trailingControls) {

        return ListPickerControl.buildPicker(
            anomalies,
            new ActivePicks<>(selectedItemId, sort, columns),
            CAPTION,
            trailingControls,
            pickerStoreFake);
    }

    // Builds the picker in the sort mode's own default direction, the default single-column layout,
    // and one stand-in trailing control - the state every test that does not exercise a flip, a
    // column change, or an empty pairing assumes, so those call sites read the mode alone.
    private List<ControlSpec> build(
            List<Anomaly> anomalies,
            String selectedItemId,
            AnomalySortMode mode) {

        return buildPicker(
            anomalies,
            selectedItemId,
            sortOf(mode),
            ListColumns.ONE,
            TRAILING);
    }

    // Test fake: the caller's store, recording each report in the order it arrived rather than
    // persisting it, so a suite reads exactly what a real consumer would have been asked to write.
    private static final class ListPickerStoreFake implements ListPickerStore {

        private final List<String> hoveredItemIds = new ArrayList<>();
        private final List<ListColumns> pickedColumns = new ArrayList<>();
        private final List<String> pickedItemIds = new ArrayList<>();
        private final List<ListSort<?>> pickedSorts = new ArrayList<>();

        // Counted rather than collected: a clear carries no value, so how many arrived is the whole
        // of what a test can read off it. One count per channel, so a test reading either can tell a
        // pointer leaving the rows from a spotlight being switched off.
        private int hoverClearCount;
        private int pickClearCount;

        @Override
        public void clearItemHover() {
            hoverClearCount++;
        }

        @Override
        public void clearItemPick() {
            pickClearCount++;
        }

        @Override
        public void reportItemHover(String itemId) {
            hoveredItemIds.add(itemId);
        }

        @Override
        public void storeColumnsPick(ListColumns columns) {
            pickedColumns.add(columns);
        }

        @Override
        public void storeItemPick(String itemId) {
            pickedItemIds.add(itemId);
        }

        @Override
        public void storeSortPick(ListSort<?> sort) {
            pickedSorts.add(sort);
        }
    }
}
