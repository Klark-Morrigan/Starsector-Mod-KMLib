package kmlib.starsector.ui.render.gl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link ScissorBox#intersectWith}: composing a clip narrows it to the region shared with the box it
 * sits within, so an inner element (a scrolling list's viewport) cannot draw past an outer clip (a
 * collapsing panel's box), and a non-overlap collapses to a zero-extent box that clips everything out.
 */
final class ScissorBoxTest {

    @Nested
    class IntersectWith {
        @Test
        void narrows_to_the_outer_box_that_bounds_an_overhanging_inner_box() {
            var outer = new ScissorBox(0, 0, 100, 40);
            var inner = new ScissorBox(20, 10, 200, 200);
            assertThat(outer.intersectWith(inner)).isEqualTo(new ScissorBox(20, 10, 80, 30));
        }

        @Test
        void keeps_an_inner_box_wholly_inside_the_outer_box_unchanged() {
            var outer = new ScissorBox(0, 0, 100, 100);
            var inner = new ScissorBox(10, 20, 30, 40);
            assertThat(outer.intersectWith(inner)).isEqualTo(new ScissorBox(10, 20, 30, 40));
        }

        @Test
        void narrows_to_the_outer_box_when_the_inner_box_wholly_contains_it() {
            var outer = new ScissorBox(30, 40, 20, 20);
            var inner = new ScissorBox(0, 0, 200, 200);
            assertThat(outer.intersectWith(inner)).isEqualTo(new ScissorBox(30, 40, 20, 20));
        }

        @Test
        void collapses_to_a_zero_extent_box_when_the_boxes_do_not_overlap() {
            var outer = new ScissorBox(0, 0, 50, 50);
            var inner = new ScissorBox(200, 200, 30, 30);
            assertThat(outer.intersectWith(inner)).isEqualTo(new ScissorBox(200, 200, 0, 0));
        }

        @Test
        void yields_a_zero_width_box_when_a_docked_inner_box_shares_only_an_edge() {
            var outer = new ScissorBox(0, 0, 20, 100);
            var inner = new ScissorBox(20, 0, 0, 100);
            assertThat(outer.intersectWith(inner)).isEqualTo(new ScissorBox(20, 0, 0, 100));
        }
    }
}
