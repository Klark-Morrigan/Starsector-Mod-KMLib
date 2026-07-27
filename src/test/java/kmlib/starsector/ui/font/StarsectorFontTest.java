package kmlib.starsector.ui.font;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class StarsectorFontTest {

    @Nested
    class ResolvePath {
        @Test
        void resolvePathWrapsBasenameInTheGamesFontDirectoryAndExtension() {
            assertThat(StarsectorFont.VANILLA_INSIGNIA_15.resolvePath())
                    .isEqualTo("graphics/fonts/insignia15LTaa.fnt");
        }

        @ParameterizedTest
        @EnumSource(StarsectorFont.class)
        void resolvePathIsLoadableForEveryFace(StarsectorFont font) {
            // The loader hands this straight to LazyLib, which resolves it against the game's own
            // file tree - so the shape has to hold for every value, not just the one spelled above.
            assertThat(font.resolvePath())
                    .isEqualTo("graphics/fonts/" + font.getBasename() + ".fnt");
        }
    }

    @Nested
    class GetNativeSize {
        // The size each atlas was rasterised at, read off the "size=" in its .fnt descriptor under
        // starsector-core/graphics/fonts. Restated here rather than parsed from the descriptor: a
        // unit test has no game install to read, and the point of the check is that a value's
        // declared native size still matches the atlas its basename names.
        @ParameterizedTest
        @EnumSource(StarsectorFont.class)
        void getNativeSizeReportsTheSizeItsAtlasWasRasterisedAt(StarsectorFont font) {
            var expected = switch (font) {
                case VANILLA_INSIGNIA_15 -> 15;
                case VANILLA_ORBITRON_20AA -> 20;
                case VANILLA_INSIGNIA_42 -> 42;
            };

            assertThat(font.getNativeSize()).isEqualTo(expected);
        }
    }
}
