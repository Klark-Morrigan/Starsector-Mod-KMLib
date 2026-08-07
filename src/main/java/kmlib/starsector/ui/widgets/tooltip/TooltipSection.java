package kmlib.starsector.ui.widgets.tooltip;

import java.util.ArrayList;
import java.util.List;

/**
 * A block of a tooltip: the lines that belong together, read as one thing, and any blocks nested
 * inside it. A box is a stack of these rather than a stack of lines, which is what settles how far
 * apart anything stands - lines inside a block sit a plain line gap apart, and one block is parted from
 * the next by the box's own break.
 *
 * <p>The parting is a fact about two blocks meeting, so it can be owned by neither of them. Spelled as a
 * flag on the line that starts a block, it makes a line's spacing depend on its neighbours and lets the
 * gap under a box's title differ from the gap between its blocks the moment the two are opened by lines
 * of different size. Grouping the lines instead says the same thing structurally: a caller states what
 * belongs together, and every parting in the box then follows from that one statement.
 *
 * <p>Blocks nest because a listing does. A colony and the factors it breaks down into are one thing to
 * set apart from the next colony, and the factions holding a system are each one thing to set apart from
 * the next faction - so the same grouping that settles the spacing at the top of a box settles it at
 * every depth, without a second vocabulary for "a group within a block". A nested block is parted from
 * the one before it by the box's {@linkplain TooltipStyle#groupBreak group break} rather than by its
 * section break, since a run inside a block should not stand as far off as two blocks do.
 *
 * <p>Two facts keep that parting from piling up where several groups end at once. It is spent between
 * two <em>members</em> only, never above the first one, so a block's own lines always hug the group they
 * open; and it is spent by the member that follows, so exactly one gap lands wherever any number of
 * groups close together. What decides it is whether the member before spanned more than one line - a run
 * of one-line members reads as a plain list, and a member that broke down into an account of its own is
 * set off from what follows it.
 *
 * <p>A block's own lines open it and its members follow, which is what the two fields say: a caller with
 * lines to interleave between its members states each such line as a member of one line rather than
 * hoping the order survives. A section is never empty of lines for the same reason it was never empty
 * before - the parting either side of it would still be spent, leaving a gap in the stack with no lines
 * in it.
 *
 * @param openingRows the block's own lines, top to bottom, drawn above every member; never empty
 * @param members     the blocks nested inside this one, in reading order; empty for a plain block
 */
public record TooltipSection(
    List<TooltipRow> openingRows,
    List<TooltipSection> members) {

    // What a block nests until a caller puts something inside it. Named so the factory reads as "this
    // block holds only its own lines" rather than as an unexplained empty list.
    private static final List<TooltipSection> NO_MEMBERS = List.of();

    /**
     * Copies the block's lines and its members and rejects an empty or null-bearing one at construction,
     * where the caller that composed the block is still on the stack - a section that reached a layout
     * empty would otherwise be found as a gap on screen with nothing to say which caller left it there.
     */
    public TooltipSection {
        openingRows = List.copyOf(openingRows);
        members = List.copyOf(members);
        if (openingRows.isEmpty()) {
            throw new IllegalArgumentException("openingRows must carry at least one line");
        }
    }

    /**
     * Builds a plain block: its own lines and nothing nested inside it, which is what most blocks are.
     * A block that groups something states it with {@link #nesting}, so a caller writes what its block
     * <em>has</em> rather than passing an empty list for the part it does not use.
     *
     * @param openingRows the block's lines, top to bottom; never empty
     * @return the block
     */
    public static TooltipSection createSection(List<TooltipRow> openingRows) {
        return new TooltipSection(openingRows, NO_MEMBERS);
    }

    /**
     * Reads {@code sections} as the flat run of lines they are drawn in, top to bottom - each block's own
     * lines, then everything nested inside it, before the next block. For a renderer, which paints
     * against per-line anchors: how the lines were grouped is what the layout spent to settle the
     * spacing, and nothing past that has any use for the grouping itself.
     *
     * @param sections the blocks, top to bottom
     * @return every line of every block, in reading order
     */
    public static List<TooltipRow> readRowsInOrder(List<TooltipSection> sections) {
        var rows = new ArrayList<TooltipRow>();
        for (var section : sections) {
            section.appendRowsInOrder(rows);
        }
        return rows;
    }

    /**
     * Reads this block as the flat run of lines it is drawn in, top to bottom - its own lines, then
     * everything nested inside it. What the whole-box read above folds over, offered on its own for a
     * caller holding one block rather than a stack.
     *
     * @return every line of this block, in reading order
     */
    public List<TooltipRow> readRowsInOrder() {
        var rows = new ArrayList<TooltipRow>();
        appendRowsInOrder(rows);
        return rows;
    }

    /**
     * How many lines this block draws as, its members included. What the parting rule reads: a member
     * that came to one line is an item of a list, and one that came to several broke down into an account
     * the next member should stand clear of.
     *
     * @return the total line count of this block and everything nested in it
     */
    public int countLines() {
        var lineCount = openingRows.size();
        for (var member : members) {
            lineCount += member.countLines();
        }
        return lineCount;
    }

    /**
     * Returns a copy of this block holding {@code members} nested inside it, drawn beneath its own lines
     * in the order given.
     *
     * @param members the blocks nested inside this one, in reading order
     * @return an otherwise-identical block holding those members
     */
    public TooltipSection nesting(List<TooltipSection> members) {
        return new TooltipSection(openingRows, members);
    }

    // Adds this block's lines and its members' to a run being built, depth first - the order they are
    // drawn in, which is also the order the layout walked them in to space them.
    private void appendRowsInOrder(List<TooltipRow> rows) {
        rows.addAll(openingRows);
        for (var member : members) {
            member.appendRowsInOrder(rows);
        }
    }
}
