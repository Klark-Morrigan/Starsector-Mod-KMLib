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

            assertThat(font.resolvePath())
                .isEqualTo(expected);
        }

        @Test
        void resolvePathIsDistinctForEveryFace() {
            // The path is what the face cache keys on, so two values sharing one - the copy-paste that
            // adding a face invites - would silently serve one atlas under two names.
            var paths = Arrays.stream(StarsectorFont.values())
                .map(StarsectorFont::resolvePath)
                .toList();

            assertThat(paths)
                .doesNotHaveDuplicates();
        }
    }

    @Nested
    class GetSmoothing {

        // Whether each atlas wants interpolating, read off the "aa" count in its .fnt descriptor under
        // starsector-core/graphics/fonts - how many samples a glyph was rasterised from, so aa=1 carries
        // no soft edge of its own and aa=4 does. Restated here rather than parsed for the reason the
        // native size is: a unit test has no install to read, and what is worth pinning is that a value's
        // declared smoothing still matches the atlas its path names.
        //
        // The neighbouring "smooth" flag is not the test, and the two disagree in both directions across
        // these five: orbitron20aa is antialiased at smooth=0, orbitron12condensed hard-edged at
        // smooth=1. Reading the wrong one is what had the condensed face drawn interpolated.
        @ParameterizedTest
        @EnumSource(StarsectorFont.class)
        void getSmoothingReportsWhatItsAtlasAsksFor(StarsectorFont font) {

            var expected = switch (font) {

            // aa=1: every stroke is one pixel wide, so interpolated each of them is entirely edge and
            // loses part of itself, which reads as text dimmer than the colour it was set in.
                case VANILLA_VICTOR_10,
                    VANILLA_ORBITRON_12_CONDENSED -> AtlasSmoothing.PIXEL_EXACT;
                case VANILLA_INSIGNIA_15,
                    VANILLA_ORBITRON_20AA,
                    VANILLA_INSIGNIA_42 -> AtlasSmoothing.SMOOTHED;
            };

            assertThat(font.getSmoothing())
                .isEqualTo(expected);
        }
    }

    @Nested
    class GetNativeSize {

        // The size each atlas draws 1:1 at, read off the "lineHeight" in its .fnt descriptor under
        // starsector-core/graphics/fonts - the number the font loader scales a request against.
        // Restated here rather than parsed from the descriptor: a unit test has no game install to
        // read, and the point of the check is that a value's declared native size still matches the
        // atlas its path names.
        @ParameterizedTest
        @EnumSource(StarsectorFont.class)
        void getNativeSizeReportsTheSizeItsAtlasDrawsOneToOneAt(StarsectorFont font) {

            var expected = switch (font) {
                case VANILLA_INSIGNIA_15 -> 15;
                case VANILLA_ORBITRON_20AA -> 20;
                // Another face whose name is not its size: "size=-12" is the character height it was
                // matched at, where the descriptor's line height - the number a request is scaled
                // against - is 15.
                case VANILLA_ORBITRON_12_CONDENSED -> 15;
                // The descriptor spells "size=-10" - BMFont writes the character height it matched as a
                // negative - but states "lineHeight=9", and the line height is what a request is scaled
                // against. So the pixel face is asked for at 9 and the 10 in its name is not a size at all.
                case VANILLA_VICTOR_10 -> 9;
                case VANILLA_INSIGNIA_42 -> 42;
            };

            assertThat(font.getNativeSize())
                .isEqualTo(expected);
        }
    }
}
