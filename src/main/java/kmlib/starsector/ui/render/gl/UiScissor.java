package kmlib.starsector.ui.render.gl;

import com.fs.starfarer.api.Global;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.input.UiCursor;

import org.lwjgl.opengl.GL11;

/**
 * Clips raw-GL drawing to a UI-space rectangle through the scissor test, so a pass drawing in UI
 * coordinates can confine one element - a scrolling list within its viewport - to a region and let its
 * overrun fall outside rather than paint over its neighbours. The scissor test operates in raw
 * framebuffer pixels, not the UI projection the layout works in, so this rescales the rectangle from UI
 * units to pixels (through {@link UiCursor#convertUiToPixel}, the inverse of the mouse's pixel-to-UI
 * mapping) before handing it to {@link GL11#glScissor}. Touches the GL surface and the settings statics,
 * so it is exercised in-engine like the other draw helpers; the rescale itself is the unit-tested pure
 * conversion.
 *
 * <p>{@link #push} saves the prior scissor state and enables the clip; {@link #pop} restores it, so the
 * clip brackets one element's draw and leaves the surrounding passes unclipped. The two must be paired.
 *
 * <p>The clip is ABSOLUTE: a raw {@link GL11#glScissor} replaces the whole clip region, so pushing while
 * an outer clip is already active does not narrow it, it supplants it for the bracketed draw. A caller
 * clipping one element within an outer clip must therefore hand in an already-composed region - the
 * element's own bound intersected with the outer one (see {@link Rectangle#intersectWith}) - rather than
 * relying on this to compose them. This deliberately reads no GL state: the current scissor cannot be
 * queried back without a synchronous {@code glGet}, which the Fast Rendering pipeline forbids per frame
 * (it stalls the async command stream), so composition is the caller's, computed from geometry it holds.
 */
public final class UiScissor {
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

        // GL_SCISSOR_BIT carries both the enable flag and the box, so a plain push/pop restores whatever
        // scissor state the surrounding pass held without this needing to read it back.
        GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        // Floor to whole pixels and never pass a negative extent (a no-display axis rescales to -1), so
        // a degenerate region clips everything out instead of erroring.
        GL11.glScissor(
            Math.round(pixelX),
            Math.round(pixelY),
            Math.max(0, Math.round(pixelBoxWidth)),
            Math.max(0, Math.round(pixelBoxHeight)));
    }

    /**
     * Restores the scissor state {@link #push} saved, ending the clip so later passes draw unclipped.
     */
    public static void pop() {
        GL11.glPopAttrib();
    }
}
