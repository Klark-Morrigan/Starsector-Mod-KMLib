package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.graphics.StarsectorSprites;
import kmlib.starsector.ui.widgets.IconLabelRow;
import kmlib.starsector.ui.widgets.RadioRow;

import java.util.List;

/**
 * Raw-GL paint for a vertical radio list whose options each show an optional small leading image: it
 * draws the radio chrome (frame, the selected option's wash, the dividers) through
 * {@link RadioRowRenderer} and then each option's icon through {@link UiSprite}. The option labels
 * stay with the consumer, which draws its text at {@link IconLabelRow#computeLabelAnchorX} over the
 * same segments this paints - the split that keeps font ownership with the host, matching how
 * {@link RadioRowRenderer} leaves labels to its caller.
 *
 * <p>Each option is described by one icon path (nullable): the political-map picker passes a faction
 * crest path per row, or null for an option with no crest (every alliance, and a crestless faction),
 * and the widget loads the sprite itself through {@link StarsectorSprites} so a missing or unknown
 * texture routes to a skipped icon rather than aborting the whole list. The number of options is the
 * size of the icon-path list, so one entry - crest or null - stands for each row.
 *
 * <p>The list is always vertical (a stacked column of options); an icon list only makes sense as a
 * column, so the alignment is fixed rather than a parameter. Its options can wrap across more than one
 * column ({@code columnCount}), filling each column top to bottom before the next, so a long list
 * reads as a grid; one column is the ordinary single stack. GL passthrough exercised in-engine like
 * the other render helpers; the caller wraps it in the GL-state save the panel already holds.
 */
public final class IconRadioListRenderer {
    private IconRadioListRenderer() {
    }

    /**
     * Draws the vertical radio list's chrome and each option's icon, all faded by {@code opacity}. A
     * {@code selectedIndex} outside the list lights no option. Options with a null or unloadable icon
     * path draw their chrome and (via the consumer) their label without an icon. The options wrap
     * across {@code columnCount} columns, so each icon draws in its option's grid cell.
     *
     * @param bounds        the list's footprint, in UI coordinates
     * @param iconPaths     one icon texture path per option, in option order; a null entry is an
     *                      option with no icon. The list size is the option count
     * @param selectedIndex the lit option's index, or a value outside the list to light none
     * @param columnCount   how many columns the options wrap across (one is a single stack)
     * @param colors        the frame stroke and selected-wash palette
     * @param opacity       overall alpha, 0..1
     */
    public static void render(
            Rectangle bounds,
            List<String> iconPaths,
            int selectedIndex,
            int columnCount,
            RadioColors colors,
            float opacity) {

        var optionCount = iconPaths.size();
        RadioRowRenderer.renderVerticalGrid(
            bounds,
            optionCount,
            selectedIndex,
            columnCount,
            colors,
            opacity);

        var segments = RadioRow.splitIntoGrid(
            bounds,
            optionCount,
            columnCount);

        for (var index = 0; index < segments.size(); index++) {

            var iconPath = iconPaths.get(index);
            if (iconPath == null) {
                continue;
            }

            var sprite = StarsectorSprites.loadSprite(iconPath);
            if (sprite == null) {
                continue;
            }

            var iconBox = IconLabelRow.computeIconBox(segments.get(index));
            UiSprite.renderQuad(sprite, iconBox, opacity);
        }
    }
}
