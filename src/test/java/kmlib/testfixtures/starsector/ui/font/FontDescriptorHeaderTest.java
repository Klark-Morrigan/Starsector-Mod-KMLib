package kmlib.testfixtures.starsector.ui.font;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FontDescriptorHeaderTest {

    // The header of vanilla's insignia15LTaa.fnt, as the install ships it, followed by one glyph line
    // the reader must leave alone.
    private static final String VANILLA_HEADER = """
        info face="InsigniaLT" size=15 bold=0 italic=0 charset="" unicode=1 stretchH=100 \
        smooth=1 aa=4 padding=0,0,0,0 spacing=1,1 outline=0
        common lineHeight=15 base=12 scaleW=256 scaleH=256 pages=1 packed=0 alphaChnl=1 redChnl=0 \
        greenChnl=0 blueChnl=0
        page id=0 file="insignia15LTaa_0.png"
        chars count=233
        """;

    // A localised edition's face name: two words and a space inside the quotes, which the split has to
    // keep as one token. "SimSong" (U+5B8B U+4F53).
    private static final String LOCALISED_FACE_NAME = "宋体 Regular";

    @Nested
    class ReadDescriptorHeader {

        @Test
        void readDescriptorHeaderCountsTheTokensAsLazyLibSplitsThem(@TempDir Path directory) throws IOException {

            var header = FontDescriptorHeader.readDescriptorHeader(writeDescriptor(directory, VANILLA_HEADER));

            assertThat(header.tokenCount())
                .isEqualTo(51);
        }

        @Test
        void readDescriptorHeaderReadsTheLineHeightAndThePageCount(@TempDir Path directory) throws IOException {

            var header = FontDescriptorHeader.readDescriptorHeader(writeDescriptor(directory, VANILLA_HEADER));

            assertThat(header.lineHeight())
                .isEqualTo(15d);
            assertThat(header.pageCount())
                .isEqualTo(1);
        }

        @Test
        void readDescriptorHeaderKeepsAQuotedFaceNameWholeWhateverScriptItIsIn(@TempDir Path directory)
                throws IOException {
            // A localised atlas names its face in its own script, written as UTF-8: the count has to come
            // out as LazyLib's does, or a face it loads would be reported as one it refuses.
            var localisedHeader = VANILLA_HEADER.replace("InsigniaLT", LOCALISED_FACE_NAME);

            var header = FontDescriptorHeader.readDescriptorHeader(writeDescriptor(directory, localisedHeader));

            assertThat(header.tokenCount())
                .isEqualTo(51);
        }

        @Test
        void readDescriptorHeaderCountsAnUnquotedSpaceAsTheExtraTokenLazyLibRefuses(@TempDir Path directory)
                throws IOException {
            // The brittleness the count exists to catch: a face name that loses its quotes splits in two.
            var brokenHeader = VANILLA_HEADER.replace("\"InsigniaLT\"", "Insignia LT");

            var header = FontDescriptorHeader.readDescriptorHeader(writeDescriptor(directory, brokenHeader));

            assertThat(header.tokenCount())
                .isEqualTo(52);
        }

        @Test
        void readDescriptorHeaderRefusesAFileShorterThanTheHeader(@TempDir Path directory) throws IOException {

            var descriptorFile = writeDescriptor(directory, "info face=\"x\"\n");

            assertThatThrownBy(() -> FontDescriptorHeader.readDescriptorHeader(descriptorFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("three-line header");
        }
    }

    private static Path writeDescriptor(Path directory, String content) throws IOException {

        var descriptorFile = directory.resolve("face.fnt");

        Files.writeString(descriptorFile, content, StandardCharsets.UTF_8);

        return descriptorFile;
    }
}
