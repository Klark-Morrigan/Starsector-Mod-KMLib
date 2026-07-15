package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;

import java.awt.Color;

/**
 * Raw-GL paint for a {@link kmlib.starsector.ui.controls.ControlSpec.Divider}: a single horizontal
 * hairline centred in the row, spanning its whole width, faded by one opacity. It parts one run of
 * body controls from the next, so a host heads a section with a rule instead of a caption. The GL
 * passthrough (over {@link UiFill}), exercised in-engine like the other draw helpers.
 *
 * <p>The rule is drawn dimmer than a control frame so it separates without competing with the lit
 * controls around it, in the same frame colour every body control strokes with.
 */
public final class DividerRenderer {
    // The rule sits below a full control frame's weight so it reads as a quiet section break rather
    // than another bordered widget.
    private static final float RULE_ALPHA_MULT = 0.5f;
    private static final float RULE_THICKNESS = 1f;

    private DividerRenderer() {
    }

    /**
     * Draws the rule across {@code bounds}, one hairline thick and vertically centred, in
     * {@code ruleColor} faded by {@code opacity}. Must run with a current GL context, like any
     * immediate-mode GL call.
     *
     * @param bounds    the divider row's footprint, in UI coordinates
     * @param ruleColor the rule colour, the body's frame colour
     * @param opacity   overall alpha, 0..1
     */
    public static void render(Rectangle bounds, Color ruleColor, float opacity) {
        var ruleY = bounds.computeCenterY() - RULE_THICKNESS / 2f;
        UiFill.renderQuad(bounds.x(), ruleY, bounds.width(), RULE_THICKNESS, ruleColor,
                opacity * RULE_ALPHA_MULT);
    }
}
