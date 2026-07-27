package kmlib.starsector.ui.widgets;

import kmlib.starsector.ui.text.TextStyle;

/**
 * The typography of one tooltip: the look each {@link TooltipLineStyle} draws in. It is the other half
 * of the row model's content-versus-look split - rows carry which kind of line they are, this carries
 * what each kind looks like - so a box restyles a whole class of its lines at once and no row has to
 * name a face.
 *
 * <p>It sits beside the row model rather than with any renderer because a tooltip is drawn on more than
 * one surface: a raw-GL box reads these styles into its own measuring and draws, while a vanilla-widget
 * tooltip reads the same two into {@code setTitleFont} / {@code setParaFont} and their colours. Both
 * surfaces want the same answer to "how does a heading look here", so the answer cannot live inside
 * either one.
 *
 * <p>Held as a pair of complete {@link TextStyle}s rather than as the parts they differ in, because the
 * kinds genuinely differ in more than one part at once - vanilla's headings are a different typeface at
 * a different size from its body text - and a caller wanting them identical simply passes one style
 * twice.
 *
 * @param headerStyle    the look of a line that heads the box
 * @param paragraphStyle the look of a line of the box's body
 */
public record TooltipStyle(
        TextStyle headerStyle,
        TextStyle paragraphStyle) {

    /**
     * Answers what {@code lineStyle} draws in - the lookup a renderer makes once per row, before
     * measuring it or drawing it, so both use the one style and a row cannot be measured on a face it
     * is not painted in.
     *
     * @param lineStyle the kind of line being laid out or drawn
     * @return the look that kind draws in
     */
    public TextStyle resolveStyleFor(TooltipLineStyle lineStyle) {
        // Matched value by value rather than by a map or an ordinal so that a new line style is a
        // compile error here - the one place that would otherwise silently hand it the body look and
        // leave the new kind indistinguishable from a paragraph on screen.
        return switch (lineStyle) {
            case HEADER -> headerStyle;
            case PARAGRAPH -> paragraphStyle;
        };
    }
}
