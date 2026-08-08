package kmlib.starsector.ui.font;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class StarsectorFontTest {

    @Nested
    class ResolvePath {
        // The atlas each value names, as it is spelled under starsector-core/graphics/fonts. Asserted
        // rather than derived: a path built from the value under test would agree with any typo in it,
        // and a misspelled basename is exactly the failure the enum exists to make impossible.
        @ParameterizedTest
        @EnumSource(StarsectorFont.class)
        void resolvePathNamesTheAtlasUnderTheGamesFontDirectory(StarsectorFont font) {
            var expected = switch (font) {
            case VANILLA_INSIGNIA_15 -> "graphics/fonts/insignia15LTaa.fnt";
            case VANILLA_ORBITRON_20AA -> "graphics/fonts/orbitron20aa.fnt";
            case VANILLA_ORBITRON_12_CONDENSED -> "graphics/fonts/orbitron12condensed.fnt";
            case VANILLA_VICTOR_10 -> "graphics/fonts/victor10.fnt";
            case VANILLA_INSIGNIA_42 -> "graphics/fonts/insignia42LTaa.fnt";
            };

            assertThat(font.resolvePath()).isEqualTo(expected);
        }

        @Test
        void resolvePathIsDistinctForEveryFace() {
            // The path is what the face cache keys on, so two values sharing one - the copy-paste that
            // adding a face invites - would silently serve one atlas under two names.
            var paths = Arrays.stream(StarsectorFont.values())
                .map(StarsectorFont::resolvePath)
                .toList();

            assertThat(paths).doesNotHaveDuplicates();
        }
    }

    @Nested
    class GetNativeSize {
        // The size each atlas was rasterised at, read off the "size=" in its .fnt descriptor under
        // starsector-core/graphics/fonts. Restated here rather than parsed from the descriptor: a unit
        // test has no game install to read, and the point of the check is that a value's declared
        // native size still matches the atlas its path names.
        @ParameterizedTest
        @EnumSource(StarsectorFont.class)
        void getNativeSizeReportsTheSizeItsAtlasWasRasterisedAt(StarsectorFont font) {
            var expected = switch (font) {
            case VANILLA_INSIGNIA_15 -> 15;
            case VANILLA_ORBITRON_20AA -> 20;
            case VANILLA_ORBITRON_12_CONDENSED -> 12;
            // The descriptor spells it "size=-10": BMFont writes the height it matched as a negative
            // when the face was rasterised to a character height rather than to a point size, so the
            // atlas is a 10 and the sign is the setting it was made with.
            case VANILLA_VICTOR_10 -> 10;
            case VANILLA_INSIGNIA_42 -> 42;
            };

            assertThat(font.getNativeSize()).isEqualTo(expected);
        }
    }
}
