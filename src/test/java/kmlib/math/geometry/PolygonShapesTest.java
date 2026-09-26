package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link PolygonShapes#computeRegularVertices}: one vertex per side, every vertex on the
 * radius, the first at the start angle and the rest counter-clockwise from it, and a side count too
 * low to enclose an area refused. The vector form is pinned against the {x, y} form, so the two
 * cannot place a vertex differently.
 */
final class PolygonShapesTest {

    // A sixth of a half turn: where a pointy-top hexagon's first vertex sits.
    private static final double POINTY_TOP_START_ANGLE = Math.PI / 6;

    private static final int HEXAGON_SIDES = 6;

    @Nested
    class ComputeRegularVertices {

        @Test
        void placesOneVertexPerSideOnTheRadius() {

            var vertices = PolygonShapes.computeRegularVertices(10, -5, 20, HEXAGON_SIDES, 0);

            assertThat(vertices)
                .hasSize(HEXAGON_SIDES)
                .allSatisfy(vertex -> assertThat(Math.hypot(vertex[0] - 10, vertex[1] + 5))
                    .isCloseTo(20, within(1e-9)));
        }

        @Test
        void placesTheFirstVertexAtTheStartAngle() {

            var vertices = PolygonShapes.computeRegularVertices(0, 0, 10, HEXAGON_SIDES, POINTY_TOP_START_ANGLE);

            // cos(30 degrees) * 10 and sin(30 degrees) * 10.
            assertThat(vertices.get(0))
                .containsExactly(new double[] {8.660254, 5}, within(1e-6));
        }

        @Test
        void stepsCounterClockwiseFromTheStartAngle() {

            var vertices = PolygonShapes.computeRegularVertices(0, 0, 10, 4, 0);

            // A square from the positive x-axis turns to the positive y-axis next.
            assertThat(vertices.get(1))
                .containsExactly(new double[] {0, 10}, within(1e-9));
        }

        @Test
        void refusesTooFewSidesToEncloseAnArea() {
            assertThatThrownBy(() -> PolygonShapes.computeRegularVertices(0, 0, 10, 2, 0))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void vectorFormPlacesTheSameVerticesAsThePairForm() {

            var pairs = PolygonShapes.computeRegularVertices(3, 4, 48, HEXAGON_SIDES, POINTY_TOP_START_ANGLE);
            var vectors = PolygonShapes.computeRegularVertices(
                new Vector2f(3f, 4f),
                48f,
                HEXAGON_SIDES,
                POINTY_TOP_START_ANGLE);

            assertThat(vectors)
                .hasSameSizeAs(pairs);

            for (var i = 0; i < pairs.size(); i++) {

                assertThat(vectors.get(i).x)
                    .isEqualTo((float) pairs.get(i)[0]);
                assertThat(vectors.get(i).y)
                    .isEqualTo((float) pairs.get(i)[1]);
            }
        }
    }
}
