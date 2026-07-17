package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link Rings#findSurvivingVertices}: a run of coincident
 * vertices is one corner, taking its position from the first of the run and its
 * outgoing edge from the last, and a ring closing back onto its own first corner
 * drops the repeat.
 *
 * <p>And of the two dedups built on it - that the plain form drops the same vertices
 * the indices say, and that the labelled form carries each surviving edge's label
 * through the same judgement rather than letting the labels slide out of step with
 * the vertices they belong to.
 */
final class RingsTest {
    // A point far enough inside the coincidence tolerance to be the same corner
    // recorded twice, but not bit-identical - the rounding whisker two routines that
    // computed the same corner separately actually differ by.
    private static final double WHISKER = Limits.MIN_EDGE_LENGTH / 10;

    // Labels the fixtures put on a ring's edges, one per side, so a test can name
    // which edge a surviving label came from.
    private static final int[] SQUARE_EDGE_LABELS = {10, 11, 12, 13};

    @Nested
    class FindSurvivingVertices {
        @Test
        void every_vertex_survives_a_ring_with_no_duplicates() {
            var survivors = Rings.findSurvivingVertices(GeometryTestSupport.square());

            assertThat(survivors.pointIndices()).containsExactly(0, 1, 2, 3);
            assertThat(survivors.outgoingEdgeIndices()).containsExactly(0, 1, 2, 3);
        }

        @Test
        void a_run_takes_its_position_from_the_first_and_its_outgoing_edge_from_the_last() {
            // The corner at index 1 is recorded three times. Its position must come from
            // the first of the run, so the answer does not drift down it, while the edge
            // that really leaves the corner is the third's - the first two only step to
            // the next duplicate.
            var ring = List.of(
                    new double[] {0, 0},
                    new double[] {10, 0},
                    new double[] {10 + WHISKER, 0},
                    new double[] {10, WHISKER},
                    new double[] {10, 10});

            var survivors = Rings.findSurvivingVertices(ring);

            assertThat(survivors.pointIndices()).containsExactly(0, 1, 4);
            assertThat(survivors.outgoingEdgeIndices()).containsExactly(0, 3, 4);
        }

        @Test
        void the_repeat_of_the_first_corner_closing_the_ring_drops() {
            // A ring stated with its first corner written again at the end closes by a
            // zero-length edge. The repeat goes, and the edge reaching it - a real one -
            // stays, so the survivors still name three real edges.
            var ring = List.of(
                    new double[] {0, 0},
                    new double[] {10, 0},
                    new double[] {10, 10},
                    new double[] {WHISKER, 0});

            var survivors = Rings.findSurvivingVertices(ring);

            assertThat(survivors.pointIndices()).containsExactly(0, 1, 2);
            assertThat(survivors.outgoingEdgeIndices()).containsExactly(0, 1, 2);
        }

        @Test
        void nothing_survives_an_empty_ring() {
            var survivors = Rings.findSurvivingVertices(List.of());

            assertThat(survivors.pointIndices()).isEmpty();
            assertThat(survivors.outgoingEdgeIndices()).isEmpty();
        }
    }

    @Nested
    class RemoveConsecutiveDuplicates {
        @Test
        void a_ring_with_no_duplicates_comes_back_whole() {
            var cleaned = Rings.removeConsecutiveDuplicates(GeometryTestSupport.square());

            assertThat(cleaned).hasSize(4);
        }

        @Test
        void a_duplicated_corner_is_recorded_once() {
            var ring = List.of(
                    new double[] {0, 0},
                    new double[] {10, 0},
                    new double[] {10 + WHISKER, 0},
                    new double[] {10, 10});

            var cleaned = Rings.removeConsecutiveDuplicates(ring);

            assertThat(cleaned).hasSize(3);
            assertThat(cleaned.get(1)).containsExactly(10, 0);
        }

        @Test
        void the_repeat_of_the_first_corner_closing_the_ring_drops() {
            var ring = List.of(
                    new double[] {0, 0},
                    new double[] {10, 0},
                    new double[] {10, 10},
                    new double[] {WHISKER, 0});

            var cleaned = Rings.removeConsecutiveDuplicates(ring);

            assertThat(cleaned).hasSize(3);
        }
    }

    @Nested
    class RemoveConsecutiveDuplicatesWithLabels {
        @Test
        void a_ring_with_no_duplicates_keeps_every_label_where_it_was() {
            var cleaned = Rings.removeConsecutiveDuplicates(LabelledPolygon.fromLabelledEdges(
                    GeometryTestSupport.square(), SQUARE_EDGE_LABELS));

            assertThat(cleaned.getEdgeLabels()).containsExactly(SQUARE_EDGE_LABELS);
        }

        @Test
        void a_surviving_corner_takes_the_label_of_the_edge_that_really_leaves_it() {
            // The corner at index 1 is recorded twice, so the edge leaving it is not the
            // zero-length step to its duplicate (label 11) but the duplicate's own
            // outgoing edge (label 12). Taking 11 here would hand the corner a label
            // naming nothing, and slide every label after it out of step.
            var ring = LabelledPolygon.fromLabelledEdges(
                    List.of(
                            new double[] {0, 0},
                            new double[] {10, 0},
                            new double[] {10 + WHISKER, 0},
                            new double[] {10, 10}),
                    SQUARE_EDGE_LABELS);

            var cleaned = Rings.removeConsecutiveDuplicates(ring);

            assertThat(cleaned.getVertices().get(1)).containsExactly(10, 0);
            assertThat(cleaned.getEdgeLabels()).containsExactly(10, 12, 13);
        }

        @Test
        void the_repeat_closing_the_ring_drops_its_label_with_it() {
            // The last vertex repeats the first, so its outgoing edge is the zero-length
            // one closing the ring and its label (13) names nothing. The edge reaching
            // it (12) is real and must survive.
            var ring = LabelledPolygon.fromLabelledEdges(
                    List.of(
                            new double[] {0, 0},
                            new double[] {10, 0},
                            new double[] {10, 10},
                            new double[] {WHISKER, 0}),
                    SQUARE_EDGE_LABELS);

            var cleaned = Rings.removeConsecutiveDuplicates(ring);

            assertThat(cleaned.getEdgeLabels()).containsExactly(10, 11, 12);
        }
    }
}
