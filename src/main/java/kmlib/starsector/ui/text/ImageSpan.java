package kmlib.starsector.ui.text;

import java.awt.Color;
import java.util.Objects;

/**
 * A small image set into a label as one of its runs - a faction crest, an icon, whatever the host
 * supplies - so a line can name something and show its mark in the same sentence rather than pushing
 * the mark out into a column of its own. What a caller reaches for when the image belongs to the words
 * ("[crest] Hegemony core territory") rather than to the row ("[crest] | Hegemony | 42").
 *
 * <p>It hangs as a square as tall as its line, the same rule a leading crest is sized by, so it sits
 * level with the words beside it whatever face they draw in and a stack of lines shows equally-sized
 * images without any line stating a size.
 *
 * <p>Its colour is the image's own rather than the label's, which is not what the {@link TextSpan} it
 * shares a label with means by one: a text span states the colour its glyphs are drawn in, while a tint
 * here is a colour the texture is multiplied by. A crest passes none - its colours are in its own
 * pixels and only ever faded by the host's opacity. An image whose colour was authored beside its path
 * states that colour here, because some families are drawn as one shared glyph told apart only by the
 * colour each type declares, and drawn untinted every member of the family comes out the same shape in
 * the same shade. Either way the tint is the asset's, so it travels with the path from wherever the
 * asset was read all the way to the draw rather than being picked by whatever paints the line.
 *
 * @param spritePath the image's {@code graphics} texture path
 * @param tintColour the colour the texture is multiplied by, or null for an image drawn as authored
 */
public record ImageSpan(
    String spritePath,
    Color tintColour) implements LabelRun {

    // How an image that states no tint spells its absence. Named so the untinted spelling has one home
    // rather than a bare null standing in for it wherever a run is built from a path alone.
    private static final Color NO_TINT = null;

    // What "as authored" comes to at the draw: white is the multiply that changes nothing, so an image
    // stating no colour and one stating white are the same pixels rather than two paths through a pass.
    private static final Color AS_AUTHORED_TINT = Color.WHITE;

    /**
     * Rejects a null path, since a label that shows no image simply carries no image run rather than
     * one with nothing to load. A null otherwise surfaces at the texture lookup inside a draw call,
     * well past the point that could say which line was meant. The tint is nullable by design - most
     * images are drawn as authored - so only the path is checked here.
     */
    public ImageSpan {
        Objects.requireNonNull(spritePath, "spritePath");
    }

    /**
     * Builds a run for an image drawn as its asset authored it, stating no tint. What a caller reaches
     * for when the colours are in the texture's own pixels - a faction crest - so it need not name a
     * white it did not choose to say it chose nothing.
     *
     * @param spritePath the image's {@code graphics} texture path
     */
    public ImageSpan(String spritePath) {
        this(spritePath, NO_TINT);
    }

    /**
     * The square the image hangs in, which is the line's own height. The measurement spends no face:
     * an image's width is geometry rather than glyphs.
     */
    @Override
    public float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer) {
        return lineHeight;
    }

    /**
     * Always true - an image run exists only because a caller had an image to set into the line, so
     * there is no blank spelling of one the way there is for a run of text.
     */
    @Override
    public boolean hasContent() {
        return true;
    }

    /**
     * Hands this run to {@code labelRunPainter} as the kind it is, so a surface paints an inline image by
     * saying what one looks like rather than by testing what the run happens to be.
     */
    @Override
    public void paintRun(LabelRunPainter labelRunPainter, float runX) {
        labelRunPainter.paintImageSpan(this, runX);
    }

    /**
     * The colour this image is actually multiplied by: its own where it states one, and the no-op
     * multiply where it states none. What a caller resolving a colour off this run reaches for, so
     * "a null tint means as authored" is answered here rather than restated wherever a run is drawn or
     * washed - two spellings of it are two chances for one of them to paint an image black.
     *
     * @return the tint to draw this image under, never null
     */
    public Color resolveDrawnTint() {
        return tintColour == null ? AS_AUTHORED_TINT : tintColour;
    }
}
