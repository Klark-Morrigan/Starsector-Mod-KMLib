package kmlib.starsector.ui.coreui;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.render.gl.GlStateGuard;
import kmlib.starsector.ui.render.gl.UiScissor;

/**
 * Repaints a core-UI component by driving its own draw entry point through {@link CoreUiTree}'s
 * by-name reach, inside a scissor clip. The live binding of {@link CoreUiComponentRepainter}, and
 * the only class that names that entry point, so the reflective reach into a component's draw
 * exists once.
 *
 * <p>A single {@link #INSTANCE}: the binding is a stateless forwarder over a static GL surface and
 * a by-name reach, so one shared value serves every caller rather than a fresh object per
 * construction.
 *
 * <p>Two conditions the clip rests on, both the caller's to satisfy. {@link UiScissor}'s clip is
 * absolute rather than nested, so a caller already inside an outer clip hands in a region it has
 * composed itself. And a component that sets its own scissor box while drawing replaces this one
 * for the remainder of its draw - the surrounding state is still restored on the way out, but the
 * containment within the handed-in region is not guaranteed against a component that clips itself.
 *
 * <p>Touches the GL surface and reaches a draw entry point by name, so it is exercised in-engine
 * rather than in unit tests; there is no logic here to pin without a context, only an ordering. A
 * caller's own decisions around it are pinned through the port instead.
 */
public enum ReflectiveCoreUiComponentRepainter implements CoreUiComponentRepainter {
    INSTANCE;

    // The core UI's own draw entry point, taking the opacity to draw at. Every component declares
    // it as part of the core's component contract, so the name survives obfuscation.
    private static final String RENDER_METHOD = "render";

    // Ask for the component's full brightness, the same as the core's own draw of it asks for. The
    // component multiplies this by its fader, so a faded-in widget repaints at exactly the alpha it
    // was already drawn at rather than at a flat opaque one.
    private static final float FULL_OPACITY = 1f;

    /**
     * Must run with a current GL context, like any immediate-mode GL call. Fails as {@link
     * CoreUiTree#invokeWithArgs} does - the component exposing no such entry point, or its own draw
     * throwing - with both the clip and the GL state still restored on the way out.
     */
    @Override
    public void repaintClippedTo(Object component, Rectangle uiRegion) {
        // The component draws with whatever texturing, blending and colour it likes; the state save
        // is what stops those reaching the rest of the pass drawing around this call. Inside the
        // clip rather than around it, so a scissor enable the component flips is restored before the
        // clip itself is lifted.
        UiScissor.runClippedTo(uiRegion, () -> GlStateGuard.bracket(
            () -> CoreUiTree.invokeWithArgs(component, RENDER_METHOD, FULL_OPACITY)));
    }
}
