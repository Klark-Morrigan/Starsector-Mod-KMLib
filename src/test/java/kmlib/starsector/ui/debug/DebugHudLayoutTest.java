package kmlib.starsector.ui.debug;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the corner layout maths for both placements - the part of the debug readout otherwise only
 * checkable by eye: each entry becomes a small key line above a larger body line stacking in push
 * order, a screen corner grows in from its edge with a padding, and a cursor corner fans out into
 * its diagonal from the cursor.
 */
class DebugHudLayoutTest {

    private static final float SCREEN_WIDTH = 1600f;
    private static final float SCREEN_HEIGHT = 900f;
    private static final float EDGE_PADDING = 30f;
    private static final float CURSOR_X = 700f;
    private static final float CURSOR_Y = 400f;

    private static final List<DebugHudEntry> TWO_ENTRIES = List.of(
            new DebugHudEntry("first-key", "first-body"),
            new DebugHudEntry("second-key", "second-body"));

    @Nested
    class LayOutAtCorner {

        @Test
        void layOutAtCornerMakesAKeyLineAboveABodyLinePerEntryStackingDown() {
            var lines = corner(DebugQuadrant.TOP_LEFT);

            assertThat(lines).extracting(DebugHudLine::text)
                    .containsExactly("first-key", "first-body", "second-key", "second-body");
            assertThat(lines.get(0).fontSize()).isLessThan(lines.get(1).fontSize());
            assertThat(lines.get(0).colour()).isNotEqualTo(lines.get(1).colour());
            for (var i = 1; i < lines.size(); i++) {
                assertThat(lines.get(i).y()).isLessThan(lines.get(i - 1).y());
            }
        }

        @Test
        void layOutAtCornerGrowsALeftCornerInFromTheLeftEdge() {
            assertThat(corner(DebugQuadrant.BOTTOM_LEFT)).allSatisfy(line -> {
                assertThat(line.isRightAligned()).isFalse();
                assertThat(line.x()).isEqualTo(EDGE_PADDING);
            });
        }

        @Test
        void layOutAtCornerGrowsARightCornerInFromTheRightEdge() {
            assertThat(corner(DebugQuadrant.TOP_RIGHT)).allSatisfy(line -> {
                assertThat(line.isRightAligned()).isTrue();
                assertThat(line.x()).isEqualTo(SCREEN_WIDTH - EDGE_PADDING);
            });
        }

        @Test
        void layOutAtCornerStartsATopCornerNearTheTopEdge() {
            assertThat(corner(DebugQuadrant.TOP_RIGHT).get(0).y())
                    .isEqualTo(SCREEN_HEIGHT - EDGE_PADDING);
        }

        @Test
        void layOutAtCornerPinsABottomCornerBlockAboveTheBottomEdge() {
            var lines = corner(DebugQuadrant.BOTTOM_RIGHT);
            var lastLine = lines.get(lines.size() - 1);

            assertThat(lastLine.y()).isGreaterThanOrEqualTo(EDGE_PADDING);
            assertThat(lastLine.y()).isLessThan(SCREEN_HEIGHT / 2f);
        }

        @Test
        void layOutAtCornerPushesTheBlockFurtherInAsPaddingGrows() {
            var near = DebugHudLayout.layOutAtCorner(
                    DebugQuadrant.TOP_LEFT, TWO_ENTRIES, SCREEN_WIDTH, SCREEN_HEIGHT, 10f);
            var far = DebugHudLayout.layOutAtCorner(
                    DebugQuadrant.TOP_LEFT, TWO_ENTRIES, SCREEN_WIDTH, SCREEN_HEIGHT, 60f);

            // More padding pushes the left corner rightward and its top downward, off the edges.
            assertThat(far.get(0).x()).isGreaterThan(near.get(0).x());
            assertThat(far.get(0).y()).isLessThan(near.get(0).y());
        }

        @Test
        void layOutAtCornerHasNothingToPlaceForNoEntries() {
            assertThat(DebugHudLayout.layOutAtCorner(
                    DebugQuadrant.TOP_LEFT, List.of(), SCREEN_WIDTH, SCREEN_HEIGHT, EDGE_PADDING))
                    .isEmpty();
        }

        private static List<DebugHudLine> corner(DebugQuadrant quadrant) {
            return DebugHudLayout.layOutAtCorner(
                    quadrant, TWO_ENTRIES, SCREEN_WIDTH, SCREEN_HEIGHT, EDGE_PADDING);
        }
    }

    @Nested
    class LayOutAroundCursor {

        @Test
        void layOutAroundCursorRightAlignsALeftQuadrantToTheCursorsLeft() {
            assertThat(cursor(DebugQuadrant.TOP_LEFT)).allSatisfy(line -> {
                assertThat(line.isRightAligned()).isTrue();
                assertThat(line.x()).isLessThan(CURSOR_X);
            });
        }

        @Test
        void layOutAroundCursorLeftAlignsARightQuadrantToTheCursorsRight() {
            assertThat(cursor(DebugQuadrant.BOTTOM_RIGHT)).allSatisfy(line -> {
                assertThat(line.isRightAligned()).isFalse();
                assertThat(line.x()).isGreaterThan(CURSOR_X);
            });
        }

        @Test
        void layOutAroundCursorStacksATopQuadrantAboveTheCursor() {
            assertThat(cursor(DebugQuadrant.TOP_RIGHT)).allSatisfy(
                    line -> assertThat(line.y()).isGreaterThan(CURSOR_Y));
        }

        @Test
        void layOutAroundCursorStacksABottomQuadrantBelowTheCursor() {
            assertThat(cursor(DebugQuadrant.BOTTOM_LEFT)).allSatisfy(
                    line -> assertThat(line.y()).isLessThan(CURSOR_Y));
        }

        private static List<DebugHudLine> cursor(DebugQuadrant quadrant) {
            return DebugHudLayout.layOutAroundCursor(quadrant, TWO_ENTRIES, CURSOR_X, CURSOR_Y);
        }
    }
}
