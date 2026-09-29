package kmlib.starsector.ui.font;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The glyph IDs a bitmap font descriptor ({@code .fnt}) declares, read off its {@code char} lines the way
 * LazyLib picks them out: a line starting {@code char } followed by a space, which leaves out the
 * {@code chars count=} line above them.
 */
final class FontDescriptorGlyphs {

    // What LazyLib starts a glyph line with, the trailing space parting it from the count line.
    private static final String GLYPH_LINE_PREFIX = "char ";

    // The ID field of a glyph line. A descriptor pads its fields with runs of spaces, so the field is
    // found rather than taken by position.
    private static final Pattern GLYPH_ID_FIELD = Pattern.compile("\\bid=(\\d+)");

    // Reads only; never instantiated.
    private FontDescriptorGlyphs() {
    }

    /**
     * Reads the glyph IDs a descriptor's lines declare.
     *
     * @param descriptorLines every line of a {@code .fnt} file
     * @return its declared glyph IDs
     */
    static GlyphIdRanges readGlyphIds(List<String> descriptorLines) {
        return GlyphIdRanges.createFromIds(descriptorLines.stream()
            .filter(line -> line.startsWith(GLYPH_LINE_PREFIX))
            .mapToInt(FontDescriptorGlyphs::readGlyphId));
    }

    private static int readGlyphId(String glyphLine) {

        var idMatch = GLYPH_ID_FIELD.matcher(glyphLine);
        if (!idMatch.find()) {
            throw new IllegalArgumentException("The descriptor holds a glyph line with no id: " + glyphLine);
        }
        return Integer.parseInt(idMatch.group(1));
    }
}
