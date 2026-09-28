package kmlib.testfixtures.starsector.ui.font;

import kmlib.starsector.ui.font.StarsectorFont;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the reading of an install: what it reads off the install, and that the two readers answer the
 * ports as that install would.
 */
class InstalledFontsTest {

    // "Hegemony" (U+9738 U+4E3B), a faction name as a localised install draws it.
    private static final String LOCALISED_NAME = "霸主";

    // A localisation marker as an edition's install carries it, reduced to the fields read and one that
    // is not.
    private static final String ZONGYI_MARKER = """
        {
          "version": "2026.09.04",
          "branch": "font-zongyi",
          "language": "zh-Hans"
        }
        """;

    // A descriptor for a face whose atlas holds a space, an ampersand, an H and the two characters of the
    // name above.
    private static final String LOCALISED_DESCRIPTOR = """
        info face="Zongyi" size=-14 bold=0 italic=0 charset="" unicode=1 stretchH=100 smooth=0 aa=4 \
        padding=0,0,0,0 spacing=1,1 outline=0
        common lineHeight=21 base=12 scaleW=2048 scaleH=2048 pages=1 packed=0 alphaChnl=1 redChnl=0 \
        greenChnl=0 blueChnl=0
        page id=0 file="insignia25LTaa_0.png"
        chars count=5
        char id=32 x=0 y=0 width=1 height=1 xoffset=0 yoffset=0 xadvance=4 page=0 chnl=15
        char id=38 x=0 y=0 width=1 height=1 xoffset=0 yoffset=0 xadvance=4 page=0 chnl=15
        char id=72 x=0 y=0 width=1 height=1 xoffset=0 yoffset=0 xadvance=4 page=0 chnl=15
        char id=20027 x=0 y=0 width=1 height=1 xoffset=0 yoffset=0 xadvance=4 page=0 chnl=15
        char id=38712 x=0 y=0 width=1 height=1 xoffset=0 yoffset=0 xadvance=4 page=0 chnl=15
        """;

    private static final InstalledFace LOCALISED_FACE = new InstalledFace(
        21d,
        1,
        51,
        GlyphIdRanges.createFromIds(IntStream.of(32, 38, 72, 20027, 38712)));

    private static InstalledFonts createZongyiFonts() {
        return new InstalledFonts(
            "font-zongyi",
            Optional.of("2026.09.04"),
            Map.of(StarsectorFont.VANILLA_INSIGNIA_25, LOCALISED_FACE));
    }

    @Nested
    class ReadInstall {

        @Test
        void readInstallReadsTheEditionAndPackVersionTheMarkerStates(@TempDir Path starsectorRoot)
                throws IOException {

            writeFile(starsectorRoot.resolve("starsector-core/localization_version.json"), ZONGYI_MARKER);

            var fonts = InstalledFonts.readInstall(starsectorRoot);

            assertThat(fonts.edition())
                .isEqualTo("font-zongyi");
            assertThat(fonts.packVersion())
                .contains("2026.09.04");
        }

        @Test
        void readInstallReadsAnInstallWithNoMarkerAsVanilla(@TempDir Path starsectorRoot) {

            var fonts = InstalledFonts.readInstall(starsectorRoot);

            assertThat(fonts.edition())
                .isEqualTo("vanilla");
            assertThat(fonts.packVersion())
                .isEmpty();
        }

        @Test
        void readInstallReadsEachFaceTheInstallCarriesAndNoOther(@TempDir Path starsectorRoot)
                throws IOException {
            // A face the install lacks is left out rather than read as empty, which is how a missing atlas
            // is told from one declaring no glyphs.
            writeFile(
                starsectorRoot.resolve("starsector-core/graphics/fonts/insignia25LTaa.fnt"),
                LOCALISED_DESCRIPTOR);

            assertThat(InstalledFonts.readInstall(starsectorRoot).faceByFont())
                .containsExactlyEntriesOf(Map.of(StarsectorFont.VANILLA_INSIGNIA_25, LOCALISED_FACE));
        }
    }

    @Nested
    class DescribeEdition {

        @Test
        void describeEditionNamesTheEditionAndItsPackVersion() {

            assertThat(createZongyiFonts().describeEdition())
                .isEqualTo("font-zongyi 2026.09.04");
        }

        @Test
        void describeEditionNamesTheGamesOwnAtlasesByTheEditionAlone() {

            assertThat(new InstalledFonts("vanilla", Optional.empty(), Map.of()).describeEdition())
                .isEqualTo("vanilla");
        }
    }

    @Nested
    class CreateLineHeightReader {

        @Test
        void createLineHeightReaderAnswersTheInstalledLineHeight() {

            assertThat(createZongyiFonts().createLineHeightReader()
                    .readLineHeight(StarsectorFont.VANILLA_INSIGNIA_25))
                .isEqualTo(21d);
        }

        @Test
        void createLineHeightReaderAnswersZeroForAFaceTheInstallDoesNotCarry() {

            assertThat(createZongyiFonts().createLineHeightReader()
                    .readLineHeight(StarsectorFont.VANILLA_INSIGNIA_42))
                .isZero();
        }
    }

    @Nested
    class CreateGlyphCoverageReader {

        @Test
        void createGlyphCoverageReaderCoversTextEveryCharacterOfWhichTheAtlasDeclares() {

            assertThat(createZongyiFonts().createGlyphCoverageReader()
                    .coversText(StarsectorFont.VANILLA_INSIGNIA_25, "H&H " + LOCALISED_NAME))
                .isTrue();
        }

        @Test
        void createGlyphCoverageReaderIsFalseForACharacterTheAtlasLacks() {

            assertThat(createZongyiFonts().createGlyphCoverageReader()
                    .coversText(StarsectorFont.VANILLA_INSIGNIA_25, "Hegemony"))
                .isFalse();
        }

        @Test
        void createGlyphCoverageReaderIsFalseForAFaceTheInstallDoesNotCarry() {

            assertThat(createZongyiFonts().createGlyphCoverageReader()
                    .coversText(StarsectorFont.VANILLA_INSIGNIA_42, "H"))
                .isFalse();
        }
    }

    private static void writeFile(Path file, String text) throws IOException {

        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }
}
