package kmlib.math.geometry;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link VoronoiCellBuilder#buildCells}:
 *  - one cell per site, in input order,
 *  - a lone site fills its whole max-radius polygon (a bounded disc),
 *  - every cell stays within the bound radius of its own site,
 *  - two sites split along their perpendicular bisector,
 *  - each site lies inside its own cell.
 */
final class VoronoiCellBuilderTest {
    private static final double MAX_CELL_RADIUS = 5000.0;

    @Test
    void empty_sites_yield_no_cells() {
        assertThat(VoronoiCellBuilder.buildCells(List.of(), MAX_CELL_RADIUS)).isEmpty();
    }

    @Test
    void one_cell_is_built_per_site() {
        List<double[]> sites = Arrays.asList(
                new double[] {-1, 0},
                new double[] {1, 0},
                new double[] {0, 1});

        assertThat(VoronoiCellBuilder.buildCells(sites, MAX_CELL_RADIUS)).hasSize(3);
    }

    @Test
    void a_lone_site_fills_a_bounded_disc() {
        double[] site = {500, 500};
        List<List<double[]>> cells = VoronoiCellBuilder.buildCells(
                List.of(site), MAX_CELL_RADIUS);

        assertThat(cells).hasSize(1);
        assertThat(cells.get(0).size()).isGreaterThan(4);
        assertThat(cells.get(0)).allMatch(vertex ->
                distance(vertex, site) <= MAX_CELL_RADIUS + 1e-6);
    }

    @Test
    void every_cell_stays_within_the_bound_radius() {
        List<double[]> sites = Arrays.asList(
                new double[] {-300, -300},
                new double[] {300, -300},
                new double[] {0, 400},
                new double[] {0, 0});

        List<List<double[]>> cells = VoronoiCellBuilder.buildCells(sites, MAX_CELL_RADIUS);

        for (int i = 0; i < sites.size(); i++) {
            double[] site = sites.get(i);
            assertThat(cells.get(i))
                    .as("cell %d stays within the bound radius of its site", i)
                    .allMatch(vertex -> distance(vertex, site) <= MAX_CELL_RADIUS + 1e-6);
        }
    }

    @Test
    void two_sites_split_along_their_bisector() {
        List<double[]> sites = Arrays.asList(
                new double[] {-1000, 0},
                new double[] {1000, 0});

        List<List<double[]>> cells = VoronoiCellBuilder.buildCells(sites, MAX_CELL_RADIUS);

        assertThat(cells.get(0)).allMatch(vertex -> vertex[0] <= 1e-9);
        assertThat(cells.get(1)).allMatch(vertex -> vertex[0] >= -1e-9);
    }

    @Test
    void each_site_lies_inside_its_own_cell() {
        List<double[]> sites = Arrays.asList(
                new double[] {-300, -300},
                new double[] {300, -300},
                new double[] {0, 400},
                new double[] {0, 0});

        List<List<double[]>> cells = VoronoiCellBuilder.buildCells(sites, MAX_CELL_RADIUS);

        for (int i = 0; i < sites.size(); i++) {
            assertThat(isPointInsidePolygon(sites.get(i), cells.get(i)))
                    .as("site %d lies inside its own cell", i)
                    .isTrue();
        }
    }

    private static double distance(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        return Math.sqrt(dx * dx + dy * dy);
    }

    // Convex-polygon containment via the sign test: a point is inside a
    // convex polygon when it lies on the same side of every directed edge.
    private static boolean isPointInsidePolygon(double[] point, List<double[]> polygon) {
        boolean hasPositive = false;
        boolean hasNegative = false;
        int count = polygon.size();
        for (int i = 0; i < count; i++) {
            double[] from = polygon.get(i);
            double[] to = polygon.get((i + 1) % count);
            double cross = (to[0] - from[0]) * (point[1] - from[1])
                    - (to[1] - from[1]) * (point[0] - from[0]);
            if (cross > 1e-9) {
                hasPositive = true;
            } else if (cross < -1e-9) {
                hasNegative = true;
            }
        }
        return !(hasPositive && hasNegative);
    }
}
