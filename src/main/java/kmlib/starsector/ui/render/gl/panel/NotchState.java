package kmlib.starsector.ui.render.gl.panel;

import kmlib.math.ranges.Ranges;

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
 * <p>That fraction is confined to its range here, on the way in, rather than by each piece of the handle
 * that reads it. Two of them do - the wash over the face and the chevron's own shade - and they have to
 * agree about what "fully lit" is, which they cannot be relied on to if each decides for itself. The
 * collapse fraction is not confined here: it is read by one computation, {@link
 * NotchRenderer#computeChevronArms}, whose own limit is a statement about the glyph never inverting rather
 * than about the number, and which is exercised directly.
 *
 * @param collapseFraction how far the body is collapsed horizontally, 0 fully expanded to 1 fully docked;
 *                         orients the chevron and gates the collapse clip
 * @param hoverFraction    how far the handle has faded onto its lit look, 0 fully at rest to 1 fully lit;
 *                         confined to that range, so an overshooting animation value settles at an end
 */
public record NotchState(
    float collapseFraction,
    float hoverFraction) {

    // The fraction a panel resting fully expanded reports; above it the fold is under way.
    private static final float NO_COLLAPSE_FRACTION = 0f;

    public NotchState {
        hoverFraction = Ranges.clampToUnit(hoverFraction);
    }

    /**
     * Whether the fold is under way at all, which is the only time the body's draw is clipped to its own
     * shrinking box. Asked rather than compared, so the draw pass names the condition instead of testing a
     * loose float against a bound it would have to keep in step by hand.
     *
     * @return whether the body is anywhere but fully expanded
     */
    public boolean isFolding() {
        return collapseFraction > NO_COLLAPSE_FRACTION;
    }
}
