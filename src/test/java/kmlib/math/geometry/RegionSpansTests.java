package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static kmlib.math.geometry.GeometryTestSupport.buildAssertionSlack;
import static kmlib.math.geometry.GeometryTestSupport.buildSquare;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link RegionSpans#findLineInteriorSpans}: a convex ring yields one
 * span, a concave ring the line exits and re-enters yields separate spans (no chord
 * bridges the notch), a hole splits a span around it, and a miss, a corner graze, or
 * a degenerate direction yields nothing.
 *
 * <p>And of {@link RegionSpans#findBandInteriorSpans}: a band clear of every
 * border keeps its whole span, a band splits around a parallel border its centreline
 * clears (its edge crosses the intrusion), a band wider than the region empties, a
 * zero-thickness band reduces to the line test, and a degenerate direction yields
 * nothing.
 */
final class RegionSpansTests {

    @Nested
    class FindLineInteriorSpans {

        @Test
        void interiorSpansYieldOneSpanAcrossAConvexRing() {
            // A horizontal line through the centre of the side-10 square enters at
            // x=0 and leaves at x=10: one span, parameters measured from the
            // through-point at x=5.
            var spans = RegionSpans.findLineInteriorSpans(
                List.of(buildSquare(10)),
                new DirectedLine(5, 5, 1, 0));

            assertThat(spans)
                .hasSize(1);
            assertThat(spans.get(0)[0])
                .isCloseTo(-5.0, buildAssertionSlack());
            assertThat(spans.get(0)[1])
                .isCloseTo(5.0, buildAssertionSlack());
        }

        @Test
        void interiorSpansSplitWhereTheLineExitsAndReentersAConcaveRing() {
            // A U shape (side-10 square with a notch cut down from the top between
            // x=4 and x=6): a horizontal line at y=7 crosses both arms but the
            // stretch between them lies in the notch, outside the region - so two
            // spans come back, never one chord bridging the gap.
            var uShape = Arrays.asList(
                new double[] {0, 0},
                new double[] {10, 0},
                new double[] {10, 10},
                new double[] {6, 10},
                new double[] {6, 4},
                new double[] {4, 4},
                new double[] {4, 10},
                new double[] {0, 10});

            var spans = RegionSpans.findLineInteriorSpans(
                List.of(uShape),
                new DirectedLine(5, 7, 1, 0));

            assertThat(spans)
                .hasSize(2);

            assertThat(spans.get(0)[0])
                .isCloseTo(-5.0, buildAssertionSlack());
            assertThat(spans.get(0)[1])
                .isCloseTo(-1.0, buildAssertionSlack());
            assertThat(spans.get(1)[0])
                .isCloseTo(1.0, buildAssertionSlack());
            assertThat(spans.get(1)[1])
                .isCloseTo(5.0, buildAssertionSlack());
        }

        @Test
        void interiorSpansSplitAroundAHoleRing() {
            // A side-20 square with a hole from x=8..12: the line through the middle
            // is interior only outside the hole, so the hole splits the single span
            // in two.
            var hole = Arrays.asList(
                new double[] {8, 8},
                new double[] {12, 8},
                new double[] {12, 12},
                new double[] {8, 12});

            var spans = RegionSpans.findLineInteriorSpans(
                List.of(
                    buildSquare(20), hole),
                new DirectedLine(10, 10, 1, 0));

            assertThat(spans)
                .hasSize(2);

            assertThat(spans.get(0)[0])
                .isCloseTo(-10.0, buildAssertionSlack());
            assertThat(spans.get(0)[1])
                .isCloseTo(-2.0, buildAssertionSlack());
            assertThat(spans.get(1)[0])
                .isCloseTo(2.0, buildAssertionSlack());
            assertThat(spans.get(1)[1])
                .isCloseTo(10.0, buildAssertionSlack());
        }

        @Test
        void interiorSpansAreEmptyWhenTheLineMissesTheRing() {

            var spans = RegionSpans.findLineInteriorSpans(
                List.of(buildSquare(10)),
                new DirectedLine(5, 50, 1, 0));

            assertThat(spans)
                .isEmpty();
        }

        @Test
        void interiorSpansAreEmptyWhenTheLineOnlyGrazesACorner() {
            // A diagonal through the square's corner touches at a single point: the
            // two crossings coincide, so the zero-length interval between them is no
            // span.
            var spans = RegionSpans.findLineInteriorSpans(
                List.of(buildSquare(10)),
                new DirectedLine(0, 0, 1, -1));

            assertThat(spans)
                .isEmpty();
        }

        @Test
        void interiorSpansAreEmptyForADegenerateDirection() {
            // A zero direction defines no line to cross, so there is nothing to span.
            var spans = RegionSpans.findLineInteriorSpans(
                List.of(buildSquare(10)),
                new DirectedLine(5, 5, 0, 0));

            assertThat(spans)
                .isEmpty();
        }
    }

    @Nested
    class FindBandInteriorSpans {

        @Test
        void bandInsideAConvexRingKeepsItsWholeSpan() {
            // A horizontal band through the side-20 square, its rails from y=7 to
            // y=13, stays clear of the vertical walls, so the band span equals the
            // line span: x=0..20, parameters from the through-point at x=10.
            var spans = RegionSpans.findBandInteriorSpans(
                List.of(buildSquare(20)),
                new DirectedLine(10, 10, 1, 0), 3);

            assertThat(spans)
                .hasSize(1);

            assertThat(spans.get(0)[0])
                .isCloseTo(-10.0, buildAssertionSlack());
            assertThat(spans.get(0)[1])
                .isCloseTo(10.0, buildAssertionSlack());
        }

        @Test
        void bandSplitsAroundAParallelBorderTheCentrelineClears() {
            // A 20-wide, 10-tall room with a rectangular notch bitten down from the
            // top to y=6 over x=8..12. A horizontal centreline at y=5 clears the notch
            // whole, but a band of half-thickness 2.5 lifts its upper rails to y=6.25
            // and y=7.5 - inside the bite over x=8..12 - so the band span splits into
            // the two clear stretches [-10, -2] and [2, 10] the centreline alone missed.
            var notchedRoom = Arrays.asList(
                new double[] {0, 0},
                new double[] {20, 0},
                new double[] {20, 10},
                new double[] {12, 10},
                new double[] {12, 6},
                new double[] {8, 6},
                new double[] {8, 10},
                new double[] {0, 10});

            var spans = RegionSpans.findBandInteriorSpans(
                List.of(notchedRoom),
                new DirectedLine(10, 5, 1, 0), 2.5);

            assertThat(spans)
                .hasSize(2);

            assertThat(spans.get(0)[0])
                .isCloseTo(-10.0, buildAssertionSlack());
            assertThat(spans.get(0)[1])
                .isCloseTo(-2.0, buildAssertionSlack());
            assertThat(spans.get(1)[0])
                .isCloseTo(2.0, buildAssertionSlack());
            assertThat(spans.get(1)[1])
                .isCloseTo(10.0, buildAssertionSlack());
        }

        @Test
        void bandIsEmptyWhenWiderThanTheRegion() {
            // Half-thickness 6 in a side-10 square lifts the outer rails to y=-1 and
            // y=11, both outside; a rail with no interior empties the whole band.
            var spans = RegionSpans.findBandInteriorSpans(List.of(
                buildSquare(10)),
                new DirectedLine(5, 5, 1, 0), 6);

            assertThat(spans)
                .isEmpty();
        }

        @Test
        void bandWithZeroHalfThicknessMatchesTheLineTest() {
            // Every rail collapses onto the centreline, so the band test reduces
            // exactly to the line test on the same concave U shape.
            var uShape = Arrays.asList(
                new double[] {0, 0},
                new double[] {10, 0},
                new double[] {10, 10},
                new double[] {6, 10},
                new double[] {6, 4},
                new double[] {4, 4},
                new double[] {4, 10},
                new double[] {0, 10});

            var bandSpans = RegionSpans.findBandInteriorSpans(
                List.of(uShape),
                new DirectedLine(5, 7, 1, 0), 0);

            var lineSpans = RegionSpans.findLineInteriorSpans(
                List.of(uShape),
                new DirectedLine(5, 7, 1, 0));

            assertThat(bandSpans)
                .hasSize(lineSpans.size());

            for (var i = 0; i < lineSpans.size(); i++) {

                assertThat(bandSpans.get(i)[0])
                    .isCloseTo(lineSpans.get(i)[0], buildAssertionSlack());

                assertThat(bandSpans.get(i)[1])
                    .isCloseTo(lineSpans.get(i)[1], buildAssertionSlack());
            }
        }

        @Test
        void bandIsEmptyForADegenerateDirection() {

            var spans = RegionSpans.findBandInteriorSpans(
                List.of(buildSquare(10)),
                new DirectedLine(5, 5, 0, 0), 2);

            assertThat(spans)
                .isEmpty();
        }
    }
}
