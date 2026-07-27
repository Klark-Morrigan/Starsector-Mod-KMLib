package kmlib.starsector.ui.widgets;

/**
 * What kind of line a tooltip row is - a heading that names the box, or a line of its body - rather
 * than how that kind looks. A row states its kind and nothing about faces or sizes, and the tooltip
 * hosting it says what each kind draws in ({@link TooltipStyle}), so restyling every heading in a box
 * is one decision on the host rather than an edit to each row that happens to be one.
 *
 * <p>That split is what keeps the row model free of typography. A row is content - text, colours, a
 * crest, a value - authored by whatever knows the subject matter; the look belongs to whatever knows
 * the surface it is painted on. Were the face on the row, every builder of rows would have to know the
 * host's typography, and two builders feeding one box could disagree about it.
 */
public enum TooltipLineStyle {
    HEADER,
    PARAGRAPH
}
