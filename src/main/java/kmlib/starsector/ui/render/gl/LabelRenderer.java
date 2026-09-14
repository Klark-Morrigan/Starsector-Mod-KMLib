package kmlib.starsector.ui.render.gl;

import kmlib.colour.Colours;
import kmlib.starsector.ui.font.DrawableStringCache;

import org.lazywizard.lazylib.ui.LazyFont;

/**
 * Raw-GL paint for one line of body text: it takes the cached glyph run for the line and draws it at
 * an anchor in a base colour faded by an opacity. The single place that whole-line draw lives, so
 * every KM UI surface that writes a plain line of text - a laid-out control, a free-floating map
 * tooltip - draws it the same way.
 *
 * <p>When the face cannot load the draw is skipped silently, so a caller's chrome still shows without
 * its text. Must run with a current GL context, like any immediate-mode GL call.
 */
public final class LabelRenderer {

    private LabelRenderer() {
    }

    /**
     * Draws {@code text} in {@code style} at {@code (x, y)}, positioned by {@code anchor}. Skipped
     * silently when the face cannot load.
     *
     * @param style  the face, colour, and opacity the line draws in
     * @param text   the line to draw
     * @param x      the anchor x, in UI coordinates
     * @param y      the anchor y, in UI coordinates (UI origin is bottom-left)
     * @param anchor where {@code (x, y)} sits relative to the text box
     */
    public static void render(
            LabelStyle style,
            String text,
            float x,
            float y,
            LazyFont.TextAnchor anchor) {

        var drawable = DrawableStringCache.resolveRun(style.face(), text);
        if (drawable == null) {
            return;
        }
        drawable.setAnchor(anchor);
        drawable.setBaseColor(Colours.scaleAlpha(style.colour(), style.opacity()));
        drawable.draw(x, y);
    }
}
