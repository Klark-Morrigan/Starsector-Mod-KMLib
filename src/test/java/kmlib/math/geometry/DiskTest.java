package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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

    private static final double[] CENTRE = {5, 5};

    @Nested
    class Construct {

        @Test
        void construct_rejects_a_null_centre() {
            assertThatThrownBy(() -> new Disk(null, 10, 8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("centre");
        }

        @Test
        void construct_rejects_a_centre_of_fewer_than_two_coordinates() {
            assertThatThrownBy(() -> new Disk(new double[] {1}, 10, 8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("centre");
        }

        @Test
        void construct_rejects_a_segment_count_below_three() {
            assertThatThrownBy(() -> new Disk(CENTRE, 10, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("segments");
        }

        @Test
        void construct_accepts_the_lowest_segment_count_that_encloses_an_area() {
            assertThatCode(() -> new Disk(CENTRE, 10, 3))
                .doesNotThrowAnyException();
        }

        @Test
        void construct_accepts_a_radius_too_small_to_enclose_an_area() {
            // Legal by design: the clips read it as "withholds nothing", so a computed
            // radius needs no guard at the call site.
            assertThatCode(() -> new Disk(CENTRE, 0, 8))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    class CentreX {

        @Test
        void centre_x_reads_the_first_coordinate() {
            assertThat(new Disk(new double[] {3, 7}, 10, 8).centreX())
                .isEqualTo(3.0);
        }
    }

    @Nested
    class CentreY {

        @Test
        void centre_y_reads_the_second_coordinate() {
            assertThat(new Disk(new double[] {3, 7}, 10, 8).centreY())
                .isEqualTo(7.0);
        }
    }
}
