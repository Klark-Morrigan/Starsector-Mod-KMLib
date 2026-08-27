package kmlib.starsector.ui.widgets.lists;

import com.fs.starfarer.api.util.Misc;

import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.ReselectBehaviour;
import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.widgets.LabelledRow;
import kmlib.starsector.ui.widgets.RowSlot;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the picker's controls, top to bottom: a section rule, the columns selector, a row pairing the
 * sort selector beside the caller's own trailing controls, and the vertical icon-radio list of
 * selectable items ranked by the active sort mode. Also pins the click wiring: an unlit option
 * reports its item, the lit option reports a clear, and both selectors report through the same
 * store.
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
    // crestless one, so the null-crest path is exercised. Their ids differ from their labels, so a
    // row lit by id cannot be passing by matching a label.
    private static final Anomaly STORM = new Anomaly("storm_1", "Storm", "crest_storm", 9, 8);
    private static final Anomaly DRIFT = new Anomaly("drift_1", "Drift", null, 2, 3);

    private static final List<Anomaly> ANOMALIES = List.of(STORM, DRIFT);

    // The caption the caller resolved and handed over for the columns segments.
    private static final String CAPTION = "Columns";

    // The trailing controls the caller pairs with the sort selector; a plain label stands in for
    // whatever a consumer actually pairs there, since the picker only places what it is handed. Its
    // tone is arbitrary - nothing under test reads what colour a placed control draws in.
    private static final ControlSpec TRAILING_MARKER =
        ControlSpec.Label.createLabel(new TextSpan("trailing", Color.WHITE));

    private static final List<ControlSpec> TRAILING = List.of(TRAILING_MARKER);

    // The block is a fixed four rows: the section rule, the columns selector, the paired sort row,
    // then the list.
    private static final int DIVIDER = 0;
    private static final int COLUMNS_SELECTOR = 1;
    private static final int SORT_ROW = 2;

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
                .isInstanceOf(ControlSpec.Divider.class);
        }

        @Test
        void buildPickerPlacesTheColumnsSelectorUnderTheDivider() {
            // The columns selector rides directly under the rule, so the column count is chosen for
            // the block as a whole: a two-segment horizontal radio under the caller's caption.
            var columnsSelector = build(ANOMALIES, null, AnomalySortMode.ALPHA)
                .get(COLUMNS_SELECTOR);

            assertThat(columnsSelector)
                .isInstanceOf(ControlSpec.HorizontalRadio.class);
            assertThat(columnsSelector.labels())
                .hasSize(2);
            assertThat(((ControlSpec.HorizontalRadio) columnsSelector).trailingLabel())
                .isEqualTo(CAPTION);
        }

        @Test
        void buildPickerPairsTheSortSelectorBesideTheCallersTrailingControls() {
            // The sort selector holds the row's left half (a vertical, re-firing radio lit on the
            // active mode's row) and whatever the caller handed over fills the right, so the metric
            // reads side by side with the caller's own knobs above the list.
            var pair = sortRowOf(build(ANOMALIES, null, AnomalySortMode.SEVERITY));
            var sortSelector = (ControlSpec.VerticalTable) pair
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
            var pair = sortRowOf(buildPicker(
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
            // The rule, the columns selector, the paired sort row, and the list: four rows, fixed
            // regardless of whether anything is spotlighted.
            assertThat(build(ANOMALIES, null, AnomalySortMode.ALPHA))
                .hasSize(4);
            assertThat(build(ANOMALIES, "storm_1", AnomalySortMode.ALPHA))
                .hasSize(4);
        }

        @Test
        void buildPickerBuildsAVerticalDeselectableIconListOfTheItems() {

            var picker = buildPickerFor(build(ANOMALIES, null, AnomalySortMode.ALPHA));

            // A vertical table by type; re-picking the lit row clears the spotlight (DESELECT).
            assertThat(picker.reselect())
                .isEqualTo(ReselectBehaviour.DESELECT);

            // The list is the block's scrolling cluster, so a long list scrolls within the capped
            // body while the controls above and below it stay pinned.
            assertThat(picker.scrolls())
                .isTrue();

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
            var picker = buildPickerFor(build(
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
            var picker = buildPickerFor(buildPicker(
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
        void buildPickerLeavesTheValuesBlankUnderAModeThatShowsNone() {
            // A mode that declares no trailing value leaves every row blank, so the list reads as a
            // plain one rather than a ranked table.
            var picker = buildPickerFor(build(ANOMALIES, null, AnomalySortMode.ALPHA));

            assertThat(readTrailingRowSlots(picker))
                .containsExactly(readValueRowSlot(""), readValueRowSlot(""));
        }

        @Test
        void buildPickerDrawsAnOrdinaryRowAtFullStrength() {
            // The baseline the receded case is read against: an item that says nothing about reading
            // back takes the plain tone throughout and its crest draws in the colours it was authored
            // in, so nothing is tinted on speculation.
            // Ranked by the one fixture mode that writes a number onto its rows, so the trailing
            // value carries a tone to read rather than the blank the other modes leave.
            var picker = buildPickerFor(build(List.of(STORM), null, AnomalySortMode.RADIUS));
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
            var picker = buildPickerFor(build(List.of(lapsed), null, AnomalySortMode.RADIUS));
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
            var picker = buildPickerFor(build(
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
            var picker = buildPickerFor(build(List.of(nameless), null, AnomalySortMode.SEVERITY));

            assertThat(picker.labels())
                .containsExactly("");
        }

        @Test
        void buildPickerLightsTheSpotlightedItemsRow() {
            // The lit row is resolved by the selected id, not by the label the row draws, so an id
            // that matches no label still lights its own item's row wherever the ranking put it.
            var picker = buildPickerFor(build(ANOMALIES, "storm_1", AnomalySortMode.ALPHA));

            // Alpha-sorted, Storm is the second row.
            assertThat(picker.selectedIndex())
                .isEqualTo(1);
        }

        @Test
        void buildPickerLightsNoRowWhenTheSelectedItemIsNotInTheList() {
            // A stale selected id (the caller's heal has not run, or the item lapsed mid-session)
            // lights nothing, so the list still shows every real option to pick from.
            var picker = buildPickerFor(build(ANOMALIES, "vanished", AnomalySortMode.ALPHA));

            assertThat(picker.selectedIndex())
                .isEqualTo(ControlSpec.NO_SELECTION);
        }

        @Test
        void buildPickerLaysTheListAcrossTheChosenColumnCount() {
            // The chosen column count reaches the list widget's geometry: a two-column choice builds
            // a two-column list, a single-column choice a one-column list, so the layout wraps the
            // rows exactly as the selector says.
            var oneColumn = buildPickerFor(buildPicker(
                ANOMALIES,
                null,
                sortOf(AnomalySortMode.ALPHA),
                ListColumns.ONE,
                TRAILING));

            var twoColumn = buildPickerFor(buildPicker(
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
            var columnsSelector = (ControlSpec.HorizontalRadio)
                build(ANOMALIES, null, AnomalySortMode.ALPHA)
                    .get(COLUMNS_SELECTOR);

            columnsSelector.action().activateCell(
                List.of(ListColumns.values()).indexOf(ListColumns.TWO));

            assertThat(pickerStoreFake.pickedColumns)
                .containsExactly(ListColumns.TWO);
        }

        @Test
        void buildPickerReportsASortRowPickToTheStore() {
            // The same pass-through for the other selector: a click on an unlit mode's row reports
            // that mode in its own default direction.
            var sortSelector = (ControlSpec.VerticalTable) sortRowOf(
                    build(ANOMALIES, null, AnomalySortMode.ALPHA))
                .leftColumn()
                .get(0);

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

            var picker = buildPickerFor(build(ANOMALIES, null, AnomalySortMode.ALPHA));

            picker.action().activateCell(0);

            // The item's own id is what is reported, not its label or its row index.
            assertThat(pickerStoreFake.pickedItemIds)
                .containsExactly("drift_1");
            assertThat(pickerStoreFake.clearCount)
                .isZero();
        }

        @Test
        void clickingTheLitOptionReportsAClear() {
            // The list is deselectable, so a press on the spotlighted row reaches the action with its
            // own index; re-picking it stops the spotlight rather than re-selecting it.
            var picker = buildPickerFor(build(ANOMALIES, "drift_1", AnomalySortMode.ALPHA));

            picker.action().activateCell(0);

            assertThat(pickerStoreFake.clearCount)
                .isEqualTo(1);
            assertThat(pickerStoreFake.pickedItemIds)
                .isEmpty();
        }

        @Test
        void clickingAnotherOptionWhileSpotlightingReportsTheNewItem() {

            var picker = buildPickerFor(build(ANOMALIES, "drift_1", AnomalySortMode.ALPHA));

            picker.action().activateCell(1);

            assertThat(pickerStoreFake.pickedItemIds)
                .containsExactly("storm_1");
        }

        @Test
        void clickingOutsideTheListReportsNothing() {
            // A stray hit - an index past the rows, or a negative one - neither spotlights nor
            // clears, so a click that lands outside the options leaves the caller's state as it was.
            var picker = buildPickerFor(build(ANOMALIES, null, AnomalySortMode.ALPHA));

            picker.action().activateCell(ANOMALIES.size());
            picker.action().activateCell(-1);

            assertThat(pickerStoreFake.pickedItemIds)
                .isEmpty();
            assertThat(pickerStoreFake.clearCount)
                .isZero();
        }
    }

    // A mode in its own natural direction - the state a caller that has never flipped the sort reads.
    private static ListSort<Anomaly> sortOf(AnomalySortMode mode) {
        return new ListSort<>(mode, mode.defaultDirection(), SORT_MODES);
    }

    // The picker list is always the block's last row, so a test reads it from the tail. Read as the
    // vertical table it is, so a test reads its rows, its scroll flag, and its re-pick behaviour.
    private static ControlSpec.VerticalTable buildPickerFor(List<ControlSpec> controls) {
        return (ControlSpec.VerticalTable) controls.get(controls.size() - 1);
    }

    // What each row leads with, top to bottom - an item's crest, or the empty slot for an item with
    // none.
    private static List<RowSlot> readLeadingRowSlots(ControlSpec.VerticalTable picker) {
        return picker.labelledRows().stream()
            .map(LabelledRow::leadingRowSlot)
            .toList();
    }

    // What each row trails with, top to bottom - the sort metric's value for that item.
    private static List<RowSlot> readTrailingRowSlots(ControlSpec.VerticalTable picker) {
        return picker.labelledRows().stream()
            .map(LabelledRow::trailingRowSlot)
            .toList();
    }

    // The paired sort row, read as the side-by-side group it is so a test reads its left column (the
    // sort selector) and its right column (the caller's trailing controls) separately.
    private static ControlSpec.SideBySide sortRowOf(List<ControlSpec> controls) {
        return (ControlSpec.SideBySide) controls.get(SORT_ROW);
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
            selectedItemId,
            sort,
            columns,
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

        private final List<ListColumns> pickedColumns = new ArrayList<>();
        private final List<String> pickedItemIds = new ArrayList<>();
        private final List<ListSort<?>> pickedSorts = new ArrayList<>();

        // Counted rather than collected: a clear carries no value, so how many arrived is the whole
        // of what a test can read off it.
        private int clearCount;

        @Override
        public void clearItemPick() {
            clearCount++;
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
