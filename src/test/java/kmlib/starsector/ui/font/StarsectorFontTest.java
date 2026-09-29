package kmlib.starsector.ui.font;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class StarsectorFontTest {

    @Nested
    class ResolvePath {

        // The atlas each value names, as it is spelled under starsector-core/graphics/fonts. Asserted
        // rather than derived: a path built from the value under test would agree with any typo in it,
        // and a misspelled basename is exactly the failure the enum exists to make impossible.
        @ParameterizedTest
        @EnumSource(StarsectorFont.class)
        void namesTheAtlasUnderTheGamesFontDirectory(StarsectorFont font) {

            var expected = switch (font) {
                case VANILLA_INSIGNIA_15 -> "graphics/fonts/insignia15LTaa.fnt";
                case VANILLA_ORBITRON_20AA -> "graphics/fonts/orbitron20aa.fnt";
                case VANILLA_ORBITRON_12_CONDENSED -> "graphics/fonts/orbitron12condensed.fnt";
                case VANILLA_VICTOR_10 -> "graphics/fonts/victor10.fnt";
                case VANILLA_INSIGNIA_21 -> "graphics/fonts/insignia21LTaa.fnt";
                case VANILLA_INSIGNIA_25 -> "graphics/fonts/insignia25LTaa.fnt";
                case VANILLA_INSIGNIA_42 -> "graphics/fonts/insignia42LTaa.fnt";
            };

            assertThat(font.resolvePath())
                .isEqualTo(expected);
        }

        @Test
        void isDistinctForEveryFace() {
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
        // no soft edge of its own and aa=4 does. Restated here rather than parsed: a unit test has no
        // install to read, and what is worth pinning is that a value's declared smoothing still matches
        // the atlas its path names.
        //
        // The neighbouring "smooth" flag is not the test, and the two disagree in both directions across
        // these five: orbitron20aa is antialiased at smooth=0, orbitron12condensed hard-edged at
        // smooth=1. Reading the wrong one is what had the condensed face drawn interpolated.
        @ParameterizedTest
        @EnumSource(StarsectorFont.class)
        void reportsWhatItsAtlasAsksFor(StarsectorFont font) {

            var expected = switch (font) {

            // aa=1: every stroke is one pixel wide, so interpolated each of them is entirely edge and
            // loses part of itself, which reads as text dimmer than the colour it was set in.
                case VANILLA_VICTOR_10,
                    VANILLA_ORBITRON_12_CONDENSED -> AtlasSmoothing.PIXEL_EXACT;
                case VANILLA_INSIGNIA_15,
                    VANILLA_ORBITRON_20AA,
                    VANILLA_INSIGNIA_21,
                    VANILLA_INSIGNIA_25,
                    VANILLA_INSIGNIA_42 -> AtlasSmoothing.SMOOTHED;
            };

            assertThat(font.getSmoothing())
                .isEqualTo(expected);
        }
    }

    @Nested
    class ResolveLowerResolutionFont {

        // The next cut down each face falls to. The insignia cuts step down through the family to its
        // smallest; every other face has no smaller cut of its own design holding what it lacks, so it
        // names none and falls straight to the game's default. Spelt out per face, so a chain rewired
        // in passing - a cut skipped, or two cuts pointing at each other - fails here.
        @ParameterizedTest
        @EnumSource(StarsectorFont.class)
        void namesTheNextCutDownInTheFacesFamily(StarsectorFont font) {

            var expected = switch (font) {
                case VANILLA_INSIGNIA_42 -> StarsectorFont.VANILLA_INSIGNIA_25;
                case VANILLA_INSIGNIA_25 -> StarsectorFont.VANILLA_INSIGNIA_21;
                case VANILLA_INSIGNIA_21 -> StarsectorFont.VANILLA_INSIGNIA_15;
                case VANILLA_INSIGNIA_15,
                    VANILLA_ORBITRON_20AA,
                    VANILLA_ORBITRON_12_CONDENSED,
                    VANILLA_VICTOR_10 -> null;
            };

            assertThat(font.resolveLowerResolutionFont())
                .isEqualTo(Optional.ofNullable(expected));
        }
    }

    @Nested
    class FindFontByPath {

        @Test
        void findsTheFaceTheGamesSettingsName() {
            // The spelling vanilla's settings.json names its defaultFont in.
            assertThat(StarsectorFont.findFontByPath("graphics/fonts/insignia15LTaa.fnt"))
                .contains(StarsectorFont.VANILLA_INSIGNIA_15);
        }

        @Test
        void findsNothingForAFaceTheEnumDoesNotName() {

            assertThat(StarsectorFont.findFontByPath("graphics/fonts/arial12.fnt"))
                .isEmpty();
        }

        @Test
        void findsNothingForNoPath() {

            assertThat(StarsectorFont.findFontByPath(null))
                .isEmpty();
        }
    }
}
