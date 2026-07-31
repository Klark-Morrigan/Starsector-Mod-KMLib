package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Stacks a column of fixed-height rows top to bottom from a shared left edge, each row as wide as
 * it needs and parted from the next by a uniform gap. Pure geometry in UI coordinates (origin
 * bottom-left): the first row hangs from {@code topY} and each later row drops one row height plus
 * one gap below it, so a column of controls or list items shares one left edge and an even rhythm
 * without the caller re-deriving each row's y. The generic half of a control body - where the rows
 * sit; what each row draws stays with the consumer.
 */
public final class RowStack {
    private RowStack() {
    }

    /**
     * Stacks one row per entry in {@code rowWidths} down from {@code topY}, each {@code rowHeight}
     * tall and left-aligned at {@code originX}, separated by {@code rowGap}.
     *
     * @param originX   the shared left edge of every row, in UI coordinates
     * @param topY      the top edge of the first row, in UI coordinates
     * @param rowHeight the height every row shares
     * @param rowGap    the gap between one row's bottom and the next row's top
     * @param rowWidths each row's width, in stack order top to bottom
     * @return one rectangle per width, in the same order
     */
    public static List<Rectangle> layoutRows(
            float originX,
            float topY,
            float rowHeight,
            float rowGap,
            List<Float> rowWidths) {

        var rowHeights = new ArrayList<Float>(rowWidths.size());
        for (var index = 0; index < rowWidths.size(); index++) {
            rowHeights.add(rowHeight);
        }
        return layoutRows(originX, topY, rowGap, rowHeights, rowWidths);
    }

    /**
     * Stacks one row per entry down from {@code topY}, each taking its own height from
     * {@code rowHeights} and its own width from {@code rowWidths}, left-aligned at {@code originX}
     * and separated by {@code rowGap}. The per-row height lets one control stand taller than the
     * rest - a stacked (vertical) radio occupies one row per option - without forcing the whole
     * column to a single height.
     *
     * @param originX    the shared left edge of every row, in UI coordinates
     * @param topY       the top edge of the first row, in UI coordinates
     * @param rowGap     the gap between one row's bottom and the next row's top
     * @param rowHeights each row's height, in stack order top to bottom
     * @param rowWidths  each row's width, in the same order (same size as {@code rowHeights})
     * @return one rectangle per row, in the same order
     */
    public static List<Rectangle> layoutRows(
            float originX,
            float topY,
            float rowGap,
            List<Float> rowHeights,
            List<Float> rowWidths) {
                
        var rows = new ArrayList<Rectangle>(rowWidths.size());
        var rowTop = topY;
        for (var index = 0; index < rowWidths.size(); index++) {
            var rowHeight = rowHeights.get(index);
            rows.add(new Rectangle(originX, rowTop - rowHeight, rowWidths.get(index), rowHeight));
            rowTop -= rowHeight + rowGap;
        }
        return List.copyOf(rows);
    }
}
