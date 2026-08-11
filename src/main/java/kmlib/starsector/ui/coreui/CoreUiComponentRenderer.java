package kmlib.starsector.ui.coreui;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.render.gl.GlStateGuard;
import kmlib.starsector.ui.render.gl.UiScissor;

/**
 * Draws a live core-UI component a second time, clipped to a region, so a pass composited after the
 * whole core UI can put one of the core's own widgets back on top of itself.
 *
 * <p>A pass drawn after the core UI wins against everything the core drew, its tooltips included.
 * That ordering is what makes such a pass visible at all, and equally why anything the core raises
 * underneath it stays hidden for as long as it is up. Repainting the hidden component within that
 * same pass is what lifts it back over: the core's own draw is still there underneath, and this one
 * lands above it.
 *
 * <p>The clip is what keeps the second draw from brightening anything. It is meant to be handed the
 * <em>occluder's</em> footprint rather than the component's, so only the part actually hidden is
 * repainted while the rest keeps the core's single draw. Every pixel on screen is then composited
 * exactly once and no blend stacks. The repaint asks for full opacity, which is what the core asked
 * for too - the component still scales that by its own fader, so the two draws agree pixel for
 * pixel where they overlap.
 *
 * <p>Two conditions the clip rests on, both the caller's to satisfy. {@link UiScissor}'s clip is
 * absolute rather than nested, so a caller already inside an outer clip hands in a region it has
 * composed itself. And a component that sets its own scissor box while drawing replaces this one
 * for the remainder of its draw - the surrounding state is still restored on the way out, but the
 * containment within the handed-in region is not guaranteed against a component that clips itself.
 *
 * <p>Touches the GL surface and reaches a draw entry point by name, so like the other draw helpers
 * it is exercised in-engine rather than in unit tests; there is no logic here to pin without a
 * context, only an ordering.
 */
public final class CoreUiComponentRenderer {

    // The core UI's own draw entry point, taking the opacity to draw at. Every component declares
    // it as part of the core's component contract, so the name survives obfuscation.
    private static final String RENDER_METHOD = "render";

    // Ask for the component's full brightness, the same as the core's own draw of it asks for. The
    // component multiplies this by its fader, so a faded-in widget repaints at exactly the alpha it
    // was already drawn at rather than at a flat opaque one.
    private static final float FULL_OPACITY = 1f;

    private CoreUiComponentRenderer() {
    }

    /**
     * Repaints {@code component} clipped to {@code uiRegion}, restoring both the clip and the GL
     * state the component's draw touched on the way out. Must run with a current GL context, like
     * any immediate-mode GL call.
     *
     * <p>Every way the draw can fail - the component exposing no such entry point, or its own draw
     * throwing - comes back out to the caller, undeclared and not necessarily as a {@link
     * RuntimeException} (see {@link CoreUiTree#invokeWithArgs}). What a failed repaint means is the
     * caller's to decide, so a guard around this catches {@link Throwable}. The clip is ended
     * whichever way the draw leaves, since a clip left enabled would silently cut every later draw
     * in the frame down to this one region.
     *
     * @param component the live core-UI component to draw again, non-null
     * @param uiRegion  the region to confine the repaint to, in UI coordinates
     */
    public static void renderClippedTo(Object component, Rectangle uiRegion) {
        UiScissor.push(uiRegion);
        try {
            // The component draws with whatever texturing, blending and colour it likes; the state
            // save is what stops those reaching the rest of the pass drawing around this call.
            GlStateGuard.bracket(
                () -> CoreUiTree.invokeWithArgs(component, RENDER_METHOD, FULL_OPACITY));
        } finally {
            UiScissor.pop();
        }
    }
}
