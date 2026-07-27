package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.BoxEdge;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link BoxBorder}: what one edge reserves follows from whether that edge is stroked, so a layout
 * growing a box outward and a renderer insetting content inward cannot disagree about how much room the
 * frame takes.
 */
final class BoxBorderTest {
    private static final float WIDTH = 4f;

    @Nested
    class ComputeEdgeInset {

        @Test
        void computeEdgeInsetReservesTheWidthOnAStrokedEdge() {
            assertThat(new BoxBorder(WIDTH).computeEdgeInset(BoxEdge.LEFT)).isEqualTo(WIDTH);
        }

        @Test
        void computeEdgeInsetReservesNothingOnAnOpenEdge() {
            // The intel panel's flush side: the edge draws no border, so it reserves no strip either and the
            // content sits hard against whatever the box abuts.
            var border = new BoxBorder(WIDTH, EnumSet.of(BoxEdge.TOP, BoxEdge.RIGHT, BoxEdge.BOTTOM));
            assertThat(border.computeEdgeInset(BoxEdge.LEFT)).isZero();
            assertThat(border.computeEdgeInset(BoxEdge.TOP)).isEqualTo(WIDTH);
        }

        @Test
        void computeEdgeInsetReservesNothingWhenTheWidthIsZero() {
            // A zero-width frame strokes nothing, so even a listed edge costs no room - a box with no border
            // is its own content.
            assertThat(new BoxBorder(0f).computeEdgeInset(BoxEdge.TOP)).isZero();
        }

        @Test
        void computeEdgeInsetReservesNothingWhenNoEdgeIsStroked() {
            assertThat(new BoxBorder(WIDTH, Set.of()).computeEdgeInset(BoxEdge.BOTTOM)).isZero();
        }
    }

    @Nested
    class WidthOnlyConstructor {

        @Test
        void widthOnlyConstructorStrokesEveryEdge() {
            // The common case - a panel floating free, framed all round - so a caller naming no edges gets a
            // full frame rather than an unstroked one.
            var border = new BoxBorder(WIDTH);
            assertThat(border.edges()).isEqualTo(BoxEdge.ALL);
            for (var edge : BoxEdge.values()) {
                assertThat(border.computeEdgeInset(edge)).isEqualTo(WIDTH);
            }
        }
    }
}
