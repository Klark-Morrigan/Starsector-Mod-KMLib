package kmlib.math.geometry;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static kmlib.math.geometry.GeometryTestSupport.buildAssertionSlack;
import static kmlib.math.geometry.GeometryTestSupport.buildSquare;
import static kmlib.math.geometry.GeometryTestSupport.computeSignedArea;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link Disks#intersectWithDisk}: a polygon reaching past the
 * disk comes back bounded to it, one wholly inside comes back whole, and one wholly
 * outside comes back empty.
 *
 * <p>And of {@link Disks#subtractDisk}: a disk strictly inside the polygon leaves a
 * hole, a disk biting an edge leaves the rest, a disk covering the polygon leaves
 * nothing, and a disk that misses leaves the polygon whole.
 *
 * <p>And of {@link Disks#subtractDiskWithLabels}: the same pieces, each edge naming what
 * lies across it - the subject's own labels carried through, the withheld disk across the
 * rim, and a sibling piece across the fan cuts that split the remainder up.
 *
 * <p>And of the pair: the two senses partition the polygon exactly - what one keeps is
 * what the other drops, with no area lost or double-counted between them.
 */
final class DisksTest {

    // Enough sides that the approximated disk's area lands within a percent of the true
    // circle's, so an area assertion can be stated against pi * r^2 with real tolerance
    // rather than against whatever the polygon happens to enclose.
    private static final int SEGMENTS = 64;

    // The slack the disk-area assertions allow, as a fraction of the disk's area: the
    // inscribed 64-gon falls about 0.16% short of its circle, so a percent absorbs the
    // chord error without hiding a real miscount.
    private static final double AREA_TOLERANCE_FRACTION = 0.01;

    // The slack the collinearity test allows, relative to the span of the run tested:
    // the vertices are clip intersections, so a straight line reaches them with rounding
    // error, and only a real corner may register as one.
    private static final double COLLINEAR_TOLERANCE_FRACTION = 1e-9;

    // The labels the labelled-subtract fixtures use for the two things a cut can have
    // across it, and for the square's own four sides. Distinct values, so an assertion
    // can name exactly which of the six a given edge came back as.
    private static final int RIM_LABEL = 7;
    private static final int FAN_CUT_LABEL = 8;
    private static final int[] SQUARE_SIDE_LABELS = {10, 11, 12, 13};

    // The total area of a set of pieces - what the difference must add up to, since its
    // pieces are disjoint.
    private static double sumAreas(List<List<double[]>> pieces) {

        var total = 0.0;
        for (var piece : pieces) {
            total += computeSignedArea(piece);
        }
        return total;
    }

    // The slack an assertion about a disk's own area allows. Scaled to the disk rather
    // than fixed, since the chord error the approximation carries grows with it.
    private static Offset<Double> computeWithinDiskArea(double radius) {
        return Offset.offset(Math.PI
            * radius
            * radius
            * AREA_TOLERANCE_FRACTION);
    }

    // The square subject with each side labelled for itself, so a side the clip left
    // alone can be told apart from one it cut and from the other three sides.
    private static LabelledPolygon buildLabelledSquare(double side, int[] sideLabels) {
        return LabelledPolygon.fromLabelledEdges(buildSquare(side), sideLabels);
    }

    // Every label appearing on any edge of any piece - what the pieces, between them,
    // claim lies across their boundaries.
    private static List<Integer> collectEdgeLabels(List<LabelledPolygon> pieces) {

        var labels = new ArrayList<Integer>();
        for (var piece : pieces) {
            for (var label : piece.getEdgeLabels()) {
                labels.add(label);
            }
        }
        return labels;
    }

    // The total length of the edges labelled {@code label} across all the pieces - how
    // much boundary the pieces hand to one thing.
    private static double computeLabelledLength(List<LabelledPolygon> pieces, int label) {

        var total = 0.0;
        for (var piece : pieces) {

            var vertices = piece.getVertices();
            var labels = piece.getEdgeLabels();

            for (var i = 0; i < vertices.size(); i++) {

                if (labels[i] == label) {

                    total += Points.computeDistance(
                        vertices.get(i),
                        vertices.get((i + 1) % vertices.size()));
                }
            }
        }
        return total;
    }

    // Whether the piece has two edges that run on in the same direction from a shared
    // vertex - one straight line the ring breaks in two - carrying the two given labels.
    private static boolean hasCollinearEdgesLabelledApart(
            LabelledPolygon piece,
            int firstLabel,
            int secondLabel) {

        var vertices = piece.getVertices();
        var labels = piece.getEdgeLabels();

        for (var i = 0; i < vertices.size(); i++) {

            var next = (i + 1) % vertices.size();
            var isPair = labels[i] == firstLabel && labels[next] == secondLabel
                    || labels[i] == secondLabel && labels[next] == firstLabel;

            if (isPair
                    && isCollinear(
                        vertices.get(i),
                        vertices.get(next),
                        vertices.get((next + 1) % vertices.size()))) {
                return true;
            }
        }
        return false;
    }

    // Whether three points lie on one line, judged by the area of the triangle they span
    // against the length of the run they span it over - a shape-scale test, so it does
    // not tighten or loosen with the coordinates' magnitude.
    private static boolean isCollinear(double[] first, double[] second, double[] third) {

        var cross = (second[0] - first[0]) * (third[1] - first[1])
            - (second[1] - first[1]) * (third[0] - first[0]);

        var span = Points.computeDistance(first, second)
            + Points.computeDistance(second, third);

        return Math.abs(cross) < span * span * COLLINEAR_TOLERANCE_FRACTION;
    }

    @Nested
    class IntersectWithDisk {

        @Test
        void polygon_is_bounded_to_the_disk_when_it_reaches_past_it() {
            // The square reaches far past a disk centred inside it, so the disk alone
            // survives: the reach bound, not the polygon, decides the result's area.
            var bounded = Disks.intersectWithDisk(
                buildSquare(100),
                new Disk(new double[] {50, 50}, 20, SEGMENTS));

            assertThat(computeSignedArea(bounded))
                .isCloseTo(Math.PI * 20 * 20, computeWithinDiskArea(20));
        }

        @Test
        void polygon_comes_back_whole_when_the_disk_encloses_it() {
            // A bound wider than the polygon clips nothing away - the reach cap only
            // ever removes, never invents, so the square keeps its full area.
            var bounded = Disks.intersectWithDisk(
                buildSquare(10),
                new Disk(new double[] {5, 5}, 1000, SEGMENTS));

            assertThat(computeSignedArea(bounded))
                .isCloseTo(100.0, buildAssertionSlack());
        }

        @Test
        void nothing_is_kept_when_the_polygon_lies_wholly_outside_the_disk() {
            // The cap case that must collapse: a wedge past its owner's reach keeps no
            // colour at all, so an empty ring - not a sliver - is the answer.
            var bounded = Disks.intersectWithDisk(
                buildSquare(10),
                new Disk(new double[] {500, 500}, 20, SEGMENTS));

            assertThat(bounded)
                .isEmpty();
        }

        @Test
        void nothing_is_kept_when_the_disk_is_too_small_to_enclose_area() {
            // A zero radius has no interior to keep, and its chords are too short to
            // carry a normal, so it is reported as enclosing nothing rather than let
            // to decide sides from noise.
            var bounded = Disks.intersectWithDisk(
                buildSquare(10),
                new Disk(new double[] {5, 5}, 0, SEGMENTS));

            assertThat(bounded)
                .isEmpty();
        }

        @Test
        void nothing_is_kept_when_the_polygon_encloses_no_area() {

            var bounded = Disks.intersectWithDisk(
                List.of(new double[] {0, 0}, new double[] {10, 0}),
                new Disk(new double[] {5, 0}, 20, SEGMENTS));

            assertThat(bounded)
                .isEmpty();
        }

    }

    @Nested
    class SubtractDisk {

        @Test
        void a_hole_is_punched_when_the_disk_lies_strictly_inside_the_polygon() {

            var centre = new double[] {50, 50};
            var remainder = Disks.subtractDisk(buildSquare(100), new Disk(centre, 20, SEGMENTS));

            // The disk's area is gone from the total, and no piece covers the centre -
            // together that is a hole: the pieces surround the withheld disk rather
            // than one of them merely being notched.
            assertThat(sumAreas(remainder))
                .isCloseTo(100 * 100 - Math.PI * 20 * 20, computeWithinDiskArea(20));

            for (var piece : remainder) {
                // A line through the centre reports the piece's interior as parameter
                // spans measured from that centre, so a span with ends of opposite sign
                // is one covering it. None may: the centre is withheld from every piece.
                assertThat(PolygonRegions.findLineInteriorSpans(
                        List.of(piece),
                        new DirectedLine(centre[0], centre[1], 1, 0)))
                    .allSatisfy(span -> assertThat(span[0] * span[1])
                        .as("no piece covers the withheld centre")
                        .isGreaterThanOrEqualTo(0.0));
            }
        }

        @Test
        void the_rest_survives_when_the_disk_bites_into_an_edge() {
            // A disk centred on a corner takes a quarter of itself out of the square;
            // the bite leaves a concave remainder, which is why the result is pieces.
            var remainder = Disks.subtractDisk(
                buildSquare(100),
                new Disk(new double[] {0, 0}, 20, SEGMENTS));

            assertThat(sumAreas(remainder))
                .isCloseTo(100 * 100 - Math.PI * 20 * 20 / 4, computeWithinDiskArea(20));
        }

        @Test
        void nothing_survives_when_the_disk_covers_the_polygon() {
            // The pocket case that must collapse: a wedge wholly inside the keep-out
            // is all pocket, so it drops out entirely rather than leaving slivers.
            var remainder = Disks.subtractDisk(
                buildSquare(10),
                new Disk(new double[] {5, 5}, 100, SEGMENTS));

            assertThat(remainder)
                .isEmpty();
        }

        @Test
        void the_polygon_comes_back_whole_when_the_disk_misses_it() {

            var remainder = Disks.subtractDisk(
                buildSquare(10),
                new Disk(new double[] {500, 500}, 20, SEGMENTS));

            assertThat(sumAreas(remainder))
                .isCloseTo(100.0, buildAssertionSlack());
        }

        @Test
        void the_polygon_comes_back_whole_when_the_disk_is_too_small_to_enclose_area() {
            // Nothing is withheld by a disk with no interior, so a caller that disables
            // its keep-out by zeroing the radius gets the unclipped polygon back.
            var remainder = Disks.subtractDisk(
                buildSquare(10),
                new Disk(new double[] {5, 5}, 0, SEGMENTS));

            assertThat(sumAreas(remainder))
                .isCloseTo(100.0, buildAssertionSlack());
        }

        @Test
        void nothing_survives_when_the_polygon_encloses_no_area() {

            var remainder = Disks.subtractDisk(
                List.of(new double[] {0, 0}, new double[] {10, 0}),
                new Disk(new double[] {50, 50}, 20, SEGMENTS));

            assertThat(remainder)
                .isEmpty();
        }

        @Test
        void the_pieces_wind_the_same_way_as_the_polygon() {
            // Clipping preserves the subject's winding, so every piece reads
            // counter-clockwise like the square it came from - the sign a consumer's
            // fold-guard and fill keep on.
            var remainder = Disks.subtractDisk(
                buildSquare(100),
                new Disk(new double[] {50, 50}, 20, SEGMENTS));

            assertThat(remainder)
                .isNotEmpty();

            for (var piece : remainder) {

                assertThat(computeSignedArea(piece))
                    .isPositive();
            }
        }
    }

    @Nested
    class SubtractDiskWithLabels {

        @Test
        void an_edge_the_clip_left_alone_still_names_what_it_arrived_naming() {
            // A disk biting one corner shortens two of the square's sides and leaves the
            // other two untouched; all four must still name the side they always were,
            // since nothing moved to the far side of any of them.
            var pieces = Disks.subtractDiskWithLabels(
                buildLabelledSquare(100, SQUARE_SIDE_LABELS),
                new Disk(new double[] {0, 0}, 20, SEGMENTS),
                RIM_LABEL,
                FAN_CUT_LABEL);

            assertThat(collectEdgeLabels(pieces))
                .contains(
                    SQUARE_SIDE_LABELS[0],
                    SQUARE_SIDE_LABELS[1],
                    SQUARE_SIDE_LABELS[2],
                    SQUARE_SIDE_LABELS[3]);
        }

        @Test
        void no_label_of_the_clips_own_leaks_out() {
            // The clip tells its own cuts apart internally, so a subject that labels its
            // sides with the very values it uses to do that must still come back saying
            // what it said - the two label spaces cannot be the same one.
            var collidingLabels = new int[] {-1, -2, -1, -2};

            var pieces = Disks.subtractDiskWithLabels(
                buildLabelledSquare(100, collidingLabels),
                new Disk(new double[] {0, 0}, 20, SEGMENTS),
                RIM_LABEL,
                FAN_CUT_LABEL);

            assertThat(collectEdgeLabels(pieces))
                .contains(-1, -2)
                .containsOnly(-1, -2, RIM_LABEL, FAN_CUT_LABEL);
        }

        @Test
        void a_rim_and_a_fan_cut_lying_on_one_line_are_labelled_apart() {
            // A disk strictly inside the square is cut around by lines that run right
            // across it, so one cut runs on past the chord it was made for: the withheld
            // disk lies across the chord's span, a sibling piece across the rest of the
            // same straight line. The piece must break at the chord's end and call the
            // two parts what they are.
            var pieces = Disks.subtractDiskWithLabels(
                buildLabelledSquare(100, SQUARE_SIDE_LABELS),
                new Disk(new double[] {50, 50}, 20, SEGMENTS),
                RIM_LABEL,
                FAN_CUT_LABEL);

            assertThat(pieces)
                .anyMatch(piece ->
                    hasCollinearEdgesLabelledApart(piece, RIM_LABEL, FAN_CUT_LABEL));
        }

        @Test
        void the_rim_is_bordered_once_over_its_whole_length() {
            // The rim label goes on exactly the withheld disk's boundary - no more, no
            // less. Length is what says so: a fan cut mistaken for rim would lengthen the
            // total past the disk's circumference, and a rim mistaken for a fan cut would
            // leave part of the hole unbordered.
            var pieces = Disks.subtractDiskWithLabels(
                buildLabelledSquare(100, SQUARE_SIDE_LABELS),
                new Disk(new double[] {50, 50}, 20, SEGMENTS),
                RIM_LABEL,
                FAN_CUT_LABEL);

            assertThat(computeLabelledLength(pieces, RIM_LABEL))
                .isCloseTo(
                    2 * Math.PI * 20,
                    Offset.offset(2 * Math.PI * 20 * AREA_TOLERANCE_FRACTION));
        }

        @Test
        void the_rim_is_bordered_once_where_the_disk_bites_into_an_edge() {
            // Half the disk's rim lies outside the square here, so only the quarter the
            // square covers is anyone's border. The chord lines running on outside the
            // subject must not be counted rim for the stretch where there is no subject
            // to border it, which the strictly-inside case cannot catch.
            var pieces = Disks.subtractDiskWithLabels(
                buildLabelledSquare(100, SQUARE_SIDE_LABELS),
                new Disk(new double[] {0, 0}, 20, SEGMENTS),
                RIM_LABEL,
                FAN_CUT_LABEL);

            assertThat(computeLabelledLength(pieces, RIM_LABEL))
                .isCloseTo(
                    2 * Math.PI * 20 / 4,
                    Offset.offset(2 * Math.PI * 20 * AREA_TOLERANCE_FRACTION));
        }

        @Test
        void the_polygon_comes_back_whole_with_its_labels_when_the_disk_encloses_no_area() {
            // A caller disabling its keep-out by zeroing the radius gets its subject back
            // untouched - and the labels it handed in, not the ones the walk uses to talk
            // to itself, since this path never reaches the cuts that translate them.
            var pieces = Disks.subtractDiskWithLabels(
                buildLabelledSquare(100, SQUARE_SIDE_LABELS),
                new Disk(new double[] {50, 50}, 0, SEGMENTS),
                RIM_LABEL,
                FAN_CUT_LABEL);

            assertThat(pieces)
                .hasSize(1);
            assertThat(pieces.get(0).getEdgeLabels())
                .containsExactly(SQUARE_SIDE_LABELS);

            assertThat(computeSignedArea(pieces.get(0).getVertices()))
                .isCloseTo(100.0 * 100, buildAssertionSlack());
        }

        @Test
        void nothing_survives_when_the_disk_covers_the_polygon() {
            // Every piece is clipped below area here, so the labels have nothing to ride
            // on: an empty list, not a set of labelled slivers.
            var pieces = Disks.subtractDiskWithLabels(
                buildLabelledSquare(10, SQUARE_SIDE_LABELS),
                new Disk(new double[] {5, 5}, 100, SEGMENTS),
                RIM_LABEL,
                FAN_CUT_LABEL);

            assertThat(pieces)
                .isEmpty();
        }

        @Test
        void the_labelled_pieces_are_the_plain_ones_with_labels_on_them() {
            // The plain subtract is this one with its labels dropped, so the two cannot
            // disagree about the shape of the remainder - which is the point of having
            // one walk rather than two.
            var centre = new double[] {30, 40};
            var plain = Disks.subtractDisk(buildSquare(100), new Disk(centre, 25, SEGMENTS));
            var labelled = Disks.subtractDiskWithLabels(
                buildLabelledSquare(100, SQUARE_SIDE_LABELS),
                new Disk(centre, 25, SEGMENTS),
                RIM_LABEL,
                FAN_CUT_LABEL);

            assertThat(labelled)
                .hasSameSizeAs(plain);

            for (var i = 0; i < plain.size(); i++) {

                var labelledVertices = labelled.get(i).getVertices();
                var plainVertices = plain.get(i);

                assertThat(labelledVertices)
                    .hasSameSizeAs(plainVertices);

                for (var vertex = 0; vertex < plainVertices.size(); vertex++) {

                    assertThat(labelledVertices.get(vertex))
                        .containsExactly(plainVertices.get(vertex), buildAssertionSlack());
                }
            }
        }
    }

    @Nested
    class BothSenses {
        
        @Test
        void the_two_senses_partition_the_polygon_between_them() {
            // The keep-out and the reach bound are the same disk read opposite ways, so
            // what one keeps is exactly what the other drops: their areas must sum to
            // the whole polygon, with no gap left between the two arcs and no strip
            // handed to both.
            var square = buildSquare(100);
            var centre = new double[] {30, 40};

            var inside = Disks.intersectWithDisk(square, new Disk(centre, 25, SEGMENTS));
            var outside = Disks.subtractDisk(square, new Disk(centre, 25, SEGMENTS));

            assertThat(computeSignedArea(inside) + sumAreas(outside))
                .isCloseTo(100 * 100, buildAssertionSlack());
        }
    }
}
