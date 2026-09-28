package kmlib.starsector.ui.font;

import kmlib.testfixtures.logging.LogAppenderFake;
import kmlib.testfixtures.starsector.ui.font.FaceLineHeightReaderFake;
import kmlib.testfixtures.starsector.ui.font.LazyFontLineHeightReaderMock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InstalledFaceCheckTest {

    @Nested
    class LoadEveryFace {

        @Test
        void loadEveryFaceStatesEachFacesInstalledLineHeightAndEachFaceThatWouldNotLoad() {
            // The one reading of which atlases a session drew with, so it names every face: an edition's
            // taller body atlas and a face missing from the install both have to be readable off it.
            var lineHeightsFake = FaceLineHeightReaderFake.createVanillaLineHeights()
                .answeringLineHeight(StarsectorFont.VANILLA_INSIGNIA_15, 17d)
                .answeringLineHeight(StarsectorFont.VANILLA_INSIGNIA_42, 0d);

            try (var lineHeightsMock = LazyFontLineHeightReaderMock.install(lineHeightsFake)) {

                var logFake = LogAppenderFake.captureLogOf(
                    InstalledFaceCheck.class,
                    InstalledFaceCheck::loadEveryFace);

                assertThat(logFake.getMessages())
                    .containsExactly("Installed font faces loaded; lineHeight by face: insignia15LTaa=17, "
                        + "orbitron20aa=20, orbitron12condensed=15, victor10=9, insignia21LTaa=21, insignia25LTaa=24, "
                        + "insignia42LTaa=unavailable");
            }
        }
    }
}
