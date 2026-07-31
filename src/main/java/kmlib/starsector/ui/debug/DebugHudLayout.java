package kmlib.starsector.ui.debug;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Places a corner's pushed entries into drawable lines: each entry becomes a small key line above a
 * larger body line, and the entries stack out from an anchor. Pure maths over the anchor and the
 * entry list - no GL, no statics - so where every line lands is verifiable without a live context,
 * which is the part of a debug overlay that is otherwise only checkable by eye.
 *
 * <p>The same stack serves two placements. Around the cursor, each quadrant fans out from the cursor
 * into its diagonal, so the reading sits beside the pointer it annotates. Pinned to the screen, each
 * quadrant grows in from its screen corner, an edge padding holding it clear of the edge. Both come
 * down to a block of lines stacking down from a top, so the quadrant only decides the anchor, the
 * alignment, and whether the block sits above or below that point.
 */
public final class DebugHudLayout {

    // The small pink key reads as a subtitle over the larger yellow body, so the two are told apart
    // at a glance without reading them.
    private static final Color KEY_COLOUR = new Color(255, 130, 200);
    private static final Color BODY_COLOUR = new Color(255, 240, 130);
    private static final float KEY_FONT_SIZE = 11f;
    private static final float BODY_FONT_SIZE = 15f;

    // The gap below a line to the next, and the extra gap between one entry and the next, so an
    // entry's key and body read as a pair and successive entries read as separate.
    private static final float LINE_GAP = 2f;
    private static final float ENTRY_GAP = 8f;

    // How far a cursor-relative block sits off the cursor point, so the reading clears the pointer
    // rather than sitting under it.
    private static final float CURSOR_GAP = 12f;

    private DebugHudLayout() {
    }

    /**
     * Lays a corner's entries out around the cursor, fanning into that corner's diagonal: a left
     * corner right-aligns to the cursor's left, a top corner stacks up off the cursor.
     *
     * @param quadrant the corner, which fixes the diagonal the block fans into
     * @param entries  the pushed entries, drawn in order out from the cursor
     * @param cursorX  the cursor x in UI coordinates
     * @param cursorY  the cursor y in UI coordinates
     * @return the lines to draw, positioned, aligned, coloured, and sized
     */
    public static List<DebugHudLine> layOutAroundCursor(
            DebugQuadrant quadrant,
            List<DebugHudEntry> entries,
            float cursorX,
            float cursorY) {

        // A left corner sits to the cursor's left, so its text is right-aligned against the cursor
        // and grows further left; a right corner grows right. The gap holds it off the pointer.
        var isRightAligned = quadrant.isLeftHalf();
        var anchorX = quadrant.isLeftHalf()
            ? cursorX - CURSOR_GAP
            : cursorX + CURSOR_GAP;

        // A top corner's block sits above the cursor (its bottom a gap up from it), a bottom corner's
        // below (its top a gap down).
        var blockTopY = quadrant.isTopHalf()
            ? cursorY + CURSOR_GAP + measureBlockHeight(entries.size())
            : cursorY - CURSOR_GAP;

        return placeStackingDown(entries, anchorX, blockTopY, isRightAligned);
    }

    /**
     * Lays a corner's entries out pinned to its screen corner, growing in from it, an edge padding
     * holding the block clear of the screen edges.
     *
     * @param quadrant     the screen corner to pin to
     * @param entries      the pushed entries, drawn in order in from the corner
     * @param screenWidth  the UI width
     * @param screenHeight the UI height
     * @param edgePadding  how far in from each screen edge the block sits
     * @return the lines to draw, positioned, aligned, coloured, and sized
     */
    public static List<DebugHudLine> layOutAtCorner(
            DebugQuadrant quadrant,
            List<DebugHudEntry> entries,
            float screenWidth,
            float screenHeight,
            float edgePadding) {

        // A left corner grows rightward in from the left edge, so its text is left-aligned there; a
        // right corner right-aligns in from the right edge.
        var isRightAligned = !quadrant.isLeftHalf();
        var anchorX = quadrant.isLeftHalf()
            ? edgePadding
            : screenWidth - edgePadding;

        // A top corner grows down from the top edge; a bottom corner pins its block's bottom to the
        // bottom edge and grows up.
        var blockTopY = quadrant.isTopHalf()
            ? screenHeight - edgePadding
            : edgePadding + measureBlockHeight(entries.size());

        return placeStackingDown(entries, anchorX, blockTopY, isRightAligned);
    }

    // Places entries as a block of lines stacking downward from blockTopY: each entry a small key
    // line above a larger body line, in push order. The two placements differ only in the anchor,
    // alignment, and top they hand in, so all of that is decided before here.
    private static List<DebugHudLine> placeStackingDown(
            List<DebugHudEntry> entries,
            float anchorX,
            float blockTopY,
            boolean isRightAligned) {
                
        var lines = new ArrayList<DebugHudLine>();
        var y = blockTopY;
        for (var entry : entries) {

            lines.add(new DebugHudLine(
                entry.key(),
                anchorX,
                y,
                isRightAligned,
                KEY_COLOUR,
                KEY_FONT_SIZE));

            y -= KEY_FONT_SIZE + LINE_GAP;

            lines.add(new DebugHudLine(
                entry.body(),
                anchorX,
                y,
                isRightAligned,
                BODY_COLOUR,
                BODY_FONT_SIZE));

            y -= BODY_FONT_SIZE + LINE_GAP + ENTRY_GAP;
        }
        return lines;
    }

    // The total height a run of entries occupies, so a block that grows upward can pin its bottom to
    // an edge or the cursor. Each entry is a key line and a body line with their gaps; the trailing
    // entry gap is left in, which only shifts the block by one small gap and keeps the maths one line.
    private static float measureBlockHeight(int entryCount) {
        return entryCount
            * (KEY_FONT_SIZE + LINE_GAP + BODY_FONT_SIZE + LINE_GAP + ENTRY_GAP);
    }
}
