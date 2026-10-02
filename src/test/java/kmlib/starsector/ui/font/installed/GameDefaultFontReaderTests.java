package kmlib.starsector.ui.font.installed;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import kmlib.starsector.ui.font.AtlasSmoothing;
import kmlib.starsector.ui.font.DeclaredFontAtlas;
import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.testfixtures.logging.LogAppenderFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins how the game's declared default face is read: the enum's face where the setting names one, the
 * declared file by its path otherwise - smoothing and all - and vanilla's face where it names nothing.
 */
final class GameDefaultFontReaderTests {

    // A language pack's own face, as a mod pointing the game's defaultFont at it would name it.
    private static final String PACK_FONT_PATH = "graphics/fonts/pack_script15.fnt";

    @AfterEach
    void clearSettings() {
        Global.setSettings(null);
    }

    @Nested
    class ReadDefaultFont {

        @Test
        void answersTheEnumsFaceWhereTheSettingNamesOne() throws IOException {

            installDefaultFont("graphics/fonts/insignia25LTaa.fnt", null);

            assertThat(GameDefaultFontReader.readDefaultFont())
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }

        @Test
        void answersADeclaredFileTheEnumDoesNotNameByItsPath() throws IOException {
            // A language pack's face is the one that may hold its script, so it is reached as named.
            installDefaultFont(PACK_FONT_PATH, "info face=\"Pack\" size=15 smooth=1 aa=4");

            assertThat(GameDefaultFontReader.readDefaultFont())
                .isEqualTo(new DeclaredFontAtlas(PACK_FONT_PATH, AtlasSmoothing.SMOOTHED));
        }

        @Test
        void readsADeclaredPixelFaceAsPixelExact() throws IOException {
            // The declared file's own descriptor decides, KM having no table of a face it did not ship.
            installDefaultFont(PACK_FONT_PATH, "info face=\"Pack\" size=-10 smooth=1 aa=1");

            assertThat(GameDefaultFontReader.readDefaultFont())
                .isEqualTo(new DeclaredFontAtlas(PACK_FONT_PATH, AtlasSmoothing.PIXEL_EXACT));
        }

        @Test
        void readsADeclaredFileItCannotOpenAsAntialiasedAndLogsIt() throws IOException {
            // Unreadable means unloadable too, so every walk passes over it whatever is answered; the line
            // is what tells a reader the setting names a file that is not there.
            installDefaultFont(PACK_FONT_PATH, null);

            var logFake = LogAppenderFake.captureLogOf(
                GameDefaultFontReader.class,
                () -> assertThat(GameDefaultFontReader.readDefaultFont())
                    .isEqualTo(new DeclaredFontAtlas(PACK_FONT_PATH, AtlasSmoothing.SMOOTHED)));

            assertThat(logFake.getMessages())
                .containsExactly("The game's defaultFont 'graphics/fonts/pack_script15.fnt' could not be read; "
                    + "taking it as antialiased");
        }

        @Test
        void answersVanillasFaceWhereTheSettingNamesNothing() throws IOException {

            installDefaultFont(null, null);

            assertThat(GameDefaultFontReader.readDefaultFont())
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_15);
        }
    }

    @Nested
    class ResolveDefaultFont {

        @Test
        void answersTheFaceThePathNames() {

            assertThat(GameDefaultFontReader.resolveDefaultFont(
                    "graphics/fonts/insignia15LTaa.fnt",
                    declaredPath -> AtlasSmoothing.PIXEL_EXACT))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_15);
        }

        @Test
        void asksTheReaderForTheSmoothingOfADeclaredFile() {

            assertThat(GameDefaultFontReader.resolveDefaultFont(
                    PACK_FONT_PATH,
                    declaredPath -> AtlasSmoothing.PIXEL_EXACT))
                .isEqualTo(new DeclaredFontAtlas(PACK_FONT_PATH, AtlasSmoothing.PIXEL_EXACT));
        }

        @Test
        void answersVanillasFaceForABlankSetting() {

            assertThat(GameDefaultFontReader.resolveDefaultFont(" ", declaredPath -> AtlasSmoothing.PIXEL_EXACT))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_15);
        }
    }

    // A settings stand-in naming the given default and serving the given descriptor first line for it, or
    // refusing to open it where there is none.
    private static void installDefaultFont(String defaultFontPath, String descriptorInfoLine) throws IOException {

        var settingsMock = mock(SettingsAPI.class);

        when(settingsMock.getString("defaultFont"))
            .thenReturn(defaultFontPath);

        if (descriptorInfoLine == null) {
            when(settingsMock.openStream(defaultFontPath))
                .thenThrow(new IOException("no such file"));
        } else {
            when(settingsMock.openStream(defaultFontPath))
                .thenReturn(new ByteArrayInputStream(
                    (descriptorInfoLine + "\n").getBytes(StandardCharsets.ISO_8859_1)));
        }
        Global.setSettings(settingsMock);
    }
}
