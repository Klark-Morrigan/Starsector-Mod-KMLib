package kmlib.starsector.ui.label;

import java.util.List;

/**
 * The text a label box is sized for: how much length it needs at a given line height and
 * line count, and what its wrapped lines actually are - the two facts the geometry-only
 * box fit cannot know on its own.
 *
 * <p>The seam between sizing a label box against a region and knowing the text that must
 * fill it. {@link LabelBoxFitter} grows a band's girth against the region boundary and,
 * at each trial, asks this how much length the text would demand there; whatever answers
 * it decides the fit. Both answers come from the same wrap - the required length is the
 * width of the widest wrapped line - so keeping them behind one estimator stops the
 * measured fit and the drawn lines from ever disagreeing.
 * {@link FontLabelLengthEstimator} answers from real glyph metrics;
 * {@link AspectLabelLengthEstimator} stands in with a fixed shape where no font or text
 * is available (the fit still sizes a band there, but no text draws).
 */
public interface LabelLengthEstimator {

    /**
     * The length a label needs to read at {@code lineHeight} per line across
     * {@code lineCount} lines, in the same world units the fit works in.
     *
     * @param lineHeight the height of a single line
     * @param lineCount  how many lines the label is stacked into - more lines carry
     *                   fewer characters each, so each line is shorter
     * @return the length the longest line of the label would occupy, or positive
     *         infinity when the text cannot be split into that many lines at all
     */
    double requiredLengthFor(double lineHeight, int lineCount);

    /**
     * The label's lines at the given line count - the same wrap
     * {@link #requiredLengthFor} measured, so a label draws exactly the block the fit
     * sized.
     *
     * @param lineCount how many lines to wrap the text into
     * @return the wrapped lines, top line first, or an empty list when the text cannot
     *         be split into that many lines or the estimator has no real text
     *         (a stand-in)
     */
    List<String> wrapIntoLines(int lineCount);
}
