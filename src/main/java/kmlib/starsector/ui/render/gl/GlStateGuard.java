package kmlib.starsector.ui.render.gl;

import org.lwjgl.opengl.GL11;

/**
 * Brackets a block of immediate-mode UI drawing in a GL attribute save, restoring the enable
 * flags, current colour, and blend state the draw touched on the way out. The single place the
 * above-UI state-save mask lives: the map chrome and its tooltips draw after any UI-overlay pass,
 * so a raw-GL widget that flips texturing, blending, or the colour has to hand the pipeline back
 * exactly as it found it or it corrupts whatever draws next.
 */
public final class GlStateGuard {
    private GlStateGuard() {
    }

    /**
     * Runs {@code draw} between a matching {@code glPushAttrib}/{@code glPopAttrib}, restoring the
     * saved enable, current, and colour-buffer state even if the draw throws. Must run with a
     * current GL context, like any immediate-mode GL call.
     *
     * @param draw the immediate-mode GL draw to bracket
     */
    public static void bracket(Runnable draw) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            draw.run();
        } finally {
            GL11.glPopAttrib();
        }
    }
}
