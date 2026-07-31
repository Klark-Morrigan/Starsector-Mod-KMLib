package kmlib.starsector.ui.debug;

import java.awt.Color;

/**
 * One laid-out line of debug text: where it draws, how it aligns, and in what colour and size. The
 * pure output of {@link DebugHudLayout}, so the placement maths is decided and testable before any
 * GL call - {@link DebugHud} only forwards each of these to the text renderer, mapping the
 * alignment to a concrete anchor there so this stays free of the render toolkit's types.
 *
 * @param text           the line to draw
 * @param x              the anchor x in UI coordinates
 * @param y              the anchor y in UI coordinates (UI origin is bottom-left; this is the line's
 *                       top)
 * @param isRightAligned whether {@code x} is the line's right edge (a left-half corner) rather than
 *                       its left (a right-half corner)
 * @param colour         the line's colour
 * @param fontSize       the glyph size to draw at
 */
public record DebugHudLine(
    String text,
    float x,
    float y,
    boolean isRightAligned,
    Color colour,
    double fontSize) {
}
