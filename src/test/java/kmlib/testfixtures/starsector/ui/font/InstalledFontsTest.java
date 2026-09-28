package kmlib.testfixtures.starsector.ui.font;

import kmlib.starsector.ui.font.AtlasSmoothing;
import kmlib.starsector.ui.font.DeclaredFontAtlas;
import kmlib.starsector.ui.font.StarsectorFont;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins that a reading of an install answers the ports as that install would.
 */
class InstalledFontsTest {

    // "Hegemony" (U+9738 U+4E3B), a faction name as a localised install draws it.
    private static final String LOCALISED_NAME = "霸主";

    // A face whose atlas holds a space, an ampersand, an H and the two characters of the name above.
    private static final InstalledFace LOCALISED_FACE = new InstalledFace(
        21d,
        1,
        51,
        AtlasSmoothing.SMOOTHED,
        GlyphIdRanges.createFromIds(IntStream.of(32, 38, 72, 20027, 38712)));

    // A language pack's own face, as a mod pointing the game's defaultFont at it would name it.
    private static final String PACK_FONT_PATH = "graphics/fonts/pack_script15.fnt";

    private static InstalledFonts createZongyiFonts() {
        return new InstalledFonts(
            "font-zongyi",
            Optional.of("2026.09.04"),
            StarsectorFont.VANILLA_INSIGNIA_15,
            Map.of(StarsectorFont.VANILLA_INSIGNIA_25, LOCALISED_FACE),
            Optional.empty());
    }

    // An install whose settings a language pack has pointed at a face it carries under starsector-core.
    private static InstalledFonts createPackDeclaredFonts() {
        return new InstalledFonts(
            "vanilla",
            Optional.empty(),
            new DeclaredFontAtlas(PACK_FONT_PATH, AtlasSmoothing.SMOOTHED),
            Map.of(),
            Optional.of(LOCALISED_FACE));
    }

    @Nested
    class CreateFaceResolver {

        @Test
        void createFaceResolverTriesTheInstallsDeclaredDefaultBeforeTheLastResort() {

            var zongyiWithAnotherDefault = new InstalledFonts(
                "font-zongyi",
                Optional.of("2026.09.04"),
                StarsectorFont.VANILLA_ORBITRON_20AA,
                Map.of(),
                Optional.empty());

            assertThat(zongyiWithAnotherDefault.createFaceResolver().listFallbackWalk(StarsectorFont.VANILLA_VICTOR_10))
                .containsExactly(
                    StarsectorFont.VANILLA_VICTOR_10,
                    StarsectorFont.VANILLA_ORBITRON_20AA,
                    StarsectorFont.VANILLA_INSIGNIA_15);
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

            var vanillaFonts = new InstalledFonts(
                "vanilla",
                Optional.empty(),
                StarsectorFont.VANILLA_INSIGNIA_15,
                Map.of(),
                Optional.empty());

            assertThat(vanillaFonts.describeEdition())
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

        @Test
        void createLineHeightReaderAnswersTheDeclaredDefaultsInstalledLineHeight() {

            assertThat(createPackDeclaredFonts().createLineHeightReader()
                    .readLineHeight(new DeclaredFontAtlas(PACK_FONT_PATH, AtlasSmoothing.SMOOTHED)))
                .isEqualTo(21d);
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

        @Test
        void createGlyphCoverageReaderReadsTheDeclaredDefaultsInstalledGlyphs() {

            assertThat(createPackDeclaredFonts().createGlyphCoverageReader()
                    .coversText(new DeclaredFontAtlas(PACK_FONT_PATH, AtlasSmoothing.SMOOTHED), LOCALISED_NAME))
                .isTrue();
        }
    }
}
