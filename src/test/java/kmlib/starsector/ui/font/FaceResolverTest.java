package kmlib.starsector.ui.font;

import kmlib.testfixtures.starsector.ui.font.FaceLineHeightReaderFake;
import kmlib.testfixtures.starsector.ui.font.GlyphCoverageReaderFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how a face is settled from the face asked for and the install: kept where it loads and holds the
 * text, walked down its family to the first cut that does, and ended at the default face either way.
 *
 * <p>The install is posed as vanilla or as a localised one: vanilla atlases hold Latin-1 alone, while a
 * localised install replaces the smaller insignia cuts with ones holding its script and leaves the
 * high-resolution cut untouched - the arrangement that drew KMU's map labels as question marks.
 */
class FaceResolverTest {

    // "Hegemony" (U+9738 U+4E3B), a faction name as a localised install draws it.
    private static final String LOCALISED_NAME = "霸主";

    // What the reader states for a face the install cannot load.
    private static final double UNLOADABLE = 0d;

    // The game's own default face on a vanilla install.
    private static final StarsectorFont DEFAULT_FONT = StarsectorFont.VANILLA_INSIGNIA_15;

    private static GlyphCoverageReaderFake createLocalisedCoverage() {
        return GlyphCoverageReaderFake.createLatinOnlyCoverage()
            .coveringEveryCharacter(StarsectorFont.VANILLA_INSIGNIA_25)
            .coveringEveryCharacter(StarsectorFont.VANILLA_INSIGNIA_21)
            .coveringEveryCharacter(StarsectorFont.VANILLA_INSIGNIA_15);
    }

    private static FaceResolver createResolver(
            FaceLineHeightReaderFake lineHeightsFake,
            GlyphCoverageReaderFake coverageFake) {

        return new FaceResolver(lineHeightsFake, coverageFake, DEFAULT_FONT);
    }

    private static FaceResolver createLocalisedResolver() {
        return createResolver(FaceLineHeightReaderFake.createVanillaLineHeights(), createLocalisedCoverage());
    }

    private static FaceResolver createLocalisedResolverWithUnloadable(StarsectorFont... unloadableFonts) {

        var lineHeightsFake = FaceLineHeightReaderFake.createVanillaLineHeights();
        for (var unloadableFont : unloadableFonts) {
            lineHeightsFake = lineHeightsFake.answeringLineHeight(unloadableFont, UNLOADABLE);
        }
        return createResolver(lineHeightsFake, createLocalisedCoverage());
    }

    @Nested
    class ResolveFont {

        @Test
        void resolveFontKeepsTheFaceAskedForWhereItsAtlasHoldsTheText() {
            // An English build's names on a localised install: the high-resolution atlas holds them, so
            // nothing moves the text off it.
            assertThat(createLocalisedResolver().resolveFont(StarsectorFont.VANILLA_INSIGNIA_42, List.of("Hegemony")))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_42);
        }

        @Test
        void resolveFontStepsDownToTheLargestCutThatHoldsTheText() {
            // The row of question marks, answered: the next cut down holds the script, so the walk stops
            // there rather than dropping further than it has to.
            assertThat(createLocalisedResolver().resolveFont(
                    StarsectorFont.VANILLA_INSIGNIA_42,
                    List.of(LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }

        @Test
        void resolveFontStepsDownWhenAnyOneOfTheTextsNeedsAGlyphTheFaceLacks() {
            // A caller settles one face for everything it draws, so one localised name among Latin ones
            // moves them all.
            assertThat(createLocalisedResolver().resolveFont(
                    StarsectorFont.VANILLA_INSIGNIA_42,
                    List.of("Hegemony", LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }

        @Test
        void resolveFontPassesOverACutThatWillNotLoad() {

            var resolver = createLocalisedResolverWithUnloadable(StarsectorFont.VANILLA_INSIGNIA_25);

            assertThat(resolver.resolveFont(StarsectorFont.VANILLA_INSIGNIA_42, List.of(LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_21);
        }

        @Test
        void resolveFontStepsDownWhenTheFaceAskedForWillNotLoadEvenForTextItWouldHold() {
            // A face that will not load draws nothing, however little it would have had to draw.
            var resolver = createLocalisedResolverWithUnloadable(StarsectorFont.VANILLA_INSIGNIA_42);

            assertThat(resolver.resolveFont(StarsectorFont.VANILLA_INSIGNIA_42, List.of("Hegemony")))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }

        @Test
        void resolveFontFallsToTheDefaultFromAFaceWithNoSmallerCut() {
            // A pixel face lacking the script has no smaller cut of its own design to try, so the default
            // is next.
            assertThat(createLocalisedResolver().resolveFont(StarsectorFont.VANILLA_VICTOR_10, List.of(LOCALISED_NAME)))
                .isEqualTo(DEFAULT_FONT);
        }

        @Test
        void resolveFontAnswersTheDefaultWhereNoFaceOnTheWalkHoldsTheText() {
            // A localised build on a vanilla install: no atlas holds the script, and the game's own face
            // is the last any text can have.
            var resolver = createResolver(
                FaceLineHeightReaderFake.createVanillaLineHeights(),
                GlyphCoverageReaderFake.createLatinOnlyCoverage());

            assertThat(resolver.resolveFont(StarsectorFont.VANILLA_INSIGNIA_42, List.of(LOCALISED_NAME)))
                .isEqualTo(DEFAULT_FONT);
        }

        @Test
        void resolveFontAnswersTheDefaultWhereNoFaceOnTheWalkLoads() {
            // Never no face: the default is answered even where it will not load itself, drawing nothing -
            // as the face asked for would have.
            var resolver = createLocalisedResolverWithUnloadable(
                StarsectorFont.VANILLA_INSIGNIA_42,
                StarsectorFont.VANILLA_INSIGNIA_25,
                StarsectorFont.VANILLA_INSIGNIA_21,
                StarsectorFont.VANILLA_INSIGNIA_15);

            assertThat(resolver.resolveFont(StarsectorFont.VANILLA_INSIGNIA_42, List.of("Hegemony")))
                .isEqualTo(DEFAULT_FONT);
        }
    }

    @Nested
    class ListFallbackWalk {

        @Test
        void listFallbackWalkStepsDownTheFamilyAndEndsAtTheDefaultOnce() {
            // The family's smallest cut is the default here, so it is tried once, not twice.
            assertThat(createLocalisedResolver().listFallbackWalk(StarsectorFont.VANILLA_INSIGNIA_42))
                .containsExactly(
                    StarsectorFont.VANILLA_INSIGNIA_42,
                    StarsectorFont.VANILLA_INSIGNIA_25,
                    StarsectorFont.VANILLA_INSIGNIA_21,
                    StarsectorFont.VANILLA_INSIGNIA_15);
        }

        @Test
        void listFallbackWalkGoesStraightToTheDefaultFromAFaceWithNoSmallerCut() {

            assertThat(createLocalisedResolver().listFallbackWalk(StarsectorFont.VANILLA_ORBITRON_20AA))
                .containsExactly(StarsectorFont.VANILLA_ORBITRON_20AA, StarsectorFont.VANILLA_INSIGNIA_15);
        }

        @Test
        void listFallbackWalkEndsAtADefaultOutsideTheFamily() {
            // A core overwrite naming another face as the game's default moves the end of every walk.
            var resolver = new FaceResolver(
                FaceLineHeightReaderFake.createVanillaLineHeights(),
                createLocalisedCoverage(),
                StarsectorFont.VANILLA_ORBITRON_20AA);

            assertThat(resolver.listFallbackWalk(StarsectorFont.VANILLA_INSIGNIA_25))
                .containsExactly(
                    StarsectorFont.VANILLA_INSIGNIA_25,
                    StarsectorFont.VANILLA_INSIGNIA_21,
                    StarsectorFont.VANILLA_INSIGNIA_15,
                    StarsectorFont.VANILLA_ORBITRON_20AA);
        }

        @Test
        void listFallbackWalkIsTheDefaultAloneWhereTheDefaultIsAskedFor() {

            assertThat(createLocalisedResolver().listFallbackWalk(StarsectorFont.VANILLA_INSIGNIA_15))
                .containsExactly(StarsectorFont.VANILLA_INSIGNIA_15);
        }
    }
}
