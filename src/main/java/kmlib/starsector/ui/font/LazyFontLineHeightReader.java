package kmlib.starsector.ui.font;

/**
 * The LazyLib-backed {@link FaceLineHeightReader}: it resolves the face through {@link LazyFontCache}
 * and reads the line height LazyLib parsed from the installed descriptor. Stateless, so it is handed to
 * a style as a method reference rather than constructed, the way {@link LazyFontSpanMeasurer} is.
 *
 * <p>A face that will not load reads as no height rather than failing the style, which pairs with the
 * draw skipping that face's runs and the span measurer giving them no width: a line with no glyphs to
 * paint takes no room.
 */
public final class LazyFontLineHeightReader {

    // Reads only; never instantiated.
    private LazyFontLineHeightReader() {
    }

    /**
     * The line height of the atlas installed at {@code atlas}'s path, or
     * {@link FaceLineHeightReader#NO_LINE_HEIGHT} when that face cannot load.
     *
     * @param atlas the face whose installed atlas is read
     * @return the atlas's line height
     */
    public static double readLineHeight(FontAtlas atlas) {

        var loaded = LazyFontCache.loadByFace(atlas);
        if (loaded == null) {
            return FaceLineHeightReader.NO_LINE_HEIGHT;
        }
        // LazyLib names it the base height; it is the descriptor's lineHeight field by token position,
        // which is the number its own glyph placement scales every requested size against.
        return loaded.getBaseHeight();
    }
}
