package kmlib.starsector.ui.widgets;

import java.awt.Color;

/**
 * One line of a {@link CursorTooltip}: an indent for its tier, an optional leading crest, a coloured
 * label, and an optional right-aligned coloured value. The content model a caller fills to say what a
 * tooltip shows, without saying how it is measured or drawn - the geometry ({@link CursorTooltip})
 * reads the indent, the crest presence, and the two texts to size the box, and the renderer reads the
 * colours and the crest path to paint it.
 *
 * <p>Header and member rows differ only in these values - a header sits at zero indent in a bright
 * colour, a nested member indents in a plainer one - so a stack of rows carries a two-tier hierarchy
 * as flat data and the draw path never branches on tier. A row with no crest leaves the path null and
 * still reserves the icon column, so its label stays aligned with the crested rows around it; a row
 * with no value passes an empty string, whose zero width collapses the value column for that line.
 *
 * @param indent          the label's inset from the box's left content edge, in UI units - zero for a
 *                        top-tier row, a positive step for a nested one
 * @param crestSpritePath the leading crest's {@code graphics} texture path, or null for no crest
 * @param text            the row's label
 * @param textColor       the label's colour before the tooltip's opacity fade
 * @param value           the right-aligned value, or the empty string for a row with none
 * @param valueColor      the value's colour before the opacity fade
 */
public record TooltipRow(
        float indent,
        String crestSpritePath,
        String text,
        Color textColor,
        String value,
        Color valueColor) {
}
