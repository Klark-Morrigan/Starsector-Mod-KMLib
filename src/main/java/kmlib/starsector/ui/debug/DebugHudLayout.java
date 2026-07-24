package kmlib.starsector.ui.debug;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Places a corner's pushed entries into drawable lines: each entry becomes a small key line above a
 * larger body line, and the entries stack in push order out from the corner. Pure maths over the
 * screen size and the entry list - no GL, no statics - so where every line lands is verifiable
 * without a live context, which is the part of a debug overlay that is otherwise only checkable by
 * eye.
 *
 * <p>The corner decides three things: left halves right-align their text to a column just left of
 * centre (right halves left-align just right of it), top halves grow the stack downward from the
 * top edge, and bottom halves anchor the whole block to the bottom edge and grow it upward - so a
 * bottom corner's last-pushed entry sits nearest the edge.
 */
public final class DebugHudLayout {

    // The small pink key reads as a subtitle over the larger yellow body, so the two are told apart
    // at a glance without reading them.
    private static final Color KEY_COLOUR = new Color(255, 130, 200);
    private static final Color BODY_COLOUR = new Color(255, 240, 130);
    private static final float KEY_FONT_SIZE = 11f;
    private static final float BODY_FONT_SIZE = 15f;

    // The inset from the screen edge the stack starts at, and from the centreline the columns align
    // to, so neither the outer edge nor the centre crowds the text.
    private static final float EDGE_MARGIN = 24f;
    private static final float CENTRE_GAP = 24f;

    // The gap below a line to the next, and the extra gap between one entry and the next, so an
    // entry's key and body read as a pair and successive entries read as separate.
    private static final float LINE_GAP = 2f;
    private static final float ENTRY_GAP = 8f;

    private DebugHudLayout() {
    }

    /**
     * Lays a corner's entries out into drawable lines, top to bottom in push order.
     *
     * @param quadrant     the corner to lay out, which fixes the alignment and stack direction
     * @param entries      the pushed entries, drawn in order
     * @param screenWidth  the UI width, for the centre column and right-half offset
     * @param screenHeight the UI height, for the top-edge start of a top corner
     * @return the lines to draw, each already positioned, anchored, coloured, and sized
     */
    public static List<DebugHudLine> layOut(
            DebugQuadrant quadrant,
            List<DebugHudEntry> entries,
            float screenWidth,
            float screenHeight) {
        var lines = new ArrayList<DebugHudLine>();
        var isRightAligned = quadrant.isLeftHalf();
        var anchorX = isRightAligned
                ? screenWidth / 2f - CENTRE_GAP
                : screenWidth / 2f + CENTRE_GAP;
        // A top corner starts its block at the top edge; a bottom corner pins the block's bottom to
        // the bottom edge, so its top is a whole block-height up from there.
        var y = quadrant.isTopHalf()
                ? screenHeight - EDGE_MARGIN
                : EDGE_MARGIN + measureBlockHeight(entries.size());
        for (var entry : entries) {
            lines.add(new DebugHudLine(
                    entry.key(), anchorX, y, isRightAligned, KEY_COLOUR, KEY_FONT_SIZE));
            y -= KEY_FONT_SIZE + LINE_GAP;
            lines.add(new DebugHudLine(
                    entry.body(), anchorX, y, isRightAligned, BODY_COLOUR, BODY_FONT_SIZE));
            y -= BODY_FONT_SIZE + LINE_GAP + ENTRY_GAP;
        }
        return lines;
    }

    // The total height a run of entries occupies, so a bottom corner can pin the block's bottom to
    // the edge. Each entry is a key line and a body line with their gaps; the trailing entry gap is
    // left in, which only shifts the whole block up by one small gap and keeps the maths one line.
    private static float measureBlockHeight(int entryCount) {
        return entryCount
                * (KEY_FONT_SIZE + LINE_GAP + BODY_FONT_SIZE + LINE_GAP + ENTRY_GAP);
    }
}
