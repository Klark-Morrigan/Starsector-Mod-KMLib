package kmlib.starsector.ui.text;

import kmlib.starsector.ui.font.TextSpanMeasurer;

/**
 * The rendered width of one {@link TextSpan} in a look that is already settled - the face, the size,
 * and any casing bound before the measurement is handed on, so whatever asks passes the span and
 * nothing else.
 *
 * <p>The binding is what separates this from {@link TextSpanMeasurer}, which takes the face per call.
 * A caller holding only a span cannot supply that face without also learning which style the line it
 * sits on resolved to, and cannot apply the casing that style shouts the text in - and text measured
 * as authored measures narrower than it paints, so a box sized from it clips the text drawn into it.
 * Binding the look once and passing the bound measurement keeps both decisions with whatever resolved
 * the style.
 *
 * <p>Asked of the span rather than of its text, so nothing above has to unpack a span and reach past
 * the binding to charge it. A span with nothing to draw is measured by the same path as one that
 * draws, since {@link TextSpan} spells absence as blank text rather than as a missing span.
 */
@FunctionalInterface
public interface StyledSpanMeasurer {

    /**
     * The rendered width of {@code textSpan} in the look this measurement is bound to, in the same
     * units that look's face measures in.
     *
     * @param textSpan the run to measure, already a single line
     * @return the run's rendered width
     */
    double measureSpanWidth(TextSpan textSpan);
}
