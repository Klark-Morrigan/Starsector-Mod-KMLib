package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * <p>Subtraction also comes in a labelled form ({@link #subtractDiskWithLabels}), for a
 * caller that must know, per edge of the remainder, what lies across it - the difference
 * between an edge to stroke as a border and a seam to fuse away. The plain form is that
 * one with its labels dropped, so the two describe the same pieces by construction.
 *
 * <p>Both operations require a convex subject: they are half-plane clips, which is what
 * lets the difference come back as convex pieces rather than needing a general polygon
 * boolean. A concave subject yields undefined output rather than an error.
 */
public final class Disks {
    // The two internal labels the disk walk stamps its own cuts with. They are kept out
    // of the caller's label space (which is every int) by living in the negatives while
    // the walk labels each subject edge by its index, so an edge's origin can be read
    // back off its label and turned into the caller's own vocabulary on the way out.
    // A cut made while keeping the disk's side of a chord line: the piece shed by that
    // same line lies across it.
    private static final int INSIDE_CUT_LABEL = -1;

    // A cut made while shedding the far side of a chord line - the cut that bounds the
    // piece being shed, and the only one whose far side depends on where along the line
    // it falls.
    private static final int OUTSIDE_CUT_LABEL = -2;

    // The label the unlabelled entry points stand in with: they return plain rings, so
    // the label bookkeeping runs but nothing reads it, and one throwaway value serves
    // every edge.
    private static final int UNUSED_EDGE_LABEL = 0;

    // The chord parameters of a disk edge's two vertices: a point on the edge's line
    // sits between them exactly when its parameter falls in this range, which is what
    // divides the rim from the line's extension past the disk.
    private static final double CHORD_START_PARAMETER = 0;
    private static final double CHORD_END_PARAMETER = 1;

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
        return computeDiskSplit(
                buildUnlabelledSubject(polygon),
                center,
                radius,
                segments,
                UNUSED_EDGE_LABEL,
                UNUSED_EDGE_LABEL)
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
        var pieces = new ArrayList<List<double[]>>();
        for (var piece : subtractDiskWithLabels(
                buildUnlabelledSubject(polygon),
                center,
                radius,
                segments,
                UNUSED_EDGE_LABEL,
                UNUSED_EDGE_LABEL)) {
            pieces.add(piece.getVertices());
        }
        return pieces;
    }

    /**
     * As {@link #subtractDisk}, with every edge of every piece naming what lies across
     * it: an edge {@code polygon} came in with keeps the label it came with, and an edge
     * the clip creates takes {@code rimLabel} or {@code fanCutLabel} by what the clip put
     * on its far side.
     *
     * <p>A cut lands on one of two things sharing a single line. Along the chord between
     * two adjacent disk vertices it is the disk's true rim, with the withheld disk across
     * it - the boundary of the keep-out. Along that same line's extension past either
     * vertex it is a fan cut, splitting the remainder into the pieces it is returned as,
     * with a sibling piece of the same remainder across it - so a consumer tracing the
     * outline fuses there rather than stroking a border that is not one. Whether a cut
     * falls inside the chord's span is the whole of the difference, so a single edge that
     * crosses a chord's end is broken at it and each part labelled for itself.
     *
     * @param polygon      the convex region to clip, its edges labelled with whatever
     *                     lies across them
     * @param center       the disk's centre as {x, y}
     * @param radius       the disk's radius; a radius too small to enclose area withholds
     *                     nothing, so the polygon comes back whole, labels and all
     * @param segments     sides of the regular polygon approximating the disk
     * @param rimLabel     the label for a cut along the withheld disk's rim
     * @param fanCutLabel  the label for a cut between two pieces of the remainder
     * @return the remainder as disjoint convex labelled pieces, in the polygon's winding;
     *         empty when the disk covers the whole polygon
     * @throws IllegalArgumentException when {@code segments} cannot enclose an area
     */
    public static List<LabelledPolygon> subtractDiskWithLabels(
            LabelledPolygon polygon,
            double[] center,
            double radius,
            int segments,
            int rimLabel,
            int fanCutLabel) {
        return computeDiskSplit(polygon, center, radius, segments, rimLabel, fanCutLabel)
                .outsidePieces();
    }

    // The two senses of one disk clip, produced by the single walk that computes them.
    // {@code inside} is the polygon bounded to the disk; {@code outsidePieces} is the
    // rest of it, as disjoint convex pieces. Together they partition the polygon.
    //
    // Only the difference carries labels out, so they are stated in its sense - what lies
    // across an edge once the disk is withheld. The intersection's caller wants the
    // opposite sense of the same lines and asks for no labels, so it takes plain rings.
    private record DiskSplit(List<double[]> inside, List<LabelledPolygon> outsidePieces) {
    }

    // Splits {@code polygon} along the disk's boundary in one walk over the disk's
    // edges. Each edge's line is a half-plane cut: what is on its far side is outside
    // the disk for good (no later edge can bring it back, the disk being convex), so it
    // drops out as a finished piece, while what is on its near side stays in play for
    // the next edge. After every edge has cut, what is still in play is inside every
    // one of them - the intersection. That is why one walk yields both senses: the
    // pieces shed along the way are the difference, the survivor is the intersection.
    private static DiskSplit computeDiskSplit(
            LabelledPolygon polygon,
            double[] center,
            double radius,
            int segments,
            int rimLabel,
            int fanCutLabel) {
        if (segments < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            throw new IllegalArgumentException(
                    "segments must be at least " + Limits.MIN_VERTICES_TO_ENCLOSE_AREA
                            + " to approximate a disk: " + segments);
        }
        var subject = Rings.removeConsecutiveDuplicates(polygon);
        var subjectLabels = subject.getEdgeLabels();
        if (subjectLabels.length < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return new DiskSplit(new ArrayList<>(), new ArrayList<>());
        }
        // A disk this small has no boundary to cut along: its chords are shorter than a
        // real edge, so their normals are noise. Report it as enclosing nothing rather
        // than letting the degenerate lines decide arbitrary sides.
        if (radius < Limits.MIN_EDGE_LENGTH) {
            var whole = new ArrayList<LabelledPolygon>();
            whole.add(subject);
            return new DiskSplit(new ArrayList<>(), whole);
        }

        var disk = LabelledPolygon
                .createRegularPolygon(center, radius, segments, UNUSED_EDGE_LABEL)
                .getVertices();
        var outsidePieces = new ArrayList<LabelledPolygon>();
        // Restate the subject's edges by index while the walk runs, so a cut edge can be
        // told from a subject edge that happens to carry the same label as the cuts do.
        var remainder = LabelledPolygon.fromLabelledEdges(
                subject.getVertices(), buildEdgeIndexLabels(subjectLabels.length));
        for (var i = 0; i < disk.size() && !remainder.isEmpty(); i++) {
            var inside = buildInsideHalfPlane(disk, i);
            var shed = remainder.clipToHalfPlane(
                    new HalfPlane(inside.pointX(), inside.pointY(),
                            -inside.normalX(), -inside.normalY()),
                    OUTSIDE_CUT_LABEL);
            var piece = Rings.removeConsecutiveDuplicates(buildResolvedPiece(
                    shed, subjectLabels, disk, i, rimLabel, fanCutLabel));
            if (isEnclosingArea(piece)) {
                outsidePieces.add(piece);
            }
            remainder = remainder.clipToHalfPlane(inside, INSIDE_CUT_LABEL);
        }

        var inside = Rings.removeConsecutiveDuplicates(remainder);
        return new DiskSplit(
                isEnclosingArea(inside) ? inside.getVertices() : new ArrayList<>(),
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

    // Restates a shed piece's edges in the caller's labels - what lies across each one.
    // An edge the subject arrived with keeps its own label, since nothing about it moved.
    // An edge cut while the disk's side was kept borders the piece that same line shed
    // moments earlier, a sibling of this one, so it fuses. The piece's own cut is the
    // only one that has to look at where it falls: it lies on disk edge
    // {@code diskEdgeIndex}'s line, and the withheld disk is across it only within the
    // chord.
    private static LabelledPolygon buildResolvedPiece(
            LabelledPolygon piece,
            int[] subjectLabels,
            List<double[]> disk,
            int diskEdgeIndex,
            int rimLabel,
            int fanCutLabel) {
        var chordStart = disk.get(diskEdgeIndex);
        var chordEnd = disk.get((diskEdgeIndex + 1) % disk.size());
        var vertices = piece.getVertices();
        var labels = piece.getEdgeLabels();
        var points = new ArrayList<double[]>(vertices.size());
        var resolved = new ArrayList<Integer>(vertices.size());
        for (var i = 0; i < vertices.size(); i++) {
            points.add(vertices.get(i));
            if (labels[i] == OUTSIDE_CUT_LABEL) {
                appendChordSpans(points, resolved,
                        vertices.get(i), vertices.get((i + 1) % vertices.size()),
                        chordStart, chordEnd, rimLabel, fanCutLabel);
            } else {
                resolved.add(labels[i] == INSIDE_CUT_LABEL
                        ? fanCutLabel
                        : subjectLabels[labels[i]]);
            }
        }
        return LabelledPolygon.fromLabelledEdges(points, buildLabelArray(resolved));
    }

    // Breaks the cut edge {@code from} -> {@code to}, which lies on the chord's line, at
    // whichever of the chord's ends it crosses, and labels each part by the side of the
    // chord span it falls on. Appends the label of the part leaving {@code from} (whose
    // point the caller has already appended), then each break point with the label of the
    // part leaving it.
    private static void appendChordSpans(
            List<double[]> points,
            List<Integer> labels,
            double[] from,
            double[] to,
            double[] chordStart,
            double[] chordEnd,
            int rimLabel,
            int fanCutLabel) {
        var fromParameter = computeChordParameter(chordStart, chordEnd, from);
        var toParameter = computeChordParameter(chordStart, chordEnd, to);
        var spanStart = fromParameter;
        for (var breakParameter : findCrossedChordEnds(fromParameter, toParameter)) {
            labels.add(pickSpanLabel(spanStart, breakParameter, rimLabel, fanCutLabel));
            points.add(computeChordPoint(chordStart, chordEnd, breakParameter));
            spanStart = breakParameter;
        }
        labels.add(pickSpanLabel(spanStart, toParameter, rimLabel, fanCutLabel));
    }

    // The chord ends strictly between the two parameters, ordered as the edge running
    // from one to the other meets them - the points at which that edge changes what is
    // across it. An end the edge merely touches is no crossing: the edge is on one side
    // of it throughout, so there is nothing to break.
    private static List<Double> findCrossedChordEnds(double fromParameter, double toParameter) {
        var crossed = new ArrayList<Double>();
        var low = Math.min(fromParameter, toParameter);
        var high = Math.max(fromParameter, toParameter);
        for (var end : new double[] {CHORD_START_PARAMETER, CHORD_END_PARAMETER}) {
            if (end > low && end < high) {
                crossed.add(end);
            }
        }
        if (fromParameter > toParameter) {
            Collections.reverse(crossed);
        }
        return crossed;
    }

    // Which side of the chord span a part of the cut falls on, read off its midpoint:
    // the part lies wholly on one side, the ends it might have crossed having already
    // broken it.
    private static int pickSpanLabel(
            double spanStart,
            double spanEnd,
            int rimLabel,
            int fanCutLabel) {
        var middle = (spanStart + spanEnd) * 0.5;
        return middle >= CHORD_START_PARAMETER && middle <= CHORD_END_PARAMETER
                ? rimLabel
                : fanCutLabel;
    }

    // Where {@code point} falls along the chord's line, as a multiple of the chord: 0 at
    // {@code chordStart}, 1 at {@code chordEnd}, outside that on the line's extension.
    // The point is taken to be on the line already (every cut edge is), so its distance
    // along the chord places it and no perpendicular component is measured.
    private static double computeChordParameter(
            double[] chordStart,
            double[] chordEnd,
            double[] point) {
        var chordX = chordEnd[0] - chordStart[0];
        var chordY = chordEnd[1] - chordStart[1];
        return ((point[0] - chordStart[0]) * chordX + (point[1] - chordStart[1]) * chordY)
                / (chordX * chordX + chordY * chordY);
    }

    // The point at {@code parameter} along the chord's line - the inverse of
    // computeChordParameter.
    private static double[] computeChordPoint(
            double[] chordStart,
            double[] chordEnd,
            double parameter) {
        return new double[] {
                chordStart[0] + parameter * (chordEnd[0] - chordStart[0]),
                chordStart[1] + parameter * (chordEnd[1] - chordStart[1]),
        };
    }

    // Labels every edge of a subject with its own index, the identity the walk needs to
    // hand an edge back the label it arrived with.
    private static int[] buildEdgeIndexLabels(int edgeCount) {
        var labels = new int[edgeCount];
        for (var i = 0; i < edgeCount; i++) {
            labels[i] = i;
        }
        return labels;
    }

    private static int[] buildLabelArray(List<Integer> labels) {
        var array = new int[labels.size()];
        for (var i = 0; i < labels.size(); i++) {
            array[i] = labels.get(i);
        }
        return array;
    }

    // A subject for a caller that has no labels to carry: every edge takes the throwaway
    // label, so the walk's bookkeeping runs over a vocabulary of one.
    private static LabelledPolygon buildUnlabelledSubject(List<double[]> polygon) {
        var labels = new int[polygon.size()];
        Arrays.fill(labels, UNUSED_EDGE_LABEL);
        return LabelledPolygon.fromLabelledEdges(polygon, labels);
    }

    // Whether a clipped polygon still bounds real area, or was cut down to a sliver of a
    // point or a line. A sliver is no shape at all rather than a degenerate one a
    // consumer would have to re-test before drawing, so both senses drop it: it is
    // neither a piece of the difference nor the intersection.
    private static boolean isEnclosingArea(LabelledPolygon polygon) {
        return polygon.getVertices().size() >= Limits.MIN_VERTICES_TO_ENCLOSE_AREA;
    }
}
