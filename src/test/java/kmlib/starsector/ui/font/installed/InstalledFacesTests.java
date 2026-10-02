package kmlib.starsector.ui.font.installed;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import kmlib.starsector.ui.font.FaceLineHeightReader;
import kmlib.starsector.ui.font.FaceResolver;
import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.font.TextFace;
import kmlib.testfixtures.starsector.ui.font.FaceLineHeightReaderFake;
import kmlib.testfixtures.starsector.ui.font.installed.LazyFontLineHeightReaderMock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class InstalledFacesTests {

    @AfterEach
    void clearSettings() {
        Global.setSettings(null);
    }

    @Nested
    class CreateNativeFace {

        @Test
        void drawsTheFaceAtTheLineHeightTheLiveInstallStates() {
            // The live reader stood in for a taller atlas than vanilla's, so the size can only have come
            // from the read.
            try (var lineHeightsMock = LazyFontLineHeightReaderMock.install(FaceLineHeightReaderFake
                    .createVanillaLineHeights()
                    .answeringLineHeight(StarsectorFont.VANILLA_VICTOR_10, 11d))) {

                assertThat(InstalledFaces.createNativeFace(StarsectorFont.VANILLA_VICTOR_10))
                    .isEqualTo(new TextFace(StarsectorFont.VANILLA_VICTOR_10, 11d));
            }
        }
    }

    @Nested
    class CreateFaceMemo {

        @Test
        void readsNeitherTheSettingsNorAnyTextUntilAsked() {
            // A holder is made before any text is drawn, often with no game settings to read yet; with
            // none installed, a memo that read them on construction would fail here.
            var textReads = new int[1];

            InstalledFaces.<String>createFaceMemo(kind -> {
                textReads[0]++;
                return List.of();
            });

            assertThat(textReads[0])
                .isZero();
        }

        @Test
        void settlesThroughTheLineHeightsTheLiveInstallStates() {
            // Every face on the walk reads as unloadable through the live reader, so the walk passes each
            // without asking about a glyph and answers the last resort - which only a memo settling over
            // the live install's line heights can do.
            var settingsMock = mock(SettingsAPI.class);
            when(settingsMock.getString("defaultFont")).thenReturn("graphics/fonts/insignia25LTaa.fnt");
            Global.setSettings(settingsMock);

            try (var lineHeightsMock = LazyFontLineHeightReaderMock.install(FaceLineHeightReaderFake
                    .createVanillaLineHeights()
                    .answeringLineHeight(StarsectorFont.VANILLA_VICTOR_10, FaceLineHeightReader.NO_LINE_HEIGHT)
                    .answeringLineHeight(StarsectorFont.VANILLA_INSIGNIA_25, FaceLineHeightReader.NO_LINE_HEIGHT)
                    .answeringLineHeight(FaceResolver.LAST_RESORT_FONT, FaceLineHeightReader.NO_LINE_HEIGHT))) {

                var memo = InstalledFaces.<String>createFaceMemo(kind -> List.of("Political map"));

                assertThat(memo.settleFace(StarsectorFont.VANILLA_VICTOR_10, Set.of("words")))
                    .isEqualTo(FaceResolver.LAST_RESORT_FONT);
            }
        }
    }

    @Nested
    class CreateFaceResolver {

        @Test
        void walksToTheDefaultTheGamesSettingsDeclare() {
            // A default other than vanilla's, so a resolver built without reading the settings would walk
            // straight from the face to the last resort.
            var settingsMock = mock(SettingsAPI.class);
            when(settingsMock.getString("defaultFont")).thenReturn("graphics/fonts/insignia25LTaa.fnt");
            Global.setSettings(settingsMock);

            assertThat(InstalledFaces.createFaceResolver().listFallbackWalk(StarsectorFont.VANILLA_VICTOR_10))
                .containsExactly(
                    StarsectorFont.VANILLA_VICTOR_10,
                    StarsectorFont.VANILLA_INSIGNIA_25,
                    StarsectorFont.VANILLA_INSIGNIA_15);
        }
    }
}
