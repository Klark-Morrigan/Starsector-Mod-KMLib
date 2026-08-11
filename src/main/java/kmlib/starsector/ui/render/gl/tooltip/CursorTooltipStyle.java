package kmlib.starsector.ui.render.gl.tooltip;

import kmlib.starsector.ui.widgets.tooltip.TooltipStyle;

import java.awt.Color;
import java.util.Objects;

/**
 * The look a {@link CursorTooltipRenderer} paints a tooltip in: the typography each kind of its lines
 * draws in, the opacity the whole box and its contents fade by, and the box's border and fill. The
 * widget-level settings that do not vary row to row, gathered so a caller states its look once rather
 * than threading it through every row and draw call - the per-row text, colour, crest, and value live on
 * the {@link kmlib.starsector.ui.widgets.TooltipRow}s instead.
 *
 * <p>The typography is held whole rather than flattened into a face and a size, because it is the one
 * part of the look that is not GL's business: the same value describes a tooltip drawn with the game's
 * own widgets. What is left here is the chrome that only a raw-GL box has to decide.
 *
 * <p>What a box always decides is stated at {@link #createStyle}, and what it may decide is layered on
 * with a refinement, so a caller writes what its box differs in rather than spelling out the parts it
 * has no opinion about. The leader rules are the first of those: most boxes want them at the weights
 * they read as text at, and a host asked for that pair up front would be restating a finding it did not
 * make. The same reason {@link TooltipStyle} takes two faces and layers the rest on.
 *
 * @param typography      the look of each kind of line, from which every row's face, size, casing, and
 *                        default colour is resolved; a line's face size is also its line height and
 *                        crest side
 * @param opacity         overall alpha, 0..1, applied to the box, the crests, and the text alike
 * @param borderWidth     the box border thickness; 0 draws only the fill
 * @param fillColour      the box's backdrop colour
 * @param borderColour    the box's border colour
 * @param leaderLineStyle how heavily the rules led between a row's label and its value draw;
 *                        {@link TooltipLeaderLineStyle#TEXT_WEIGHTED} unless the host tunes them
 */
public record CursorTooltipStyle(
    TooltipStyle typography,
    float opacity,
    float borderWidth,
    Color fillColour,
    Color borderColour,
    TooltipLeaderLineStyle leaderLineStyle) {

    /**
     * Rejects a null leader look at construction: a box that rules no leaders states
     * {@link TooltipLeaderLineStyle#TEXT_WEIGHTED} at no thickness rather than leaving the look
     * unstated, so nothing below has to read an absent look and a switched-off one as the same thing.
     */
    public CursorTooltipStyle {
        Objects.requireNonNull(leaderLineStyle, "leaderLineStyle");
    }

    /**
     * Builds the look a box always has to state: its typography, its fade, and its frame - with the
     * rules between label and value led at {@link TooltipLeaderLineStyle#TEXT_WEIGHTED}, the weights
     * they read as greyed-out text at. A host that tunes them layers its own on with {@link #ruledBy}.
     *
     * <p>Defaulted rather than asked for, because that pair is a finding about how a solid run reads
     * beside glyphs rather than a taste a box holds: every caller made to name it would be restating a
     * measurement someone else made, and the callers that never thought about leaders at all would be
     * the ones most likely to restate it wrongly.
     *
     * @param typography   the look of each kind of line
     * @param opacity      overall alpha, 0..1, applied to the box, the crests, and the text alike
     * @param borderWidth  the box border thickness; 0 draws only the fill
     * @param fillColour   the box's backdrop colour
     * @param borderColour the box's border colour
     * @return the look, ruling its leaders at the standard weights
     */
    public static CursorTooltipStyle createStyle(
            TooltipStyle typography,
            float opacity,
            float borderWidth,
            Color fillColour,
            Color borderColour) {

        return new CursorTooltipStyle(
            typography,
            opacity,
            borderWidth,
            fillColour,
            borderColour,
            TooltipLeaderLineStyle.TEXT_WEIGHTED);
    }

    /**
     * Returns a copy of this look ruling its leaders at {@code leaderLineStyle} - for a host that has
     * measured the balance on its own faces, or that puts it in the player's hands.
     *
     * @param leaderLineStyle how thick each rule draws and how far it is let down from the box's opacity
     * @return an otherwise-identical look ruling its leaders at those weights
     */
    public CursorTooltipStyle ruledBy(TooltipLeaderLineStyle leaderLineStyle) {
        return new CursorTooltipStyle(
            typography,
            opacity,
            borderWidth,
            fillColour,
            borderColour,
            leaderLineStyle);
    }
}
