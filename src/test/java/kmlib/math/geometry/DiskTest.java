package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins {@link Disk}'s construction invariant: a disk that exists is one the clips can
 * walk the edges of, so a segment count too low to enclose an area is rejected at
 * construction rather than at each clip, as is a centre that is not an {x, y} pair.
 *
 * <p>And that a radius too small to enclose an area is deliberately <em>not</em>
 * rejected: a caller computing a radius from live state would otherwise have to
 * pre-check it, where the clips already answer "withholds nothing" for it.
 */
final class DiskTest {

    // How far two readings of one length may differ and still be the same length. The gap is
    // one cosine off the radius, so this is arithmetic rather than room the shapes need.
    private static final double SAME_LENGTH = 1e-9;

    private static final double[] CENTRE = {5, 5};

    @Nested
    class Construct {

        @Test
        void constructRejectsANullCentre() {
            assertThatThrownBy(() -> new Disk(null, 10, 8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("centre");
        }

        @Test
        void constructRejectsACentreOfFewerThanTwoCoordinates() {
            assertThatThrownBy(() -> new Disk(new double[] {1}, 10, 8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("centre");
        }

        @Test
        void constructRejectsASegmentCountBelowThree() {
            assertThatThrownBy(() -> new Disk(CENTRE, 10, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("segments");
        }

        @Test
        void constructAcceptsTheLowestSegmentCountThatEnclosesAnArea() {
            assertThatCode(() -> new Disk(CENTRE, 10, 3))
                .doesNotThrowAnyException();
        }

        @Test
        void constructAcceptsARadiusTooSmallToEncloseAnArea() {
            // Legal by design: the clips read it as "withholds nothing", so a computed
            // radius needs no guard at the call site.
            assertThatCode(() -> new Disk(CENTRE, 0, 8))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    class MeasureSagitta {

        // A round radius, so each expected gap below is the radius times a cosine a reader can
        // look up rather than a number only this code produces.
        private static final double RADIUS = 100;

        @Test
        void theGapIsTheRadiusLessItsCosineOverHalfAStep() {
            // Four sides: each chord spans a quarter turn, so its middle falls short of the
            // arc by the radius less its cosine over an eighth of a turn.
            assertThat(Disk.measureSagitta(RADIUS, 4))
                .isCloseTo(29.289321881345245, within(SAME_LENGTH));
        }

        @Test
        void moreSidesLeaveASmallerGap() {

            assertThat(Disk.measureSagitta(RADIUS, 6))
                .isCloseTo(13.397459621556138, within(SAME_LENGTH));
        }

        @Test
        void theGapGrowsWithTheRadius() {
            // A share of the radius rather than a length of its own, which is why a consumer
            // welding by it has to read it off the disk it is welding rather than carry a
            // number that was right at some other reach.
            assertThat(Disk.measureSagitta(2 * RADIUS, 4))
                .isCloseTo(58.57864376269049, within(SAME_LENGTH));
        }
    }

    @Nested
    class CentreX {

        @Test
        void centreXReadsTheFirstCoordinate() {
            assertThat(new Disk(new double[] {3, 7}, 10, 8).centreX())
                .isEqualTo(3.0);
        }
    }

    @Nested
    class CentreY {

        @Test
        void centreYReadsTheSecondCoordinate() {
            assertThat(new Disk(new double[] {3, 7}, 10, 8).centreY())
                .isEqualTo(7.0);
        }
    }
}
