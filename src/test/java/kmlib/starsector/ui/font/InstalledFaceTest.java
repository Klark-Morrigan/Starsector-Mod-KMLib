package kmlib.starsector.ui.font;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstalledFaceTest {

    // A localised pixel face as an install ships it, written as UTF-8: its face name is in the
    // localisation's own script - "SimSong" (U+5B8B U+4F53) - and its info line states a single sample.
    private static final String LOCALISED_PIXEL_DESCRIPTOR = """
        info face="宋体 Regular" size=-10 bold=0 italic=0 charset="" unicode=1 stretchH=100 smooth=1 aa=1 \
        padding=0,0,0,0 spacing=1,1 outline=0
        common lineHeight=10 base=8 scaleW=256 scaleH=256 pages=1 packed=0 alphaChnl=1 redChnl=0 \
        greenChnl=0 blueChnl=0
        page id=0 file="victor10_0.png"
        chars count=2
        char id=65 x=0 y=0 width=1 height=1 xoffset=0 yoffset=0 xadvance=4 page=0 chnl=15
        char id=19968 x=0 y=0 width=1 height=1 xoffset=0 yoffset=0 xadvance=4 page=0 chnl=15
        """;

    @Nested
    class ReadDescriptor {

        @Test
        void readDescriptorReadsEveryFieldOffOneFileWhateverScriptItsFaceIsNamedIn(@TempDir Path directory)
                throws IOException {
            // The face name's UTF-8 bytes, read as Latin-1, split no token, so the count comes out as
            // LazyLib's does.
            var descriptorFile = writeDescriptor(directory, LOCALISED_PIXEL_DESCRIPTOR);

            assertThat(InstalledFace.readDescriptor(descriptorFile))
                .isEqualTo(new InstalledFace(
                    10d,
                    1,
                    51,
                    AtlasSmoothing.PIXEL_EXACT,
                    GlyphIdRanges.createFromIds(IntStream.of(65, 19968))));
        }

        @Test
        void readDescriptorNamesTheFileAMalformedDescriptorSitsIn(@TempDir Path directory) throws IOException {

            var descriptorFile = writeDescriptor(directory, "info face=\"x\"\n");

            assertThatThrownBy(() -> InstalledFace.readDescriptor(descriptorFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith(descriptorFile.toString())
                .hasMessageContaining("three-line header");
        }
    }

    private static Path writeDescriptor(Path directory, String content) throws IOException {

        var descriptorFile = directory.resolve("face.fnt");

        Files.writeString(descriptorFile, content, StandardCharsets.UTF_8);

        return descriptorFile;
    }
}
