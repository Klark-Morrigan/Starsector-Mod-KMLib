package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static kmlib.math.geometry.GeometryTestSupport.buildReferenceSquare;
import static kmlib.math.geometry.GeometryTestSupport.buildSquare;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link PolygonSmoothing#roundCorners}: a corner becomes an
 * arc of {@code segmentsPerCorner + 1} points while straight edges are preserved,
 * the radius is clamped so it never spikes, a zero radius leaves the polygon
 * untouched, a corner sharper than the bevel threshold is chamfered flat instead
 * of rounded, and a corner flatter than the rounding threshold keeps its vertex
 * verbatim.
 *
 * <p>And of {@link PolygonSmoothing#removeSpikes}: a sharp thin protrusion and an
 * equally sharp inward cusp are both spliced out, a sharp but tall peninsula is
 * kept (clears the height bar), a gently curved run is kept (clears the angle bar),
 * a non-positive threshold disables the pass, and a ring that is all sliver never
 * drops below three vertices.
 */
final class PolygonSmoothingTest {

    @Nested
    class RoundCorners {

        @Test
        void round_corners_arcs_each_corner_and_keeps_straight_edges() {
            // Side-100 square, radius 10, 3 segments/corner -> 4 corners x 4 points
            // = 16. The cut is a fixed 10 units, so the bottom edge stays straight
            // through its middle: the arc endpoints (10,0) and (90,0) lie on y = 0.
            var rounded = PolygonSmoothing.roundCorners(
                buildSquare(100),
                new CornerRounding(10.0, 3, 0.0, CornerRounding.ROUND_EVERY_CORNER));

            assertThat(rounded)
                .hasSize(16);

            assertThat(rounded)
                .allMatch(vertex -> vertex[0] >= 0
                    && vertex[0] <= 100
                    && vertex[1] >= 0
                    && vertex[1] <= 100);

            // A straight-edge point survives untouched between the rounded corners.
            assertThat(rounded)
                .anyMatch(vertex -> Math.abs(vertex[1]) < 1e-6
                    && vertex[0] > 0
                    && vertex[0] < 100);
        }

        @Test
        void round_corners_clamps_an_oversized_radius_without_spiking() {
            // Radius far larger than the side: clamped to half the edge, so the
            // result still stays inside the square instead of overshooting.
            var rounded = PolygonSmoothing.roundCorners(
                buildSquare(100),
                new CornerRounding(10_000.0, 3, 0.0, CornerRounding.ROUND_EVERY_CORNER));

            assertThat(rounded)
                .allMatch(vertex -> vertex[0] >= 0
                    && vertex[0] <= 100
                    && vertex[1] >= 0
                    && vertex[1] <= 100);
        }

        @Test
        void round_corners_leaves_the_polygon_unchanged_for_zero_radius() {
            assertThat(PolygonSmoothing.roundCorners(
                    buildReferenceSquare(),
                    new CornerRounding(0.0, 3, 0.0, CornerRounding.ROUND_EVERY_CORNER)))
                .hasSize(4);
        }

        @Test
        void round_corners_leaves_the_polygon_unchanged_for_no_segments_per_corner() {
            // An arc sampled into no segments is not an arc; the pass declines rather
            // than replacing each corner with its two bare step-back points.
            assertThat(PolygonSmoothing.roundCorners(
                    buildReferenceSquare(),
                    new CornerRounding(2.0, 0, 0.0, CornerRounding.ROUND_EVERY_CORNER)))
                .hasSize(4);
        }

        @Test
        void round_corners_returns_the_input_below_three_vertices() {

            var segment = Arrays.asList(
                new double[] {0, 0},
                new double[] {10, 0});

            assertThat(PolygonSmoothing.roundCorners(
                    segment,
                    new CornerRounding(2.0, 3, 0.0, CornerRounding.ROUND_EVERY_CORNER)))
                .hasSize(2);
        }

        @Test
        void round_corners_chords_a_straight_through_corner() {
            // (5,0) sits mid-edge, so its two step-back points and the corner are
            // collinear and no arc centre exists. The pass falls back to the chord
            // between those points instead of dividing through the degenerate centre.
            var squareWithMidEdgeVertex = Arrays.asList(
                new double[] {0, 0},
                new double[] {5, 0},
                new double[] {10, 0},
                new double[] {10, 10},
                new double[] {0, 10});

            var rounded = PolygonSmoothing.roundCorners(
                squareWithMidEdgeVertex,
                new CornerRounding(1.0, 3, 0.0, CornerRounding.ROUND_EVERY_CORNER));

            // Four real corners arc into 3 + 1 points each; the straight-through one
            // contributes its two step-back points alone. 16 + 2 = 18.
            assertThat(rounded)
                .hasSize(18);
            assertThat(rounded)
                .allMatch(vertex -> vertex[1] >= -1e-6);
        }

        @Test
        void round_corners_chamfers_a_corner_sharper_than_the_threshold() {
            // Thin CCW triangle: the apex at the origin spans ~5.7 deg, well below
            // the 45 deg threshold, so it is chamfered flat (its two step-back
            // points only) while the two near-90 deg corners stay rounded into
            // 3 + 1 points each. 2 + 4 + 4 = 10.
            var triangle = Arrays.asList(
                new double[] {0, 0},
                new double[] {100, 0},
                new double[] {100, 10});

            var rounded = PolygonSmoothing.roundCorners(
                triangle,
                new CornerRounding(
                    5.0, 3, Math.toRadians(45), CornerRounding.ROUND_EVERY_CORNER));

            assertThat(rounded)
                .hasSize(10);

            // The spike is gone: no vertex pinches back toward the apex the way a
            // bezier arc would. Both chamfer points sit ~5 units (the cut) out.
            assertThat(rounded)
                .allMatch(vertex -> Math.hypot(vertex[0], vertex[1]) >= 4.0);
        }

        @Test
        void round_corners_arcs_a_reflex_corner_into_the_concavity() {
            // CCW L-shape with a reflex corner at (10,10). The arc there must bulge
            // toward the notch (the corner apex), not fly outside the shape: with
            // radius 3 it steps back to (13,10) and (10,13) and curves through about
            // (10.9,10.9). Six corners arced at 3 + 1 points each = 24.
            var lShape = Arrays.asList(
                new double[] {0, 0},
                new double[] {30, 0},
                new double[] {30, 10},
                new double[] {10, 10},
                new double[] {10, 30},
                new double[] {0, 30});

            var rounded = PolygonSmoothing.roundCorners(
                lShape,
                new CornerRounding(3.0, 3, 0.0, CornerRounding.ROUND_EVERY_CORNER));

            assertThat(rounded)
                .hasSize(24);

            assertThat(rounded)
                .allMatch(vertex -> vertex[0] >= -1e-6
                    && vertex[0] <= 30 + 1e-6
                    && vertex[1] >= -1e-6
                    && vertex[1] <= 30 + 1e-6);

            // The reflex arc bulges into the notch rather than cutting across it.
            assertThat(rounded)
                .anyMatch(vertex -> vertex[0] > 10
                    && vertex[0] < 12
                    && vertex[1] > 10
                    && vertex[1] < 12);
        }

        @Test
        void round_corners_keeps_a_corner_flatter_than_the_rounding_threshold() {
            // A square's corners span 90 deg, flatter than the 45 deg rounding
            // threshold, so every one keeps its vertex verbatim: same four points.
            var rounded = PolygonSmoothing.roundCorners(
                buildSquare(100),
                new CornerRounding(10.0, 3, 0.0, Math.toRadians(45)));

            assertThat(rounded)
                .hasSize(4);
        }

        @Test
        void round_corners_rounds_only_a_corner_sharper_than_the_rounding_threshold() {
            // Thin CCW triangle: the apex at the origin spans ~5.7 deg and is the
            // only corner under the 45 deg rounding threshold, so it alone arcs
            // into 3 + 1 points while the other two corners pass through verbatim.
            // 4 + 1 + 1 = 6.
            var triangle = Arrays.asList(
                new double[] {0, 0},
                new double[] {100, 0},
                new double[] {100, 10});

            var rounded = PolygonSmoothing.roundCorners(
                triangle,
                new CornerRounding(5.0, 3, 0.0, Math.toRadians(45)));

            assertThat(rounded)
                .hasSize(6);

            // The flat corners survive untouched, coordinates and all.
            assertThat(rounded)
                .anyMatch(vertex -> vertex[0] == 100 && vertex[1] == 0);
            assertThat(rounded)
                .anyMatch(vertex -> vertex[0] == 100 && vertex[1] == 10);
        }

        @Test
        void round_corners_rounds_nothing_for_a_non_positive_rounding_threshold() {
            // No interior angle sits below zero, so a threshold there switches the
            // pass off corner by corner: the square comes back as its four points.
            var rounded = PolygonSmoothing.roundCorners(
                buildSquare(100),
                new CornerRounding(10.0, 3, 0.0, 0.0));

            assertThat(rounded)
                .hasSize(4);
        }
    }

    @Nested
    class RemoveSpikes {

        // Sharp enough that only a genuine needle/cusp qualifies; a square's right
        // angles (90 deg) stay well clear.
        private static final double MAX_CORNER_ANGLE = Math.toRadians(45);

        @Test
        void remove_spikes_splices_out_an_outward_needle() {
            // A side-100 square whose top edge is interrupted by a thin needle poking
            // up to (50,108): a sharp corner (well under 45 deg) rising only 8 above
            // the y = 100 chord. With the height bar at 20 it is spliced out, leaving
            // the four square corners.
            var squareWithNeedle = Arrays.asList(
                new double[] {0, 0},
                new double[] {100, 0},
                new double[] {100, 100},
                new double[] {51, 100},
                new double[] {50, 108},
                new double[] {49, 100},
                new double[] {0, 100});

            var cleaned = PolygonSmoothing.removeSpikes(
                squareWithNeedle,
                20.0,
                MAX_CORNER_ANGLE);

            // The needle apex is gone: nothing rises above the top edge. Splicing the
            // apex leaves its two shoulders collinear on y = 100 (this pass sands
            // spikes, not redundant-collinear points), so the count drops but not to 4.
            assertThat(cleaned)
                .noneMatch(vertex -> vertex[1] > 100 + 1e-6);
            assertThat(cleaned)
                .hasSizeLessThan(7);
        }

        @Test
        void remove_spikes_splices_out_an_inward_cusp() {
            // The same square with a sharp nick biting DOWN to (50,92) from the top
            // edge - an inward cusp of the same 8-unit depth. Because the interior
            // angle is unsigned, it is treated like the outward needle and removed.
            var squareWithCusp = Arrays.asList(
                new double[] {0, 0},
                new double[] {100, 0},
                new double[] {100, 100},
                new double[] {51, 100},
                new double[] {50, 92},
                new double[] {49, 100},
                new double[] {0, 100});

            var cleaned = PolygonSmoothing.removeSpikes(
                squareWithCusp,
                20.0,
                MAX_CORNER_ANGLE);

            // The inward nick apex at (50,92) is gone. As with the needle the spliced
            // shoulders stay collinear on y = 100, so the count drops short of 4.
            assertThat(cleaned)
                .noneMatch(vertex -> Math.abs(vertex[0] - 50) < 1e-6 && Math.abs(vertex[1] - 92) < 1e-6);
            assertThat(cleaned)
                .hasSizeLessThan(7);
        }

        @Test
        void remove_spikes_keeps_a_sharp_but_tall_peninsula() {
            // A sharp corner that juts far is real shape, not a sliver: the apex at
            // (50,160) rises 60 above the y = 100 chord, past the 20 height bar, so
            // even though it is sharp it survives.
            var squareWithPeninsula = Arrays.asList(
                new double[] {0, 0},
                new double[] {100, 0},
                new double[] {100, 100},
                new double[] {60, 100},
                new double[] {50, 160},
                new double[] {40, 100},
                new double[] {0, 100});

            var cleaned = PolygonSmoothing.removeSpikes(
                squareWithPeninsula,
                20.0,
                MAX_CORNER_ANGLE);

            assertThat(cleaned)
                .anyMatch(vertex -> vertex[1] > 150);
        }

        @Test
        void remove_spikes_keeps_a_gently_curved_run() {
            // A shallow bump only 8 above the chord but spread wide, so its corner is
            // near-straight (well over 45 deg). It clears the angle bar and stays even
            // though it is under the height bar - the pass sands slivers, not curves.
            var squareWithBump = Arrays.asList(
                new double[] {0, 0},
                new double[] {100, 0},
                new double[] {100, 100},
                new double[] {70, 100},
                new double[] {50, 108},
                new double[] {30, 100},
                new double[] {0, 100});

            var cleaned = PolygonSmoothing.removeSpikes(
                squareWithBump,
                20.0,
                MAX_CORNER_ANGLE);

            assertThat(cleaned)
                .anyMatch(vertex -> vertex[1] > 100 + 1e-6);
        }

        @Test
        void remove_spikes_leaves_the_polygon_unchanged_for_a_non_positive_threshold() {

            var squareWithNeedle = Arrays.asList(
                new double[] {0, 0},
                new double[] {100, 0},
                new double[] {100, 100},
                new double[] {51, 100},
                new double[] {50, 108},
                new double[] {49, 100},
                new double[] {0, 100});

            assertThat(PolygonSmoothing.removeSpikes(squareWithNeedle, 0.0, MAX_CORNER_ANGLE))
                .hasSize(7);
            assertThat(PolygonSmoothing.removeSpikes(squareWithNeedle, 20.0, 0.0))
                .hasSize(7);
        }

        @Test
        void remove_spikes_never_drops_below_three_vertices() {
            // A degenerate sliver triangle (all corners sharp and thin): the pass must
            // not eat it down to a line - it stops at three so the caller's own area
            // check discards it.
            var sliver = Arrays.asList(
                new double[] {0, 0},
                new double[] {100, 1},
                new double[] {50, 2});

            assertThat(PolygonSmoothing.removeSpikes(sliver, 20.0, MAX_CORNER_ANGLE))
                .hasSize(3);
        }
    }
}
