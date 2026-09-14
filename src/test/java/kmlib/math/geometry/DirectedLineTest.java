package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.math.geometry.GeometryTestSupport.buildAssertionSlack;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link DirectedLine#toUnitLine}: it scales the direction to unit length without
 * moving the origin, so a parameter along the result reads as a world distance, and it
 * reports a direction too short to define a line as null rather than dividing by a
 * near-zero length. Every operation measuring parameters along a line normalises through
 * it, so the two verdicts are pinned here once instead of in each of those suites.
 */
class DirectedLineTest {

    @Nested
    class ToUnitLine {

        @Test
        void toUnitLineScalesTheDirectionToUnitLengthAndLeavesTheOriginWhereItWas() {
            // The 3-4-5 direction has length 5, so unit length is (0.6, 0.8); the origin
            // is parameter zero either way and must not shift with the rescaling.
            var unitLine = new DirectedLine(10, -5, 3, 4)
                .toUnitLine();

            assertThat(unitLine.originX())
                .isCloseTo(10.0, buildAssertionSlack());
            assertThat(unitLine.originY())
                .isCloseTo(-5.0, buildAssertionSlack());
            assertThat(unitLine.directionX())
                .isCloseTo(0.6, buildAssertionSlack());
            assertThat(unitLine.directionY())
                .isCloseTo(0.8, buildAssertionSlack());
        }

        @Test
        void toUnitLineLeavesAnAlreadyUnitDirectionAsItIs() {
            var unitLine = new DirectedLine(0, 0, 0, 1)
                .toUnitLine();

            assertThat(unitLine.directionX())
                .isCloseTo(0.0, buildAssertionSlack());
            assertThat(unitLine.directionY())
                .isCloseTo(1.0, buildAssertionSlack());
        }

        @Test
        void toUnitLineIsNullForAZeroDirection() {
            // No direction at all is no line: there is nothing to measure parameters
            // along, which callers read as "skip this line" rather than as a zero span.
            assertThat(new DirectedLine(0, 0, 0, 0).toUnitLine())
                .isNull();
        }

        @Test
        void toUnitLineIsNullForADirectionShorterThanTheMinimumEdgeLength() {
            // Below the shared degenerate threshold the direction is noise, and dividing
            // by its length would amplify that noise into the parameter frame.
            assertThat(new DirectedLine(0, 0, 1e-9, 0).toUnitLine())
                .isNull();
        }
    }
}
