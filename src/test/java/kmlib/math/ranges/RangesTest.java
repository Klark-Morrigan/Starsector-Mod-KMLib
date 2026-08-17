package kmlib.math.ranges;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link Ranges#clampToUnit}: a fraction already within is unchanged,
 * one below or above is pulled to the nearer end, and both ends are themselves allowed.
 *
 * <p>And of {@link Ranges#clampInto}: a value within the bounds is unchanged, one outside is
 * pulled to the bound it passed, and an EMPTY range - a floor pushed past its ceiling by two
 * constraints that do not overlap - collapses to whichever end is nearer rather than handing
 * back the ceiling regardless, which would silently prefer one of the two constraints.
 */
final class RangesTest {

    @Nested
    class ClampToUnit {

        @Test
        void a_fraction_within_the_unit_range_is_left_where_it_is() {
            assertThat(Ranges.clampToUnit(0.4f))
                .isEqualTo(0.4f);
        }

        @Test
        void a_fraction_below_the_range_is_pulled_up_to_its_floor() {
            assertThat(Ranges.clampToUnit(-0.5f))
                .isEqualTo(0f);
        }

        @Test
        void a_fraction_above_the_range_is_pulled_down_to_its_ceiling() {
            assertThat(Ranges.clampToUnit(1.5f))
                .isEqualTo(1f);
        }

        @Test
        void the_floor_itself_is_allowed() {
            assertThat(Ranges.clampToUnit(0f))
                .isEqualTo(0f);
        }

        @Test
        void the_ceiling_itself_is_allowed() {
            assertThat(Ranges.clampToUnit(1f))
                .isEqualTo(1f);
        }
    }

    @Nested
    class ClampInto {

        @Test
        void a_value_between_the_bounds_is_left_where_it_is() {
            assertThat(Ranges.clampInto(5.0, 1.0, 10.0))
                .isEqualTo(5.0);
        }

        @Test
        void a_value_below_the_floor_is_pulled_up_to_it() {
            assertThat(Ranges.clampInto(-3.0, 1.0, 10.0))
                .isEqualTo(1.0);
        }

        @Test
        void a_value_above_the_ceiling_is_pulled_down_to_it() {
            assertThat(Ranges.clampInto(30.0, 1.0, 10.0))
                .isEqualTo(10.0);
        }

        @Test
        void either_bound_is_itself_allowed() {

            assertThat(Ranges.clampInto(1.0, 1.0, 10.0))
                .isEqualTo(1.0);
            assertThat(Ranges.clampInto(10.0, 1.0, 10.0))
                .isEqualTo(10.0);
        }

        @Test
        void a_range_of_no_width_leaves_only_the_one_value_it_allows() {
            assertThat(Ranges.clampInto(5.0, 7.0, 7.0))
                .isEqualTo(7.0);
        }

        @Test
        void an_empty_range_gives_the_floor_when_the_value_is_nearer_to_it() {
            // The floor has been pushed past the ceiling: 10 is wanted at least, 4 at most.
            // A value of 9 is one away from the floor and five from the ceiling.
            assertThat(Ranges.clampInto(9.0, 10.0, 4.0))
                .isEqualTo(10.0);
        }

        @Test
        void an_empty_range_gives_the_ceiling_when_the_value_is_nearer_to_that() {
            // The same impossible range, asked with a value down at the ceiling's end.
            assertThat(Ranges.clampInto(5.0, 10.0, 4.0))
                .isEqualTo(4.0);
        }

        @Test
        void an_empty_range_prefers_the_floor_when_the_value_sits_exactly_between() {
            // Seven is three from each end. The tie goes to the floor, so an equidistant
            // value is answered the same way every time rather than by rounding.
            assertThat(Ranges.clampInto(7.0, 10.0, 4.0))
                .isEqualTo(10.0);
        }
    }
}
