package kmlib.starsector.ui.render.gl;

import kmlib.starsector.ui.font.TextFace;

import java.awt.Color;

/**
 * The look a {@link CursorTooltipRenderer} paints a tooltip in: the body face every row draws in - its
 * size doubling as each row's line height - the opacity the whole box and its contents fade by, and the
 * box's border and fill. The widget-level settings that do not vary row to row, gathered so a caller
 * states its face once rather than threading it through every row and draw call - the per-row text,
 * colour, crest, and value live on the {@link kmlib.starsector.ui.widgets.TooltipRow}s instead.
 *
 * @param face        the body face - basename and size - resolved once for every row; the size is also
 *                    each row's line height and crest side
 * @param opacity     overall alpha, 0..1, applied to the box, the crests, and the text alike
 * @param borderWidth the box border thickness; 0 draws only the fill
 * @param fillColor   the box's backdrop colour
 * @param borderColor the box's border colour
 */
public record CursorTooltipStyle(
        TextFace face,
        float opacity,
        float borderWidth,
        Color fillColor,
        Color borderColor) {
}
