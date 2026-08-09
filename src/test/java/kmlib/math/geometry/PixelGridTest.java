package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins where a span has to sit to land on whole pixels, and the trap that makes the centred case worth a
 * rule of its own: an odd extent and an even one want their centres in different places, and snapping the
 * centre itself is right for exactly one of them.
 */
final class PixelGridTest {

    // Odd and even, so the two cases below cannot agree by coincidence.
    private static final float ODD_EXTENT = 9f;
    private static final float EVEN_EXTENT = 10f;

    private static final float TOLERANCE = 0.001f;

    @Nested
    class ComputeSnappedEdge {

        @Test
        void PixelGrid_computeSnappedEdge_movesAnEdgeToTheNearestWholePixel() {
            assertThat(PixelGrid.computeSnappedEdge(12.4f))
                .isCloseTo(12f, within(TOLERANCE));
            assertThat(PixelGrid.computeSnappedEdge(12.6f))
                .isCloseTo(13f, within(TOLERANCE));
        }

        @Test
        void PixelGrid_computeSnappedEdge_leavesAnEdgeAlreadyOnTheGridWhereItIs() {
            // The no-op case is the one every already-correct caller runs through, so a rule that nudged
            // it would move text that was landing right.
            assertThat(PixelGrid.computeSnappedEdge(12f))
                .isCloseTo(12f, within(TOLERANCE));
        }
    }

    @Nested
    class ComputeSnappedCentre {

        @Test
        void PixelGrid_computeSnappedCentre_putsAnOddSpansEdgesOnTheGridRatherThanItsCentre() {
            // The trap: a span of 9 wants its centre on a half pixel, since that is what puts its two
            // edges on whole ones. Snapping the centre would land this span at 12, its edges at 7.5 and
            // 16.5 - the very smear the rule exists to remove.
            assertThat(PixelGrid.computeSnappedCentre(12.1f, ODD_EXTENT))
                .isCloseTo(12.5f, within(TOLERANCE));
        }

        @Test
        void PixelGrid_computeSnappedCentre_putsAnEvenSpansCentreOnAWholePixel() {
            // The other parity, where the answer is the whole pixel - so the rule is read as "the edges
            // decide" rather than as "always add a half".
            assertThat(PixelGrid.computeSnappedCentre(12.4f, EVEN_EXTENT))
                .isCloseTo(12f, within(TOLERANCE));
        }

        @Test
        void PixelGrid_computeSnappedCentre_leavesASpanAlreadyOnTheGridWhereItIs() {
            assertThat(PixelGrid.computeSnappedCentre(12.5f, ODD_EXTENT))
                .isCloseTo(12.5f, within(TOLERANCE));
            assertThat(PixelGrid.computeSnappedCentre(12f, EVEN_EXTENT))
                .isCloseTo(12f, within(TOLERANCE));
        }
    }
}
