package kmlib.starsector.ui.font;

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
    PIXEL_EXACT
}
