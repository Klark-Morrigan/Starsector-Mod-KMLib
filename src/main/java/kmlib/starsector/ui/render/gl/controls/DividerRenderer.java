package kmlib.starsector.ui.render.gl.controls;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;

import java.awt.Color;

/**
 * Raw-GL paint for a {@link kmlib.starsector.ui.controls.ControlSpec.Divider}: a single horizontal
 * hairline centred in the row, spanning its whole width, faded by one opacity. It parts one run of
 * body controls from the next, so a host heads a section with a rule instead of a caption. The GL
 * passthrough (over {@link UiFill}), run only in-engine like the other draw helpers.
 *
 * <p>The rule is drawn dimmer than a control frame so it separates without competing with the lit
 * controls around it, in the same frame colour every body control strokes with.
 */
public final class DividerRenderer {
    // The rule sits below a full control frame's weight so it reads as a quiet section break rather
    // than another bordered widget.
    private static final float DIVIDER_ALPHA_MULT = 0.5f;
    private static final float DIVIDER_THICKNESS = 1f;

    private DividerRenderer() {
    }

    /**
     * Draws the rule across {@code bounds}, one hairline thick and vertically centred, in
     * {@code ruleColour} faded by {@code opacity}. Must run with a current GL context, like any
     * immediate-mode GL call.
     *
     * @param dividerBounds the divider row's footprint, in UI coordinates
     * @param ruleColour    the rule colour, the body's frame colour
     * @param opacity       overall alpha, 0..1
     */
    public static void render(
            Rectangle dividerBounds,
            Color ruleColour,
            float opacity) {

        var dividerY = dividerBounds.computeCenterY() - DIVIDER_THICKNESS / 2f;
        var rect = new Rectangle(
            dividerBounds.x(),
            dividerY,
            dividerBounds.width(),
            DIVIDER_THICKNESS);
        var rulePaint = new UiElementPaint(
            ruleColour,
            opacity * DIVIDER_ALPHA_MULT);

        UiFill.renderQuad(rect, rulePaint);
    }
}
