package kmlib.starsector.ui.widgets.tooltip;

import kmlib.starsector.ui.text.TextStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * A box's {@link TooltipSection blocks} flattened into the run of lines they draw as, each bound to the
 * look its kind of line resolves to and to the room it takes above itself. The whole of what the
 * grouping is spent on: how far apart two lines stand is settled here, and nothing past this point has
 * any use for which block a line came from.
 *
 * <p>Its own type because it is what two questions are both answered from - how tall a box comes to, and
 * where each of its rows lands - and those are asked at different moments by callers holding different
 * things. A caller weighing content against the room it has holds no cursor and no way to measure text,
 * yet needs the same stacking a layout will later place; taking the run apart from the placement is what
 * lets it ask. Both answers coming off one walk is what stops a box weighed as fitting from being drawn
 * at some other height.
 *
 * <p>Nothing here measures a width or names a surface. A line's height is its face's size, and every
 * parting is the typography's answer for the tier of the line above it, so the whole stack resolves from
 * the blocks and the {@link TooltipStyle} alone.
 */
final class TooltipRowStack {

    // What the box's very first row takes above itself: nothing, since the box's own padding already
    // sits there. A break spent there would pad the top edge unevenly against every other side.
    private static final float NO_LEADING_GAP = 0f;

    // Where a block's opening row sits within it. Named because the position is what makes a row the
    // one parted from the block above, which a bare zero in the walk below would not say.
    private static final int FIRST_ROW_OF_SECTION = 0;

    // What stands above a block's first nested block: nothing, since a block's own lines are its voice
    // rather than a sibling of the groups beneath them, so the first group hugs the lines that introduce
    // it. Named rather than passed as a bare null, so the walk below reads as "no group closed here"
    // instead of as an unexplained absence.
    private static final TooltipSection NO_PRECEDING_MEMBER = null;

    // The most a nested block can come to and still read as one item of a list. Past it the block broke
    // down into an account of its own, which is what the next member is set clear of.
    private static final int ONE_LINE = 1;

    // Where the row a line gap is resolved from sits in the run stacked so far: the last of them, since
    // the gap belongs to the tier of the line just laid. Named because that position is what makes it
    // the row being read, which a bare one subtracted from a size would not say.
    private static final int LAST_STACKED_ROW = 1;

    private final TooltipStyle style;
    private final List<StackedRow> stackedRows = new ArrayList<>();

    // Filled by the walk the factory below runs and read afterwards. Private because a half-walked stack
    // is not a stack of anything - a gap resolved against rows that were never stacked would be spacing
    // one box's lines by another box's structure.
    private TooltipRowStack(TooltipStyle style) {
        this.style = style;
    }

    /**
     * Flattens {@code sections} into the run of lines they draw as, in reading order, resolving each
     * line's look and the room it takes above itself under {@code style}. Blocks are parted by the
     * style's section break, the blocks nested inside one by its narrower group break, and two lines of
     * one block by whatever gap that style holds their tier at.
     *
     * @param sections the content blocks, top to bottom; an empty list yields an empty stack
     * @param style    the look each kind of line draws in and how far apart the blocks stand
     * @return the stacked run those blocks come to
     */
    static TooltipRowStack stackRows(List<TooltipSection> sections, TooltipStyle style) {

        var rowStack = new TooltipRowStack(style);
        for (var section : sections) {

            // The box's very first row is not asked what parts it from what came before, since nothing
            // did: the box's own padding already sits above it, and a break spent there would pad the
            // top edge unevenly against every other side.
            var leadingGap = rowStack.stackedRows.isEmpty()
                ? NO_LEADING_GAP
                : style.spacing().sectionBreak();

            rowStack.stackSectionRows(section, leadingGap);
        }
        return rowStack;
    }

    /**
     * How tall this run stands: each line's own height plus the room it takes above itself. The content
     * height before a box wraps its padding around it.
     *
     * @return the height the lines stack to, in UI units
     */
    double measureContentHeight() {

        var height = 0d;
        for (var stackedRow : stackedRows) {
            height += stackedRow.measureLineHeight() + stackedRow.leadingGap();
        }
        return height;
    }

    /**
     * @return the stacked lines in reading order, each carrying its look and the room above it
     */
    List<StackedRow> readStackedRows() {
        return List.copyOf(stackedRows);
    }

    // Stacks one block and everything nested in it, in draw order: its own lines, then each member block
    // beneath them. Recursive because the grouping is - a block nests blocks to whatever depth its
    // subject matter has - and one walk is what keeps a group three deep spaced by the same rule as one
    // at the top of the box.
    //
    // Only the block's first line is handed the parting; the rest of its lines continue what it opened,
    // and every member works out its own from what it follows.
    private void stackSectionRows(TooltipSection section, float leadingGap) {

        var openingRows = section.openingRows();

        for (var index = 0; index < openingRows.size(); index++) {

            var row = openingRows.get(index);
            stackedRows.add(new StackedRow(
                row,
                style.resolveStyleFor(row.lineStyle(), row.subordinationLevel()),
                index == FIRST_ROW_OF_SECTION ? leadingGap : resolveGapAfterLastStackedRow()));
        }
        // Carried forward rather than read back out of the rows by index, since the rule is about what
        // just ended: the block that closed above a member is the whole of what decides its gap.
        var precedingMember = NO_PRECEDING_MEMBER;

        for (var member : section.members()) {

            stackSectionRows(member, measureMemberGap(precedingMember));
            precedingMember = member;
        }
    }

    // What a nested block takes above itself. The rule that turns nesting into spacing, and the one place
    // it is decided, so a parting cannot pile up where several groups end on the same line: it is spent
    // by the member that follows, never above the first, and only where the member before it broke down
    // into more than a line - a run of one-line members reads as the plain list it is.
    //
    // Where no parting is due, the member opens at the plain gap under the line above it, which is where
    // nearly every gap in a listing comes from: a caller that gives each entry a block of its own has no
    // two lines sharing one block for the gap to fall between.
    private float measureMemberGap(TooltipSection precedingMember) {

        if (precedingMember == NO_PRECEDING_MEMBER
                || precedingMember.countLines() <= ONE_LINE) {
                    
            return resolveGapAfterLastStackedRow();
        }
        return style.spacing().groupBreak();
    }

    // The room a line takes above itself where no block boundary parts it from what came before: what the
    // box's typography spends after the row just stacked, resolved from that row's own tier.
    //
    // Read off the rows stacked so far rather than handed in, because the row a gap follows is the last
    // of them either way - whether the gap falls between two lines of one block or above a member opening
    // beneath its parent's lines - and both callers would otherwise have to work out which row that is.
    // There is always one: a TooltipSection carries at least one line, so a row is stacked before any gap
    // inside or beneath that block is asked for.
    private float resolveGapAfterLastStackedRow() {
        var lastStackedRow = stackedRows.get(stackedRows.size() - LAST_STACKED_ROW).row();
        return style.resolveLineGapAfter(lastStackedRow.subordinationLevel());
    }

    /**
     * One line of the run: the row as its caller authored it, the look its kind of line resolved to at
     * the depth it stands, and the room it takes above itself.
     *
     * <p>The look is resolved once, here, rather than by each reader of the stack. Resolved twice, a row
     * could be measured at one size and painted at another, which on a demoted line overlaps its own
     * words - and the second reader would have to be handed the typography to get it wrong with.
     *
     * @param row        the content row as its caller authored it
     * @param textStyle  the face, size, and casing this row draws in
     * @param leadingGap the room taken above the row before its own line, in UI units
     */
    record StackedRow(TooltipRow row, TextStyle textStyle, float leadingGap) {

        /**
         * @return the height this row stacks at, in UI units - its face's own size, that being the room
         *         one line of a bitmap face needs
         */
        double measureLineHeight() {
            return textStyle.face().size();
        }
    }
}
