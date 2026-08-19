package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.screen.ScreenAxis;
import kmlib.starsector.ui.screen.VanillaScreen;

import org.lwjgl.opengl.GL11;

/**
 * Clips raw-GL drawing to a UI-space rectangle through the scissor test, so a pass drawing in UI
 * coordinates can confine one element - a scrolling list within its viewport - to a region and let its
 * overrun fall outside rather than paint over its neighbours. The scissor test operates in raw
 * framebuffer pixels, not the UI projection the layout works in, so this rescales the rectangle from UI
 * units to pixels - each edge along its own {@link ScreenAxis}, the inverse of the mapping the mouse
 * arrives through - before handing it to {@link GL11#glScissor}. Touches the GL surface and the live
 * screen ({@link VanillaScreen}), so it is exercised in-engine like the other draw helpers; the rescale
 * itself is the axis's own unit-tested arithmetic.
 *
 * <p>{@link #runClippedTo} is how a caller brackets a draw: it pairs the two halves below and ends the
 * clip whichever way the draw leaves, which matters because a clip left enabled does not fail loudly -
 * it silently cuts every later draw in the frame down to one element's region. Reach for the raw {@link
 * #push} / {@link #pop} only where the clip cannot bracket a single call, and pair them by hand there.
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
     * Runs {@code draw} clipped to {@code uiRegion}, ending the clip on the way out even if the draw
     * throws. Must run with a current GL context, like any immediate-mode GL call.
     *
     * <p>The bracket form rather than a paired {@link #push} / {@link #pop} wherever the clip covers one
     * call, because the two failure modes are not comparable: a leaked state save shows up as the next
     * pass drawing wrong, where a leaked clip shows up as later passes not drawing at all, in a place
     * with no clue pointing back here. Mirrors {@link GlStateGuard#bracket}, which brackets the other
     * piece of state a UI draw borrows.
     *
     * @param uiRegion the clip rectangle, in UI coordinates
     * @param draw     the drawing to confine to it
     */
    public static void runClippedTo(Rectangle uiRegion, Runnable draw) {
        push(uiRegion);
        try {
            draw.run();
        } finally {
            pop();
        }
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
        var xAxis = VanillaScreen.resolveXAxis();
        var yAxis = VanillaScreen.resolveYAxis();

        // The scissor box is the UI rectangle in framebuffer pixels: both spaces share the bottom-left
        // origin, so the lower-left corner and the size each rescale on their own axis.
        var pixelX = xAxis.convertUiToPixel(uiRegion.x());
        var pixelY = yAxis.convertUiToPixel(uiRegion.y());
        var pixelBoxWidth = xAxis.convertUiToPixel(uiRegion.width());
        var pixelBoxHeight = yAxis.convertUiToPixel(uiRegion.height());

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
