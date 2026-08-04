package kmlib.starsector.ui.widgets;

import java.util.ArrayList;
import java.util.List;

/**
 * A block of a tooltip: the lines that belong together, read as one thing. A box is a stack of these
 * rather than a stack of lines, which is what settles how far apart anything stands - lines inside a
 * block sit a plain line gap apart, and one block is parted from the next by the box's own break.
 *
 * <p>The parting is a fact about two blocks meeting, so it can be owned by neither of them. Spelled as a
 * flag on the line that starts a block, it makes a line's spacing depend on its neighbours and lets the
 * gap under a box's title differ from the gap between its blocks the moment the two are opened by lines
 * of different size. Grouping the lines instead says the same thing structurally: a caller states what
 * belongs together, and every parting in the box then follows from that one statement.
 *
 * <p>A section is never empty. An empty one would be a block a reader is told about and shown nothing
 * of - the parting either side of it would still be spent, leaving a gap in the stack with no lines in
 * it - so a caller with nothing to list contributes no section at all.
 *
 * @param rows the block's lines, top to bottom; never empty
 */
public record TooltipSection(
    List<TooltipRow> rows) {

    /**
     * Copies the block's lines and rejects an empty or null-bearing one at construction, where the
     * caller that composed the block is still on the stack - a section that reached a layout empty would
     * otherwise be found as a gap on screen with nothing to say which caller left it there.
     */
    public TooltipSection {
        rows = List.copyOf(rows);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("rows must carry at least one line");
        }
    }

    /**
     * Reads {@code sections} as the flat run of lines they are drawn in, top to bottom. For a renderer,
     * which paints against per-line anchors: how the lines were grouped is what the layout spent to
     * settle the spacing, and nothing past that has any use for the grouping itself.
     *
     * @param sections the blocks, top to bottom
     * @return every line of every block, in reading order
     */
    public static List<TooltipRow> readRowsInOrder(List<TooltipSection> sections) {
        var rows = new ArrayList<TooltipRow>();
        for (var section : sections) {
            rows.addAll(section.rows());
        }
        return rows;
    }
}
