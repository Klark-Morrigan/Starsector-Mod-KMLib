package kmlib.starsector.ui.render.gl.tooltip;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.colour.StarsectorUiColour;
import kmlib.starsector.ui.font.LazyFontCache;
import kmlib.starsector.ui.font.LazyFontMeasurer;
import kmlib.starsector.ui.font.TextFace;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;
import kmlib.starsector.ui.widgets.tooltip.TooltipLeaderLine;

/**
 * Paints the rule a tooltip row leads from its label across to its value: a hairline set on the optical
 * line of the words either side of it, in the engine's own grey let down to the weight that grey reads at
 * as text, and faded by the box's opacity on top of that. The visual aid a wide row needs - the value
 * column is anchored to the box's right edge whatever the label measures, so a reader tracking a number
 * back to its name has the rule to follow rather than empty space.
 *
 * <p>Its own pass rather than a step inside the row draw, because it is the one part of a row that is
 * neither glyphs nor a slot's content: it is chrome set <em>between</em> two columns, and it lines itself
 * up on a font metric no other part of the draw reads.
 *
 * <p>The colour is its own rather than the host style's, where the weight is not. A leader takes no part
 * in what a box says, so it draws in the engine's grey whatever colours the box states for its text and
 * its frame - a box that could name a colour for it could name one that competes with the words it runs
 * between. How heavily that grey lands is a different question, and one the code cannot settle: it turns
 * on the face, the size, and how the atlas was rasterised, so the host states it as a
 * {@link TooltipLeaderLineStyle} and the paint asks no opinion of its own.
 */
public final class TooltipLeaderLineRenderer {

    // What the rule is dropped by to sit centred on the line it was given, rather than hanging below it:
    // half of whatever thickness the host asked for.
    private static final float HALVED = 2f;

    private TooltipLeaderLineRenderer() {
    }

    /**
     * Rules {@code leaderLine} across a row whose line starts at {@code rowTopY} and is drawn in
     * {@code face}, at the weights {@code leaderLineStyle} states and faded by {@code opacity} on top of
     * them. A row that leads no rule, a rule the style has weighted down to nothing, and a face that will
     * not load all draw nothing - the row's words are unaffected in every case, so a missing aid costs
     * the box nothing it says. Must run with a current GL context, like any immediate-mode GL call.
     *
     * @param leaderLine      the stretch between label and value the rule runs along
     * @param rowTopY         the row's top edge, in UI coordinates (UI origin is bottom-left)
     * @param face            the face the row's words draw in, whose lower-case band the rule is set on
     * @param leaderLineStyle how thick the rule draws and how far it is let down from the box's opacity
     * @param opacity         overall alpha, 0..1, the same fade the box and its text draw at
     */
    public static void render(
            TooltipLeaderLine leaderLine,
            float rowTopY,
            TextFace face,
            TooltipLeaderLineStyle leaderLineStyle,
            float opacity) {

        if (!leaderLine.isRuled()) {
            return;
        }
        // Without the face there is no band to sit on, and a rule placed off a guessed one would cut
        // through the very words it is meant to run between.
        var font = LazyFontCache.loadByFace(face.font());
        if (font == null) {
            return;
        }
        var bandCentreY = rowTopY
            - (float) new LazyFontMeasurer(font).measureLowercaseBandCentreDrop(face.size());

        var thickness = leaderLineStyle.thickness();

        UiFill.renderQuad(
            new Rectangle(
                leaderLine.leftX(),
                bandCentreY - thickness / HALVED,
                leaderLine.computeWidth(),
                thickness),
            new UiElementPaint(
                StarsectorUiColour.VANILLA_GRAY.resolve(),
                opacity * leaderLineStyle.alphaMult()));
    }
}
