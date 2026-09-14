package kmlib.starsector.ui.render.gl;

import kmlib.opengl.GlPasses;

/**
 * Brackets a block of immediate-mode UI drawing in a GL attribute save, restoring the state the
 * draw touched on the way out. The map chrome and its tooltips draw after any UI-overlay pass, so
 * a raw-GL widget that flips texturing, blending, or the colour has to hand the pipeline back
 * exactly as it found it or it corrupts whatever draws next.
 *
 * <p>Which attributes that covers is {@link GlPasses}' to decide, not this tier's: a widget's
 * save and an overlay pass's save protect the same pipeline from the same class of leak, and two
 * masks would mean the narrower one is wrong the first time a widget strokes something the other
 * saves for.
 */
public final class GlStateGuard {
    private GlStateGuard() {
    }

    /**
     * Runs {@code draw} inside a saved GL state, restoring it even if the draw throws. Must run
     * with a current GL context, like any immediate-mode GL call.
     *
     * @param draw the immediate-mode GL draw to bracket
     */
    public static void bracket(Runnable draw) {
        GlPasses.runWithSavedState(draw);
    }
}
