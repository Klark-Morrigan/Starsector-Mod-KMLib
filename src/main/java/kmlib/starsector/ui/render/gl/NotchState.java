package kmlib.starsector.ui.render.gl;

/**
 * The transient collapse-handle state a render pass needs to draw the notch: how far the body is collapsed
 * and how far the handle has lit under the pointer. It bundles the two so the render call names the pairing
 * rather than threading two loose primitives through, and stays a plain value with no coupling to the
 * consumer's animation holders - the renderer reads what to draw, not where the state lives.
 *
 * <p>The hover arrives as a fraction rather than a flag because the handle travels onto its lit look and
 * back off it, at the pace the header's tabs do. Whoever owns the panel's live state resolves it, so this
 * pass reads no cursor and holds no timing.
 *
 * @param collapseFraction how far the body is collapsed horizontally, 0 fully expanded to 1 fully docked;
 *                         orients the chevron and gates the collapse clip
 * @param hoverFraction    how far the handle has faded onto its lit look, 0 fully at rest to 1 fully lit
 */
public record NotchState(
    float collapseFraction,
    float hoverFraction) {
}
