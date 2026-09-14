package kmlib.starsector.ui.font;

/**
 * Measures how wide one line of text renders at a given font size, in the font's own
 * units - the one fact a text-layout routine (line wrapping, block balancing) needs
 * from a glyph font, without depending on the concrete font itself.
 *
 * <p>A font-agnostic port: layout code depends on this narrow measurement rather than a
 * heavyweight rendering font, so it stays independent of the font implementation it
 * reads its metrics from. {@link LazyFontMeasurer} is the LazyLib-backed adapter; any
 * other source of widths implements the same one method.
 */
@FunctionalInterface
public interface LineWidthMeasurer {

    /**
     * The rendered width of {@code line} at {@code fontSize}, in the same units the font
     * measures in. Assumed linear in {@code fontSize} (as bitmap-glyph advances are), so
     * one measurement at a reference size scales to any line height.
     *
     * @param line     the text to measure, already a single line
     * @param fontSize the font size to measure at
     * @return the line's rendered width
     */
    double measureLineWidth(String line, double fontSize);
}
