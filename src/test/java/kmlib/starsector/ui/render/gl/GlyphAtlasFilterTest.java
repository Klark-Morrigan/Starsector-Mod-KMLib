package kmlib.starsector.ui.render.gl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one thing about the glyph filter that is arithmetic rather than drawing: the sharpness a caller
 * sets is confined to the range the two passes are weighted from. Everything else here is raw GL and is
 * exercised in-engine.
 *
 * <p>It matters because the value arrives from a player-facing setting: out of range it would weight the
 * interpolated pass negatively - a pass that subtracts what the hard one laid down - or past full, which
 * hides the hard pass the sharpness was raised to show.
 */
final class GlyphAtlasFilterTest {

    @AfterEach
    void restoreTheDefaultSharpness() {
        // Static state shared with every other test in the run, so what one case sets it to must not be
        // what the next one finds.
        GlyphAtlasFilter.setPixelFaceSharpness(GlyphAtlasFilter.DEFAULT_PIXEL_FACE_SHARPNESS);
    }

    @Nested
    class SetPixelFaceSharpness {

        @Test
        void GlyphAtlasFilter_setPixelFaceSharpness_confinesAValueAboveTheRange() {

            GlyphAtlasFilter.setPixelFaceSharpness(1.5f);

            assertThat(GlyphAtlasFilter.getPixelFaceSharpness())
                .isEqualTo(1f);
        }

        @Test
        void GlyphAtlasFilter_setPixelFaceSharpness_confinesAValueBelowTheRange() {

            GlyphAtlasFilter.setPixelFaceSharpness(-0.5f);

            assertThat(GlyphAtlasFilter.getPixelFaceSharpness())
                .isEqualTo(0f);
        }

        @Test
        void GlyphAtlasFilter_setPixelFaceSharpness_keepsAValueInsideTheRange() {
            
            GlyphAtlasFilter.setPixelFaceSharpness(0.35f);

            assertThat(GlyphAtlasFilter.getPixelFaceSharpness())
                .isEqualTo(0.35f);
        }
    }
}
