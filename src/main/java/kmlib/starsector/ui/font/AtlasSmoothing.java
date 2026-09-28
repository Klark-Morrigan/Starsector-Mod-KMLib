package kmlib.starsector.ui.font;

import java.util.regex.Pattern;

/**
 * Whether a font atlas wants its glyphs interpolated when they are drawn. The distinction is not cosmetic -
 * a face of single-pixel strokes drawn through an interpolating filter loses part of every stroke, because
 * every pixel of it is an edge pixel, so it arrives both softer and dimmer than the colour it was set in.
 *
 * <p>The atlas states it, in BMFont's {@code aa=} rather than its {@code smooth=}: {@code aa} is how many
 * samples each glyph was rasterised from, so {@code aa=1} is a face with no soft edge to interpolate and
 * {@code aa=4} is one that carries its own. The neighbouring {@code smooth=} flag is not the test and
 * reading it as one misclassifies faces in both directions - vanilla's {@code orbitron20aa} is antialiased
 * at {@code smooth=0}, and its {@code orbitron12condensed} is hard-edged at {@code smooth=1}.
 *
 * <p>Carried per face rather than decided at the draw site, since it is a property of the atlas and the
 * same for every caller that ever draws in it.
 */
public enum AtlasSmoothing {

    /**
     * An antialiased atlas, whose glyphs already carry their own soft edges and are meant to be
     * interpolated when scaled - what every face here but the pixel ones is.
     */
    SMOOTHED,

    /**
     * A pixel face: its glyphs are hard-edged by design and each pixel of them is meant to land on one
     * pixel of screen, so nothing should be blended between them.
     */
    PIXEL_EXACT;

    // The info line's sample count, a whole word so a longer key ending in "aa" is never read for it.
    private static final Pattern SAMPLE_COUNT_FIELD = Pattern.compile("\\baa=(\\d+)");

    // The sample count of an atlas rasterised with no antialiasing at all.
    private static final int SINGLE_SAMPLE = 1;

    /**
     * Reads an atlas's smoothing off its descriptor's first line, the {@code info} line carrying
     * {@code aa=}: a single sample is a pixel face, and more is an antialiased one. A line stating no
     * sample count reads as antialiased, the kind every face but a deliberate pixel face is.
     *
     * @param infoLine the descriptor's first line, may be {@code null}
     * @return the smoothing the line states
     */
    public static AtlasSmoothing resolveFromInfoLine(String infoLine) {

        if (infoLine == null) {
            return SMOOTHED;
        }
        var sampleCountMatch = SAMPLE_COUNT_FIELD.matcher(infoLine);

        return sampleCountMatch.find() && Integer.parseInt(sampleCountMatch.group(1)) == SINGLE_SAMPLE
            ? PIXEL_EXACT
            : SMOOTHED;
    }
}
