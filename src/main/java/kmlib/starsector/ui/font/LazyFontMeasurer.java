package kmlib.starsector.ui.font;

import org.lazywizard.lazylib.ui.LazyFont;

/**
 * A {@link LineWidthMeasurer} backed by a LazyLib {@link LazyFont}: the one place a
 * concrete bitmap font's own {@code calcWidth} is read behind the font-agnostic
 * measurement port, so text-layout code depending on the port never touches LazyLib.
 *
 * @param font the loaded face whose glyph metrics measure each line; must be non-null
 */
public record LazyFontMeasurer(LazyFont font) implements LineWidthMeasurer {

    @Override
    public double measureLineWidth(String line, double fontSize) {
        return font.calcWidth(line, (float) fontSize);
    }
}
