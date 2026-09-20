package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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

    // The square the split tests divide, and the area it encloses - the total the
    // pieces must add back up to.
    private static final double SPLIT_SQUARE_SIDE = 10.0;
    private static final double WHOLE_SQUARE_AREA = SPLIT_SQUARE_SIDE * SPLIT_SQUARE_SIDE;

    @Nested
    class BuildCells {

        @Test
        void emptySitesYieldNoCells() {
            assertThat(VoronoiCellBuilder.buildCells(List.of(), MAX_CELL_RADIUS))
                .isEmpty();
        }

        @Test
        void oneCellIsBuiltPerSite() {

            var sites = Arrays.asList(
                new double[] {-1, 0},
                new double[] {1, 0},
                new double[] {0, 1});

            assertThat(VoronoiCellBuilder.buildCells(sites, MAX_CELL_RADIUS))
                .hasSize(3);
        }

        @Test
        void aLoneSiteFillsABoundedDisc() {

            double[] site = {500, 500};

            var cells = VoronoiCellBuilder.buildCells(
                List.of(site),
                MAX_CELL_RADIUS);

            assertThat(cells)
                .hasSize(1);
            assertThat(cells.get(0).size())
                .isGreaterThan(4);
            assertThat(cells.get(0))
                .allMatch(vertex -> computeDistance(vertex, site) <= MAX_CELL_RADIUS + 1e-6);
        }

        @Test
        void everyCellStaysWithinTheBoundRadius() {

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
                    .allMatch(vertex -> computeDistance(vertex, site) <= MAX_CELL_RADIUS + 1e-6);
            }
        }

        @Test
        void twoSitesSplitAlongTheirBisector() {

            var sites = Arrays.asList(
                new double[] {-1000, 0},
                new double[] {1000, 0});

            var cells = VoronoiCellBuilder.buildCells(sites, MAX_CELL_RADIUS);

            assertThat(cells.get(0))
                .allMatch(vertex -> vertex[0] <= 1e-9);
            assertThat(cells.get(1))
                .allMatch(vertex -> vertex[0] >= -1e-9);
        }

        @Test
        void eachSiteLiesInsideItsOwnCell() {

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
        void labelledCellVerticesMatchTheUnlabelledBuildCell() {

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

                assertThat(labelled.vertices())
                    .hasSameSizeAs(plain);

                for (var v = 0; v < plain.size(); v++) {

                    assertThat(labelled.vertices().get(v))
                        .containsExactly(plain.get(v),
                            org.assertj.core.data.Offset.offset(1e-9));
                }
            }
        }

        @Test
        void thereIsOneEdgeLabelPerEdge() {

            var sites = Arrays.asList(
                new double[] {-300, -300},
                new double[] {300, -300},
                new double[] {0, 0});

            for (var i = 0; i < sites.size(); i++) {

                var cell = VoronoiCellBuilder.buildLabelledCell(i, sites, MAX_CELL_RADIUS);

                assertThat(cell.edgeNeighbourSiteIndices())
                    .hasSize(cell.vertices().size());
            }
        }

        @Test
        void theBoundSegmentCountSetsALoneCellVertexCount() {

            var site = List.of(new double[] {500, 500});

            // A lone site is clipped by nothing, so its cell is the whole seed
            // polygon: its vertex count is exactly the requested bound-segment
            // count. This is the lever a caller trades frontier smoothness for
            // fewer vertices with; the no-count builder uses the default.
            assertThat(VoronoiCellBuilder.buildLabelledCell(0, site, MAX_CELL_RADIUS, 24).vertices())
                .hasSize(24);

            assertThat(VoronoiCellBuilder.buildLabelledCell(0, site, MAX_CELL_RADIUS).vertices())
                .hasSize(VoronoiCellBuilder.DEFAULT_CELL_BOUND_SEGMENTS);
        }

        @Test
        void aLoneSiteHasOnlyBoundEdges() {

            var cell = VoronoiCellBuilder.buildLabelledCell(
                0,
                List.of(new double[] {500, 500}),
                MAX_CELL_RADIUS);

            // Nothing to clip against, so every edge is the max-radius bound: no
            // neighbour, no adjacency.
            assertThat(cell.edgeNeighbourSiteIndices())
                .containsOnly(VoronoiCellBuilder.BOUND_EDGE);
        }

        @Test
        void twoCloseSitesEachTagTheOtherAcrossTheirSharedEdge() {

            var sites = Arrays.asList(
                new double[] {-1000, 0},
                new double[] {1000, 0});

            // The only non-bound edge of each cell is the shared bisector, tagged
            // with the other site - the adjacency between them.
            assertThat(listNeighboursOf(VoronoiCellBuilder.buildLabelledCell(0, sites, MAX_CELL_RADIUS)))
                .containsExactly(1);

            assertThat(listNeighboursOf(VoronoiCellBuilder.buildLabelledCell(1, sites, MAX_CELL_RADIUS)))
                .containsExactly(0);
        }

        @Test
        void aFarSiteIsNotTaggedAsANeighbour() {

            var sites = Arrays.asList(
                new double[] {0, 0},
                new double[] {100, 0},
                new double[] {50_000, 0});

            // The distant third site never clips either near cell, so neither
            // names it; the two near sites remain each other's only neighbour.
            assertThat(listNeighboursOf(VoronoiCellBuilder.buildLabelledCell(0, sites, MAX_CELL_RADIUS)))
                .containsExactly(1);

            assertThat(listNeighboursOf(VoronoiCellBuilder.buildLabelledCell(1, sites, MAX_CELL_RADIUS)))
                .containsExactly(0);
        }

        @Test
        void adjacencyIsSymmetric() {

            var sites = Arrays.asList(
                new double[] {-300, -300},
                new double[] {300, -300},
                new double[] {0, 400},
                new double[] {0, 0});

            var neighboursBySite = new ArrayList<Set<Integer>>();

            for (var i = 0; i < sites.size(); i++) {

                neighboursBySite.add(
                    listNeighboursOf(VoronoiCellBuilder.buildLabelledCell(i, sites, MAX_CELL_RADIUS)));
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
        private static Set<Integer> listNeighboursOf(VoronoiCellBuilder.LabelledCell cell) {

            var neighbours = new LinkedHashSet<Integer>();
            for (var label : cell.edgeNeighbourSiteIndices()) {
                if (label != VoronoiCellBuilder.BOUND_EDGE) {
                    neighbours.add(label);
                }
            }
            return neighbours;
        }
    }

    /**
     * A timing/allocation harness comparing the labelled build against the bare
     * double[] build it replaced. Its real assertion is correctness - the labelled
     * cell must equal the bare control vertex for vertex, so labelling provably
     * does not perturb the geometry - and it prints per-build cost as diagnostics.
     * The bare control is kept here rather than in production because the
     * production path no longer has an unlabelled implementation to measure.
     */
    @Nested
    class SharedFrontierCorners {

        // Two sites inside two radii of each other, so each cell is part bisector and
        // part bound, and the two corners where one gives way to the other are shared.
        private static final List<double[]> NEIGHBOURS = List.of(
            new double[] {0, 0},
            new double[] {4000, 0});

        // Deliberately coarse. The seed polygon's chord falls furthest from the bound
        // at a low count, so a corner placed on the chord instead of the bound is
        // wrong by the most here - and a count this low is what a caller passes to
        // trade smoothness for vertices.
        private static final int COARSE_SEGMENTS = 8;

        // Fine enough that the band between the seed and the bound is too thin to swallow a
        // corner, so this count stands for "however smoothly it could be drawn".
        private static final int FINE_SEGMENTS = 256;

        // How far a corner may sit from the bound and still be on it. The corner is
        // computed from the radius rather than approached, so this is rounding.
        private static final double SAME_POINT = 1e-9;

        @Test
        void twoNeighboursPutTheirSharedCornersInExactlyTheSamePlace() {
            // Not "within a tolerance": both cells work the corner out from the two
            // sites and the radius, never from their own seed polygon, so the two
            // arrive at the same doubles. A consumer chaining one cell's frontier to
            // the next can then weld at rounding instead of across a sagitta.
            var first = VoronoiCellBuilder.buildLabelledCell(
                0, NEIGHBOURS, MAX_CELL_RADIUS, COARSE_SEGMENTS);

            var second = VoronoiCellBuilder.buildLabelledCell(
                1, NEIGHBOURS, MAX_CELL_RADIUS, COARSE_SEGMENTS);

            assertThat(collectFrontierCorners(first))
                .containsExactlyInAnyOrderElementsOf(collectFrontierCorners(second));
        }

        @Test
        void aSharedCornerStandsAtTheBoundRadiusFromBothSites() {
            // What makes one point serve both cells: it is on the bisector, so it is
            // the same distance from each site, and that distance is the bound. A
            // corner left on the chord sits short of it.
            var cell = VoronoiCellBuilder.buildLabelledCell(
                0, NEIGHBOURS, MAX_CELL_RADIUS, COARSE_SEGMENTS);

            var corners = collectFrontierCorners(cell);

            assertThat(corners).hasSize(2);

            for (var corner : corners) {

                assertThat(Points.computeDistance(
                    new double[] {corner.get(0), corner.get(1)}, NEIGHBOURS.get(0)))
                    .isCloseTo(MAX_CELL_RADIUS, within(SAME_POINT));

                assertThat(Points.computeDistance(
                    new double[] {corner.get(0), corner.get(1)}, NEIGHBOURS.get(1)))
                    .isCloseTo(MAX_CELL_RADIUS, within(SAME_POINT));
            }
        }

        // Three sites on one circle, so the corner all three share stands at its centre -
        // 4,800 from each, which is inside the 5,000 bound and OUTSIDE the seed drawn at a
        // coarse count, whose flat sides come no nearer than 4,619 at their middles. Turned
        // so that the direction from each site to that corner falls on a seed side rather
        // than a seed vertex, which is where the gap between the two is widest.
        private static final List<double[]> ROUND_A_SHARED_CORNER = List.of(
            new double[] {-1836.88, 4434.62},
            new double[] {-2922.05, -3808.10},
            new double[] {4758.94, -626.53});

        @Test
        void aCornerTheSeedCutsAwayIsPutBack() {
            // The corners a cell offers cannot depend on how smoothly its bound is drawn. The
            // seed is inscribed in that bound, so at a coarse count its sides cut through the
            // band where a shared corner can fall, and the corner is gone from the ring
            // altogether - not merely moved, which is why placing corners is not enough on its
            // own. Put back, the two cells meeting there meet at one point again instead of
            // stopping short of each other with a gap between.
            for (var cell = 0; cell < ROUND_A_SHARED_CORNER.size(); cell++) {

                var coarse = collectFrontierCorners(VoronoiCellBuilder.buildLabelledCell(
                    cell, ROUND_A_SHARED_CORNER, MAX_CELL_RADIUS, COARSE_SEGMENTS));

                var fine = collectFrontierCorners(VoronoiCellBuilder.buildLabelledCell(
                    cell, ROUND_A_SHARED_CORNER, MAX_CELL_RADIUS, FINE_SEGMENTS));

                assertThat(coarse)
                    .as("cell %d, drawn at %d segments against %d",
                        cell, COARSE_SEGMENTS, FINE_SEGMENTS)
                    .containsExactlyInAnyOrderElementsOf(fine);
            }
        }

        @Test
        void thoseCornersAreThereToBeLost() {
            // Guards the fixture rather than the code: a layout offering no corners at all
            // would pass the comparison above without exercising anything.
            assertThat(collectFrontierCorners(VoronoiCellBuilder.buildLabelledCell(
                    0, ROUND_A_SHARED_CORNER, MAX_CELL_RADIUS, COARSE_SEGMENTS)))
                .isNotEmpty();
        }

        @Test
        void aLoneCellIsUntouched() {
            // Nothing cuts it, so it has no corner where a frontier starts or stops,
            // and every vertex is a sample of the bound as the seed drew it.
            var lone = VoronoiCellBuilder.buildLabelledCell(
                0, List.of(new double[] {0, 0}), MAX_CELL_RADIUS, COARSE_SEGMENTS);

            assertThat(lone.vertices()).hasSize(COARSE_SEGMENTS);

            assertThat(collectFrontierCorners(lone)).isEmpty();
        }

        // The corners where the frontier gives way to a neighbour's border: exactly
        // one of the two edges meeting there came from the bound.
        private static List<List<Double>> collectFrontierCorners(
                VoronoiCellBuilder.LabelledCell cell) {

            var labels = cell.edgeNeighbourSiteIndices();
            var count = cell.vertices().size();
            var corners = new ArrayList<List<Double>>();

            for (var index = 0; index < count; index++) {

                var arriving = labels[(index + count - 1) % count] == VoronoiCellBuilder.BOUND_EDGE;
                var leaving = labels[index] == VoronoiCellBuilder.BOUND_EDGE;

                if (arriving != leaving) {
                    // As boxed coordinates, so two corners compare by value and a
                    // shared one has to be equal rather than merely close.
                    corners.add(List.of(
                        cell.vertices().get(index)[0], cell.vertices().get(index)[1]));
                }
            }
            return corners;
        }
    }

    @Nested
    class BuildLabelledCellPerformance {

        // A partition big enough that cells actually clip against neighbours, so
        // the harness exercises the clip loop rather than lone bounded discs.
        private static final int SITE_COUNT = 150;
        private static final double SITE_SPREAD = 20_000.0;

        // The bare control seed must match the production default seed vertex for
        // vertex, or the "labelling does not perturb geometry" gate compares
        // mismatched seeds - so it reads the same constant the default builders use.
        private static final int BOUND_SEGMENTS = VoronoiCellBuilder.DEFAULT_CELL_BOUND_SEGMENTS;
        private static final long RANDOM_SEED = 918_273_645L;
        private static final int WARMUP_BUILDS = 10;
        private static final int TIMED_BUILDS = 30;

        // How far a corner may sit from the bound and still be on it, and how far two
        // readings of one clipped vertex may differ and still be the same vertex.
        private static final double SAME_POINT = 1e-9;

        @Test
        void buildLabelledCellClipsAsTheBareBuildDoesAndReportsCost() {

            var sites = buildRandomSites();

            // Correctness gate: the clip walk must be the same walk, so every vertex
            // the labels have nothing to say about must equal the bare double[]
            // control exactly. Only then does the timing below compare like with like.
            //
            // The frontier corners are the exception, and they are excepted rather
            // than loosened. There the labelled build places the corner on the true
            // bound, which a label-free control cannot do because it does not know
            // which corners those are - so the two differ on purpose, and what is
            // checked is that the labelled one is the one standing on the bound.
            for (var i = 0; i < sites.size(); i++) {

                var labelled = VoronoiCellBuilder.buildLabelledCell(i, sites, MAX_CELL_RADIUS);
                var bare = buildBareCell(i, sites);

                assertThat(labelled.vertices())
                    .hasSameSizeAs(bare);

                var labels = labelled.edgeNeighbourSiteIndices();
                var count = bare.size();

                for (var v = 0; v < count; v++) {

                    var arriving =
                        labels[(v + count - 1) % count] == VoronoiCellBuilder.BOUND_EDGE;
                    var leaving = labels[v] == VoronoiCellBuilder.BOUND_EDGE;

                    if (arriving != leaving) {

                        assertThat(Points.computeDistance(
                            labelled.vertices().get(v), sites.get(i)))
                            .as("frontier corner %d of cell %d", v, i)
                            .isCloseTo(MAX_CELL_RADIUS, within(SAME_POINT));
                        continue;
                    }

                    assertThat(labelled.vertices().get(v))
                        .containsExactly(
                            bare.get(v), org.assertj.core.data.Offset.offset(SAME_POINT));
                }
            }

            var labelledCost = measure(() -> {
                for (var i = 0; i < sites.size(); i++) {
                    VoronoiCellBuilder.buildLabelledCell(i, sites, MAX_CELL_RADIUS);
                }
            });

            var bareCost = measure(() -> {
                for (var i = 0; i < sites.size(); i++) {
                    buildBareCell(i, sites);
                }
            });

            computeReportCost(labelledCost, bareCost);
        }

        private List<double[]> buildRandomSites() {

            var random = new java.util.Random(RANDOM_SEED);
            var sites = new ArrayList<double[]>(SITE_COUNT);

            for (var i = 0; i < SITE_COUNT; i++) {

                sites.add(new double[] {
                    random.nextDouble() * SITE_SPREAD,
                    random.nextDouble() * SITE_SPREAD});
            }
            return sites;
        }

        // The pre-labelling build path, kept as the benchmark control: a bare
        // double[] max-radius polygon clipped by each bisector, carrying no
        // per-edge labels.
        private List<double[]> buildBareCell(int siteIndex, List<double[]> sites) {

            var site = sites.get(siteIndex);
            var cell = buildBareRegularPolygon(site);

            for (var other = 0; other < sites.size(); other++) {
                if (other == siteIndex) {
                    continue;
                }

                var neighbour = sites.get(other);

                cell = buildBareClipToHalfPlane(cell, new HalfPlane(
                    (site[0] + neighbour[0]) * 0.5,
                    (site[1] + neighbour[1]) * 0.5,
                    site[0] - neighbour[0],
                    site[1] - neighbour[1]));

                if (cell.isEmpty()) {
                    break;
                }
            }
            return cell;
        }

        // The bare double[] Sutherland-Hodgman clip this benchmark measures the
        // labelled build against. Production keeps a single clip walk
        // (LabelledPolygon.clipToHalfPlane); this label-free twin lives here on
        // purpose, so the "labelling does not perturb the geometry" gate compares
        // the labelled walk against an independent baseline rather than against
        // itself. Shares only the point math via Points, so any vertex mismatch is
        // labelling, not a different crossing formula.
        private static List<double[]> buildBareClipToHalfPlane(
                List<double[]> polygon,
                HalfPlane boundary) {

            var result = new ArrayList<double[]>();
            var count = polygon.size();

            for (var i = 0; i < count; i++) {

                var current = polygon.get(i);
                var next = polygon.get((i + 1) % count);
                var currentOffset = Lines.computeSignedOffsetFromLine(current, boundary);
                var nextOffset = Lines.computeSignedOffsetFromLine(next, boundary);

                if (currentOffset >= 0) {
                    result.add(current);
                }
                if ((currentOffset >= 0) != (nextOffset >= 0)) {
                    result.add(
                        Segments.computeCrossingPoint(current, next, currentOffset, nextOffset));
                }
            }
            return result;
        }

        private List<double[]> buildBareRegularPolygon(double[] center) {

            var polygon = new ArrayList<double[]>(BOUND_SEGMENTS);

            for (var i = 0; i < BOUND_SEGMENTS; i++) {

                var angle = 2.0 * Math.PI * i / BOUND_SEGMENTS;

                polygon.add(new double[] {
                    center[0] + MAX_CELL_RADIUS * Math.cos(angle),
                    center[1] + MAX_CELL_RADIUS * Math.sin(angle)});
            }
            return polygon;
        }

        // Warms up, then times TIMED_BUILDS full-partition builds, capturing per-
        // build nanoseconds and, where the HotSpot bean exposes it, bytes
        // allocated. Returns {nsPerBuild, bytesPerBuild}; bytes is -1 when the JVM
        // does not expose the counter.
        private long[] measure(Runnable buildPartition) {

            for (var i = 0; i < WARMUP_BUILDS; i++) {
                buildPartition.run();
            }

            var bean = buildAllocationBean();
            var allocBefore = bean == null ? 0 : bean.getCurrentThreadAllocatedBytes();
            var start = System.nanoTime();

            for (var i = 0; i < TIMED_BUILDS; i++) {
                buildPartition.run();
            }

            var nanosPerBuild = (System.nanoTime() - start) / TIMED_BUILDS;
            var bytesPerBuild = bean == null
                ? -1
                : (bean.getCurrentThreadAllocatedBytes() - allocBefore) / TIMED_BUILDS;

            return new long[] {nanosPerBuild, bytesPerBuild};
        }

        private void computeReportCost(long[] labelled, long[] bare) {

            System.out.printf(
                "%nVoronoiCellBuilder partition (%d sites):%n",
                SITE_COUNT);

            System.out.printf(
                "  labelled: %,d ns/build (%,d ns/cell)  %s%n",
                labelled[0],
                labelled[0] / SITE_COUNT,
                readAllocationText(labelled[1]));

            System.out.printf(
                "  bare:     %,d ns/build (%,d ns/cell)  %s%n",
                bare[0],
                bare[0] / SITE_COUNT,
                readAllocationText(bare[1]));

            System.out.printf(
                "  labelled/bare: %.2fx time%s%n",
                computeRatio(labelled[0], bare[0]),
                readAllocationRatioText(labelled[1], bare[1]));
        }

        private String readAllocationText(long bytesPerBuild) {
            return bytesPerBuild < 0
                ? "(alloc n/a)"
                : String.format("%,d bytes/build", bytesPerBuild);
        }

        private String readAllocationRatioText(long labelledBytes, long bareBytes) {
            return labelledBytes < 0 || bareBytes <= 0
                ? ""
                : String.format(", %.2fx alloc", computeRatio(labelledBytes, bareBytes));
        }

        private double computeRatio(long numerator, long denominator) {
            return denominator == 0
                ? Double.NaN
                : (double) numerator / denominator;
        }

        // HotSpot's per-thread allocation counter, or null when the running JVM
        // does not expose it (the harness then reports time only).
        private com.sun.management.ThreadMXBean buildAllocationBean() {

            var bean = java.lang.management.ManagementFactory.getThreadMXBean();

            if (bean instanceof com.sun.management.ThreadMXBean hotspotBean
                    && hotspotBean.isThreadAllocatedMemorySupported()) {

                hotspotBean.setThreadAllocatedMemoryEnabled(true);
                return hotspotBean;
            }
            return null;
        }
    }

    @Nested
    class BuildCell {

        @Test
        void buildCellMatchesTheSameSiteFromBuildCells() {

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
                    sites.get(i),
                    sites,
                    MAX_CELL_RADIUS);

                assertThat(single)
                    .hasSameSizeAs(cells.get(i));

                for (var v = 0; v < single.size(); v++) {

                    assertThat(single.get(v))
                        .containsExactly(cells.get(i).get(v),
                            org.assertj.core.data.Offset.offset(1e-9));
                }
            }
        }
    }

    @Nested
    class SplitPolygonAmongSites {

        @Test
        void noSitesYieldNoPieces() {
            assertThat(VoronoiCellBuilder.splitPolygonAmongSites(
                GeometryTestSupport.buildSquare(SPLIT_SQUARE_SIDE),
                List.of()))
                .isEmpty();
        }

        @Test
        void aLoneSiteTakesTheWholePolygon() {

            var polygon = GeometryTestSupport.buildSquare(SPLIT_SQUARE_SIDE);
            var pieces = VoronoiCellBuilder.splitPolygonAmongSites(
                polygon,
                List.of(new double[] {5, 5}));

            // Nothing to clip against, so the split is the identity on the polygon.
            assertThat(pieces)
                .hasSize(1);
            assertThat(GeometryTestSupport.computeSignedArea(pieces.get(0)))
                .isCloseTo(WHOLE_SQUARE_AREA, GeometryTestSupport.buildAssertionSlack());
        }

        @Test
        void twoSitesSplitThePolygonAlongTheirBisector() {

            var sites = Arrays.asList(
                new double[] {2, 5},
                new double[] {8, 5});

            var pieces = VoronoiCellBuilder.splitPolygonAmongSites(
                GeometryTestSupport.buildSquare(SPLIT_SQUARE_SIDE),
                sites);

            // The bisector of the two sites is x = 5, so each takes its own half of
            // the square and neither crosses the line.
            assertThat(pieces.get(0))
                .allMatch(vertex -> vertex[0] <= 5 + 1e-9);
            assertThat(pieces.get(1))
                .allMatch(vertex -> vertex[0] >= 5 - 1e-9);

            assertThat(GeometryTestSupport.computeSignedArea(pieces.get(0)))
                .isCloseTo(WHOLE_SQUARE_AREA / 2, GeometryTestSupport.buildAssertionSlack());
            assertThat(GeometryTestSupport.computeSignedArea(pieces.get(1)))
                .isCloseTo(WHOLE_SQUARE_AREA / 2, GeometryTestSupport.buildAssertionSlack());
        }

        @Test
        void threeSitesTakeDisjointPiecesThatCoverThePolygon() {

            var sites = Arrays.asList(
                new double[] {3, 3},
                new double[] {7, 3},
                new double[] {5, 8});

            var pieces = VoronoiCellBuilder.splitPolygonAmongSites(
                GeometryTestSupport.buildSquare(SPLIT_SQUARE_SIDE),
                sites);

            // Areas summing to the whole square pins both halves of the contract at
            // once: no piece is dropped (they cover it) and no area is handed out
            // twice (they would over-sum if they overlapped).
            var total = 0.0;

            for (var i = 0; i < sites.size(); i++) {

                assertThat(isPointInsidePolygon(sites.get(i), pieces.get(i)))
                    .as("site %d lies inside its own piece", i)
                    .isTrue();

                total += GeometryTestSupport.computeSignedArea(pieces.get(i));
            }
            assertThat(total)
                .isCloseTo(WHOLE_SQUARE_AREA, GeometryTestSupport.buildAssertionSlack());
        }

        @Test
        void aSiteWithNoNearestRegionGetsAnEmptyPiece() {

            var sites = Arrays.asList(
                new double[] {5, 5},
                new double[] {1000, 1000});

            var pieces = VoronoiCellBuilder.splitPolygonAmongSites(
                GeometryTestSupport.buildSquare(SPLIT_SQUARE_SIDE),
                sites);

            // The far site is beaten to every point of the square by the near one,
            // so it is handed nothing rather than a degenerate shape.
            assertThat(GeometryTestSupport.computeSignedArea(pieces.get(0)))
                .isCloseTo(WHOLE_SQUARE_AREA, GeometryTestSupport.buildAssertionSlack());
            assertThat(pieces.get(1))
                .isEmpty();
        }

        @Test
        void aPolygonThatEnclosesNoAreaSplitsIntoEmptyPieces() {

            var degenerate = Arrays.asList(
                new double[] {0, 0},
                new double[] {10, 0});
            var sites = Arrays.asList(
                new double[] {2, 5},
                new double[] {8, 5});

            assertThat(VoronoiCellBuilder.splitPolygonAmongSites(degenerate, sites))
                .hasSize(2)
                .allMatch(List::isEmpty);
        }
    }

    private static double computeDistance(double[] a, double[] b) {

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
