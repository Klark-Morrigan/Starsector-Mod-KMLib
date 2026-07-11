package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.graphics.StarsectorSprites;
import kmlib.starsector.ui.controls.RadioAlignment;
import kmlib.starsector.ui.widgets.IconLabelRow;
import kmlib.starsector.ui.widgets.RadioRow;

import java.awt.Color;
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
 * column, so the alignment is fixed rather than a parameter. GL passthrough exercised in-engine like
 * the other render helpers; the caller wraps it in the GL-state save the panel already holds.
 */
public final class IconRadioListRenderer {
    private IconRadioListRenderer() {
    }

    /**
     * Draws the vertical radio list's chrome and each option's icon, all faded by {@code opacity}. A
     * {@code selectedIndex} outside the list lights no option. Options with a null or unloadable icon
     * path draw their chrome and (via the consumer) their label without an icon.
     *
     * @param bounds        the list's footprint, in UI coordinates
     * @param iconPaths     one icon texture path per option, in top-to-bottom order; a null entry is
     *                      an option with no icon. The list size is the option count
     * @param selectedIndex the lit option's index, or a value outside the list to light none
     * @param frameColor    the outline and divider colour
     * @param selectedColor the lit-option wash colour
     * @param opacity       overall alpha, 0..1
     */
    public static void render(Rectangle bounds, List<String> iconPaths, int selectedIndex,
            Color frameColor, Color selectedColor, float opacity) {
        var optionCount = iconPaths.size();
        RadioRowRenderer.render(bounds, optionCount, selectedIndex, RadioAlignment.VERTICAL,
                frameColor, selectedColor, opacity);
        var segments = RadioRow.splitIntoSegments(bounds, optionCount, RadioAlignment.VERTICAL);
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
            UiSprite.renderQuad(sprite, iconBox.x(), iconBox.y(), iconBox.width(), iconBox.height(),
                    opacity);
        }
    }
}
