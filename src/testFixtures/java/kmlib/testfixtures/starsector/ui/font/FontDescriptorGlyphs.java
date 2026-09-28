package kmlib.testfixtures.starsector.ui.font;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * The glyph IDs a bitmap font descriptor ({@code .fnt}) declares, read off its {@code char} lines the way
 * LazyLib picks them out: a line starting {@code char } followed by a space, which leaves out the
 * {@code chars count=} line above them.
 *
 * <p>Read byte for byte as Latin-1, for the reason {@link FontDescriptorHeader} is: every character that
 * matters here is ASCII, and a localised face name elsewhere in the file must not fail the read.
 */
public final class FontDescriptorGlyphs {

    // What LazyLib starts a glyph line with, the trailing space parting it from the count line.
    private static final String GLYPH_LINE_PREFIX = "char ";

    // The ID field of a glyph line. A descriptor pads its fields with runs of spaces, so the field is
    // found rather than taken by position.
    private static final Pattern GLYPH_ID_FIELD = Pattern.compile("\\bid=(\\d+)");

    // Reads only; never instantiated.
    private FontDescriptorGlyphs() {
    }

    /**
     * Reads the glyph IDs the descriptor at {@code descriptorFile} declares.
     *
     * @param descriptorFile a {@code .fnt} file
     * @return its declared glyph IDs
     */
    public static GlyphIdRanges readGlyphIds(Path descriptorFile) {
        try (var lines = Files.lines(descriptorFile, StandardCharsets.ISO_8859_1)) {

            return GlyphIdRanges.createFromIds(lines
                .filter(line -> line.startsWith(GLYPH_LINE_PREFIX))
                .mapToInt(line -> readGlyphId(line, descriptorFile)));

        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read " + descriptorFile, exception);
        }
    }

    private static int readGlyphId(String glyphLine, Path descriptorFile) {

        var idMatch = GLYPH_ID_FIELD.matcher(glyphLine);
        if (!idMatch.find()) {
            throw new IllegalArgumentException(descriptorFile + " holds a glyph line with no id: " + glyphLine);
        }
        return Integer.parseInt(idMatch.group(1));
    }
}
