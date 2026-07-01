package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    @Nested
    class BuildCells {
        @Test
        void empty_sites_yield_no_cells() {
            assertThat(VoronoiCellBuilder.buildCells(List.of(), MAX_CELL_RADIUS)).isEmpty();
        }

        @Test
        void one_cell_is_built_per_site() {
            var sites = Arrays.asList(
                    new double[] {-1, 0},
                    new double[] {1, 0},
                    new double[] {0, 1});

            assertThat(VoronoiCellBuilder.buildCells(sites, MAX_CELL_RADIUS)).hasSize(3);
        }

        @Test
        void a_lone_site_fills_a_bounded_disc() {
            double[] site = {500, 500};
            var cells = VoronoiCellBuilder.buildCells(
                    List.of(site), MAX_CELL_RADIUS);

            assertThat(cells).hasSize(1);
            assertThat(cells.get(0).size()).isGreaterThan(4);
            assertThat(cells.get(0)).allMatch(vertex ->
                    distance(vertex, site) <= MAX_CELL_RADIUS + 1e-6);
        }

        @Test
        void every_cell_stays_within_the_bound_radius() {
            var sites = Arrays.asList(
                    new double[] {-300, -300},
                    new double[] {300, -300},
                    new double[] {0, 400},
                    new double[] {0, 0});

            var cells = VoronoiCellBuilder.buildCells(sites, MAX_CELL_RADIUS);

            for (var i = 0; i < sites.size(); i++) {
                var site = sites.get(i);
                assertThat(cells.get(i))
                        .as("cell %d stays within the bound radius of its site", i)
                        .allMatch(vertex -> distance(vertex, site) <= MAX_CELL_RADIUS + 1e-6);
            }
        }

        @Test
        void two_sites_split_along_their_bisector() {
            var sites = Arrays.asList(
                    new double[] {-1000, 0},
                    new double[] {1000, 0});

            var cells = VoronoiCellBuilder.buildCells(sites, MAX_CELL_RADIUS);

            assertThat(cells.get(0)).allMatch(vertex -> vertex[0] <= 1e-9);
            assertThat(cells.get(1)).allMatch(vertex -> vertex[0] >= -1e-9);
        }

        @Test
        void each_site_lies_inside_its_own_cell() {
            var sites = Arrays.asList(
                    new double[] {-300, -300},
                    new double[] {300, -300},
                    new double[] {0, 400},
                    new double[] {0, 0});

            var cells = VoronoiCellBuilder.buildCells(sites, MAX_CELL_RADIUS);

            for (var i = 0; i < sites.size(); i++) {
                assertThat(isPointInsidePolygon(sites.get(i), cells.get(i)))
                        .as("site %d lies inside its own cell", i)
                        .isTrue();
            }
        }
    }

    @Nested
    class BuildLabelledCell {
        @Test
        void labelled_cell_vertices_match_the_unlabelled_build_cell() {
            var sites = Arrays.asList(
                    new double[] {-300, -300},
                    new double[] {300, -300},
                    new double[] {0, 400},
                    new double[] {0, 0});

            // The labelled build only adds edge tags; its geometry must be the
            // same polygon the unlabelled path returns.
            for (var i = 0; i < sites.size(); i++) {
                var labelled = VoronoiCellBuilder.buildLabelledCell(i, sites, MAX_CELL_RADIUS);
                var plain = VoronoiCellBuilder.buildCell(sites.get(i), sites, MAX_CELL_RADIUS);
                assertThat(labelled.vertices()).hasSameSizeAs(plain);
                for (var v = 0; v < plain.size(); v++) {
                    assertThat(labelled.vertices().get(v)).containsExactly(plain.get(v),
                            org.assertj.core.data.Offset.offset(1e-9));
                }
            }
        }

        @Test
        void there_is_one_edge_label_per_edge() {
            var sites = Arrays.asList(
                    new double[] {-300, -300},
                    new double[] {300, -300},
                    new double[] {0, 0});

            for (var i = 0; i < sites.size(); i++) {
                var cell = VoronoiCellBuilder.buildLabelledCell(i, sites, MAX_CELL_RADIUS);
                assertThat(cell.edgeNeighbourSiteIndices()).hasSize(cell.vertices().size());
            }
        }

        @Test
        void a_lone_site_has_only_bound_edges() {
            var cell = VoronoiCellBuilder.buildLabelledCell(
                    0, List.of(new double[] {500, 500}), MAX_CELL_RADIUS);

            // Nothing to clip against, so every edge is the max-radius bound: no
            // neighbour, no adjacency.
            assertThat(cell.edgeNeighbourSiteIndices())
                    .containsOnly(VoronoiCellBuilder.BOUND_EDGE);
        }

        @Test
        void two_close_sites_each_tag_the_other_across_their_shared_edge() {
            var sites = Arrays.asList(
                    new double[] {-1000, 0},
                    new double[] {1000, 0});

            // The only non-bound edge of each cell is the shared bisector, tagged
            // with the other site - the adjacency between them.
            assertThat(neighboursOf(VoronoiCellBuilder.buildLabelledCell(0, sites, MAX_CELL_RADIUS)))
                    .containsExactly(1);
            assertThat(neighboursOf(VoronoiCellBuilder.buildLabelledCell(1, sites, MAX_CELL_RADIUS)))
                    .containsExactly(0);
        }

        @Test
        void a_far_site_is_not_tagged_as_a_neighbour() {
            var sites = Arrays.asList(
                    new double[] {0, 0},
                    new double[] {100, 0},
                    new double[] {50_000, 0});

            // The distant third site never clips either near cell, so neither
            // names it; the two near sites remain each other's only neighbour.
            assertThat(neighboursOf(VoronoiCellBuilder.buildLabelledCell(0, sites, MAX_CELL_RADIUS)))
                    .containsExactly(1);
            assertThat(neighboursOf(VoronoiCellBuilder.buildLabelledCell(1, sites, MAX_CELL_RADIUS)))
                    .containsExactly(0);
        }

        @Test
        void adjacency_is_symmetric() {
            var sites = Arrays.asList(
                    new double[] {-300, -300},
                    new double[] {300, -300},
                    new double[] {0, 400},
                    new double[] {0, 0});

            var neighboursBySite = new ArrayList<Set<Integer>>();
            for (var i = 0; i < sites.size(); i++) {
                neighboursBySite.add(
                        neighboursOf(VoronoiCellBuilder.buildLabelledCell(i, sites, MAX_CELL_RADIUS)));
            }

            // A shared Voronoi edge belongs to both cells, so adjacency must read
            // the same from either end.
            for (var i = 0; i < sites.size(); i++) {
                for (var j = 0; j < sites.size(); j++) {
                    assertThat(neighboursBySite.get(i).contains(j))
                            .as("site %d lists %d iff %d lists %d", i, j, j, i)
                            .isEqualTo(neighboursBySite.get(j).contains(i));
                }
            }
        }

        // The set of distinct neighbour site indices a cell names across its
        // edges, dropping the bound-edge sentinel (a frontier, not a neighbour).
        private static Set<Integer> neighboursOf(VoronoiCellBuilder.LabelledCell cell) {
            var neighbours = new LinkedHashSet<Integer>();
            for (var label : cell.edgeNeighbourSiteIndices()) {
                if (label != VoronoiCellBuilder.BOUND_EDGE) {
                    neighbours.add(label);
                }
            }
            return neighbours;
        }
    }

    @Nested
    class BuildCell {
        @Test
        void build_cell_matches_the_same_site_from_build_cells() {
            var sites = Arrays.asList(
                    new double[] {-300, -300},
                    new double[] {300, -300},
                    new double[] {0, 400},
                    new double[] {0, 0});
            var cells = VoronoiCellBuilder.buildCells(sites, MAX_CELL_RADIUS);

            // Recomputing one site's cell on its own yields the same polygon as the
            // full partition - the property the incremental update relies on.
            for (var i = 0; i < sites.size(); i++) {
                var single = VoronoiCellBuilder.buildCell(
                        sites.get(i), sites, MAX_CELL_RADIUS);
                assertThat(single).hasSameSizeAs(cells.get(i));
                for (var v = 0; v < single.size(); v++) {
                    assertThat(single.get(v)).containsExactly(cells.get(i).get(v),
                            org.assertj.core.data.Offset.offset(1e-9));
                }
            }
        }
    }

    private static double distance(double[] a, double[] b) {
        var dx = a[0] - b[0];
        var dy = a[1] - b[1];
        return Math.sqrt(dx * dx + dy * dy);
    }

    // Convex-polygon containment via the sign test: a point is inside a
    // convex polygon when it lies on the same side of every directed edge.
    private static boolean isPointInsidePolygon(double[] point, List<double[]> polygon) {
        var hasPositive = false;
        var hasNegative = false;
        var count = polygon.size();
        for (var i = 0; i < count; i++) {
            var from = polygon.get(i);
            var to = polygon.get((i + 1) % count);
            var cross = (to[0] - from[0]) * (point[1] - from[1])
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
