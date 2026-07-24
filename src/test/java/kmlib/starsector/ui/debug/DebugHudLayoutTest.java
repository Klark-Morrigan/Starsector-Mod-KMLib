package kmlib.starsector.ui.debug;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the corner layout maths - the part of the debug readout otherwise only checkable by eye:
 * each entry becomes a small key line above a larger body line, entries stack in push order, a left
 * corner right-aligns toward the centre while a right corner left-aligns, a top corner starts at the
 * top edge, and a bottom corner pins its block to the bottom edge.
 */
class DebugHudLayoutTest {

    private static final float SCREEN_WIDTH = 1600f;
    private static final float SCREEN_HEIGHT = 900f;

    private static final List<DebugHudEntry> TWO_ENTRIES = List.of(
            new DebugHudEntry("first-key", "first-body"),
            new DebugHudEntry("second-key", "second-body"));

    @Nested
    class LayOut {

        @Test
        void layOutMakesAKeyLineAboveABodyLinePerEntry() {
            var lines = DebugHudLayout.layOut(
                    DebugQuadrant.TOP_LEFT, TWO_ENTRIES, SCREEN_WIDTH, SCREEN_HEIGHT);

            // Two entries, each a key line then a body line, in push order.
            assertThat(lines).extracting(DebugHudLine::text)
                    .containsExactly("first-key", "first-body", "second-key", "second-body");
            // The key reads as a subtitle: smaller than the body and drawn above it.
            assertThat(lines.get(0).fontSize()).isLessThan(lines.get(1).fontSize());
            assertThat(lines.get(0).y()).isGreaterThan(lines.get(1).y());
            // The two lines of one entry are a different colour, so key and body are told apart.
            assertThat(lines.get(0).colour()).isNotEqualTo(lines.get(1).colour());
        }

        @Test
        void layOutStacksEveryLineDownwardInOrder() {
            var lines = DebugHudLayout.layOut(
                    DebugQuadrant.TOP_LEFT, TWO_ENTRIES, SCREEN_WIDTH, SCREEN_HEIGHT);

            // No line overlaps the one before it: y strictly decreases down the whole stack.
            for (var i = 1; i < lines.size(); i++) {
                assertThat(lines.get(i).y()).isLessThan(lines.get(i - 1).y());
            }
        }

        @Test
        void layOutRightAlignsALeftCornerJustLeftOfCentre() {
            var lines = DebugHudLayout.layOut(
                    DebugQuadrant.BOTTOM_LEFT, TWO_ENTRIES, SCREEN_WIDTH, SCREEN_HEIGHT);

            assertThat(lines).allSatisfy(line -> {
                assertThat(line.isRightAligned()).isTrue();
                assertThat(line.x()).isLessThan(SCREEN_WIDTH / 2f);
            });
        }

        @Test
        void layOutLeftAlignsARightCornerJustRightOfCentre() {
            var lines = DebugHudLayout.layOut(
                    DebugQuadrant.TOP_RIGHT, TWO_ENTRIES, SCREEN_WIDTH, SCREEN_HEIGHT);

            assertThat(lines).allSatisfy(line -> {
                assertThat(line.isRightAligned()).isFalse();
                assertThat(line.x()).isGreaterThan(SCREEN_WIDTH / 2f);
            });
        }

        @Test
        void layOutStartsATopCornerNearTheTopEdge() {
            var lines = DebugHudLayout.layOut(
                    DebugQuadrant.TOP_RIGHT, TWO_ENTRIES, SCREEN_WIDTH, SCREEN_HEIGHT);

            // The first line sits within a line-height of the top edge, above the vertical centre.
            assertThat(lines.get(0).y()).isGreaterThan(SCREEN_HEIGHT / 2f);
            assertThat(lines.get(0).y()).isLessThan(SCREEN_HEIGHT);
        }

        @Test
        void layOutPinsABottomCornerBlockToTheBottomEdge() {
            var lines = DebugHudLayout.layOut(
                    DebugQuadrant.BOTTOM_RIGHT, TWO_ENTRIES, SCREEN_WIDTH, SCREEN_HEIGHT);

            // The last line sits low, near the bottom edge and below the vertical centre - the block
            // grew upward from the bottom rather than down from the top.
            var lastLine = lines.get(lines.size() - 1);
            assertThat(lastLine.y()).isLessThan(SCREEN_HEIGHT / 2f);
            assertThat(lastLine.y()).isGreaterThan(0f);
        }

        @Test
        void layOutHasNothingToPlaceForNoEntries() {
            assertThat(DebugHudLayout.layOut(
                    DebugQuadrant.TOP_LEFT, List.of(), SCREEN_WIDTH, SCREEN_HEIGHT))
                    .isEmpty();
        }
    }
}
