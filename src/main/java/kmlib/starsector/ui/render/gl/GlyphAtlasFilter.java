package kmlib.starsector.ui.render.gl;

import kmlib.math.ranges.Ranges;
import kmlib.opengl.GlTextureFilter;
import kmlib.starsector.ui.font.AtlasSmoothing;
import kmlib.starsector.ui.font.LazyFontCache;
import kmlib.starsector.ui.font.TextFace;

/**
 * Draws a run of text under the sampling its atlas asks for, and hands the atlas back the way it found it.
 *
 * <p>The font loader takes its atlas from the engine's sprite loader and never sets a filter on it, so
 * every face draws through whatever the engine left there - interpolated. An antialiased face is happy with
 * that; a pixel face is not, and loses part of every stroke to it, because each of its strokes is one pixel
 * wide and so is entirely edge. That is a shortfall in the glyphs rather than in the colour: the same
 * string drawn several times larger loses the same edge and keeps the rest, so it reads at full strength
 * while the small one reads short of it.
 *
 * <p>Restored afterwards because a font atlas is shared with whoever else draws in that face - the engine's
 * own chrome included, which letters its compact buttons in the same pixel atlas. Sharpening our own text
 * is ours to choose; sharpening the game's is not, and a filter left set would do exactly that until
 * something else happened to set it back.
 *
 * <p>Fully unfiltered is not the aim, though, and cannot be dialled back by choosing a third filter: the
 * pipeline offers these two and nothing between them. So a pixel face is drawn twice - once hard, then the
 * interpolated pass over it at a weight - and the sharpness is where between the two the text lands. Hard alone reads harsher than the chrome around it; interpolated alone is what cost the
 * strokes their weight in the first place.
 *
 * <p>GL passthrough, exercised in-engine like the other draw helpers.
 */
public final class GlyphAtlasFilter {

    /**
     * How much of a pixel face's hard edge survives where nothing has said otherwise: 1 draws it unfiltered
     * and nothing else, 0 leaves it interpolated as the loader found it, and a value between draws the hard
     * pass and lays the interpolated one over it at the remainder. A visual choice rather than a derived
     * one - the two ends are what the atlas asks for and what the engine does to it, and neither of them is
     * what looks right beside the chrome this text sits among.
     *
     * <p>Settled in game against the engine's own row: mostly hard, which reads more aliased than that row
     * does, but it is the tone that matches rather than the edge - the interpolated end costs the strokes
     * enough of themselves to read as a different colour, and that shows first.
     */
    public static final float DEFAULT_PIXEL_FACE_SHARPNESS = 0.8f;

    // What the atlas is handed back at. The engine's own default for a sprite, and so what every other
    // consumer of a shared atlas is drawing under.
    private static final GlTextureFilter SHARED_DEFAULT_FILTER = GlTextureFilter.SMOOTHED;

    // The pass that lays down the whole of what it draws, over which any second pass is weighted.
    private static final float FULL_PASS = 1f;

    // What the passes below actually read. Live state rather than a constant because the amount is a look
    // rather than a number: it answers to the chrome it happens to sit beside, and a host that draws among
    // different chrome - or a player who simply reads it differently - sets its own. The default above is
    // what it stands at until one does.
    private static float pixelFaceSharpness = DEFAULT_PIXEL_FACE_SHARPNESS;

    private GlyphAtlasFilter() {
    }

    /**
     * Draws {@code drawRuns} under the sampling {@code face} asks for - once at full strength, and for a
     * pixel face held short of fully hard, again through the interpolated filter at the remaining weight -
     * then puts the atlas back under the shared default.
     *
     * <p>A face whose atlas cannot be loaded, or one that wants the default anyway, draws once with nothing
     * changed - so a caller wraps every text pass in this rather than deciding per face which ones need it.
     *
     * @param face     the face the runs are drawn in
     * @param drawRuns the text pass itself, run once per pass and told what weight to draw at, which it
     *                 folds into every colour it sets - so the two passes composite to a point between the
     *                 two filters rather than to one drawn twice
     */
    public static void drawUnderAtlasFilter(TextFace face, GlyphPass drawRuns) {

        var font = LazyFontCache.loadByFace(face.font());

        if (font == null || face.font().getSmoothing() == AtlasSmoothing.SMOOTHED) {
            drawRuns.drawAt(FULL_PASS);
            return;
        }
        try {
            // The hard pass lays the glyphs down whole and the interpolated one is composited over it at
            // what is left of the weight, so the two land at the point between the filters the sharpness
            // names. Both run at either end of the range as well - at fully hard the second draws at no
            // weight and shows nothing, at fully soft the first is covered outright - which costs a pass
            // that draws nothing rather than a branch on a value that is meant to sit between the ends.
            GlTextureFilter.PIXEL_EXACT.applyTo(font.getTextureId());
            drawRuns.drawAt(FULL_PASS);

            GlTextureFilter.SMOOTHED.applyTo(font.getTextureId());
            drawRuns.drawAt(FULL_PASS - pixelFaceSharpness);

        } finally {
            SHARED_DEFAULT_FILTER.applyTo(font.getTextureId());
        }
    }

    /**
     * Sets how much of a pixel face's hard edge survives from here on, confined to 0..1 - the same meaning
     * {@link #DEFAULT_PIXEL_FACE_SHARPNESS} carries. A caller with no opinion sets nothing and takes the
     * default; one whose text sits among chrome of its own dials it against that.
     *
     * @param sharpness how hard the glyphs read, 0 fully interpolated and 1 fully unfiltered
     */
    public static void setPixelFaceSharpness(float sharpness) {
        pixelFaceSharpness = Ranges.clampToUnit(sharpness);
    }

    /**
     * @return how much of a pixel face's hard edge currently survives
     */
    public static float getPixelFaceSharpness() {
        return pixelFaceSharpness;
    }

    /**
     * One laying-down of a caller's text, at the weight this pass contributes.
     *
     * <p>The weight reaches the glyphs rather than the pass because that is the only place it can: two
     * filters cannot be blended in the pipeline, so they are blended on the screen, and a pass drawn at
     * full strength over another simply hides it.
     */
    @FunctionalInterface
    public interface GlyphPass {

        /**
         * @param passOpacity what to scale every colour this pass sets by, 0..1
         */
        void drawAt(float passOpacity);
    }
}
