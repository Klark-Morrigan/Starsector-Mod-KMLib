package kmlib.starsector.ui.font;

import org.lazywizard.lazylib.ui.LazyFont;

/**
 * The LazyLib-backed {@link GlyphCoverageReader}: it resolves the face through {@link LazyFontCache} and
 * asks it for each character's glyph. Stateless, so it is handed over as a method reference, the way
 * {@link LazyFontLineHeightReader} is.
 *
 * <p>LazyLib publishes no "does the atlas hold this" query. It answers a character it lacks with its
 * fallback glyph - the question mark, or the space where the atlas has no question mark either - so a
 * character is uncovered when the glyph it gets back is that fallback. The fallback is read off the face
 * by asking for the question mark itself: that call answers the question mark's own glyph when the atlas
 * holds one and the fallback when it does not, and those are the same glyph either way. Comparing the
 * answer's ID with the character asked for instead would miss LazyLib's own substitutions - it draws a
 * typographic quote as the straight quote - and call a drawable character uncovered.
 *
 * <p>LazyLib logs a warning the first time a face is asked for a character it lacks, once per character,
 * so a probe that finds a gap leaves the same line a draw of that character would.
 */
public final class LazyFontGlyphCoverageReader {

    // The character whose lookup yields the face's fallback glyph: its own glyph where the atlas holds
    // one, which is then the fallback, and the fallback itself where it does not.
    private static final char FALLBACK_PROBE_CHARACTER = '?';

    // Reads only; never instantiated.
    private LazyFontGlyphCoverageReader() {
    }

    /**
     * Whether {@code font}'s installed atlas draws every character of {@code text} as itself, or false
     * when that face cannot load.
     *
     * @param font the face whose installed atlas is read
     * @param text the text the face would draw
     * @return whether no character of the text would fall back
     */
    public static boolean coversText(StarsectorFont font, String text) {

        var loaded = LazyFontCache.loadByFace(font);
        if (loaded == null) {
            return false;
        }
        var fallbackId = loaded.getChar(FALLBACK_PROBE_CHARACTER).getId();

        return text
            .codePoints()
            .filter(codePoint -> !Character.isWhitespace(codePoint))
            .allMatch(codePoint -> isCodePointCovered(loaded, codePoint, fallbackId));
    }

    // Whether one character draws as itself. A character outside the basic plane cannot be asked at
    // all - LazyLib looks glyphs up by a single UTF-16 unit - so it is uncovered by definition. A glyph
    // answering with the character's own ID is covered even where that ID is the fallback's, which is
    // what keeps a present question mark from reading as its own absence.
    private static boolean isCodePointCovered(LazyFont font, int codePoint, int fallbackId) {

        if (Character.isSupplementaryCodePoint(codePoint)) {
            return false;
        }
        var glyphId = font
            .getChar((char) codePoint)
            .getId();

        return glyphId == codePoint || glyphId != fallbackId;
    }
}
