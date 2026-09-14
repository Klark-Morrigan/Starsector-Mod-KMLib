package kmlib.opengl;

import org.lwjgl.opengl.GL11;

/**
 * Whether a pass's lines are antialiased. The choice is not free either way: smoothing costs
 * a blend per covered pixel and makes a thin line read as softer and slightly wider, so a
 * pass drawing long strokes a player looks at wants it and one drawing dense hatching or a
 * hairline grid usually does not.
 *
 * <p>Named as a quality rather than passed as a bare flag so a call site says which it
 * chose. The smoothing hint travels with it, since a pass that asks for smoothed lines and
 * leaves the hint at the driver's default is asking for an outcome it did not pick.
 */
public enum GlLineQuality {

    /** Hard-edged lines - no smoothing pass, and the geometry lands on exact pixels. */
    ALIASED,

    /** Antialiased lines, hinted for quality over speed. */
    SMOOTHED;

    // Applies this quality to the live GL state. Package-private for the same reason as the
    // blend mode: it is one part of a pass's setup, not state a caller should leave behind.
    void applyLineSmoothing() {
        if (this != SMOOTHED) {
            return;
        }
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
    }
}
