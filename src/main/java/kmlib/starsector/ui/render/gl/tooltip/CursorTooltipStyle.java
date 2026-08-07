package kmlib.starsector.ui.render.gl.tooltip;

import kmlib.starsector.ui.widgets.tooltip.TooltipStyle;

import java.awt.Color;

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
 * @param typography   the look of each kind of line, from which every row's face, size, casing, and
 *                     default colour is resolved; a line's face size is also its line height and
 *                     crest side
 * @param opacity      overall alpha, 0..1, applied to the box, the crests, and the text alike
 * @param borderWidth  the box border thickness; 0 draws only the fill
 * @param fillColour   the box's backdrop colour
 * @param borderColour the box's border colour
 */
public record CursorTooltipStyle(
    TooltipStyle typography,
    float opacity,
    float borderWidth,
    Color fillColour,
    Color borderColour) {
}
