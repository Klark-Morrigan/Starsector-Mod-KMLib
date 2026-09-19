package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Stacks a column of rows top to bottom from a shared left edge, each row as tall and as wide as it
 * was measured and parted from the next by a uniform gap. Pure geometry in UI coordinates (origin
 * bottom-left): the first row hangs from {@code topY} and each later row drops one row height plus
 * one gap below it, so a column of controls or list items shares one left edge and an even rhythm
 * without the caller re-deriving each row's y. The generic half of a control body - where the rows
 * sit; what each row draws stays with the consumer.
 *
 * <p>One entry point rather than one per way of stating the rows. A run whose rows all stand the
 * same height is a {@link RowDimensions#createUniform} run, not a second stacking rule, so there is
 * no pair of same-shaped signatures for a caller to pick the wrong one of.
 */
public final class RowStack {

    private RowStack() {
    }

    /**
     * Stacks one row per entry down from {@code topY}, each taking its own height and width from
     * {@code rowDimensions}, left-aligned at {@code originX} and separated by {@code rowGap}. The
     * per-row height lets one control stand taller than the rest - a stacked (vertical) radio
     * occupies one row per option - without forcing the whole column to a single height.
     *
     * @param originX       the shared left edge of every row, in UI coordinates
     * @param topY          the top edge of the first row, in UI coordinates
     * @param rowGap        the gap between one row's bottom and the next row's top
     * @param rowDimensions each row's height and width, in stack order top to bottom
     * @return one rectangle per row, in the same order
     */
    public static List<Rectangle> layoutRows(
            float originX,
            float topY,
            float rowGap,
            RowDimensions rowDimensions) {

        var rows = new ArrayList<Rectangle>(rowDimensions.countRows());
        var rowTop = topY;
        for (var index = 0; index < rowDimensions.countRows(); index++) {
            var rowHeight = rowDimensions.rowHeights().get(index);
            rows.add(new Rectangle(
                originX,
                rowTop - rowHeight,
                rowDimensions.rowWidths().get(index),
                rowHeight));
            rowTop -= rowHeight + rowGap;
        }
        return List.copyOf(rows);
    }
}
