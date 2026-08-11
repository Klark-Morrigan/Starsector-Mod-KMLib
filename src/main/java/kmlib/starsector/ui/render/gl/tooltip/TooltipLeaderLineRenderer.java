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
 * Paints the rule a tooltip row leads from its label across to its value: a hairline in the engine's own
 * grey, set on the optical line of the words either side of it and faded by the box's opacity. The visual
 * aid a wide row needs - the value column is anchored to the box's right edge whatever the label
 * measures, so a reader tracking a number back to its name has the rule to follow rather than empty space.
 *
 * <p>Its own pass rather than a step inside the row draw, because it is the one part of a row that is
 * neither glyphs nor a slot's content: it is chrome set <em>between</em> two columns, and it lines itself
 * up on a font metric no other part of the draw reads.
 *
 * <p>The colour and the weight are its own rather than the host style's. A leader is not content and
 * takes no part in what a box says: it is the quietest mark on the line by definition, so it draws in the
 * engine's grey at a hairline whatever colours the box states for its text and its frame - and a box that
 * could name a colour for it could name one that competes with the words it runs between.
 */
public final class TooltipLeaderLineRenderer {

    // A hairline: the thinnest mark that still reads as a continuous line at every UI scale. Any heavier
    // and the rule starts reading as a divider parting the label from the value, which is the opposite of
    // what a leader is for - it ties the two together.
    private static final float LEADER_LINE_THICKNESS = 1f;

    // Half the thickness - what the rule is dropped by to sit centred on the line it was given, rather
    // than hanging below it.
    private static final float HALF_THICKNESS = LEADER_LINE_THICKNESS / 2f;

    private TooltipLeaderLineRenderer() {
    }

    /**
     * Rules {@code leaderLine} across a row whose line starts at {@code rowTopY} and is drawn in
     * {@code face}, faded by {@code opacity}. A row that leads no rule, and a face that will not load,
     * both draw nothing - the row's words are unaffected either way, so a missing aid costs the box
     * nothing it says. Must run with a current GL context, like any immediate-mode GL call.
     *
     * @param leaderLine the stretch between label and value the rule runs along
     * @param rowTopY    the row's top edge, in UI coordinates (UI origin is bottom-left)
     * @param face       the face the row's words draw in, whose lower-case band the rule is set on
     * @param opacity    overall alpha, 0..1, the same fade the box and its text draw at
     */
    public static void render(
            TooltipLeaderLine leaderLine,
            float rowTopY,
            TextFace face,
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

        UiFill.renderQuad(
            new Rectangle(
                leaderLine.leftX(),
                bandCentreY - HALF_THICKNESS,
                leaderLine.computeWidth(),
                LEADER_LINE_THICKNESS),
            new UiElementPaint(StarsectorUiColour.VANILLA_GRAY.resolve(), opacity));
    }
}
