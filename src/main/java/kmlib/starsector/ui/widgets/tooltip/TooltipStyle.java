package kmlib.starsector.ui.widgets.tooltip;

import kmlib.starsector.ui.text.TextStyle;

/**
 * The typography of one tooltip: the look each {@link TooltipLineStyle} draws in, and how far apart its
 * {@link TooltipSection blocks} stand. It is the other half of the row model's content-versus-look split
 * - rows carry which kind of line they are, this carries what each kind looks like - so a box restyles a
 * whole class of its lines at once and no row has to name a face.
 *
 * <p>It sits beside the row model rather than with any renderer because a tooltip is drawn on more than
 * one surface: a raw-GL box reads these styles into its own measuring and draws, while a vanilla-widget
 * tooltip reads the same two into {@code setTitleFont} / {@code setParaFont} and their colours. Both
 * surfaces want the same answer to "how does a heading look here", so the answer cannot live inside
 * either one. The parting between blocks is here for the same reason - every surface that stacks blocks
 * parts them, so it is not GL chrome the way an opacity or a border is.
 *
 * <p>Held as complete {@link TextStyle}s rather than as the parts they differ in, because the kinds
 * genuinely differ in more than one part at once - vanilla's headings are a different typeface at a
 * different size from its body text - and a caller wanting two of them identical simply passes one style
 * twice.
 *
 * @param headerStyle    the look of a line that heads the box
 * @param paragraphStyle the look of a line of the box's body
 * @param footnoteStyle  the look of a note at the box's foot; a box with nothing to note never resolves
 *                       it, so it defaults to the body look rather than being stated by every caller
 * @param sectionBreak   the room taken above a block for the one above it, in UI units - what parts two
 *                       blocks, where two lines of one block sit a plain line gap apart
 */
public record TooltipStyle(
    TextStyle headerStyle,
    TextStyle paragraphStyle,
    TextStyle footnoteStyle,
    float sectionBreak) {

    // How far apart blocks stand unless a box says otherwise: half a line of body text past the gap two
    // lines of one block already sit at, which reads as a parted block without looking like a dropped
    // line. Stated as one measurement rather than derived from whichever line happens to open a block,
    // so every parting in a box is the same width whatever sizes its blocks begin at.
    private static final float DEFAULT_SECTION_BREAK = 11.5f;

    /**
     * Builds the plainest typography there is: the two looks a box always has, with blocks parted by the
     * standard break and a note at the foot set in the body look. What a box wants beyond that it layers
     * on with {@link #partedBy} or {@link #footnotedIn}, so a caller states only what differs from the
     * baseline.
     *
     * <p>The footnote defaults rather than being asked for because most boxes note nothing at all, and
     * one that does not never resolves the look - so demanding a third face here would have every caller
     * name a face for a line it will not draw.
     *
     * @param headerStyle    the look of a line that heads the box
     * @param paragraphStyle the look of a line of the box's body
     * @return the typography drawing those looks at the standard parting
     */
    public static TooltipStyle createStyle(TextStyle headerStyle, TextStyle paragraphStyle) {
        return new TooltipStyle(
            headerStyle,
            paragraphStyle,
            paragraphStyle,
            DEFAULT_SECTION_BREAK);
    }

    /**
     * Returns a copy of this typography setting notes at the box's foot in {@code footnoteStyle} - the
     * smaller, quieter face such a line is set apart from the content above it by.
     *
     * @param footnoteStyle the look of a note at the box's foot
     * @return an otherwise-identical typography setting its footnotes in that look
     */
    public TooltipStyle footnotedIn(TextStyle footnoteStyle) {
        return rebuildOnTheSameFaces(footnoteStyle, sectionBreak);
    }

    /**
     * Returns a copy of this typography whose blocks stand {@code sectionBreak} apart - a tighter box
     * for a dense list, a wider one where the blocks answer separate questions.
     *
     * @param sectionBreak the room taken above a block for the one above it, in UI units
     * @return an otherwise-identical typography parting its blocks by that much
     */
    public TooltipStyle partedBy(float sectionBreak) {
        return rebuildOnTheSameFaces(footnoteStyle, sectionBreak);
    }

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
            case FOOTNOTE -> footnoteStyle;
        };
    }

    // Rebuilds the typography around whatever a refinement changed, carrying the two looks a box always
    // has over untouched. Shared rather than each refinement restating the parts it leaves alone - which
    // is where a fourth part, and then a fifth, eventually gets restated wrongly in one of them.
    private TooltipStyle rebuildOnTheSameFaces(TextStyle footnoteStyle, float sectionBreak) {
        return new TooltipStyle(headerStyle, paragraphStyle, footnoteStyle, sectionBreak);
    }
}
