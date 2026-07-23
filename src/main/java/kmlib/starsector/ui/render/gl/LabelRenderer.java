package kmlib.starsector.ui.render.gl;

import kmlib.color.Colors;
import kmlib.starsector.ui.font.LazyFontCache;

import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Raw-GL paint for one line of body text: it resolves the face by basename, mints the glyph run
 * once, and draws it at an anchor in a base colour faded by an opacity. The single place the
 * cached-{@link DrawableString} draw pattern lives, so every KM UI surface that writes text -
 * a laid-out control, a free-floating map tooltip - draws it the same way and shares the cache
 * rather than each minting its own GL buffer per frame.
 *
 * <p>A steady body is a handful of distinct strings, so one GL text buffer per distinct
 * (font, size, text) serves the whole run; the base colour is re-applied before each draw, so
 * one buffer serves every frame at any colour or opacity. When the face cannot load the draw is
 * skipped silently, so a caller's chrome still shows without its text. Must run with a current GL
 * context, like any immediate-mode GL call.
 */
public final class LabelRenderer {
    // Cached across the run: one GL text buffer per distinct (font, size, text) rather than a fresh
    // buffer per frame. Keyed on all three because a differing size or text is a different glyph run;
    // colour is not in the key because it is re-set before each draw.
    private static final Map<String, DrawableString> TEXT_CACHE = new HashMap<>();

    private LabelRenderer() {
    }

    /**
     * Draws {@code text} in {@code font} at {@code (x, y)}, positioned by {@code anchor}, in
     * {@code baseColor} scaled by {@code opacity}. Skipped silently when the face cannot load.
     *
     * @param font      the body font's {@code graphics/fonts} basename
     * @param text      the line to draw
     * @param x         the anchor x, in UI coordinates
     * @param y         the anchor y, in UI coordinates (UI origin is bottom-left)
     * @param anchor    where {@code (x, y)} sits relative to the text box
     * @param baseColor the text colour before the opacity fade
     * @param opacity   overall alpha, 0..1
     * @param fontSize  the glyph size to render at
     */
    public static void render(
            String font,
            String text,
            float x,
            float y,
            LazyFont.TextAnchor anchor,
            Color baseColor,
            float opacity,
            double fontSize) {
        var drawable = resolveText(font, text, fontSize);
        if (drawable == null) {
            return;
        }
        drawable.setAnchor(anchor);
        drawable.setBaseColor(Colors.scaleAlpha(baseColor, opacity));
        drawable.draw(x, y);
    }

    // Mints the glyph run once per (font, size, text) and reuses it; the base colour is re-set before
    // each draw, so one buffer serves every frame. Null when the face cannot load, in which case the
    // caller draws without that text.
    private static DrawableString resolveText(String font, String text, double fontSize) {
        var key = font + "|" + fontSize + "|" + text;
        var cached = TEXT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        var face = LazyFontCache.loadByBasename(font);
        if (face == null) {
            return null;
        }
        var drawable = face.createText(text, Color.WHITE, (float) fontSize);
        TEXT_CACHE.put(key, drawable);
        return drawable;
    }
}
