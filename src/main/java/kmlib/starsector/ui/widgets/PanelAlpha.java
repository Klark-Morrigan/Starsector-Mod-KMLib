package kmlib.starsector.ui.widgets;

/**
 * The two alphas a panel is painted at, carried together because they answer different questions and
 * every surface of the panel needs both to settle its own.
 *
 * <p><b>Body opacity</b> is how see-through the panel is meant to be at rest - a look, usually a
 * player's own setting, that a panel wears for as long as it is on screen. <b>Panel fade</b> is
 * whether the panel is on screen at all, which is what a panel arriving or leaving travels through.
 * One is a property of the panel, the other of the moment.
 *
 * <p>They are not interchangeable, and the difference shows on the chrome. A tab row is opaque chrome
 * standing on a translucent body, so it opts out of the body's opacity - that is what makes a
 * see-through panel read as having a solid cap rather than as one uniformly faint sheet. It does
 * <em>not</em> opt out of the fade: a panel leaving the screen takes its chrome with it, and a row
 * that ignored the fade would hang there at full strength over a body dissolving underneath it,
 * which reads worse than no fade at all. So the two channels are asked for separately -
 * {@link #resolveBodyAlpha()} for what honours the look, {@link #resolveChromeAlpha()} for what only
 * honours the moment - and no caller multiplies them itself.
 *
 * <p>Bundled rather than passed as two floats in a row, for the reason every same-typed pair in this
 * library is: transposing them is invisible wherever the panel happens to be fully present, which is
 * every frame but the ones a fade is running, and those are exactly the frames nobody is looking at
 * a still image of.
 *
 * <p>Both are 0..1 and neither is clamped here - a caller handing in a number outside that range is
 * describing a paint the surface below will take literally, which is a caller's bug rather than a
 * value this type should quietly correct.
 */
public record PanelAlpha(
    float bodyOpacity,
    float panelFade) {

    // A panel wholly on screen, which is every frame outside a fade.
    private static final float FULLY_PRESENT = 1f;

    /**
     * A panel at rest, wearing its look and wholly present - the state a caller that never fades its
     * panel is always in, so it names one number rather than a number and a constant.
     *
     * @param bodyOpacity how see-through the body is meant to be, 0..1
     */
    public PanelAlpha(float bodyOpacity) {
        this(bodyOpacity, FULLY_PRESENT);
    }

    /**
     * @return the alpha for what honours both the look and the moment - the frame, the body's fill and
     *         controls, the scrollbar, and the collapse handle
     */
    public float resolveBodyAlpha() {
        return bodyOpacity * panelFade;
    }

    /**
     * @return the alpha for chrome that stands opaque on the body and so honours the moment alone - a
     *         tab row being the one such surface a panel carries
     */
    public float resolveChromeAlpha() {
        return panelFade;
    }
}
