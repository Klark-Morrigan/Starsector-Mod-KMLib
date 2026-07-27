package kmlib.starsector.ui.font;

/**
 * The LazyLib-backed {@link TextSpanMeasurer}: it resolves each face through {@link LazyFontCache} as it
 * is asked for, then measures the span on it through {@link LazyFontMeasurer}. Resolving the face is all
 * it adds - which face a span belongs to is the only thing a multi-face layout knows that a single-face
 * one does not. Stateless, so it is handed to a layout as a method reference rather than constructed:
 * where the single-face measurer holds the one face it measures, this one is told a face per span.
 *
 * <p>A face that will not load measures as nothing rather than failing the layout, which pairs with the
 * draw skipping that same run: the line takes no width because no glyphs will be painted for it, so the
 * box stays sized to the text it can actually show instead of reserving room for text it cannot.
 */
public final class LazyFontSpanMeasurer {

    // What a face that will not load measures: nothing, since no glyphs of it will be painted either.
    private static final double NO_WIDTH = 0d;

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
            return NO_WIDTH;
        }
        // Handed to the single-face measurer rather than reading the loaded face's own metrics again:
        // resolving which face to measure is this class's whole contribution, and measuring one already
        // stays the one place it was.
        return new LazyFontMeasurer(font).measureLineWidth(span, face.size());
    }
}
