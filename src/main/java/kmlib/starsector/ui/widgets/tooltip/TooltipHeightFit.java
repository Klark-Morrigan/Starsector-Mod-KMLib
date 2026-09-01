package kmlib.starsector.ui.widgets.tooltip;

import java.util.List;

/**
 * Fits a box's typography to the room the box has: where the blocks stand taller than the height they
 * are given, it compresses the tiers above the deepest line shown until they fit, and hands back the
 * typography to draw them in.
 *
 * <p>The answer to what a box does about its own overflow. A tooltip is sized by its content and then
 * clamped on screen, so one asked for more than the screen holds is drawn with its ends past both edges
 * and says nothing about what it lost - and a tooltip takes no input, so it cannot be scrolled to what
 * was cut. Nothing is dropped here either: the box keeps every line it was asked for and gives up size
 * instead.
 *
 * <p>Which size it gives up is the whole of the rule. The ordinary ramp reads largest at the top, so
 * compressing it further would quiet the deepest lines - the ones a reader who asked for that depth is
 * there to read. Anchored at the deepest tier instead, it is the context above that comes down to meet
 * them: the account they stand under is what the reader already has. That inverts what the box normally
 * looks like, which is why it is an overflow response rather than a resting state - a box that fits is
 * handed back exactly as it was.
 *
 * <p>Solved rather than stepped, since how tall a box comes to falls as the ramp rises and no closed
 * form joins the two: a bisection over a fixed count of probes is enough, and the ramp that floors every
 * line one step from the anchor bounds it, no larger one changing anything. Where even that still
 * overflows the box is handed back fully compressed, still too tall - what remains is content that has
 * to be given up, which is a caller's decision about its own subject matter rather than a typography's.
 *
 * <p>The compression is spent through {@link TooltipStyle} alone, so nothing that measures or paints a
 * box learns that a fit happened: a fitted box is stacked, sized, and drawn by exactly the arithmetic an
 * unfitted one is.
 */
public final class TooltipHeightFit {

    // How many times the solve halves the range it is searching. Eight settles the ramp to a 256th of
    // the largest face the box is set in - hundredths of a unit of glyph size, well under anything a
    // reader could see - and every probe past that costs another walk of every line in the box, which a
    // box redrawn each frame pays each frame. A count rather than a tolerance, so the walk is bounded by
    // construction: no content, budget, or style can make it run longer than this.
    private static final int SOLVE_PROBES = 8;

    // The two ends the solve searches between: no compression at all, and the whole of it, which is as
    // much as the ramp can buy.
    private static final float NO_COMPRESSION = 0f;
    private static final float FULL_COMPRESSION = 1f;

    // What a range is divided by to halve it - the two ends being averaged.
    private static final float BOTH_ENDS = 2f;

    private TooltipHeightFit() {
    }

    /**
     * Answers the typography {@code sections} should be drawn in to stand no taller than
     * {@code heightBudget}: {@code style} itself where the box already fits, and otherwise a copy
     * compressed toward the deepest line the blocks hold, so that line keeps its size while the tiers
     * above it draw smaller and closer together.
     *
     * <p>The budget is the caller's to state, since what a box has to fit inside is not the box's own
     * business - the screen for a tooltip that follows the cursor
     * ({@code CursorTooltipRenderer.resolveHeightBudget}), some smaller region for a box drawn into one.
     *
     * <p>Where no ramp brings the box within the budget it comes back at full compression - as short as
     * this can make it, and still too tall. A caller that must not overflow weighs the result again and
     * gives up content.
     *
     * @param sections     the content blocks, top to bottom
     * @param style        the look each kind of line draws in and how far apart the blocks stand
     * @param heightBudget the tallest the box may stand, in UI units
     * @return the typography to draw those blocks in
     */
    public static TooltipStyle fitToHeight(
            List<TooltipSection> sections,
            TooltipStyle style,
            float heightBudget) {

        // A box that fits is handed back untouched, which is nearly every box: the compression inverts
        // how a listing reads, and one that was never the problem must not pay for it.
        if (CursorTooltip.measureBoxHeight(sections, style) <= heightBudget) {
            return style;
        }
        var deepestLevel = readDeepestLevel(sections);

        // A box whose lines all speak at one tier has nothing standing above its deepest line, so the
        // ramp has nothing to act on however far it is pushed. Handed back as it stands rather than
        // solved for pointlessly - it overflows, and giving up content is the only answer left.
        if (deepestLevel <= TooltipRow.TableRow.NO_SUBORDINATION) {
            return style;
        }
        var probe = new CompressionProbe(
            sections,
            style,
            deepestLevel,
            measureSaturatingRamp(style),
            heightBudget);

        // Past the ramp that floors every tier above the anchor there is nothing further to give up, so
        // a box still overflowing there is as short as this pass can make it and is handed back that way.
        return probe.isFittingAt(FULL_COMPRESSION)
            ? probe.compressTo(solveGentlestFittingShare(probe))
            : probe.compressTo(FULL_COMPRESSION);
    }

    // The gentlest compression that brings the box within its budget, as a share of the saturating ramp.
    // Halved toward it from two ends whose verdicts are already known - the far end weighed by the
    // caller, and the near one by argument - so every probe narrows a range whose far end fits and the
    // share returned is one of the probes that did.
    //
    // The near end needs no probe of its own: a share of nothing leaves every tier at its kind's own
    // size, which is the authored box with whatever ramp it carried removed, and so stands at least as
    // tall as the box that was already too tall.
    //
    // Gentlest rather than any share that fits, because the compression costs legibility at every step:
    // the faces are bitmap atlases crisp at one size, so a box scaled further off its own atlas than it
    // had to be is softer for nothing.
    private static float solveGentlestFittingShare(CompressionProbe probe) {

        var overflowingShare = NO_COMPRESSION;
        var fittingShare = FULL_COMPRESSION;

        for (var halving = 0; halving < SOLVE_PROBES; halving++) {

            var probedShare = (overflowingShare + fittingShare) / BOTH_ENDS;

            if (probe.isFittingAt(probedShare)) {
                fittingShare = probedShare;
            } else {
                overflowingShare = probedShare;
            }
        }
        return fittingShare;
    }

    // The ramp past which nothing changes: one as large as the box's largest face takes every line a
    // step from the anchor to the smallest size the style will resolve, so a larger one resolves the
    // same look. That is what bounds the solve - the search runs over a share of this rather than over
    // an open-ended step, and the floor the style already holds is what makes such a bound exist.
    private static float measureSaturatingRamp(TooltipStyle style) {

        var largestFaceSize = Math.max(
            style.headerStyle().face().size(),
            Math.max(
                style.paragraphStyle().face().size(),
                style.footnoteStyle().face().size()));

        return (float) largestFaceSize;
    }

    // The deepest tier any line of the box stands at, which is what the compression anchors on. Read off
    // the content rather than taken from the caller: the caller's own notion of how deep it composed can
    // differ from what the blocks actually hold - a system listing nothing at its deepest tier is one
    // such box - and the line that keeps its size has to be a line the box is drawing.
    private static int readDeepestLevel(List<TooltipSection> sections) {

        var deepestLevel = TooltipRow.TableRow.NO_SUBORDINATION;
        for (var row : TooltipSection.readRowsInOrder(sections)) {
            deepestLevel = Math.max(deepestLevel, row.subordinationLevel());
        }
        return deepestLevel;
    }

    /**
     * One box, weighed at whatever compression it is asked about: the blocks, the typography they were
     * authored in, the tier a compression anchors at, the ramp a full one spends, and the height it all
     * has to come inside.
     *
     * <p>Bound once because every probe of the solve reads all five, and because compressing and then
     * measuring is one act - taken apart, a probe could be weighed at some compression other than the
     * one it was built at, and the solve would narrow its range on a verdict about a box it never
     * considered.
     *
     * @param sections          the content blocks, top to bottom
     * @param style             the typography the blocks were authored in, uncompressed
     * @param shrinkAnchorLevel the tier a compression holds at its kind's own size
     * @param saturatingRamp    the ramp a full compression spends
     * @param heightBudget      the tallest the box may stand, in UI units
     */
    private record CompressionProbe(
        List<TooltipSection> sections,
        TooltipStyle style,
        int shrinkAnchorLevel,
        float saturatingRamp,
        float heightBudget) {

        // The typography this box draws in at some share of a full compression. Built off the authored
        // typography every time rather than off the share before it, since the refinement tightens the
        // gaps of whatever it is applied to - chained probe to probe, each would tighten the last
        // probe's answer and the solve would leave the ramp it thinks it is measuring.
        private TooltipStyle compressTo(float compressionShare) {
            return style.compressedTowardLevel(shrinkAnchorLevel, compressionShare * saturatingRamp);
        }

        private boolean isFittingAt(float compressionShare) {
            return CursorTooltip.measureBoxHeight(sections, compressTo(compressionShare))
                <= heightBudget;
        }
    }
}
