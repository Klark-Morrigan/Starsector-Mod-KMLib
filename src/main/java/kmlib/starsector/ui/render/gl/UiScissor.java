package kmlib.starsector.ui.render.gl;

import com.fs.starfarer.api.Global;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.input.UiCursor;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Clips raw-GL drawing to a UI-space rectangle through the scissor test, so a pass drawing in UI
 * coordinates can confine one element - a scrolling list within its viewport - to a region and let its
 * overrun fall outside rather than paint over its neighbours. The scissor test operates in raw
 * framebuffer pixels, not the UI projection the layout works in, so this rescales the rectangle from UI
 * units to pixels (through {@link UiCursor#convertUiToPixel}, the inverse of the mouse's pixel-to-UI
 * mapping) before handing it to {@link GL11#glScissor}. Touches the GL surface and the settings statics,
 * so it is exercised in-engine like the other draw helpers; the rescale and the box intersection are the
 * unit-tested pure computations.
 *
 * <p>{@link #push} saves the prior scissor state and enables the clip; {@link #pop} restores it, so the
 * clip brackets one element's draw and leaves the surrounding passes unclipped. The two must be paired.
 *
 * <p>A nested {@link #push} NARROWS the active clip rather than replacing it: a raw {@link GL11#glScissor}
 * is absolute, so pushing an inner region while an outer clip is enabled would let the inner draw escape
 * the outer bound. Instead this intersects the new region with the currently-enabled scissor box, so an
 * element with its own clip (a scrolling list's viewport) still stays inside an outer clip it is drawn
 * within (a collapsing panel's box wiping its body toward the docked rail). With no outer clip active the
 * region is used as-is.
 */
public final class UiScissor {
    // GL_SCISSOR_BOX reports the active clip as four ints (lower-left x, y, then width, height); the read
    // buffer is sized to hold exactly that so the current outer clip can be intersected with a nested push.
    private static final int SCISSOR_BOX_INT_COUNT = 4;

    private UiScissor() {
    }

    /**
     * Saves the current scissor state and clips subsequent drawing to {@code uiRegion}, rescaled from UI
     * units to framebuffer pixels. A degenerate region (no display, or zero-size after rescaling) clips
     * to nothing rather than dividing by zero, so the bracketed draw simply paints nothing. Pair with
     * {@link #pop}.
     *
     * @param uiRegion the clip rectangle, in UI coordinates
     */
    public static void push(Rectangle uiRegion) {
        var settings = Global.getSettings();
        var uiWidth = settings.getScreenWidth();
        var uiHeight = settings.getScreenHeight();
        var pixelWidth = settings.getScreenWidthPixels();
        var pixelHeight = settings.getScreenHeightPixels();
        // The scissor box is the UI rectangle in framebuffer pixels: both spaces share the bottom-left
        // origin, so the lower-left corner and the size each rescale on their own axis.
        var pixelX = UiCursor.convertUiToPixel(uiRegion.x(), uiWidth, pixelWidth);
        var pixelY = UiCursor.convertUiToPixel(uiRegion.y(), uiHeight, pixelHeight);
        var pixelBoxWidth = UiCursor.convertUiToPixel(uiRegion.width(), uiWidth, pixelWidth);
        var pixelBoxHeight = UiCursor.convertUiToPixel(uiRegion.height(), uiHeight, pixelHeight);
        // Floor to whole pixels and never pass a negative extent (a no-display axis rescales to -1), so
        // a degenerate region clips everything out instead of erroring.
        var region = new ScissorBox(
                Math.round(pixelX),
                Math.round(pixelY),
                Math.max(0, Math.round(pixelBoxWidth)),
                Math.max(0, Math.round(pixelBoxHeight)));
        // GL_SCISSOR_BIT carries both the enable flag and the box, so a plain push/pop restores whatever
        // scissor state the surrounding pass held without this needing to read it back. Saved before the
        // new box is applied, and while the current box is still the outer clip this reads to intersect.
        GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
        // Compose with any outer clip rather than replace it, so a nested push (a scrolling list's viewport)
        // cannot draw past an outer clip it sits within (a collapsing panel's box). No outer clip means the
        // region stands on its own.
        var clip = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
                ? readActiveScissorBox().intersectWith(region)
                : region;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(clip.x(), clip.y(), clip.width(), clip.height());
    }

    /**
     * Restores the scissor state {@link #push} saved, ending the clip so later passes draw unclipped.
     */
    public static void pop() {
        GL11.glPopAttrib();
    }

    // The active scissor box in framebuffer pixels, read straight from GL so the intersection composes with
    // whatever clip the surrounding pass set, not only clips this class pushed. glGet* only writes into a
    // direct buffer, so the read lands in one and its four ints (lower-left x, y, then width, height) are
    // copied into the box value.
    private static ScissorBox readActiveScissorBox() {
        var buffer = BufferUtils.createIntBuffer(SCISSOR_BOX_INT_COUNT);
        GL11.glGetInteger(GL11.GL_SCISSOR_BOX, buffer);
        // Sequential reads, not indexed: Java evaluates the arguments left to right, so each get() advances
        // the buffer position, taking the four ints in order (lower-left x, y, then width, height).
        return new ScissorBox(buffer.get(), buffer.get(), buffer.get(), buffer.get());
    }
}
