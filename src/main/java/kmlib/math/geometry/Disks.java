package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Clips convex polygons against a disk, approximated by a regular polygon.
 *
 * <p>A disk is the shape both a keep-out and a reach bound take: "stay at least this
 * far from that point" and "reach at most this far from that point" are the same disk
 * read in opposite senses. So one clip answers both - {@link #intersectWithDisk} keeps
 * what falls inside it, {@link #subtractDisk} keeps what falls outside - and a caller
 * that needs both senses of the same disk gets two halves that fit together exactly,
 * rather than two independently rounded shapes that leave a seam.
 *
 * <p>The disk is approximated at a caller-chosen segment count rather than treated as a
 * true circle, so its arc is a chain of straight chords. A caller whose shapes already
 * carry a rounded bound (a {@link VoronoiCellBuilder} cell, whose max-radius seed is a
 * regular polygon) passes the same count that bound was seeded at, and the two arcs land
 * on the same chords instead of crossing each other at every vertex.
 *
 * <p>Both operations require a convex subject: they are half-plane clips, which is what
 * lets the difference come back as convex pieces rather than needing a general polygon
 * boolean. A concave subject yields undefined output rather than an error.
 */
public final class Disks {
    // The label stamped on every cut edge. These clips carry no per-edge identity out
    // (they return plain rings), so LabelledPolygon's label bookkeeping is unused and
    // one throwaway value serves every clip.
    private static final int UNUSED_EDGE_LABEL = 0;

    private Disks() {
    }

    /**
     * Keeps the part of {@code polygon} inside the disk of {@code radius} about
     * {@code center} - the polygon bounded to a maximum reach from a point.
     *
     * @param polygon  the convex region to clip, as {x, y} vertices in winding order
     * @param center   the disk's centre as {x, y}
     * @param radius   the disk's radius; a radius too small to enclose area leaves
     *                 nothing inside, so the result is empty
     * @param segments sides of the regular polygon approximating the disk
     * @return the clipped region as {x, y} vertices in the polygon's winding; empty
     *         when nothing of the polygon lies inside the disk
     * @throws IllegalArgumentException when {@code segments} cannot enclose an area
     */
    public static List<double[]> intersectWithDisk(
            List<double[]> polygon,
            double[] center,
            double radius,
            int segments) {
        return computeDiskSplit(polygon, center, radius, segments)
                .inside();
    }

    /**
     * Keeps the part of {@code polygon} outside the disk of {@code radius} about
     * {@code center} - the polygon with the disk withheld from it as a keep-out.
     *
     * <p>The remainder is returned as convex pieces rather than one ring, because
     * removing a disk from a polygon need not leave a shape one ring can describe: a
     * disk biting into an edge leaves a concave region, and a disk strictly inside the
     * polygon leaves a hole. Pieces are disjoint and together cover exactly the
     * remainder, so a caller filling them paints the remainder once, and a caller
     * chaining their edges into rings (see {@link EdgeRings}) recovers the outline -
     * including the hole, as its own ring - with the seams between pieces dropped as
     * shared interior edges.
     *
     * @param polygon  the convex region to clip, as {x, y} vertices in winding order
     * @param center   the disk's centre as {x, y}
     * @param radius   the disk's radius; a radius too small to enclose area withholds
     *                 nothing, so the polygon comes back whole
     * @param segments sides of the regular polygon approximating the disk
     * @return the remainder as disjoint convex pieces, each a list of {x, y} vertices
     *         in the polygon's winding; empty when the disk covers the whole polygon
     * @throws IllegalArgumentException when {@code segments} cannot enclose an area
     */
    public static List<List<double[]>> subtractDisk(
            List<double[]> polygon,
            double[] center,
            double radius,
            int segments) {
        return computeDiskSplit(polygon, center, radius, segments)
                .outsidePieces();
    }

    // The two senses of one disk clip, produced by the single walk that computes them.
    // {@code inside} is the polygon bounded to the disk; {@code outsidePieces} is the
    // rest of it, as disjoint convex pieces. Together they partition the polygon.
    private record DiskSplit(List<double[]> inside, List<List<double[]>> outsidePieces) {
    }

    // Splits {@code polygon} along the disk's boundary in one walk over the disk's
    // edges. Each edge's line is a half-plane cut: what is on its far side is outside
    // the disk for good (no later edge can bring it back, the disk being convex), so it
    // drops out as a finished piece, while what is on its near side stays in play for
    // the next edge. After every edge has cut, what is still in play is inside every
    // one of them - the intersection. That is why one walk yields both senses: the
    // pieces shed along the way are the difference, the survivor is the intersection.
    private static DiskSplit computeDiskSplit(
            List<double[]> polygon,
            double[] center,
            double radius,
            int segments) {
        if (segments < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            throw new IllegalArgumentException(
                    "segments must be at least " + Limits.MIN_VERTICES_TO_ENCLOSE_AREA
                            + " to approximate a disk: " + segments);
        }
        var subject = Rings.removeConsecutiveDuplicates(polygon);
        if (subject.size() < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return new DiskSplit(new ArrayList<>(), new ArrayList<>());
        }
        // A disk this small has no boundary to cut along: its chords are shorter than a
        // real edge, so their normals are noise. Report it as enclosing nothing rather
        // than letting the degenerate lines decide arbitrary sides.
        if (radius < Limits.MIN_EDGE_LENGTH) {
            return new DiskSplit(new ArrayList<>(), List.of(subject));
        }

        var disk = LabelledPolygon
                .createRegularPolygon(center, radius, segments, UNUSED_EDGE_LABEL)
                .getVertices();
        var outsidePieces = new ArrayList<List<double[]>>();
        var remainder = LabelledPolygon.fromLabelledEdges(subject, new int[subject.size()]);
        for (var i = 0; i < disk.size() && !remainder.isEmpty(); i++) {
            var inside = buildInsideHalfPlane(disk, i);
            addRingIfItEnclosesArea(outsidePieces, remainder.clipToHalfPlane(
                    new HalfPlane(inside.pointX(), inside.pointY(),
                            -inside.normalX(), -inside.normalY()),
                    UNUSED_EDGE_LABEL));
            remainder = remainder.clipToHalfPlane(inside, UNUSED_EDGE_LABEL);
        }

        var pieces = new ArrayList<List<double[]>>();
        addRingIfItEnclosesArea(pieces, remainder);
        return new DiskSplit(
                pieces.isEmpty() ? new ArrayList<>() : pieces.get(0),
                outsidePieces);
    }

    // The half-plane of the disk's interior side of edge {@code index}: the line the
    // edge lies on, with the normal pointing into the disk. createRegularPolygon winds
    // counter-clockwise, so the interior is to the left of each edge and the left
    // normal of the edge direction points at it.
    private static HalfPlane buildInsideHalfPlane(List<double[]> disk, int index) {
        var from = disk.get(index);
        var to = disk.get((index + 1) % disk.size());
        return new HalfPlane(
                from[0], from[1],
                -(to[1] - from[1]),
                to[0] - from[0]);
    }

    // Collects a clipped polygon's ring, dropping one clipped down to a sliver of a
    // point or a line: it bounds nothing, so it is no piece at all rather than a
    // degenerate one a consumer would have to re-test before drawing.
    private static void addRingIfItEnclosesArea(
            List<List<double[]>> rings,
            LabelledPolygon polygon) {
        var ring = Rings.removeConsecutiveDuplicates(polygon.getVertices());
        if (ring.size() >= Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            rings.add(ring);
        }
    }
}
