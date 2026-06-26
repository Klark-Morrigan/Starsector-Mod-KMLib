package kmlib.math.geometry;

import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.assertj.core.api.Assertions.assertThat;

class PointsTest {
    @Test
    void computeDistanceIsEuclidean() {
        assertThat(Points.computeDistance(0, 0, 3, 4)).isEqualTo(5.0);
    }

    @Test
    void computeDistanceIsZeroForCoincidentPoints() {
        assertThat(Points.computeDistance(2, 7, 2, 7)).isZero();
    }

    @Test
    void computeDistanceArrayOverloadMatchesCoordinateForm() {
        assertThat(Points.computeDistance(new double[] {1, 1}, new double[] {4, 5}))
                .isEqualTo(5.0);
    }

    @Test
    void computeDistanceVectorOverloadMatchesCoordinateForm() {
        assertThat(Points.computeDistance(new Vector2f(1, 1), new Vector2f(4, 5)))
                .isEqualTo(5.0);
    }
}
