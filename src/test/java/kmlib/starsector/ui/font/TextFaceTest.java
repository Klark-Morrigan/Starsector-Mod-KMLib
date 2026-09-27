package kmlib.starsector.ui.font;

import kmlib.testfixtures.starsector.ui.font.FaceLineHeightReaderFake;

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
}
