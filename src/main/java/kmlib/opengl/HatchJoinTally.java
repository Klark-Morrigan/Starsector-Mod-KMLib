package kmlib.opengl;

/**
 * How a merging {@link HatchJoining} closed the joins it made, split by what closed each one: two
 * crossings that landed on the same point exactly, a gap the join tolerance reached across, or two
 * spans that overlapped and were never two strokes to begin with.
 *
 * <p>Evidence about the tolerance rather than an input to anything drawn. Two triangles sharing an
 * edge ought to cross a hatch line at the same point to the last bit, in which case the tolerance
 * guards nothing and the merge could test for plain abutment; a tessellator that does not agree to
 * the last bit makes the tolerance load-bearing, and then the gap it actually has to close - not a
 * guessed constant - is what it should be set to. Neither reading can be argued from the geometry,
 * so the merge counts them as it runs and a caller settles the question on a real region.
 *
 * <p>An overlap is counted apart from the two rather than folded into the tolerance's count
 * precisely because the widest gap is what the tolerance gets set from. Overlapping spans merge
 * whatever the tolerance is - they need no reach to close - and an overlap can be as long as the
 * region, so counting one as a tolerated gap would report a magnitude hundreds of times anything
 * the tolerance is there for and set it from a number it never had to close.
 *
 * <p>The two gap readings bracket the tolerance from either side, which is why both are here. The
 * widest closed gap is the reach the tolerance actually had to have; the narrowest one left open
 * is what the next notch up would start joining. Without the second, a run at zero tolerance -
 * the run that asks whether the tolerance is needed at all - can only report that nothing was
 * tolerated, which is true of every zero-tolerance run and so answers nothing; with it, one such
 * run says outright how far the merge fell short.
 *
 * @param exactJoinCount             joins closed on crossings that coincided exactly
 * @param toleranceJoinCount         joins that closed only by way of the tolerance
 * @param overlappingJoinCount       joins between spans that already overlapped, which close
 *                                   whatever the tolerance is
 * @param widestToleranceGapFraction the widest gap any tolerated join spanned, as a fraction of
 *                                   the hatch spacing so it compares directly against the
 *                                   tolerance; zero when no join needed the tolerance
 * @param narrowestOpenGapFraction   the narrowest gap the merge refused to close, in the same
 *                                   units; {@link Double#POSITIVE_INFINITY} when it refused
 *                                   none, since then there is no such gap rather than one of no
 *                                   width
 */
public record HatchJoinTally(
    int exactJoinCount,
    int toleranceJoinCount,
    int overlappingJoinCount,
    double widestToleranceGapFraction,
    double narrowestOpenGapFraction) {

    /** What a joining that merges nothing reports: it closes and refuses no join at all. */
    public static final HatchJoinTally NO_JOINS =
        new HatchJoinTally(0, 0, 0, 0, Double.POSITIVE_INFINITY);
}
