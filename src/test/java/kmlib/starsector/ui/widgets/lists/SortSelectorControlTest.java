package kmlib.starsector.ui.widgets.lists;

import com.fs.starfarer.api.util.Misc;

import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.ReselectBehaviour;
import kmlib.starsector.ui.controls.RowGeometry;
import kmlib.starsector.ui.widgets.RowSlot;
import kmlib.starsector.ui.widgets.TriangleDirection;
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
 * Pins the sort selector: a vertical, re-firing radio with one row per mode of the caller's set in
 * the caller's own order, lit on the active mode, each row trailed by the direction it would sort
 * in. Clicking a different mode switches to it at its default direction; re-clicking the lit mode
 * flips its direction. Exercised on the {@link AnomalySortMode} fixture throughout - the proof the
 * selector serves whatever modes a consumer declares - with the pick collected through the reporting
 * callback, so this pins the selector's shape and wiring without any store in reach.
 */
final class SortSelectorControlTest {

    // The mode rows in the order the selector stacks them, so a test maps a row index back to a
    // mode; the vocabulary bundles them with the default a caller's resolution falls back to.
    private static final List<AnomalySortMode> MODES = List.of(AnomalySortMode.values());
    private static final AnomalySortMode DEFAULT_MODE = AnomalySortMode.ALPHA;
    private static final ListSortModes<Anomaly> SORT_MODES =
        new ListSortModes<>(MODES, DEFAULT_MODE);

    // The engine tone the selector's rows carry, stood in for so the rows can be built without the
    // live palette in reach.
    private static final Color TEXT = Color.LIGHT_GRAY;

    // The sorts a click reported, in the order they were reported, so a test reads what the caller
    // would have been asked to persist.
    private final List<ListSort<Anomaly>> pickedSorts = new ArrayList<>();

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
    }

    @AfterEach
    void clearColours() {
        miscMock.close();
        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class BuildSelector {

        @Test
        void buildSelectorBuildsAVerticalReFiringRadio() {

            var selector = buildSelector(DEFAULT_MODE, DEFAULT_MODE.defaultDirection());

            // A vertical table by type; a sort is always active, so it never deselects - instead a
            // re-pick re-fires so the handler can flip the direction.
            assertThat(selector.reselect())
                .isEqualTo(ReselectBehaviour.REFIRE);
        }

        @Test
        void buildSelectorLabelsARowPerModeInTheCallersOrder() {

            var selector = buildSelector(DEFAULT_MODE, DEFAULT_MODE.defaultDirection());

            assertThat(selector.labels())
                .containsExactly("Alpha", "Severity", "Radius");
        }

        @Test
        void buildSelectorLightsTheActiveModesRow() {

            var selector = buildSelector(AnomalySortMode.RADIUS, SortDirection.DESCENDING);

            assertThat(selector.selectedIndex())
                .isEqualTo(MODES.indexOf(AnomalySortMode.RADIUS));
        }

        @Test
        void buildSelectorTrailsTheActiveRowWithItsLiveDirectionAndOthersWithTheirDefaults() {
            // The lit row previews the direction the list is sorting in now (flipped to ascending,
            // an UP triangle); the other numeric row previews its own default descending DOWN
            // triangle, and the alpha row its default ascending UP triangle - so each row reads as
            // "pick me and the list sorts this way".
            var selector = buildSelector(AnomalySortMode.SEVERITY, SortDirection.ASCENDING);

            var alphaRow = MODES.indexOf(AnomalySortMode.ALPHA);
            var severityRow = MODES.indexOf(AnomalySortMode.SEVERITY);
            var radiusRow = MODES.indexOf(AnomalySortMode.RADIUS);

            assertThat(readTrailingRowSlotAt(selector, severityRow))
                .isEqualTo(new RowSlot.Triangle(TriangleDirection.UP));
            assertThat(readTrailingRowSlotAt(selector, radiusRow))
                .isEqualTo(new RowSlot.Triangle(TriangleDirection.DOWN));
            assertThat(readTrailingRowSlotAt(selector, alphaRow))
                .isEqualTo(new RowSlot.Triangle(TriangleDirection.UP));
        }

        @Test
        void buildSelectorLeavesEveryRowLeadingWithNothing() {
            // The selector's rows carry no crest, so each leads with the empty slot rather than an
            // image - and the mode names still start where a crested list's names would.
            var selector = buildSelector(DEFAULT_MODE, DEFAULT_MODE.defaultDirection());

            assertThat(selector.labelledRows())
                .allMatch(labelledRow -> RowSlot.EMPTY.equals(labelledRow.leadingRowSlot()));
        }

        @Test
        void buildSelectorLaysItsRowsInColumns() {
            // The selector reads as the picker list's own table - names left, triangles flush right -
            // which it states rather than leaves to be inferred from rows that lead with nothing.
            var selector = buildSelector(DEFAULT_MODE, DEFAULT_MODE.defaultDirection());

            assertThat(selector.rowGeometry())
                .isEqualTo(RowGeometry.COLUMNS);
        }
    }

    @Nested
    class ApplySelection {

        @Test
        void clickingADifferentModeReportsItAtItsDefaultDirection() {
            // Alpha is active; clicking severity switches to it and resets the direction to
            // severity's default (descending), so a mode switch always starts natural.
            var selector = buildSelector(DEFAULT_MODE, DEFAULT_MODE.defaultDirection());

            selector.action().activateCell(MODES.indexOf(AnomalySortMode.SEVERITY));

            assertThat(pickedSorts)
                .containsExactly(new ListSort<>(
                    AnomalySortMode.SEVERITY,
                    SortDirection.DESCENDING,
                    SORT_MODES));
        }

        @Test
        void reClickingTheLitModeReportsItWithTheFlippedDirection() {
            // Severity is active ascending; re-clicking its row reports the same mode descending, so
            // the caller stores a flip rather than a switch.
            var selector = buildSelector(AnomalySortMode.SEVERITY, SortDirection.ASCENDING);

            selector.action().activateCell(MODES.indexOf(AnomalySortMode.SEVERITY));

            assertThat(pickedSorts)
                .containsExactly(new ListSort<>(
                    AnomalySortMode.SEVERITY,
                    SortDirection.DESCENDING,
                    SORT_MODES));
        }

        @Test
        void clickingOutsideTheModeRowsReportsNothing() {
            // A stray hit past the last row names no mode, so it is ignored rather than reporting a
            // phantom choice the caller would persist.
            var selector = buildSelector(DEFAULT_MODE, DEFAULT_MODE.defaultDirection());

            selector.action().activateCell(MODES.size());

            assertThat(pickedSorts)
                .isEmpty();
        }
    }

    // Builds the selector over the fixture vocabulary from a mode and direction the tests spell out
    // as a pair, collecting whatever a click reports, so each call site reads as the
    // mode-and-direction it exercises rather than a record construction and two shared arguments.
    private ControlSpec.VerticalTable buildSelector(
            AnomalySortMode mode,
            SortDirection direction) {

        return SortSelectorControl.buildSelector(
            new ListSort<>(mode, direction, SORT_MODES),
            pickedSorts::add);
    }

    // What the row at this index trails with - the slot a test reads to learn which direction that
    // mode previews.
    private static RowSlot readTrailingRowSlotAt(ControlSpec.VerticalTable selector, int rowIndex) {
        return selector.labelledRows().get(rowIndex).trailingRowSlot();
    }
}
