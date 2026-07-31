package kmlib.starsector.ui.render.gl;

/**
 * The transient collapse-handle state a render pass needs to draw the notch: how far the body is collapsed
 * and whether the pointer is over the handle. It bundles the two so the render call names the pairing
 * rather than threading two loose primitives through, and stays a plain value with no coupling to the
 * consumer's collapse-state holder - the renderer reads what to draw, not where the state lives.
 *
 * @param collapseFraction how far the body is collapsed horizontally, 0 fully expanded to 1 fully docked;
 *                         orients the chevron and gates the collapse clip
 * @param isHovered        true when the pointer is over the notch, lighting the handle
 */
public record NotchState(
    float collapseFraction,
    boolean isHovered) {
}
