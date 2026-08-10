package kmlib.opengl;

import org.lwjgl.opengl.GL11;

/**
 * How a blended pass combines what it draws with what is already in the frame buffer. Named
 * for the result rather than for the GL factor pair that produces it, so a call site states
 * the look it wants and the pair stays in one place - a factor pair written out at the call
 * site is two constants whose meaning has to be recognised rather than read.
 *
 * <p>Each mode sets itself as the live blend function rather than exposing its factors,
 * which keeps the only knowledge of what the pair means on the type that names it.
 */
public enum GlBlendMode {

    /**
     * Draws over the frame buffer at the source's own alpha - the ordinary translucent
     * overlay, where a fully opaque pixel replaces what is under it and a fade lets it
     * through.
     */
    ALPHA(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA),

    /**
     * Adds to the frame buffer instead of replacing it, so overlapping draws brighten and
     * nothing this pass emits can darken what is under it - a glow, halo, or highlight wash.
     */
    ADDITIVE(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

    private final int destinationFactor;
    private final int sourceFactor;

    GlBlendMode(int sourceFactor, int destinationFactor) {
        this.sourceFactor = sourceFactor;
        this.destinationFactor = destinationFactor;
    }

    /**
     * Makes this mode the live blend function.
     *
     * <p>It changes state nothing here restores, so a caller is one that is already inside a save -
     * either {@link GlPasses#runBlendedPass}, which chooses the mode for a whole pass, or a draw
     * primitive setting its own mode per call inside a bracketed one. What it must not be is a caller
     * with no save above it at all, which would hand the pipeline on blending however it last drew.
     */
    public void applyBlendFunction() {
        GL11.glBlendFunc(sourceFactor, destinationFactor);
    }
}
