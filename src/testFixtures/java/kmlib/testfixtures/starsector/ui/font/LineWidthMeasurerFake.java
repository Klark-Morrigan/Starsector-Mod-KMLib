package kmlib.testfixtures.starsector.ui.font;

import kmlib.starsector.ui.font.LineWidthMeasurer;

/**
 * A {@link LineWidthMeasurer} that reports a width proportional to the character count, so
 * text-snapped layout can be exercised without a loaded bitmap font. Shipped from KMLib so both
 * KMLib's and consuming mods' layout tests drive the measurement seam through one shared double
 * with predictable, hand-checkable widths.
 *
 * <p>The width is {@code line length * widthPerCharacter}, independent of the font size: a fixed
 * per-character metric is all a deterministic layout test needs, and holding it constant keeps
 * expected widths a simple multiplication.
 */
public final class LineWidthMeasurerFake implements LineWidthMeasurer {
    private final double widthPerCharacter;

    /**
     * @param widthPerCharacter the width each character contributes to a line
     */
    public LineWidthMeasurerFake(double widthPerCharacter) {
        this.widthPerCharacter = widthPerCharacter;
    }

    @Override
    public double measureLineWidth(String line, double fontSize) {
        return line.length() * widthPerCharacter;
    }
}
