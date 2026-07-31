package kmlib.starsector.ui.text;

import kmlib.starsector.ui.color.StarsectorUiColor;
import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.font.TextFace;

import java.awt.Color;
import java.util.Locale;

/**
 * The baseline look of a run of text - the face and size it draws in, the colour it draws in, where it
 * sits relative to the point it is drawn at, and whether it is shouted. One value so a control's text
 * look travels as a unit: the alternative is a face, a size, a colour, and a casing flag threaded side by
 * side through every style record, layout hop, and draw call, which is where two of the four quietly stop
 * agreeing.
 *
 * <p>It names no drawing surface, the way {@link TextAlignment} names none: everything it carries is
 * either an AWT value or a KMLib one, so the same style can be handed to a raw-GL drawer and read into
 * vanilla's {@code setParaFont} / {@code setTitleFont} and their colours. A look value is carried by every
 * styled control whichever surface paints it, so binding it to one would fork it per surface.
 *
 * <p>A style is built per paint and never frozen into a static constant, because the colours it draws
 * from resolve live: {@link StarsectorUiColor} tracks the running game's palette, including the player
 * faction's own recolours. Role baselines - "how a heading looks", "how body text looks" - are therefore
 * verb-named factory methods on whatever owns the role, each calling {@link #createStyle} afresh.
 *
 * @param face          the atlas the text draws in and the size it draws at
 * @param colour        the text colour before any per-span override or opacity fade the host applies
 * @param alignment     which edge or corner of the text box lands on the draw point, used by a renderer
 *                      whose layout does not pin an anchor of its own
 * @param isUpperCased  whether the text is shouted, as vanilla draws its headings
 */
public record TextStyle(
    TextFace face,
    Color colour,
    TextAlignment alignment,
    boolean isUpperCased) {

    // The baseline a caller gets without saying anything: plain body colour, hanging down and to the
    // right of its draw point, spoken rather than shouted. Named here so the one place that builds the
    // baseline reads as the decisions it is making rather than as a row of bare arguments.
    private static final TextAlignment DEFAULT_ALIGNMENT = TextAlignment.TOP_LEFT;
    private static final StarsectorUiColor DEFAULT_COLOUR = StarsectorUiColor.VANILLA_TEXT;
    private static final boolean IS_NOT_UPPER_CASED = false;
    private static final boolean IS_UPPER_CASED = true;

    /**
     * Builds the plainest style there is: {@code font} at the size its atlas was rasterised at, in the
     * palette's body-text colour. Every other part is layered on with a refinement below, so a caller
     * states only what differs from the baseline.
     *
     * <p>The size comes from the face rather than from the caller because a bitmap atlas is crisp at
     * exactly one size, so a caller with no size of its own wants that one - and naming the face alone
     * is then a complete style rather than half of one.
     *
     * @param font the atlas the text draws in
     * @return the baseline style for that face
     */
    public static TextStyle createStyle(StarsectorFont font) {
        return new TextStyle(
            new TextFace(font, font.getNativeSize()),
            DEFAULT_COLOUR.resolve(),
            DEFAULT_ALIGNMENT,
            IS_NOT_UPPER_CASED);
    }

    /**
     * Returns a copy of this style drawn at {@code size} instead of its face's current one, for a run
     * that must fit a box the atlas's own size overflows.
     *
     * @param size the size the text draws at
     * @return an otherwise-identical style at that size
     */
    public TextStyle sizedAt(double size) {
        return new TextStyle(new TextFace(face.font(), size), colour, alignment, isUpperCased);
    }

    /**
     * Returns a copy of this style drawing in {@code colour} - the run's normal, unhighlighted colour,
     * which a host may still override for one span or fade by its own opacity.
     *
     * @param colour the text colour
     * @return an otherwise-identical style in that colour
     */
    public TextStyle inColour(Color colour) {
        return new TextStyle(face, colour, alignment, isUpperCased);
    }

    /**
     * Returns a copy of this style anchored by {@code alignment} - which edge or corner of the text box
     * lands on the point it is drawn at.
     *
     * @param alignment where the run sits relative to its draw point
     * @return an otherwise-identical style with that alignment
     */
    public TextStyle alignedTo(TextAlignment alignment) {
        return new TextStyle(face, colour, alignment, isUpperCased);
    }

    /**
     * Returns a copy of this style shouted, the way vanilla draws its titles and section headings. The
     * casing is a property of the look rather than of the text, so a caller passes its label as authored
     * and one style decides how every run wearing it is spoken.
     *
     * @return an otherwise-identical style drawn in upper case
     */
    public TextStyle inUpperCase() {
        return new TextStyle(face, colour, alignment, IS_UPPER_CASED);
    }

    /**
     * Applies the style's casing to {@code text}, yielding exactly the characters that reach the atlas.
     * Both measuring and drawing go through it: a shouted line measured as authored measures narrower
     * than it paints, and the box sized from that measurement clips the text it drew.
     *
     * @param text the run as its caller authored it
     * @return the text as it is drawn
     */
    public String resolveDisplayText(String text) {
        // Locale.ROOT, not the default locale: a player's system locale must not change how a label
        // shouts (Turkish maps 'i' to a dotted capital the atlases carry no glyph for).
        return isUpperCased ? text.toUpperCase(Locale.ROOT) : text;
    }
}
