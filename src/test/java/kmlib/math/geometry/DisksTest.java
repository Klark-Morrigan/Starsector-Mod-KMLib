package kmlib.math.geometry;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static kmlib.math.geometry.GeometryTestSupport.bigSquare;
import static kmlib.math.geometry.GeometryTestSupport.signedArea;
import static kmlib.math.geometry.GeometryTestSupport.within;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the contract of {@link Disks#intersectWithDisk}: a polygon reaching past the
 * disk comes back bounded to it, one wholly inside comes back whole, and one wholly
 * outside comes back empty.
 *
 * <p>And of {@link Disks#subtractDisk}: a disk strictly inside the polygon leaves a
 * hole, a disk biting an edge leaves the rest, a disk covering the polygon leaves
 * nothing, and a disk that misses leaves the polygon whole.
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

    // The total area of a set of pieces - what the difference must add up to, since its
    // pieces are disjoint.
    private static double sumAreas(List<List<double[]>> pieces) {
        var total = 0.0;
        for (var piece : pieces) {
            total += signedArea(piece);
        }
        return total;
    }

    // The slack an assertion about a disk's own area allows. Scaled to the disk rather
    // than fixed, since the chord error the approximation carries grows with it.
    private static Offset<Double> withinDiskArea(double radius) {
        return Offset.offset(Math.PI * radius * radius * AREA_TOLERANCE_FRACTION);
    }

    @Nested
    class IntersectWithDisk {
        @Test
        void polygon_is_bounded_to_the_disk_when_it_reaches_past_it() {
            // The square reaches far past a disk centred inside it, so the disk alone
            // survives: the reach bound, not the polygon, decides the result's area.
            var bounded = Disks.intersectWithDisk(
                    bigSquare(100), new double[] {50, 50}, 20, SEGMENTS);

            assertThat(signedArea(bounded))
                    .isCloseTo(Math.PI * 20 * 20, withinDiskArea(20));
        }

        @Test
        void polygon_comes_back_whole_when_the_disk_encloses_it() {
            // A bound wider than the polygon clips nothing away - the reach cap only
            // ever removes, never invents, so the square keeps its full area.
            var bounded = Disks.intersectWithDisk(
                    bigSquare(10), new double[] {5, 5}, 1000, SEGMENTS);

            assertThat(signedArea(bounded)).isCloseTo(100.0, within());
        }

        @Test
        void nothing_is_kept_when_the_polygon_lies_wholly_outside_the_disk() {
            // The cap case that must collapse: a wedge past its owner's reach keeps no
            // colour at all, so an empty ring - not a sliver - is the answer.
            var bounded = Disks.intersectWithDisk(
                    bigSquare(10), new double[] {500, 500}, 20, SEGMENTS);

            assertThat(bounded).isEmpty();
        }

        @Test
        void nothing_is_kept_when_the_disk_is_too_small_to_enclose_area() {
            // A zero radius has no interior to keep, and its chords are too short to
            // carry a normal, so it is reported as enclosing nothing rather than let
            // to decide sides from noise.
            var bounded = Disks.intersectWithDisk(
                    bigSquare(10), new double[] {5, 5}, 0, SEGMENTS);

            assertThat(bounded).isEmpty();
        }

        @Test
        void nothing_is_kept_when_the_polygon_encloses_no_area() {
            var bounded = Disks.intersectWithDisk(
                    List.of(new double[] {0, 0}, new double[] {10, 0}),
                    new double[] {5, 0}, 20, SEGMENTS);

            assertThat(bounded).isEmpty();
        }

        @Test
        void a_disk_of_too_few_segments_is_rejected() {
            assertThatThrownBy(() -> Disks.intersectWithDisk(
                    bigSquare(10), new double[] {5, 5}, 2, 2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("segments");
        }
    }

    @Nested
    class SubtractDisk {
        @Test
        void a_hole_is_punched_when_the_disk_lies_strictly_inside_the_polygon() {
            var center = new double[] {50, 50};

            var remainder = Disks.subtractDisk(bigSquare(100), center, 20, SEGMENTS);

            // The disk's area is gone from the total, and no piece covers the centre -
            // together that is a hole: the pieces surround the withheld disk rather
            // than one of them merely being notched.
            assertThat(sumAreas(remainder))
                    .isCloseTo(100 * 100 - Math.PI * 20 * 20, withinDiskArea(20));
            for (var piece : remainder) {
                // A line through the centre reports the piece's interior as parameter
                // spans measured from that centre, so a span with ends of opposite sign
                // is one covering it. None may: the centre is withheld from every piece.
                assertThat(PolygonRegions.findLineInteriorSpans(
                        List.of(piece), new DirectedLine(center[0], center[1], 1, 0)))
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
                    bigSquare(100), new double[] {0, 0}, 20, SEGMENTS);

            assertThat(sumAreas(remainder))
                    .isCloseTo(100 * 100 - Math.PI * 20 * 20 / 4, withinDiskArea(20));
        }

        @Test
        void nothing_survives_when_the_disk_covers_the_polygon() {
            // The pocket case that must collapse: a wedge wholly inside the keep-out
            // is all pocket, so it drops out entirely rather than leaving slivers.
            var remainder = Disks.subtractDisk(
                    bigSquare(10), new double[] {5, 5}, 100, SEGMENTS);

            assertThat(remainder).isEmpty();
        }

        @Test
        void the_polygon_comes_back_whole_when_the_disk_misses_it() {
            var remainder = Disks.subtractDisk(
                    bigSquare(10), new double[] {500, 500}, 20, SEGMENTS);

            assertThat(sumAreas(remainder)).isCloseTo(100.0, within());
        }

        @Test
        void the_polygon_comes_back_whole_when_the_disk_is_too_small_to_enclose_area() {
            // Nothing is withheld by a disk with no interior, so a caller that disables
            // its keep-out by zeroing the radius gets the unclipped polygon back.
            var remainder = Disks.subtractDisk(
                    bigSquare(10), new double[] {5, 5}, 0, SEGMENTS);

            assertThat(sumAreas(remainder)).isCloseTo(100.0, within());
        }

        @Test
        void nothing_survives_when_the_polygon_encloses_no_area() {
            var remainder = Disks.subtractDisk(
                    List.of(new double[] {0, 0}, new double[] {10, 0}),
                    new double[] {50, 50}, 20, SEGMENTS);

            assertThat(remainder).isEmpty();
        }

        @Test
        void a_disk_of_too_few_segments_is_rejected() {
            assertThatThrownBy(() -> Disks.subtractDisk(
                    bigSquare(10), new double[] {5, 5}, 2, 2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("segments");
        }

        @Test
        void the_pieces_wind_the_same_way_as_the_polygon() {
            // Clipping preserves the subject's winding, so every piece reads
            // counter-clockwise like the square it came from - the sign a consumer's
            // fold-guard and fill keep on.
            var remainder = Disks.subtractDisk(
                    bigSquare(100), new double[] {50, 50}, 20, SEGMENTS);

            assertThat(remainder).isNotEmpty();
            for (var piece : remainder) {
                assertThat(signedArea(piece)).isPositive();
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
            var square = bigSquare(100);
            var center = new double[] {30, 40};

            var inside = Disks.intersectWithDisk(square, center, 25, SEGMENTS);
            var outside = Disks.subtractDisk(square, center, 25, SEGMENTS);

            assertThat(signedArea(inside) + sumAreas(outside))
                    .isCloseTo(100 * 100, within());
        }
    }
}
