package kmlib.opengl.hatch;

import kmlib.opengl.GlVertexRuns;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Collects the spans a hatch clip walk finds and merges each line's into as few segments as the
 * region allows, so a line spanning several triangles comes back whole and breaks only where it
 * genuinely leaves the region.
 *
 * <p>Nothing can be emitted as the walk finds it: a crossing only turns out to continue an earlier
 * one once both are in hand, and the walk visits triangles in soup order rather than along any one
 * line. So the spans are held until the run is packed, gathered per line and merged then. That is
 * the whole reason this is a collaborator rather than a few locals in the clip - it outlives the
 * walk that fills it.
 *
 * <p>Held in a {@link TreeMap} keyed by line index, so the packed run comes out in ascending line
 * order however the soup was ordered. A hash map would pack the same geometry in an order that
 * changed with the keys, which is the kind of difference that makes a run impossible to assert on
 * and a rendering impossible to compare frame to frame.
 *
 * <p>Endpoints are rebuilt from the axes rather than carried through from the clip, so a merged
 * segment lies exactly on its line - and so the question of which of two coincident crossing points
 * won a merge never arises, since neither is kept. The merge itself runs entirely in the
 * (line, distance) terms the clip offers, and only the packing crosses back into coordinates.
 */
final class HatchSpanMerger {

    private final HatchAxes axes;

    // The widest gap between two of one line's spans that still counts as the same stroke, in the
    // soup's own units - the caller's fraction resolved against the spacing once here, so the
    // merge below compares two world distances rather than converting per join.
    private final double joinTolerance;

    private final Map<Integer, List<ClippedSpan>> spansByLineIndex = new TreeMap<>();
    private final List<Float> segments = new ArrayList<>();

    // The running tally, accumulated as the merge runs; see HatchJoinTally for why the three kinds
    // of closed join are counted apart.
    private int exactJoinCount;
    private int toleranceJoinCount;
    private int overlappingJoinCount;
    private double widestToleranceGap;

    // Starts unbounded so the first gap left open sets it, and so a merge that refused nothing
    // reports that there was no such gap rather than one of no width.
    private double narrowestOpenGap = Double.POSITIVE_INFINITY;

    HatchSpanMerger(HatchAxes axes, double joinToleranceFraction) {
        this.axes = axes;
        this.joinTolerance = joinToleranceFraction * axes.spacing();
    }

    /**
     * Records one hatch line's crossing of one triangle.
     *
     * @param lineIndex which line of the family the span lies on
     * @param minAlong  where the span starts, as a distance along the line direction
     * @param maxAlong  where it ends, always at or past {@code minAlong}
     */
    void acceptClippedSpan(int lineIndex, double minAlong, double maxAlong) {
        spansByLineIndex
            .computeIfAbsent(lineIndex, line -> new ArrayList<>())
            .add(new ClippedSpan(minAlong, maxAlong));
    }

    /**
     * @return everything accepted so far, merged, as a drawable run paired with how the merge
     *         closed the joins it made
     */
    HatchRun packHatchRun() {
        for (var line : spansByLineIndex.entrySet()) {
            mergeSpansOfLine(line.getKey(), line.getValue());
        }
        return new HatchRun(
            GlVertexRuns.packFloats(segments),
            new HatchJoinTally(
                exactJoinCount,
                toleranceJoinCount,
                overlappingJoinCount,
                widestToleranceGap / axes.spacing(),
                narrowestOpenGap / axes.spacing()));
    }

    // Emits one segment per point-to-point stretch of the numbered line. Sorting by start is what
    // makes a single forward sweep enough: every span that could continue the one being extended
    // comes after it, so a stretch ends the first time the next span starts too far along.
    private void mergeSpansOfLine(int lineIndex, List<ClippedSpan> spans) {

        spans.sort(Comparator.comparingDouble(ClippedSpan::start));

        var stretchStart = spans.get(0).start();
        var stretchEnd = spans.get(0).end();

        for (var next = 1; next < spans.size(); next++) {

            var span = spans.get(next);
            var gap = span.start() - stretchEnd;

            if (gap > joinTolerance) {
                // The break is correct where the region genuinely stops, so this is recorded
                // rather than guarded: how near the nearest refusal came is what says whether
                // the tolerance is set below gaps it was meant to close.
                narrowestOpenGap = Math.min(narrowestOpenGap, gap);
                addSegmentOnLine(lineIndex, stretchStart, stretchEnd);
                stretchStart = span.start();
                stretchEnd = span.end();
                continue;
            }
            tallyClosedJoin(gap);

            // A span wholly inside the stretch would otherwise pull its end backwards, which the
            // sorted sweep permits: spans are ordered by start, not by end.
            stretchEnd = Math.max(stretchEnd, span.end());
        }
        addSegmentOnLine(lineIndex, stretchStart, stretchEnd);
    }

    // Records what closed one join. Exact means the two crossings came out of their triangles
    // bit-for-bit identical, and a negative gap means the spans already overlapped - neither
    // needed the tolerance to reach anything, so only the middle case may set the widest gap the
    // tolerance is read back as having had to close.
    private void tallyClosedJoin(double gap) {
        if (gap == 0) {
            exactJoinCount++;
            return;
        }
        if (gap < 0) {
            overlappingJoinCount++;
            return;
        }
        toleranceJoinCount++;
        widestToleranceGap = Math.max(widestToleranceGap, gap);
    }

    // Appends one merged stretch as its two endpoints, each packed x then y.
    private void addSegmentOnLine(int lineIndex, double start, double end) {
        addPointOnLine(lineIndex, start);
        addPointOnLine(lineIndex, end);
    }

    // Appends one endpoint as its packed x then y.
    private void addPointOnLine(int lineIndex, double distanceAlong) {
        segments.add((float) axes.computeXOnLine(lineIndex, distanceAlong));
        segments.add((float) axes.computeYOnLine(lineIndex, distanceAlong));
    }

    // One crossing of the region by one hatch line, as the two distances along that line it runs
    // between. Named ends rather than a two-element array, so the sort and the merge read as start
    // and end rather than as index 0 and index 1.
    private record ClippedSpan(
        double start,
        double end) {
    }
}
