package kmlib.starsector.ui.font;

/**
 * Whether a font atlas wants its glyphs interpolated when they are drawn, which the atlas itself states:
 * BMFont writes {@code smooth=1} for a face rendered with soft edges and {@code smooth=0} for one whose
 * pixels are the design. The distinction is not cosmetic - a face of single-pixel strokes drawn through an
 * interpolating filter loses part of every stroke, because every pixel of it is an edge pixel, so it
 * arrives both softer and dimmer than the colour it was set in.
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
