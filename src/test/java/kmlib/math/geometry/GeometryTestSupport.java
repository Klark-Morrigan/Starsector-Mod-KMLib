package kmlib.math.geometry;

import org.assertj.core.data.Offset;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared fixtures for the {@code kmlib.math.geometry} tests: the reference shapes,
 * the floating-point tolerance, and the winding check that several suites would
 * otherwise each restate. Kept package-private in the test tree, since only the
 * geometry tests need it.
 */
final class GeometryTestSupport {

    // The side the reference square is built at: small enough that an inset of 2 or 3
    // leaves a shape to assert on, large enough that it does not itself read as
    // degenerate.
    private static final double REFERENCE_SIDE = 10;

    private GeometryTestSupport() {
    }

    // The floating-point slack the geometry assertions allow: tight enough to pin exact
    // corner coordinates, loose enough to absorb offset/clip rounding. The default for
    // suites comparing coordinates a clip produced; one wanting a tighter bound than
    // that (Points and PrincipalAxis go to 1e-9 and below) states its own inline, so a
    // number written at an assertion always means a deliberate departure from this.
    static Offset<Double> buildAssertionSlack() {
        return Offset.offset(1e-6);
    }

    // CCW square with side 10, the reference shape for the offset and smoothing
    // tests. Delegates rather than restating the corners, so the shape the suites
    // assert exact coordinates against cannot drift from the parameterised one.
    static List<double[]> buildReferenceSquare() {
        return buildSquare(REFERENCE_SIDE);
    }

    // CCW square of the given side, anchored at the origin.
    static List<double[]> buildSquare(double side) {
        return Arrays.asList(
            new double[] {0, 0},
            new double[] {side, 0},
            new double[] {side, side},
            new double[] {0, side});
    }

    // Asserts a list of points matches the expected one in order, size and position, at
    // the shared slack. The check every suite pinning computed geometry against literal
    // coordinates makes, held here so the slack it is made at is settled once rather
    // than once per suite - two suites drifting apart on it would disagree about what
    // counts as the same point.
    static void assertThatPointsAre(List<double[]> points, List<double[]> expected) {

        assertThat(points)
            .hasSameSizeAs(expected);

        for (var i = 0; i < expected.size(); i++) {

            assertThat(points.get(i))
                .containsExactly(expected.get(i), buildAssertionSlack());
        }
    }

    // The signed area of a closed ring; positive is counter-clockwise. Delegates to
    // the production shoelace so a test never restates it.
    static double computeSignedArea(List<double[]> ring) {
        return PolygonRegions.computeSignedArea(ring);
    }
}
