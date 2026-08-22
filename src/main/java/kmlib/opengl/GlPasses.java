package kmlib.opengl;

import org.lwjgl.opengl.GL11;

/**
 * Runs a block of immediate-mode drawing inside saved GL state, so a pass that flips texturing,
 * blending, the colour, or a line width hands the pipeline back exactly as it found it. Every
 * attribute save in the library goes through here: a pass that saves its own is a pass whose mask
 * can be wrong on its own.
 *
 * <p>The saved attribute set is one deliberate superset rather than a mask each pass chooses. A
 * mask naming only the bits a pass touches today has to be widened the day that pass strokes a
 * point or sets a hint, and the failure when it is not is invisible at the site that caused it:
 * the leaked state lands on whatever draws next, which is a different pass entirely. Saving more
 * than a pass needs costs one deeper copy of state the driver is already holding.
 *
 * <p>The restore runs in a finally block. An exception thrown mid-draw would otherwise leave the
 * attribute stack one deep, and since these run every frame, that silently overflows a stack the
 * spec only guarantees 16 entries of - taking every later pass's state with it, long after the
 * frame that threw.
 *
 * <p>Touches the GL context, so like {@link GlRuns} it runs only in-engine.
 */
public final class GlPasses {

    // Every attribute an immediate-mode 2D pass can alter: the enable flags (texturing, blending,
    // line smoothing), the current colour, the blend function, line width, point size, and the
    // smoothing hints.
    private static final int SAVED_ATTRIBUTES = GL11.GL_ENABLE_BIT
        | GL11.GL_CURRENT_BIT
        | GL11.GL_COLOR_BUFFER_BIT
        | GL11.GL_LINE_BIT
        | GL11.GL_POINT_BIT
        | GL11.GL_HINT_BIT;

    // Scopes state around a caller's drawing; never instantiated.
    private GlPasses() {
    }

    /**
     * Sets up an untextured, blended pass, runs {@code emitDrawCalls} inside it, and restores the
     * state that was live before. The two things that vary between overlay passes are how they
     * blend and whether their lines are antialiased, so those are chosen here; colour, line width
     * and point size stay the caller's to set inside the pass - they are saved here, not chosen
     * here.
     *
     * @param blendMode     how the pass's pixels combine with the frame buffer
     * @param lineQuality   whether the pass's lines are antialiased
     * @param emitDrawCalls the pass itself, run once with the state above in force
     */
    public static void runBlendedPass(
            GlBlendMode blendMode,
            GlLineQuality lineQuality,
            Runnable emitDrawCalls) {

        runWithSavedState(() -> {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            blendMode.applyBlendFunction();
            lineQuality.applyLineSmoothing();

            emitDrawCalls.run();
        });
    }

    /**
     * Runs {@code emitDrawCalls} between a matching save and restore, choosing nothing about how
     * it draws. For a pass that sets up its own state, or one drawing under whatever the caller
     * already established and only needing not to leak its own changes.
     *
     * @param emitDrawCalls the immediate-mode drawing to bracket
     */
    public static void runWithSavedState(Runnable emitDrawCalls) {
        GL11.glPushAttrib(SAVED_ATTRIBUTES);
        try {
            emitDrawCalls.run();
        } finally {
            GL11.glPopAttrib();
        }
    }
}
