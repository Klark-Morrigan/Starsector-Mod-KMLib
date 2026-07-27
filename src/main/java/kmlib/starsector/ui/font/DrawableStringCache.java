package kmlib.starsector.ui.font;

import org.lazywizard.lazylib.ui.LazyFont.DrawableString;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Mints a face's glyph run for one string and caches it, so a line of text costs one GL text buffer
 * for the whole run rather than a fresh buffer every frame it is drawn. The companion to
 * {@link LazyFontCache}: that one caches the faces, this one the runs minted from them.
 *
 * <p>One cache for every caller, so two surfaces drawing the same string in the same face share the
 * buffer instead of each minting its own. The base colour is deliberately not part of the identity
 * of a run - a caller re-sets it (and its opacity) on the returned drawable before each draw, which
 * is what lets one buffer serve every frame at any colour.
 *
 * <p>Must be called with a current GL context on a cache miss, like any immediate-mode GL call.
 */
public final class DrawableStringCache {

    // The colour a freshly minted run is baked with. Arbitrary: every caller re-sets the base colour
    // before drawing, so this is never the colour anything appears in - it only has to be non-null.
    private static final Color MINTING_COLOUR = Color.WHITE;

    private static final Map<TextRun, DrawableString> RUN_BY_TEXT = new HashMap<>();

    // Caches only; never instantiated.
    private DrawableStringCache() {
    }

    /**
     * The cached glyph run for {@code text} in {@code face}, minted on first ask, or null when the
     * face cannot load - in which case the caller draws its chrome without that text.
     *
     * @param face the atlas and size the run is minted at
     * @param text the line the run draws
     * @return the cached drawable, whose base colour the caller sets before each draw, or null when
     *         the face will not load
     */
    public static DrawableString resolveRun(TextFace face, String text) {
        var run = new TextRun(face, text);
        var cached = RUN_BY_TEXT.get(run);
        if (cached != null) {
            return cached;
        }
        var font = LazyFontCache.loadByFace(face.font());
        if (font == null) {
            return null;
        }
        var drawable = font.createText(text, MINTING_COLOUR, (float) face.size());
        RUN_BY_TEXT.put(run, drawable);
        return drawable;
    }

    // What makes two runs the same buffer: the same text drawn in the same face at the same size. A
    // record rather than a composed string key, so adding to the identity is a compile-time change
    // rather than a silently divergent bit of concatenation at each call site.
    private record TextRun(TextFace face, String text) {
    }
}
