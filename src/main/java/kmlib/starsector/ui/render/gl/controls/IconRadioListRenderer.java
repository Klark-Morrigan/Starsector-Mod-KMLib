package kmlib.starsector.ui.render.gl.controls;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.render.gl.UiSprite;
import kmlib.starsector.ui.widgets.IconLabelRow;
import kmlib.starsector.ui.widgets.RadioRow;
import kmlib.starsector.ui.widgets.RowSlot;

import java.util.List;

/**
 * Raw-GL paint for a vertical radio list whose options each show an optional small leading image: it
 * draws the radio chrome (frame, the selected option's wash, the dividers) through
 * {@link RadioRowRenderer} and then each option's icon through {@link UiSprite}. The option labels
 * stay with the consumer, which draws its text at {@link IconLabelRow#computeLabelAnchorX} over the
 * same segments this paints - the split that keeps font ownership with the host, matching how
 * {@link RadioRowRenderer} leaves labels to its caller.
 *
 * <p>Each option is described by the {@link RowSlot} its row leads with: a slot holding an image draws
 * that image in whatever tint the slot states, and every other kind of slot - a row leading with
 * nothing, or with something this widget does not paint - simply leaves the leading column clear. The
 * image is drawn from its path through {@link UiSprite#renderImage}, so a missing or unknown texture
 * routes to a skipped image rather than aborting the whole list. The number of options is the size of
 * the slot list, so one slot stands for each row.
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
     * Draws the vertical radio list's chrome and each option's leading image, all faded by {@code
     * opacity}. A {@code selectedIndex} outside the list lights no option. An option whose slot holds no
     * image, or one whose image will not load, draws its chrome and (via the consumer) its label with the
     * leading column clear. The options wrap across {@code columnCount} columns, so each image draws in
     * its option's grid cell.
     *
     * @param bounds           the list's footprint, in UI coordinates
     * @param leadingRowSlots  what each option leads with, in option order; the list size is the option
     *                         count
     * @param selectedIndex    the lit option's index, or a value outside the list to light none
     * @param columnCount      how many columns the options wrap across (one is a single stack)
     * @param colours          the frame stroke and selected-wash palette
     * @param cellPaints       the wash and press light each option takes
     * @param opacity          overall alpha, 0..1
     */
    public static void render(
            Rectangle bounds,
            List<RowSlot> leadingRowSlots,
            int selectedIndex,
            int columnCount,
            RadioColours colours,
            CellPaintSources cellPaints,
            float opacity) {

        var optionCount = leadingRowSlots.size();

        // Straight through to the grid, both cell treatments included: an icon list is a vertical radio with
        // a leading image column, so what the pointer lights and what a press lifts is the radio's own cell
        // and not a second thing this widget would have to place.
        RadioRowRenderer.renderVerticalGrid(
            bounds,
            optionCount,
            selectedIndex,
            columnCount,
            colours,
            cellPaints,
            opacity);

        var segments = RadioRow.splitIntoGrid(
            bounds,
            optionCount,
            columnCount);

        for (var index = 0; index < segments.size(); index++) {

            // Only an image slot paints here; a row leading with nothing, or with a slot this widget has
            // no paint for, leaves the column clear rather than the widget guessing at a stand-in.
            if (!(leadingRowSlots.get(index) instanceof RowSlot.Image image)) {
                continue;
            }

            // Multiplied by whatever tint the slot states, so a row the caller marked as receding
            // draws its icon back with its words rather than at full strength beside greyed text. A
            // slot stating none passes null, which UiSprite already draws as authored.
            UiSprite.renderImage(
                image.spritePath(),
                IconLabelRow.computeIconBox(segments.get(index)),
                opacity,
                image.tintColour());
        }
    }
}
