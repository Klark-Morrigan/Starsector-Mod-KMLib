package kmlib.starsector.ui.coreui;

import kmlib.math.geometry.Rectangle;

/**
 * Port for drawing a live core-UI component a second time, clipped to a region, so a pass
 * composited after the whole core UI can put one of the core's own widgets back on top of itself.
 *
 * <p>A pass drawn after the core UI wins against everything the core drew, its tooltips included.
 * That ordering is what makes such a pass visible at all, and equally why anything the core raises
 * underneath it stays hidden for as long as it is up. Repainting the hidden component within that
 * same pass is what lifts it back over: the core's own draw is still there underneath, and the
 * second one lands above it.
 *
 * <p>The clip is what keeps that second draw from brightening anything. It is meant to be handed
 * the <em>occluder's</em> footprint rather than the component's, so only the part actually hidden
 * is repainted while the rest keeps the core's single draw. Every pixel on screen is then
 * composited exactly once and no blend stacks.
 *
 * <p>A port rather than a static call because the only binding of it reaches a draw entry point by
 * name and writes to the GL surface, neither of which a test can stand under: whether a caller
 * repaints at all, what region it hands over, and how it survives a failed draw are decisions worth
 * pinning without a live game. {@link ReflectiveCoreUiComponentRepainter} is the live binding.
 */
public interface CoreUiComponentRepainter {

    /**
     * Repaints {@code component} clipped to {@code uiRegion}, restoring both the clip and any GL
     * state the draw touched on the way out.
     *
     * <p>Every way the draw can fail comes back out to the caller, undeclared and not necessarily
     * as a {@link RuntimeException}. What a failed repaint means is the caller's to decide, so a
     * guard around this catches {@link Throwable}.
     *
     * @param component the live core-UI component to draw again, non-null
     * @param uiRegion  the region to confine the repaint to, in UI coordinates
     */
    void repaintClippedTo(Object component, Rectangle uiRegion);
}
