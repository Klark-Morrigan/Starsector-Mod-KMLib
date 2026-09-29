package kmlib.starsector.ui.font.installed;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.font.TextFace;
import kmlib.testfixtures.starsector.ui.font.FaceLineHeightReaderFake;
import kmlib.testfixtures.starsector.ui.font.installed.LazyFontLineHeightReaderMock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstalledFacesTests {

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
