package kmlib.starsector.ui.label;

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
 * <p>The text estimator is injected, not baked in: the fitter never asks how long the
 * text actually is, only the estimator does, so the same fit serves the font-measured
 * text and the aspect stand-in alike.
 */
public final class LabelBoxFitter {

    // How many times the font-height search halves its interval - enough to land the
    // fitted height within a fraction of a world unit, since each step doubles precision
    // and the font clamp spans a few thousand units at most.
    private static final int FONT_HEIGHT_BISECTION_STEPS = 20;

    private final double minFontHeight;
    private final double maxFontHeight;
    private final int maxLines;
    private final double lineSpacing;
    private final double keepOutClearance;
    private final double endInsetDistance;
    private final LabelLengthEstimator textLength;

    public LabelBoxFitter(
            NameFitSpecification fit,
            double keepOutClearance,
            double endInsetDistance,
            LabelLengthEstimator textLength) {
        this.minFontHeight = fit.minFontHeight();
        this.maxFontHeight = fit.maxFontHeight();
        this.maxLines = fit.maxLines();
        this.lineSpacing = fit.lineSpacing();
        this.keepOutClearance = keepOutClearance;
        this.endInsetDistance = endInsetDistance;
        this.textLength = textLength;
    }

    // Sizes the largest text that fits the chord, as the box it occupies, or null
    // when even the minimum-height font cannot hold text at any line count. Keeps
    // the line count whose box carries the tallest font, so more lines are chosen only
    // when they buy a strictly bigger font by spending the chord's spare girth.
    public BoxFit fitLargestBox(RegionChord chord) {
        BoxFit best = null;
        for (var lineCount = 1; lineCount <= maxLines; lineCount++) {
            best = Picks.pickHigher(best, fitForLineCount(chord, lineCount),
                    BoxFit::fontHeight);
        }
        return best;
    }

    // Fits one candidate band against the rings, the keep-out points, and the end inset,
    // widened to the given half thickness. Exposed for a search's near-miss diagnostic,
    // which reads the pre-margin clear span of a minimum-height band; the fit proper
    // reaches it through the line-count sizing below.
    public BandSpan fitBand(RegionChord chord, double halfThickness) {
        var interiorSpans = PolygonRegions.findBandInteriorSpans(chord.rings(), chord.line(),
                halfThickness);
        if (interiorSpans.isEmpty()) {
            return new BandSpan(null, null);
        }
        var clear = Spans.findLongestClearSubsegment(
                interiorSpans,
                chord.line(),
                chord.keepOuts(),
                keepOutClearance);
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

    // Sizes the box for one fixed line count by growing the font to the largest height
    // whose band still holds the text, then reading that band's clear span back. Null
    // when even the minimum font cannot hold the text.
    private BoxFit fitForLineCount(RegionChord chord, int lineCount) {
        var linesFactor = (lineCount - 1) * lineSpacing + 1.0;
        if (!bandHoldsText(chord, minFontHeight, lineCount, linesFactor)) {
            return null;
        }
        var fontHeight = Bisection.findLargestPassing(minFontHeight, maxFontHeight,
                FONT_HEIGHT_BISECTION_STEPS,
                candidate -> bandHoldsText(chord, candidate, lineCount, linesFactor));
        var thickness = fontHeight * linesFactor;
        var span = fitBand(chord, thickness / 2.0).insetSpan();
        return new BoxFit(chord.toSegment(span), thickness, lineCount, fontHeight);
    }

    // Whether a font of the given height, stacked into lineCount lines, has room along
    // the chord for the text: the band those lines occupy must have a clear
    // (border-, keep-out-, and margin-trimmed) length at least the length the estimator
    // says the text needs at that line height.
    private boolean bandHoldsText(
            RegionChord chord,
            double fontHeight,
            int lineCount,
            double linesFactor) {
        var band = fitBand(chord, fontHeight * linesFactor / 2.0);
        if (band.insetSpan() == null) {
            return false;
        }
        var clearLength = band.insetSpan()[1] - band.insetSpan()[0];
        return clearLength >= textLength.requiredLengthFor(fontHeight, lineCount);
    }

    /**
     * One band fit's spans, both along the candidate direction as {@code {tStart, tEnd}}:
     * the clear span is the roomiest border- and keep-out-clear stretch a band of the
     * given thickness has before the end margin (the near-miss a red diagnostic shows),
     * and the inset span the same pulled in by the end inset at both ends (the room text
     * actually gets), or null when the margin leaves nothing. The clear span is null only
     * when the band finds no keep-out-clear interior at all.
     */
    public record BandSpan(double[] clearSpan, double[] insetSpan) {
    }

    /**
     * One fitted label box: its clear span as a world segment (the line the label
     * follows), the band girth that fits along it, the line count the text is stacked
     * into, and the per-line font height the fit achieved - the quantity a search
     * maximises, before it docks steep candidates by any slope penalty.
     */
    public record BoxFit(Segment segment, double thickness, int lineCount, double fontHeight) {
    }
}
