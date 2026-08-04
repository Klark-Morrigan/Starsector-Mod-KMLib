package kmlib.starsector.ui.text;

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
 * <p>It carries no colour, unlike the {@link TextSpan} it shares a label with: a crest is drawn as it
 * was authored and is only ever faded by the host's opacity, so a colour here would be a tint nothing
 * asked for. That asymmetry is why the two are separate members of {@link LabelRun} rather than one
 * record with an optional image.
 *
 * @param spritePath the image's {@code graphics} texture path
 */
public record ImageSpan(
    String spritePath) implements LabelRun {

    /**
     * Rejects a null path, since a label that shows no image simply carries no image run rather than
     * one with nothing to load. A null otherwise surfaces at the texture lookup inside a draw call,
     * well past the point that could say which line was meant.
     */
    public ImageSpan {
        Objects.requireNonNull(spritePath, "spritePath");
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
}
