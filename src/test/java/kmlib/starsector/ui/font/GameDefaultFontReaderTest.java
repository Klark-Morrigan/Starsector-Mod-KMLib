package kmlib.starsector.ui.font;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import kmlib.testfixtures.logging.LogAppenderFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins how the game's default face is read: the face its settings name, and vanilla's where they name a
 * face KM does not draw in.
 */
class GameDefaultFontReaderTest {

    @AfterEach
    void clearSettings() {
        Global.setSettings(null);
    }

    @Nested
    class ReadDefaultFont {

        @Test
        void readDefaultFontAnswersTheFaceTheGamesSettingsName() {
            // A core overwrite naming another face the enum knows moves KM's last resort with it.
            installDefaultFont("graphics/fonts/insignia25LTaa.fnt");

            assertThat(GameDefaultFontReader.readDefaultFont())
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }

        @Test
        void readDefaultFontAnswersVanillasFaceWhereTheSettingsNameOneKmDoesNotKnow() {
            // KM draws only in faces the enum names, so a default it cannot draw in is no default.
            installDefaultFont("graphics/fonts/arial12.fnt");

            assertThat(GameDefaultFontReader.readDefaultFont())
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_15);
        }

        @Test
        void readDefaultFontLogsTheFaceItCouldNotUse() {

            installDefaultFont("graphics/fonts/arial12.fnt");

            var logFake = LogAppenderFake.captureLogOf(
                GameDefaultFontReader.class,
                GameDefaultFontReader::readDefaultFont);

            assertThat(logFake.getMessages())
                .containsExactly("The game's defaultFont 'graphics/fonts/arial12.fnt' is no face KM draws in; "
                    + "falling back to insignia15LTaa");
        }
    }

    @Nested
    class ResolveDefaultFont {

        @Test
        void resolveDefaultFontAnswersTheFaceThePathNames() {

            assertThat(GameDefaultFontReader.resolveDefaultFont("graphics/fonts/insignia15LTaa.fnt"))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_15);
        }

        @Test
        void resolveDefaultFontAnswersVanillasFaceForNoPath() {
            // A settings file with no such key reads as vanilla's, the face its settings would name.
            assertThat(GameDefaultFontReader.resolveDefaultFont(null))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_15);
        }
    }

    private static void installDefaultFont(String defaultFontPath) {

        var settingsMock = mock(SettingsAPI.class);

        when(settingsMock.getString("defaultFont"))
            .thenReturn(defaultFontPath);

        Global.setSettings(settingsMock);
    }
}
