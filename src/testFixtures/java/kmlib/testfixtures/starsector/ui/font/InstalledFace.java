package kmlib.testfixtures.starsector.ui.font;

import kmlib.starsector.ui.font.AtlasSmoothing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * What an install's descriptor for one face states: everything the font checks ask of a descriptor and
 * nothing else - no glyph geometry, no kerning, no texture.
 *
 * @param lineHeight        the size the atlas draws 1:1 at
 * @param pageCount         how many texture pages the descriptor declares
 * @param headerTokenCount  how many tokens LazyLib splits its header into
 * @param smoothing         whether the atlas wants its glyphs interpolated, by the {@code aa} it states
 * @param glyphIds          the glyph IDs it declares
 */
public record InstalledFace(
    double lineHeight,
    int pageCount,
    int headerTokenCount,
    AtlasSmoothing smoothing,
    GlyphIdRanges glyphIds) {

    /**
     * Reads the descriptor at {@code descriptorFile}, once, for every field.
     *
     * <p>Read byte for byte as Latin-1 rather than as UTF-8. A localised atlas names its face in the
     * localisation's own script, and a byte never splits a token here: no byte of a multi-byte UTF-8
     * sequence is an equals sign, a quote or whitespace, so the header's token count comes out as LazyLib's
     * does whatever encoding the file is in, and a file in some other encoding still reads. Every other
     * character read is ASCII.
     *
     * @param descriptorFile a {@code .fnt} file
     * @return what it states
     */
    public static InstalledFace readDescriptor(Path descriptorFile) {

        try {
            var descriptorLines = Files.readAllLines(descriptorFile, StandardCharsets.ISO_8859_1);
            var header = FontDescriptorHeader.readDescriptorHeader(descriptorLines);

            return new InstalledFace(
                header.lineHeight(),
                header.pageCount(),
                header.tokenCount(),
                AtlasSmoothing.resolveFromInfoLine(descriptorLines.get(0)),
                FontDescriptorGlyphs.readGlyphIds(descriptorLines));

        } catch (IOException ioException) {

            throw new UncheckedIOException(
                "Could not read " + descriptorFile,
                ioException);

        } catch (IllegalArgumentException illegalArgumentException) {
            // The parsers read lines, not files, so the file a malformed descriptor sits in is named here.
            throw new IllegalArgumentException(
                descriptorFile + ": " + illegalArgumentException.getMessage(),
                illegalArgumentException);
        }
    }
}
