package kmlib.math.geometry;

import org.assertj.core.data.Offset;

import java.util.Arrays;
import java.util.List;

/**
 * Shared fixtures for the {@code kmlib.math.geometry} tests: the reference shapes,
 * the floating-point tolerance, and the winding check that several suites would
 * otherwise each restate. Kept package-private in the test tree, since only the
 * geometry tests need it.
 */
final class GeometryTestSupport {

    private GeometryTestSupport() {
    }

    // The floating-point slack the geometry assertions allow: tight enough to pin
    // exact corner coordinates, loose enough to absorb offset/clip rounding.
    static Offset<Double> within() {
        return Offset.offset(1e-6);
    }

    // CCW square with side 10, the reference shape for the offset and smoothing
    // tests.
    static List<double[]> square() {
        return Arrays.asList(
            new double[] {0, 0},
            new double[] {10, 0},
            new double[] {10, 10},
            new double[] {0, 10});
    }

    // CCW square of the given side, anchored at the origin.
    static List<double[]> bigSquare(double side) {
        return Arrays.asList(
            new double[] {0, 0},
            new double[] {side, 0},
            new double[] {side, side},
            new double[] {0, side});
    }

    // The signed area of a closed ring; positive is counter-clockwise. Delegates to
    // the production shoelace so a test never restates it.
    static double signedArea(List<double[]> ring) {
        return PolygonRegions.computeSignedArea(ring);
    }
}
