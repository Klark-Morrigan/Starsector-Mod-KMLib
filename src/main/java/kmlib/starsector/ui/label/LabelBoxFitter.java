package kmlib.starsector.ui.label;

import kmlib.math.geometry.LineBlockers;
import kmlib.math.geometry.PolygonRegions;
import kmlib.math.geometry.RegionChord;
import kmlib.math.geometry.Segment;
import kmlib.math.geometry.Spans;
import kmlib.math.solving.Bisection;
import kmlib.math.solving.Picks;

/**
 * Sizes the largest label box that fits one candidate chord: the tallest font the
 * region holds, how many lines the text stacks into, and the clear line the box centres
 * on. The sizing half of a label search, apart from the candidate generation and
 * selection its caller does - one chord in, one box out.
 *
 * <p>The fit is a monotone growth on the per-line font height, clamped to
 * {@code [minFontHeight, maxFontHeight]} - the readability floor and the oversize
 * ceiling, both per line so they mean the same at any line count. A font of height
 * {@code F} stacked into {@code lineCount} lines occupies a band of thickness
 * {@code F * ((lineCount - 1) * lineSpacing + 1)}; the {@link LabelLengthEstimator} says
 * how much length the text needs at that line height. A taller font fattens the band
 * (which fits in fewer places, so its clear length only shrinks) while the text's needed
 * length grows - the largest readable font is therefore at the largest height whose
 * band still holds the text, found by {@link Bisection}. Trying every line count and
 * keeping the tallest font is what lets a length-poor but girth-rich chord win by
 * stacking lines instead of shrinking; on a tie the lower line count holds
 * ({@link Picks} keeps the incumbent), so text goes multi-line only when stacking
 * buys a strictly larger font.
 *
 * <p>How closely that growth must land on the largest passing height is the caller's to
 * state, as a tolerance in the font's own units rather than a halving count fixed here.
 * Every halving measures one more band against the rings and the keep-outs, which is
 * where a sizing spends nearly all of its time, so the count is the fit's dominant cost
 * knob - and a fixed one resolves the height to whatever fraction of the passed clamp it
 * happens to work out as, paying full price for precision far below anything a caller
 * can draw.
 *
 * <p>The text estimator is injected, not baked in: the fitter never asks how long the
 * text actually is, only the estimator does, so the same fit serves the font-measured
 * text and the aspect stand-in alike.
 */
public final class LabelBoxFitter {

    private final double minFontHeight;
    private final double maxFontHeight;
    private final int maxLines;
    private final double lineSpacing;
    private final double keepOutClearance;
    private final double endInsetDistance;

    // How many times the font-height search halves the clamp, derived once from the
    // caller's tolerance: every halving is a band fit, so this is the sizing's dominant
    // cost knob and it must not be recomputed per line count inside the search.
    private final int fontHeightBisectionSteps;
    private final LabelLengthEstimator textLength;

    // How many bands this fitter has measured. A band fit walks the rings and every
    // keep-out, and the sizing runs one per bisection step per line count, so the count -
    // not the candidate count above it - is what a fit's cost actually scales with. Kept
    // per fitter rather than globally so it stays a plain field on a single-threaded fit
    // and needs no synchronisation.
    private int bandFitCount;

    public LabelBoxFitter(
            NameFitSpecification fit,
            BandFitSpecification bandFit,
            LabelLengthEstimator textLength) {

        this.minFontHeight = fit.minFontHeight();
        this.maxFontHeight = fit.maxFontHeight();
        this.maxLines = fit.maxLines();
        this.lineSpacing = fit.lineSpacing();
        this.keepOutClearance = bandFit.keepOutClearance();
        this.endInsetDistance = bandFit.endInsetDistance();
        this.fontHeightBisectionSteps = Bisection.countStepsForTolerance(
            fit.minFontHeight(),
            fit.maxFontHeight(),
            bandFit.fontHeightTolerance());
        this.textLength = textLength;
    }

    // Sizes the largest text that fits the chord, as the box it occupies, or null
    // when even the minimum-height font cannot hold text at any line count. Keeps
    // the line count whose box carries the tallest font, so more lines are chosen only
    // when they buy a strictly bigger font by spending the chord's spare girth.
    public BoxFit fitLargestBox(RegionChord chord) {

        var sizingChord = SizingChord.prepareFor(chord, keepOutClearance);

        // A line too short to define a direction has no interior to size along either,
        // so every band would fail; there is nothing to measure.
        if (sizingChord == null) {
            return null;
        }
        BoxFit best = null;
        for (var lineCount = 1; lineCount <= maxLines; lineCount++) {
            best = Picks.pickHigher(
                best,
                fitForLineCount(sizingChord, lineCount),
                BoxFit::fontHeight);
        }
        return best;
    }

    // Fits one candidate band against the rings, the keep-out points, and the end inset,
    // widened to the given half thickness. Exposed for a search's near-miss diagnostic,
    // which reads the pre-margin clear span of a minimum-height band; the fit proper
    // reaches it through the line-count sizing below, which projects the keep-outs once
    // for its whole sweep rather than per band as this single measurement must.
    public BandSpan fitBand(RegionChord chord, double halfThickness) {

        var sizingChord = SizingChord.prepareFor(chord, keepOutClearance);
        return sizingChord == null
            ? new BandSpan(null, null)
            : measureBand(sizingChord, halfThickness);
    }

    /**
     * How many bands this fitter has measured since it was built - the work the sizing
     * actually did, which the candidate count alone understates: each candidate costs one
     * band fit per bisection step per line count, and a candidate whose minimum font
     * already fails costs a single one. Reported so a slow fit is read off the count that
     * grew rather than off the tuning it was asked for.
     *
     * @return the running total of {@link #fitBand} calls on this fitter
     */
    public int getBandFitCount() {
        return bandFitCount;
    }

    // Sizes the box for one fixed line count by growing the font to the largest height
    // whose band still holds the text, then reading that band's clear span back. Null
    // when even the minimum font cannot hold the text.
    private BoxFit fitForLineCount(SizingChord sizingChord, int lineCount) {

        if (!bandHoldsText(sizingChord, minFontHeight, lineCount)) {
            return null;
        }
        var fontHeight = Bisection.findLargestPassing(
            minFontHeight,
            maxFontHeight,
            fontHeightBisectionSteps,
            candidate -> bandHoldsText(sizingChord, candidate, lineCount));

        var thickness = fontHeight * computeLinesFactor(lineCount);
        var span = measureBand(sizingChord, thickness / 2.0).insetSpan();

        return new BoxFit(
            sizingChord.chord().toSegment(span),
            thickness,
            lineCount,
            fontHeight);
    }

    // Whether a font of the given height, stacked into lineCount lines, has room along
    // the chord for the text: the band those lines occupy must have a clear
    // (border-, keep-out-, and margin-trimmed) length at least the length the estimator
    // says the text needs at that line height.
    private boolean bandHoldsText(SizingChord sizingChord, double fontHeight, int lineCount) {

        var band = measureBand(
            sizingChord,
            fontHeight * computeLinesFactor(lineCount) / 2.0);

        if (band.insetSpan() == null) {
            return false;
        }
        var clearLength = band.insetSpan()[1] - band.insetSpan()[0];
        return clearLength >= textLength.requiredLengthFor(fontHeight, lineCount);
    }

    // One band's spans against the chord's pre-projected keep-outs: the border test
    // reads the thickness, the keep-out trim does not, so only the former runs here.
    private BandSpan measureBand(SizingChord sizingChord, double halfThickness) {

        bandFitCount++;

        var interiorSpans = PolygonRegions.findBandInteriorSpans(
            sizingChord.chord().rings(),
            sizingChord.chord().line(),
            halfThickness);

        if (interiorSpans.isEmpty()) {
            return new BandSpan(null, null);
        }
        var clear = Spans.findLongestClearSubsegment(
            interiorSpans,
            sizingChord.keepOutBlockers());

        if (clear == null) {
            return new BandSpan(null, null);
        }
        var start = clear[0] + endInsetDistance;
        var end = clear[1] - endInsetDistance;
        // An interval shorter than twice the end inset leaves no room for text between
        // the margins; only the pre-margin clear span survives, for the red diagnostic.
        return start < end
            ? new BandSpan(clear, new double[] {start, end})
            : new BandSpan(clear, null);
    }

    // How much thicker than a single line's font a stack of lineCount lines is, as the
    // multiplier a band's thickness takes: the gaps between the lines plus the one line
    // every stack starts from.
    private double computeLinesFactor(int lineCount) {
        return (lineCount - 1) * lineSpacing + 1.0;
    }

    /**
     * One band fit's spans, both along the candidate direction as {@code {tStart, tEnd}}:
     * the clear span is the roomiest border- and keep-out-clear stretch a band of the
     * given thickness has before the end margin (the near-miss a red diagnostic shows),
     * and the inset span the same pulled in by the end inset at both ends (the room text
     * actually gets), or null when the margin leaves nothing. The clear span is null only
     * when the band finds no keep-out-clear interior at all.
     */
    public record BandSpan(
        double[] clearSpan,
        double[] insetSpan) {
    }

    /**
     * One fitted label box: its clear span as a world segment (the line the label
     * follows), the band girth that fits along it, the line count the text is stacked
     * into, and the per-line font height the fit achieved - the quantity a search
     * maximises, before it docks steep candidates by any slope penalty.
     */
    public record BoxFit(
        Segment segment,
        double thickness,
        int lineCount,
        double fontHeight) {
    }

    // A chord together with its keep-outs already projected onto its line. The
    // projection depends on the line, the obstacles and the clearance alone - only the
    // interior test reads a band's thickness - so it is invariant across every band a
    // sizing measures, and pairing the two here is what lets it be taken once per chord
    // rather than once per band. Blockers belonging to some other chord cannot reach a
    // measurement, since the only way to hold a pair is to have minted it from one.
    private record SizingChord(
        RegionChord chord,
        LineBlockers keepOutBlockers) {

        // Null when the chord's line is too short to define a direction: there is no
        // frame to measure parameters in, so nothing can be sized along it.
        private static SizingChord prepareFor(RegionChord chord, double keepOutClearance) {

            var keepOutBlockers = Spans.computeLineBlockers(
                chord.line(),
                chord.keepOuts(),
                keepOutClearance);

            return keepOutBlockers == null
                ? null
                : new SizingChord(chord, keepOutBlockers);
        }
    }
}
