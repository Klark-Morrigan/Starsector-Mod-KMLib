package kmlib.starsector.ui.widgets.tooltip;

/**
 * What kind of line a tooltip row is - a heading that names the box, a line of its body, or a note at
 * its foot - rather than how that kind looks. A row states its kind and nothing about faces or sizes,
 * and the tooltip hosting it says what each kind draws in ({@link TooltipStyle}), so restyling every
 * heading in a box is one decision on the host rather than an edit to each row that happens to be one.
 *
 * <p>That split is what keeps the row model free of typography. A row is content - text, colours, a
 * crest, a value - authored by whatever knows the subject matter; the look belongs to whatever knows
 * the surface it is painted on. Were the face on the row, every builder of rows would have to know the
 * host's typography, and two builders feeding one box could disagree about it.
 */
public enum TooltipLineStyle {

    /** A line that names the box, read above whatever it goes on to say. */
    HEADER,

    /** A line of what the box has to say - the ordinary kind, and the default a row is built at. */
    PARAGRAPH,

    /**
     * A note at the foot of the box, about the box rather than about its subject - what a key press
     * would do to it, say. Its own kind because it is set apart from the body wherever tooltips are
     * drawn: the game's own boxes end their key hints in a smaller, narrower face than the content
     * above, so a note left to read as a paragraph would claim the same weight as a finding.
     */
    FOOTNOTE
}
