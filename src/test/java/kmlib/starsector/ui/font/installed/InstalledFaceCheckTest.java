package kmlib.starsector.ui.font.installed;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import kmlib.starsector.ui.font.AtlasSmoothing;
import kmlib.starsector.ui.font.DeclaredFontAtlas;
import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.testfixtures.logging.LogAppenderFake;
import kmlib.testfixtures.starsector.ui.font.FaceLineHeightReaderFake;
import kmlib.testfixtures.starsector.ui.font.installed.LazyFontLineHeightReaderMock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstalledFaceCheckTest {

    // A language pack's own face, as a mod pointing the game's defaultFont at it would name it.
    private static final String PACK_FONT_PATH = "graphics/fonts/pack_script15.fnt";

    @AfterEach
    void clearSettings() {
        Global.setSettings(null);
    }

    @Nested
    class LoadEveryFace {

        @Test
        void statesEachFacesInstalledLineHeightAndEachFaceThatWouldNotLoad() throws IOException {
            // The one reading of which atlases a session drew with, so it names every face: an edition's
            // taller body atlas and a face missing from the install both have to be readable off it.
            installDefaultFont("graphics/fonts/insignia15LTaa.fnt");

            var lineHeightsFake = FaceLineHeightReaderFake.createVanillaLineHeights()
                .answeringLineHeight(StarsectorFont.VANILLA_INSIGNIA_15, 17d)
                .answeringLineHeight(StarsectorFont.VANILLA_INSIGNIA_42, 0d);

            try (var lineHeightsMock = LazyFontLineHeightReaderMock.install(lineHeightsFake)) {

                var logFake = LogAppenderFake.captureLogOf(
                    InstalledFaceCheck.class,
                    InstalledFaceCheck::loadEveryFace);

                assertThat(logFake.getMessages())
                    .containsExactly("Installed font faces loaded; lineHeight by face: insignia15LTaa=17, "
                        + "orbitron20aa=20, orbitron12condensed=15, victor10=9, insignia21LTaa=21, "
                        + "insignia25LTaa=24, insignia42LTaa=unavailable; "
                        + "defaultFont graphics/fonts/insignia15LTaa.fnt=17");
            }
        }

        @Test
        void statesTheLineHeightOfADeclaredDefaultTheEnumDoesNotName() throws IOException {
            // A face a mod declared is one a walk can reach, so whether it loads belongs in the same line.
            installDefaultFont(PACK_FONT_PATH);

            var lineHeightsFake = FaceLineHeightReaderFake.createVanillaLineHeights()
                .answeringLineHeight(new DeclaredFontAtlas(PACK_FONT_PATH, AtlasSmoothing.SMOOTHED), 16d);

            try (var lineHeightsMock = LazyFontLineHeightReaderMock.install(lineHeightsFake)) {

                var logFake = LogAppenderFake.captureLogOf(
                    InstalledFaceCheck.class,
                    InstalledFaceCheck::loadEveryFace);

                assertThat(logFake.getMessages())
                    .singleElement()
                    .asString()
                    .endsWith("; defaultFont graphics/fonts/pack_script15.fnt=16");
            }
        }
    }

    // A settings stand-in naming the given default, whose descriptor states an antialiased atlas.
    private static void installDefaultFont(String defaultFontPath) throws IOException {

        var settingsMock = mock(SettingsAPI.class);

        when(settingsMock.getString("defaultFont"))
            .thenReturn(defaultFontPath);
        when(settingsMock.openStream(defaultFontPath))
            .thenReturn(new ByteArrayInputStream(
                "info face=\"Pack\" size=15 aa=4\n".getBytes(StandardCharsets.ISO_8859_1)));

        Global.setSettings(settingsMock);
    }
}
