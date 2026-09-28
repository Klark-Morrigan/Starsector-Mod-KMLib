package kmlib.starsector.ui.font;

import kmlib.testfixtures.starsector.ui.font.FaceLineHeightReaderFake;
import kmlib.testfixtures.starsector.ui.font.LazyFontLineHeightReaderMock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextFaceTest {

    @Nested
    class CreateNativeFace {

        @Test
        void createNativeFaceDrawsTheFaceAtTheLineHeightTheInstallStates() {
            // An install whose atlas under this basename is taller than vanilla's, as a core localisation's
            // is. The face has to land on that atlas's own 1:1 size, which no size written down for
            // vanilla could produce.
            var lineHeightsFake = FaceLineHeightReaderFake
                .createVanillaLineHeights()
                .answeringLineHeight(StarsectorFont.VANILLA_INSIGNIA_15, 17d);

            var face = TextFace.createNativeFace(StarsectorFont.VANILLA_INSIGNIA_15, lineHeightsFake);

            assertThat(face)
                .isEqualTo(new TextFace(StarsectorFont.VANILLA_INSIGNIA_15, 17d));
        }
    }

    @Nested
    class CreateInstalledNativeFace {

        @Test
        void createInstalledNativeFaceDrawsTheFaceAtTheLineHeightTheLiveInstallStates() {
            // The live reader stood in for a taller atlas than vanilla's, so the size can only have come
            // from the read.
            try (var lineHeightsMock =LazyFontLineHeightReaderMock.install(FaceLineHeightReaderFake
                    .createVanillaLineHeights()
                    .answeringLineHeight(StarsectorFont.VANILLA_VICTOR_10, 11d))) {

                assertThat(TextFace.createInstalledNativeFace(StarsectorFont.VANILLA_VICTOR_10))
                    .isEqualTo(new TextFace(StarsectorFont.VANILLA_VICTOR_10, 11d));
            }
        }
    }
}
