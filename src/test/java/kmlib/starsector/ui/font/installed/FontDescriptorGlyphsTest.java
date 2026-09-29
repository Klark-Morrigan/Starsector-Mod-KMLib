package kmlib.starsector.ui.font.installed;

import kmlib.starsector.ui.font.installed.GlyphIdRanges.GlyphIdRange;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FontDescriptorGlyphsTest {

    // A descriptor's tail as an install ships it: the count line, then glyph lines with their fields padded
    // by runs of spaces, then a kerning line the reader must leave alone.
    private static final String DESCRIPTOR = """
        info face="InsigniaLT" size=15
        common lineHeight=15 base=12 scaleW=256 scaleH=256 pages=1
        page id=0 file="insignia15LTaa_0.png"
        chars count=4
        char id=32   x=254   y=1831   width=1     height=1     xoffset=0     yoffset=12    xadvance=4
        char id=33   x=165   y=1830   width=2     height=11    xoffset=1     yoffset=2     xadvance=4
        char id=34   x=160   y=1830   width=4     height=4     xoffset=0     yoffset=2     xadvance=5
        char id=19968 x=0    y=0      width=15    height=2     xoffset=0     yoffset=7     xadvance=15
        kernings count=1
        kerning first=32 second=33 amount=-1
        """;

    @Nested
    class ReadGlyphIds {

        @Test
        void readGlyphIdsReadsEveryGlyphLineAndNothingElse() {
            // The count line starts "chars", and a reader matching "char" alone would read its 4 as a
            // glyph; the kerning line names two IDs that are no glyphs of their own.
            assertThat(FontDescriptorGlyphs.readGlyphIds(DESCRIPTOR.lines().toList()).idRanges())
                .containsExactly(new GlyphIdRange(32, 34), new GlyphIdRange(19968, 19968));
        }
    }
}
