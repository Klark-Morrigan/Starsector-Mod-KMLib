package kmlib.starsector.ui.label;

/**
 * The per-line font clamp and line budget {@link LabelBoxFitter} sizes text within: the
 * readability floor and the oversize ceiling a single line may take, the most lines the
 * text may stack into, and the spacing between them.
 *
 * <p>Gathered into one value so the fitter takes its whole per-line sizing surface as data
 * rather than four loose constructor arguments, keeping the font clamp and the line budget
 * together at the call site.
 *
 * @param minFontHeight the smallest per-line font height a fit will accept, world units -
 *                      the readability floor; text that cannot hold even one line this tall
 *                      does not fit
 * @param maxFontHeight the largest per-line font height a fit will grow to, world units, so
 *                      a roomy region does not mint an oversized label
 * @param maxLines      the most lines the text may wrap into, spending girth to shorten the
 *                      length its widest line needs
 * @param lineSpacing   the line-height multiple between stacked lines, at least 1
 */
public record NameFitSpecification(
        double minFontHeight,
        double maxFontHeight,
        int maxLines,
        double lineSpacing) {
}
