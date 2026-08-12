package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static kmlib.math.geometry.GeometryTestSupport.buildAssertionSlack;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link RegionChord#toSegment}: the step that turns a fitted parameter span back
 * into world coordinates puts the segment's ends at {@code origin + t * direction}, so
 * parameter zero lands on the origin, a negative parameter lands behind it, and a
 * diagonal direction carries both coordinates.
 */
class RegionChordTest {

    @Nested
    class ToSegment {
        @Test
        void toSegmentPlacesTheEndsAtTheParametersMeasuredFromTheOrigin() {
            // Origin (10, 20) running along +x: t=2 and t=5 are 2 and 5 units past it.
            var segment = computeChordAlong(new DirectedLine(10, 20, 1, 0))
                .toSegment(new double[] {2, 5});

            assertThat(segment.startX())
                .isCloseTo(12.0, buildAssertionSlack());
            assertThat(segment.startY())
                .isCloseTo(20.0, buildAssertionSlack());
            assertThat(segment.endX())
                .isCloseTo(15.0, buildAssertionSlack());
            assertThat(segment.endY())
                .isCloseTo(20.0, buildAssertionSlack());
        }

        @Test
        void toSegmentPlacesANegativeParameterBehindTheOrigin() {
            // The origin is parameter zero, not the span's start, so a span straddling
            // it reaches back along the direction rather than clamping at it.
            var segment = computeChordAlong(new DirectedLine(0, 0, 1, 0))
                .toSegment(new double[] {-4, 4});

            assertThat(segment.startX())
                .isCloseTo(-4.0, buildAssertionSlack());
            assertThat(segment.endX())
                .isCloseTo(4.0, buildAssertionSlack());
        }

        @Test
        void toSegmentCarriesBothCoordinatesForADiagonalDirection() {
            // Ten units along the 3-4-5 unit direction lands at (6, 8).
            var segment = computeChordAlong(new DirectedLine(0, 0, 0.6, 0.8))
                .toSegment(new double[] {0, 10});

            assertThat(segment.startX())
                .isCloseTo(0.0, buildAssertionSlack());
            assertThat(segment.startY())
                .isCloseTo(0.0, buildAssertionSlack());
            assertThat(segment.endX())
                .isCloseTo(6.0, buildAssertionSlack());
            assertThat(segment.endY())
                .isCloseTo(8.0, buildAssertionSlack());
        }
    }

    // A chord along the given line against no region at all: the projection reads only
    // the line, so the rings and keep-outs a chord also carries play no part in it.
    private static RegionChord computeChordAlong(DirectedLine line) {
        return new RegionChord(List.of(), List.of(), line);
    }
}
