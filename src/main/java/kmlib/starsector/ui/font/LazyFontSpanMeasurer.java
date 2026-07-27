package kmlib.starsector.ui.font;

/**
 * The LazyLib-backed {@link TextSpanMeasurer}: it resolves each face through {@link LazyFontCache} as it
 * is asked for and reads that face's own glyph metrics. Stateless, so it is handed to a layout as a
 * method reference rather than constructed - unlike {@link LazyFontMeasurer}, which holds the one face it
 * measures, a multi-face layout names its faces per span and so has nothing to hold.
 *
 * <p>A face that will not load measures as nothing rather than failing the layout, which pairs with the
 * draw skipping that same run: the line takes no width because no glyphs will be painted for it, so the
 * box stays sized to the text it can actually show instead of reserving room for text it cannot.
 */
public final class LazyFontSpanMeasurer {

    private LazyFontSpanMeasurer() {
    }

    /**
     * The rendered width of {@code span} in {@code face}, or zero when that face cannot load.
     *
     * @param face the atlas and size the span draws in
     * @param span the text to measure, already a single line
     * @return the span's rendered width
     */
    public static double measureSpanWidth(TextFace face, String span) {
        var font = LazyFontCache.loadByFace(face.font());
        if (font == null) {
            return 0d;
        }
        return font.calcWidth(
                span,
                (float) face.size());
    }
}
