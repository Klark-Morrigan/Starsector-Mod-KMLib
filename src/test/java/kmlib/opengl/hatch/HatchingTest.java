package kmlib.opengl.hatch;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the hatch-clipping contract: a family of parallel lines spaced by the given
 * distance, clipped to each triangle of the soup and returned as a {@code GL_LINES} run
 * in the soup's own coordinate space. The right triangle {@code (0,0)-(4,0)-(0,4)} with
 * unit-spaced horizontal lines is the worked example the exact-value test reads against;
 * the rest fix the guards, the spacing response, and the line orientation.
 *
 * <p>Clipping and packing are separate: a line crossing several triangles is clipped once per
 * triangle and then merged, so what comes back breaks only where the region does. The two-triangle
 * strip is where that shows - one unbroken stroke over what the clip saw as two crossings - and
 * the join tolerance is what decides how near two crossings must be to count as one stroke.
 */
final class HatchingTest {

    @Nested
    class ComputeHatchRun {
        // A right triangle with legs on the axes: the level axis for a horizontal hatch is
        // its y extent (0..4), so unit spacing lays lines at y = 0, 1, 2, 3, with the apex
        // line at y = 4 collapsing to the corner and dropping out.
        private static final float[] RIGHT_TRIANGLE = {
            0f, 0f, 4f, 0f, 0f, 4f};

        // The same square split along its (0,0)-(4,4) diagonal into two triangles, so a
        // horizontal hatch line crosses both and each triangle answers for its own half.
        private static final float[] SPLIT_SQUARE = {
            0f, 0f, 4f, 0f, 4f, 4f,
            0f, 0f, 4f, 4f, 0f, 4f};

        // Two of those squares with clear space between them, so every line crossing the soup
        // crosses two separated stretches of the region, each itself split into two triangles. What the
        // merge has to get right on both counts at once: join inside a lobe, break between them. A
        // concave region presents the sink with exactly this.
        private static final float[] TWO_SPLIT_SQUARE_LOBES = {
            0f, 0f, 4f, 0f, 4f, 4f,
            0f, 0f, 4f, 4f, 0f, 4f,
            10f, 0f, 14f, 0f, 14f, 4f,
            10f, 0f, 14f, 4f, 10f, 4f};

        // The split square with a third triangle laid over the middle of it, so one line's spans
        // overlap rather than abut. A tessellation should not produce this, which is exactly why
        // the merge has to survive it: an overlap that pulled the merged stretch back to the
        // shorter span's end would truncate the stroke.
        private static final float[] OVERLAPPING_TRIANGLE_SOUP = {
            0f, 0f, 4f, 0f, 4f, 4f,
            0f, 0f, 4f, 4f, 0f, 4f,
            1f, 0f, 3f, 0f, 3f, 4f};

        // A rectangle as two triangles, and a lone triangle starting a thousandth of a unit past
        // its right edge - close enough to read as one stroke, far enough that no exact-abutment
        // test would join them. The gap is what the tolerance exists for, and the rectangle's own
        // internal join (whole numbers throughout) is what an exact join looks like beside it.
        private static final float[] NEARLY_ABUTTING_LOBES = {
            0f, 0f, 2f, 0f, 2f, 4f,
            0f, 0f, 2f, 4f, 0f, 4f,
            2.001f, 0f, 6.001f, 0f, 2.001f, 4f};

        // Wide enough to close the thousandth-unit gap above at unit spacing, and far too narrow
        // to close the six-unit gap between the two lobes.
        private static final double GENEROUS_JOIN_TOLERANCE = 0.01;

        // No tolerance at all: only crossings that coincide exactly still merge.
        private static final double NO_JOIN_TOLERANCE = 0;

        @Test
        void computeHatchRunYieldsAnEmptyRunForZeroSpacing() {
            assertThat(buildHatchRun(RIGHT_TRIANGLE, 0, 0, GENEROUS_JOIN_TOLERANCE).segments())
                .isEmpty();
        }

        @Test
        void computeHatchRunYieldsAnEmptyRunForNegativeSpacing() {
            assertThat(buildHatchRun(RIGHT_TRIANGLE, 0, -1, GENEROUS_JOIN_TOLERANCE).segments())
                .isEmpty();
        }

        @Test
        void computeHatchRunYieldsAnEmptyRunForASoupSmallerThanATriangle() {
            // Two vertices cannot form a triangle, so there is no area to hatch.
            var tooSmall = new float[] {0f, 0f, 1f, 0f};

            assertThat(buildHatchRun(tooSmall, 0, 1, GENEROUS_JOIN_TOLERANCE).segments())
                .isEmpty();
        }

        @Test
        void computeHatchRunClipsHorizontalLinesToARightTriangle() {
            // Horizontal lines (angle 0) at y = 0, 1, 2, 3; each clipped to the triangle's
            // narrowing width, and the apex line at y = 4 dropped as a corner-only touch. One
            // triangle, so every line crosses once and the merge has nothing to join.
            var run = buildHatchRun(RIGHT_TRIANGLE, 0, 1, GENEROUS_JOIN_TOLERANCE);

            assertThat(run.segments())
                .containsExactly(
                    0f, 0f, 4f, 0f,
                    0f, 1f, 3f, 1f,
                    0f, 2f, 2f, 2f,
                    0f, 3f, 1f, 3f);
        }

        @Test
        void computeHatchRunReturnsEachLineWholeAcrossAStrip() {
            // The strip's shared diagonal splits every interior line in two as it is clipped -
            // x = 1..4 from one triangle, x = 0..1 from the other - and the merge puts them back,
            // so five segments come back where the clip found eight crossings. The merged
            // endpoints are the outer ends of the clip, not anything the diagonal contributed, and
            // lines come out in ascending order whatever order the soup presented the triangles in.
            var run = buildHatchRun(SPLIT_SQUARE, 0, 1, GENEROUS_JOIN_TOLERANCE);

            assertThat(run.segments())
                .containsExactly(
                    0f, 0f, 4f, 0f,
                    0f, 1f, 4f, 1f,
                    0f, 2f, 4f, 2f,
                    0f, 3f, 4f, 3f,
                    0f, 4f, 4f, 4f);
        }

        @Test
        void computeHatchRunClosesASharedEdgeJoinExactly() {
            // The three interior lines each join across the shared diagonal, and every one of
            // them closes on crossings that came out of their two triangles identical - so on
            // this soup the tolerance is doing nothing at all. That is the reading the tolerance
            // has to earn, and it earns it only where this count is non-zero.
            var run = buildHatchRun(SPLIT_SQUARE, 0, 1, GENEROUS_JOIN_TOLERANCE);

            assertThat(run.joins())
                .isEqualTo(new HatchJoinTally(3, 0, 0, 0, Double.POSITIVE_INFINITY));
        }

        @Test
        void computeHatchRunBreaksBetweenLobesButJoinsInsideOne() {
            // Each lobe's own diagonal join closes; the six units of empty space between the
            // lobes does not, so every line comes back as two strokes rather than one spanning
            // an area the region does not cover.
            var run = buildHatchRun(TWO_SPLIT_SQUARE_LOBES, 0, 1, GENEROUS_JOIN_TOLERANCE);

            assertThat(run.segments())
                .containsExactly(
                    0f, 0f, 4f, 0f, 10f, 0f, 14f, 0f,
                    0f, 1f, 4f, 1f, 10f, 1f, 14f, 1f,
                    0f, 2f, 4f, 2f, 10f, 2f, 14f, 2f,
                    0f, 3f, 4f, 3f, 10f, 3f, 14f, 3f,
                    0f, 4f, 4f, 4f, 10f, 4f, 14f, 4f);
        }

        @Test
        void computeHatchRunKeepsTheLongerReachWhenTwoSpansOverlap() {
            // The overlaid triangle's spans all start after the square's and end before them, so a
            // merge that took the later span's end would cut every stroke short of the square's
            // right edge. The run is the square's own five full-width strokes, unchanged by an area
            // that was already covered.
            var run = buildHatchRun(OVERLAPPING_TRIANGLE_SOUP, 0, 1, GENEROUS_JOIN_TOLERANCE);

            assertThat(run.segments())
                .containsExactly(
                    0f, 0f, 4f, 0f,
                    0f, 1f, 4f, 1f,
                    0f, 2f, 4f, 2f,
                    0f, 3f, 4f, 3f,
                    0f, 4f, 4f, 4f);
        }

        @Test
        void computeHatchRunCountsAnOverlapApartFromAToleratedGap() {
            // Four of the joins close on spans that already overlapped, by as much as three units
            // - three hundred times the tolerance. They are counted as overlaps and leave the
            // widest tolerated gap at zero, because reading that magnitude back as the reach the
            // tolerance needed is what would set it from a gap it never had to close.
            var run = buildHatchRun(OVERLAPPING_TRIANGLE_SOUP, 0, 1, GENEROUS_JOIN_TOLERANCE);

            assertThat(run.joins())
                .isEqualTo(new HatchJoinTally(3, 0, 4, 0, Double.POSITIVE_INFINITY));
        }

        @Test
        void computeHatchRunReportsAJoinTheToleranceClosedApartFromAnExactOne() {
            // The rectangle's three interior joins land exactly; the four joins across the
            // thousandth-unit gap to the lone triangle land only because the tolerance reaches
            // that far, and the widest of them is reported as the fraction of the spacing it
            // spanned - which is what sets the tolerance rather than a guess at it.
            var run = buildHatchRun(NEARLY_ABUTTING_LOBES, 0, 1, GENEROUS_JOIN_TOLERANCE);

            assertThat(run.joins().exactJoinCount())
                .isEqualTo(3);
            assertThat(run.joins().toleranceJoinCount())
                .isEqualTo(4);
            assertThat(run.joins().overlappingJoinCount())
                .isZero();
            assertThat(run.joins().widestToleranceGapFraction())
                .isCloseTo(0.001, within(1e-5));

            // Every gap closed, so there is no narrowest one left open - not one of no width.
            assertThat(run.joins().narrowestOpenGapFraction())
                .isInfinite();
        }

        @Test
        void computeHatchRunLeavesANearlyAbuttingJoinOpenAtNoTolerance() {
            // The same soup with the tolerance taken away: the exact joins still close - they
            // need no tolerance to - and the four the tolerance was closing reopen, so every
            // line the gap crosses breaks in two.
            var run = buildHatchRun(NEARLY_ABUTTING_LOBES, 0, 1, NO_JOIN_TOLERANCE);

            assertThat(countSegments(run.segments()))
                .isEqualTo(9);
            assertThat(run.joins().exactJoinCount())
                .isEqualTo(3);
            assertThat(run.joins().toleranceJoinCount())
                .isZero();
        }

        @Test
        void computeHatchRunMeasuresTheGapItRefusedAtNoTolerance() {
            // The reading a zero-tolerance run exists to give. Nothing was tolerated - nothing
            // can be, at zero - so the count and the widest closed gap say only what every
            // zero-tolerance run says. The narrowest gap left open is the one number that
            // distinguishes a region with nothing to join from one whose joins the tolerance was
            // simply set below, and it names what the tolerance would have to reach.
            var run = buildHatchRun(NEARLY_ABUTTING_LOBES, 0, 1, NO_JOIN_TOLERANCE);

            assertThat(run.joins().widestToleranceGapFraction())
                .isZero();
            assertThat(run.joins().narrowestOpenGapFraction())
                .isCloseTo(0.001, within(1e-5));
        }

        @Test
        void computeHatchRunClipsLinesBelowTheOriginToARightTriangle() {
            // The same right triangle reflected below the x axis, so every line it crosses is a
            // negative multiple of the spacing. Emitted points are derived from the line's own
            // offset rather than carried through from the clip, and that derivation is where a
            // sign convention can invert without any positive-coordinate case noticing.
            var belowOrigin = new float[] {
                0f, -4f, 4f, -4f, 0f, 0f};

            assertThat(buildHatchRun(belowOrigin, 0, 1, GENEROUS_JOIN_TOLERANCE).segments())
                .containsExactly(
                    0f, -4f, 4f, -4f,
                    0f, -3f, 3f, -3f,
                    0f, -2f, 2f, -2f,
                    0f, -1f, 1f, -1f);
        }

        @Test
        void computeHatchRunLaysFewerLinesAsTheSpacingWidens() {
            // Doubling the spacing halves how many lines fall within the triangle's y span.
            var tight = buildHatchRun(RIGHT_TRIANGLE, 0, 1, GENEROUS_JOIN_TOLERANCE);
            var loose = buildHatchRun(RIGHT_TRIANGLE, 0, 2, GENEROUS_JOIN_TOLERANCE);

            assertThat(countSegments(tight.segments())).isEqualTo(4);
            assertThat(countSegments(loose.segments())).isEqualTo(2);
        }

        @Test
        void computeHatchRunRunsEverySegmentAlongTheGivenAngle() {
            // A 45-degree hatch: every clipped segment must lie parallel to that direction,
            // so its direction vector's cross product with (cos, sin) is zero.
            var angle = Math.PI / 4;
            var run = buildHatchRun(RIGHT_TRIANGLE, angle, 1, GENEROUS_JOIN_TOLERANCE).segments();

            assertThat(countSegments(run))
                .isPositive();

            for (var segment = 0; segment < run.length; segment += 4) {

                var deltaX = run[segment + 2] - run[segment];
                var deltaY = run[segment + 3] - run[segment + 1];
                var cross = deltaX * Math.sin(angle) - deltaY * Math.cos(angle);

                assertThat(cross)
                    .isCloseTo(0, within(1e-3));
            }
        }

        @Test
        void computeHatchRunIgnoresAZeroAreaTriangle() {
            // Three collinear points enclose no area, so no line crosses them.
            var collinear = new float[] {0f, 0f, 2f, 0f, 4f, 0f};

            assertThat(buildHatchRun(collinear, 0, 1, GENEROUS_JOIN_TOLERANCE).segments())
                .isEmpty();
        }

        // The whole run, since a case asserts on the tally as often as on the geometry. The
        // tolerance is named at every call rather than defaulted: it decides what comes back on
        // half the soups here, so a case that did not state it would be reading an expectation
        // against a number it never chose.
        private static HatchRun buildHatchRun(
                float[] triangleSoup,
                double angleRadians,
                double spacing,
                double joinToleranceFraction) {

            return Hatching.computeHatchRun(
                triangleSoup,
                angleRadians,
                spacing,
                joinToleranceFraction);
        }

        // A GL_LINES run packs four floats per segment (two endpoints).
        private static int countSegments(float[] run) {
            return run.length / 4;
        }
    }
}
