package kmlib.starsector.ui.font;

import org.lazywizard.lazylib.ui.LazyFont;

/**
 * A {@link LineWidthMeasurer} backed by a LazyLib {@link LazyFont}: the one place a
 * concrete bitmap font's own metrics are read behind the font-agnostic measurement
 * port, so text-layout code depending on the port never touches LazyLib.
 *
 * <p>Width is what the port asks for and all a layout needs. Where a glyph sits within
 * its line is asked here as well, unported, because only a surface holding a loaded face
 * can want it: it is what something drawn <em>among</em> a line's words rather than as one
 * of them lines itself up on, and nothing that merely places text has such a thing.
 *
 * @param font the loaded face whose glyph metrics measure each line; must be non-null
 */
public record LazyFontMeasurer(
    LazyFont font) implements LineWidthMeasurer {

    // The glyph a face's lower-case band is read off. An x is the letter with a flat top and a flat
    // bottom on every atlas here, so it states where lower-case letters actually sit without the
    // overshoot a round glyph adds at both ends.
    private static final char LOWERCASE_BAND_GLYPH = 'x';

    // Half of that band's own height - where the middle of it falls, measured down from its top.
    private static final float BAND_MIDPOINT_FRACTION = 0.5f;

    @Override
    public double measureLineWidth(String line, double fontSize) {
        return font.calcWidth(line, (float) fontSize);
    }

    /**
     * How far below a line's top edge the middle of its lower-case letters sits, when that line is drawn
     * at {@code fontSize}. What a mark set among the words - a rule led from a label across to the value
     * it points at - is centred on, so it reads as sitting on the same optical line as the text either
     * side of it rather than floating over the words or dropping under them.
     *
     * <p>Read off the face's own glyph box rather than taken as a fraction of the line, because a bitmap
     * face's lower-case letters sit wherever its atlas puts them: the fraction that centres one face's
     * band rides high on the next, and the faces a box mixes are the ones that differ most.
     *
     * @param fontSize the size the line draws at
     * @return the drop from the line's top edge to the middle of its lower-case band, in UI units
     */
    public double measureLowercaseBandCentreDrop(double fontSize) {

        var lowercaseGlyph = font.getChar(LOWERCASE_BAND_GLYPH);

        // The atlas's own units brought to the size the line draws at, by the same scale LazyLib places
        // the glyph quads it emits by - so the rule lands on the text as drawn rather than as stored.
        var atlasScale = fontSize / font.getBaseHeight();

        return (lowercaseGlyph.getYOffset() + lowercaseGlyph.getHeight() * BAND_MIDPOINT_FRACTION)
            * atlasScale;
    }
}
