package kmlib.starsector.ui.font.installed;

import kmlib.starsector.ui.font.AtlasSmoothing;
import kmlib.starsector.ui.font.DeclaredFontAtlas;
import kmlib.starsector.ui.font.StarsectorFont;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what is read off an install: its edition, its faces, and the default its settings declare.
 */
class InstalledFontsReaderTest {

    // A localisation marker as an edition's install carries it, reduced to the fields read and one that
    // is not.
    private static final String ZONGYI_MARKER = """
        {
          "version": "2026.09.04",
          "branch": "font-zongyi",
          "language": "zh-Hans"
        }
        """;

    // A descriptor for a face whose atlas holds a space, an ampersand, an H and the two characters of
    // "Hegemony" (U+9738 U+4E3B).
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
        AtlasSmoothing.SMOOTHED,
        GlyphIdRanges.createFromIds(IntStream.of(32, 38, 72, 20027, 38712)));

    // A language pack's own face, as a mod pointing the game's defaultFont at it would name it.
    private static final String PACK_FONT_PATH = "graphics/fonts/pack_script15.fnt";

    @Nested
    class ReadInstall {

        @Test
        void readInstallReadsTheEditionAndPackVersionTheMarkerStates(@TempDir Path starsectorRoot)
                throws IOException {

            writeFile(starsectorRoot.resolve("starsector-core/localization_version.json"), ZONGYI_MARKER);

            var fonts = InstalledFontsReader.readInstall(starsectorRoot);

            assertThat(fonts.edition())
                .isEqualTo("font-zongyi");
            assertThat(fonts.packVersion())
                .contains("2026.09.04");
        }

        @Test
        void readInstallReadsAnInstallWithNoMarkerAsVanilla(@TempDir Path starsectorRoot) {

            var fonts = InstalledFontsReader.readInstall(starsectorRoot);

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

            assertThat(InstalledFontsReader.readInstall(starsectorRoot).faceByFont())
                .containsExactlyEntriesOf(Map.of(StarsectorFont.VANILLA_INSIGNIA_25, LOCALISED_FACE));
        }

        @Test
        void readInstallReadsTheDefaultFaceTheInstallsSettingsName(@TempDir Path starsectorRoot)
                throws IOException {
            // Settings as the game ships them: a comment, and the key among others.
            writeFile(
                starsectorRoot.resolve("starsector-core/data/config/settings.json"),
                """
                {
                    # the face vanilla text is set in
                    "defaultFont":"graphics/fonts/insignia25LTaa.fnt",
                    "otherKey":1,
                }
                """);

            assertThat(InstalledFontsReader.readInstall(starsectorRoot).defaultAtlas())
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }

        @Test
        void readInstallReadsAnInstallWithNoSettingsAsVanillasDefault(@TempDir Path starsectorRoot) {

            assertThat(InstalledFontsReader.readInstall(starsectorRoot).defaultAtlas())
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_15);
        }

        @Test
        void readInstallReadsADeclaredDefaultTheEnumDoesNotNameWithTheDescriptorTheInstallCarries(
                @TempDir Path starsectorRoot) throws IOException {
            // The pack's face is read by its own descriptor, smoothing included, so a walk over this reading
            // reaches it as the running game on the install would.
            writeFile(
                starsectorRoot.resolve("starsector-core/data/config/settings.json"),
                "{\"defaultFont\":\"" + PACK_FONT_PATH + "\"}");
            writeFile(
                starsectorRoot.resolve("starsector-core/" + PACK_FONT_PATH),
                LOCALISED_DESCRIPTOR.replace("aa=4", "aa=1"));

            var fonts = InstalledFontsReader.readInstall(starsectorRoot);

            assertThat(fonts.defaultAtlas())
                .isEqualTo(new DeclaredFontAtlas(PACK_FONT_PATH, AtlasSmoothing.PIXEL_EXACT));
            assertThat(fonts.declaredDefaultFace())
                .map(InstalledFace::lineHeight)
                .contains(21d);
        }

        @Test
        void readInstallReadsADeclaredDefaultTheInstallDoesNotCarryAsNoFace(@TempDir Path starsectorRoot)
                throws IOException {
            // A mod's own file lives in the mod's folder, out of this reading's reach: it reads as a face that
            // will not load, which a walk passes over.
            writeFile(
                starsectorRoot.resolve("starsector-core/data/config/settings.json"),
                "{\"defaultFont\":\"" + PACK_FONT_PATH + "\"}");

            var fonts = InstalledFontsReader.readInstall(starsectorRoot);

            assertThat(fonts.defaultAtlas())
                .isEqualTo(new DeclaredFontAtlas(PACK_FONT_PATH, AtlasSmoothing.SMOOTHED));
            assertThat(fonts.declaredDefaultFace())
                .isEmpty();
        }
    }

    private static void writeFile(Path file, String text) throws IOException {

        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }
}
