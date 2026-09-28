package kmlib.testfixtures.starsector.ui.font;

import java.nio.file.Path;

/**
 * What an install's descriptor for one face states: everything the font checks ask of a descriptor and
 * nothing else - no glyph geometry, no kerning, no texture.
 *
 * @param lineHeight        the size the atlas draws 1:1 at
 * @param pageCount         how many texture pages the descriptor declares
 * @param headerTokenCount  how many tokens LazyLib splits its header into
 * @param glyphIds          the glyph IDs it declares
 */
public record InstalledFace(
    double lineHeight,
    int pageCount,
    int headerTokenCount,
    GlyphIdRanges glyphIds) {

    /**
     * Reads the descriptor at {@code descriptorFile}.
     *
     * @param descriptorFile a {@code .fnt} file
     * @return what it states
     */
    public static InstalledFace readDescriptor(Path descriptorFile) {

        var header = FontDescriptorHeader.readDescriptorHeader(descriptorFile);

        return new InstalledFace(
            header.lineHeight(),
            header.pageCount(),
            header.tokenCount(),
            FontDescriptorGlyphs.readGlyphIds(descriptorFile));
    }
}
