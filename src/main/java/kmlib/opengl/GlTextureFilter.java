package kmlib.opengl;

import org.lwjgl.opengl.GL11;

/**
 * How a texture's texels are sampled when they do not land exactly on screen pixels. Named for what the
 * result looks like rather than for the GL constant, so a call site states the look it wants and the
 * constant stays in one place.
 *
 * <p>It is texture state rather than pass state: a filter set here belongs to the texture object and
 * outlives the pass that set it, which is why a caller that changes one for its own draw is the caller that
 * has to put it back.
 */
public enum GlTextureFilter {

    /** Texels blended where they fall between pixels - what an antialiased image wants. */
    SMOOTHED(GL11.GL_LINEAR),

    /**
     * The nearest texel taken whole, with nothing blended between them - what a pixel-art image wants,
     * whose every texel is meant to be a pixel and whose strokes lose part of themselves to any blending.
     */
    PIXEL_EXACT(GL11.GL_NEAREST);

    private final int glFilter;

    GlTextureFilter(int glFilter) {
        this.glFilter = glFilter;
    }

    /**
     * Binds {@code textureId} and makes this the filter it is sampled through, both when it is drawn
     * smaller than itself and when larger.
     *
     * <p>The binding is left in place, as it is by any pass that draws a texture: this is called
     * immediately before drawing that texture, and a caller drawing something else afterwards binds its
     * own.
     *
     * @param textureId the texture object to set the filter on
     */
    public void applyTo(int textureId) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, glFilter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, glFilter);
    }
}
