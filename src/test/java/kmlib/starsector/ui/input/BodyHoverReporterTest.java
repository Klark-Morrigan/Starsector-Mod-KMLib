package kmlib.starsector.ui.input;

import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.sound.PointerArrivalTarget;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what a host answering a hover is told and when: the cell as the pointer comes onto it, the new cell
 * as it moves, and the leave as it goes - each once, off a reading taken every frame.
 *
 * <p>The leave is what most of these are about. A stream of readings never says the pointer left, so a host
 * told only where the pointer is would hold the last cell it heard about for the rest of the session - and
 * the control it holds that cell for is gone from the strip by then, which is why the channel a reading went
 * out on is kept rather than re-read.
 */
final class BodyHoverReporterTest {

    // Two cells of one control and one of another. The second control's cell index differs from both, so a
    // report that named the slot's control where it meant its cell would read as a wrong number rather than
    // as the right one by coincidence.
    private static final BodyCellSlot FIRST_ROW = new BodyCellSlot(2, 0);
    private static final BodyCellSlot SECOND_ROW = new BodyCellSlot(2, 1);
    private static final BodyCellSlot OTHER_CONTROL_CELL = new BodyCellSlot(3, ControlSpec.SINGLE_CELL);

    // The pointer on no body cell at all - off the strip, or on chrome that resolves to nothing.
    private static final HoveredBodyCell NO_CELL_HOVERED = null;

    // What a host is told as the pointer leaves, named so a case reads as a leave rather than as a bare
    // null among cell indices. The name the panel's own cases use for it, one concept reading one way
    // across the package.
    private static final Integer NO_CELL_REPORTED = null;

    private final BodyHoverReporter hoverReporter = new BodyHoverReporter();

    // What one host was told, in the order it was told. A list rather than a last-value field: what these
    // cases turn on is how many times a host heard something, which a field cannot show.
    private final List<Integer> reportedCells = new ArrayList<>();
    private final List<Integer> otherHostReportedCells = new ArrayList<>();

    @Nested
    class ReportHoverChangeTo {

        @Test
        void reportHoverChangeToReportsTheCellThePointerCameOnto() {

            hoverReporter.reportHoverChangeTo(buildHoveredCell(FIRST_ROW));

            assertThat(reportedCells)
                .containsExactly(0);
        }

        @Test
        void reportHoverChangeToReportsNothingFurtherWhileThePointerRestsOnTheCell() {
            // A report per change and not per frame: a host that acts on what it is told would otherwise
            // redo that work sixty times a second for a pointer standing still. Each of these readings
            // carries a channel of its own, as a host rebuilding its specs every frame hands over - so a
            // reporter comparing channels rather than slots would call every frame a change.
            hoverReporter.reportHoverChangeTo(buildHoveredCell(FIRST_ROW));
            hoverReporter.reportHoverChangeTo(buildHoveredCell(FIRST_ROW));
            hoverReporter.reportHoverChangeTo(buildHoveredCell(FIRST_ROW));

            assertThat(reportedCells)
                .containsExactly(0);
        }

        @Test
        void reportHoverChangeToReportsTheNewCellAloneWhenThePointerMovesWithinOneControl() {
            // Moving down a list supersedes on the one channel. A leave sent first would tell the host it
            // holds nothing an instant before telling it what it holds, which reads as a flicker in
            // whatever the host drives.
            hoverReporter.reportHoverChangeTo(buildHoveredCell(FIRST_ROW));
            hoverReporter.reportHoverChangeTo(buildHoveredCell(SECOND_ROW));

            assertThat(reportedCells)
                .containsExactly(0, 1);
        }

        @Test
        void reportHoverChangeToReportsTheLeaveAsThePointerGoesOffEveryCell() {

            hoverReporter.reportHoverChangeTo(buildHoveredCell(FIRST_ROW));
            hoverReporter.reportHoverChangeTo(NO_CELL_HOVERED);

            assertThat(reportedCells)
                .containsExactly(0, NO_CELL_REPORTED);
        }

        @Test
        void reportHoverChangeToReportsNothingFurtherWhileThePointerStaysOffEveryCell() {
            // The leave is a change like any other, so the frames after it are frames where nothing
            // changed - a host hearing the leave once a frame could not tell a pointer that left from one
            // crossing back and forth over the strip's edge.
            hoverReporter.reportHoverChangeTo(buildHoveredCell(FIRST_ROW));
            hoverReporter.reportHoverChangeTo(NO_CELL_HOVERED);
            hoverReporter.reportHoverChangeTo(NO_CELL_HOVERED);

            assertThat(reportedCells)
                .containsExactly(0, NO_CELL_REPORTED);
        }

        @Test
        void reportHoverChangeToTellsTheControlLeftBehindBeforeTheOneReached() {
            // Two hosts, so the order is readable: the control the pointer left hears the leave, and the
            // one it reached hears its cell. Told only the arrival, the first host would go on answering a
            // hover the pointer has moved off.
            hoverReporter.reportHoverChangeTo(buildHoveredCell(FIRST_ROW));
            hoverReporter.reportHoverChangeTo(
                new HoveredBodyCell(
                    OTHER_CONTROL_CELL,
                    PointerArrivalTarget.SINGLE_OPTION_CONTROL,
                    otherHostReportedCells::add));

            assertThat(reportedCells)
                .containsExactly(0, NO_CELL_REPORTED);
            assertThat(otherHostReportedCells)
                .containsExactly(ControlSpec.SINGLE_CELL);
        }

    }

    @Nested
    class ReportHoverCleared {

        @Test
        void reportHoverClearedReportsTheLeaveToWhoeverWasLastTold() {
            // What a panel standing down is to a host answering a hover: no further frame resolves a
            // reading, so nothing else would ever tell it to let go of the cell it was handed.
            hoverReporter.reportHoverChangeTo(buildHoveredCell(FIRST_ROW));

            hoverReporter.reportHoverCleared();

            assertThat(reportedCells)
                .containsExactly(0, NO_CELL_REPORTED);
        }

        @Test
        void reportHoverClearedReportsNothingASecondTime() {
            // A panel stood down twice tells its host once, the second clear having nothing left to let go
            // of - and a host that had since been told about a fresh cell must not hear a leave for it.
            hoverReporter.reportHoverChangeTo(buildHoveredCell(FIRST_ROW));

            hoverReporter.reportHoverCleared();
            hoverReporter.reportHoverCleared();

            assertThat(reportedCells)
                .containsExactly(0, NO_CELL_REPORTED);
        }

        @Test
        void reportHoverClearedReportsNothingWhereNothingWasReported() {
            // A panel that stood down having never had the pointer on it owes nobody anything.
            hoverReporter.reportHoverCleared();

            assertThat(reportedCells)
                .isEmpty();
        }

        @Test
        void reportHoverClearedLeavesTheSameCellReportableAfresh() {
            // The next session opening with the cursor already over that cell has to be told about it: the
            // strip was not there a moment ago, so the host holds nothing and would otherwise wait for the
            // player to move off the cell and back onto it.
            hoverReporter.reportHoverChangeTo(buildHoveredCell(FIRST_ROW));
            hoverReporter.reportHoverCleared();

            hoverReporter.reportHoverChangeTo(buildHoveredCell(FIRST_ROW));

            assertThat(reportedCells)
                .containsExactly(0, NO_CELL_REPORTED, 0);
        }
    }

    // One frame's reading with the pointer on a cell of this case's own host - what the panel's walk would
    // have built, with the kind of thing reached left at the whole-row answer since nothing here reads it.
    private HoveredBodyCell buildHoveredCell(BodyCellSlot slot) {
        return new HoveredBodyCell(
            slot,
            PointerArrivalTarget.SINGLE_OPTION_CONTROL,
            reportedCells::add);
    }
}
