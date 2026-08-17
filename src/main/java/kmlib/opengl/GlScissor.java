package kmlib.opengl;

import kmlib.math.geometry.Rectangle;
import kmlib.math.geometry.Rectangles;

import org.lwjgl.opengl.GL11;

/**
 * The pixel rectangle a pass is permitted to write into, or nothing at all when it is writing
 * unclipped.
 *
 * <p>Reading it is how a pass can be asked what part of the screen it owns. A panel that draws
 * inside its own bounds clips itself to them, so the rectangle is the panel; a pass drawing across
 * the whole frame sets no clip. That makes the clip a statement about which pixels a pass's answers
 * can be about - a pass that may not paint the pixel under the pointer is not the pass that knows
 * what is under it - which is a containment question rather than an ordering one, and so holds
 * however many passes a frame turns out to have and in whatever order.
 *
 * <p>Absent and empty are deliberately different answers. No clip means the pass is unconstrained,
 * which contains every pixel; a clip of zero area means it may write nowhere, which contains none.
 * Folding the first into a full-screen rectangle would be a guess about a framebuffer size nothing
 * here reads - so it answers null, and a caller asking whether a pixel is owned reads that null as
 * "yes" through {@link Rectangle#containsPoint}'s absence rather than through a rule of its own.
 *
 * <p>The read side alone. {@code UiScissor} sets a clip from UI coordinates and deliberately reads
 * none, so the two are opposite halves of the same GL state and never overlap; that class also
 * records why it avoids this read, which is worth knowing before calling it per frame.
 *
 * <p>Distinct from {@link GlViewport}: that one is how a pass's coordinates are stretched, this one
 * is where its output may land. They are frequently the same rectangle and never the same question.
 */
public final class GlScissor {

    private GlScissor() {
    }

    /**
     * The clip in force, in framebuffer pixels.
     *
     * <p>Must be called with a GL context current, which for this library's callers means from
     * inside a render pass.
     *
     * @return the clip, or null while the scissor test is off - the pass is writing unclipped and no
     *         rectangle bounds it
     */
    public static Rectangle readScissorBox() {

        if (!GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)) {
            return null;
        }
        return GlPixelRectangle.toRectangle(GlPixelRectangle.readFrom(GL11.GL_SCISSOR_BOX));
    }

    /**
     * Words a clip for a diagnostic line.
     *
     * @param scissorBox the clip in force, as {@link #readScissorBox} reports it, or null for none
     * @return its origin and size, or that the pass is unclipped - which is a different reading from
     *         a clip that could not be measured, and says so
     */
    public static String describeScissorBox(Rectangle scissorBox) {
        return scissorBox == null ? "unclipped" : Rectangles.describe(scissorBox);
    }
}
