package kmlib.starsector.ui.font;

import kmlib.testfixtures.starsector.ui.font.FaceLineHeightReaderFake;
import kmlib.testfixtures.starsector.ui.font.GlyphCoverageReaderFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how a face is settled from the face asked for and the install: kept where it loads and holds the
 * text, walked down its family, then to the game's declared default, and ended at KMLib's own last resort
 * whatever the settings say.
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

    // The game's own declared default on a vanilla install, which the enum names.
    private static final StarsectorFont VANILLA_DECLARED_DEFAULT = StarsectorFont.VANILLA_INSIGNIA_15;

    // A face a language pack declares as the game's default, one the enum does not name.
    private static final DeclaredFontAtlas PACK_DECLARED_DEFAULT = new DeclaredFontAtlas(
        "graphics/fonts/pack_script15.fnt",
        AtlasSmoothing.SMOOTHED);

    private static GlyphCoverageReaderFake createLocalisedCoverage() {
        return GlyphCoverageReaderFake.createLatinOnlyCoverage()
            .coveringEveryCharacter(StarsectorFont.VANILLA_INSIGNIA_25)
            .coveringEveryCharacter(StarsectorFont.VANILLA_INSIGNIA_21)
            .coveringEveryCharacter(StarsectorFont.VANILLA_INSIGNIA_15);
    }

    private static FaceResolver createLocalisedResolver() {
        return new FaceResolver(
            FaceLineHeightReaderFake.createVanillaLineHeights(),
            createLocalisedCoverage(),
            VANILLA_DECLARED_DEFAULT);
    }

    private static FaceResolver createLocalisedResolverWithUnloadable(StarsectorFont... unloadableFonts) {

        var lineHeightsFake = FaceLineHeightReaderFake.createVanillaLineHeights();
        for (var unloadableFont : unloadableFonts) {
            lineHeightsFake = lineHeightsFake.answeringLineHeight(unloadableFont, UNLOADABLE);
        }
        return new FaceResolver(lineHeightsFake, createLocalisedCoverage(), VANILLA_DECLARED_DEFAULT);
    }

    // A vanilla install whose settings a language pack has pointed at its own face, which holds the pack's
    // script where no vanilla atlas does.
    private static FaceResolver createPackDeclaredResolver(double packLineHeight) {
        return new FaceResolver(
            FaceLineHeightReaderFake.createVanillaLineHeights()
                .answeringLineHeight(PACK_DECLARED_DEFAULT, packLineHeight),
            GlyphCoverageReaderFake.createLatinOnlyCoverage().coveringEveryCharacter(PACK_DECLARED_DEFAULT),
            PACK_DECLARED_DEFAULT);
    }

    @Nested
    class ResolveFont {

        @Test
        void keepsTheFaceAskedForWhereItsAtlasHoldsTheText() {
            // An English build's names on a localised install: the high-resolution atlas holds them, so
            // nothing moves the text off it.
            assertThat(createLocalisedResolver().resolveFont(StarsectorFont.VANILLA_INSIGNIA_42, List.of("Hegemony")))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_42);
        }

        @Test
        void stepsDownToTheLargestCutThatHoldsTheText() {
            // The row of question marks, answered: the next cut down holds the script, so the walk stops
            // there rather than dropping further than it has to.
            assertThat(createLocalisedResolver().resolveFont(
                    StarsectorFont.VANILLA_INSIGNIA_42,
                    List.of(LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }

        @Test
        void stepsDownWhenAnyOneOfTheTextsNeedsAGlyphTheFaceLacks() {
            // A caller settles one face for everything it draws, so one localised name among Latin ones
            // moves them all.
            assertThat(createLocalisedResolver().resolveFont(
                    StarsectorFont.VANILLA_INSIGNIA_42,
                    List.of("Hegemony", LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }

        @Test
        void passesOverACutThatWillNotLoad() {

            var resolver = createLocalisedResolverWithUnloadable(StarsectorFont.VANILLA_INSIGNIA_25);

            assertThat(resolver.resolveFont(StarsectorFont.VANILLA_INSIGNIA_42, List.of(LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_21);
        }

        @Test
        void stepsDownWhenTheFaceAskedForWillNotLoadEvenForTextItWouldHold() {
            // A face that will not load draws nothing, however little it would have had to draw.
            var resolver = createLocalisedResolverWithUnloadable(StarsectorFont.VANILLA_INSIGNIA_42);

            assertThat(resolver.resolveFont(StarsectorFont.VANILLA_INSIGNIA_42, List.of("Hegemony")))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }

        @Test
        void fallsToTheDeclaredDefaultFromAFaceWithNoSmallerCut() {
            // A pixel face lacking the script has no smaller cut of its own design to try.
            assertThat(createLocalisedResolver().resolveFont(StarsectorFont.VANILLA_VICTOR_10, List.of(LOCALISED_NAME)))
                .isEqualTo(VANILLA_DECLARED_DEFAULT);
        }

        @Test
        void reachesADeclaredDefaultTheEnumDoesNotNameWhereItHoldsTheText() {
            // The language pack's case: no vanilla atlas holds the script, and the face the pack declared is
            // the one that does - reached by its path, since the enum has never heard of it.
            assertThat(createPackDeclaredResolver(16d).resolveFont(
                    StarsectorFont.VANILLA_INSIGNIA_42,
                    List.of(LOCALISED_NAME)))
                .isEqualTo(PACK_DECLARED_DEFAULT);
        }

        @Test
        void answersTheLastResortWhereTheDeclaredDefaultWillNotLoad() {
            // A setting naming a broken or missing file is passed over, and the walk ends on KMLib's own face.
            assertThat(createPackDeclaredResolver(UNLOADABLE).resolveFont(
                    StarsectorFont.VANILLA_INSIGNIA_42,
                    List.of(LOCALISED_NAME)))
                .isEqualTo(FaceResolver.LAST_RESORT_FONT);
        }

        @Test
        void answersTheLastResortRatherThanTheDeclaredDefaultWhereNothingHoldsTheText() {
            // A localised build on a vanilla install with a mod's declared default lacking the script too:
            // nothing on the walk draws it, and the answer is KMLib's own face, never the setting's.
            var resolver = new FaceResolver(
                FaceLineHeightReaderFake.createVanillaLineHeights()
                    .answeringLineHeight(PACK_DECLARED_DEFAULT, 16d),
                GlyphCoverageReaderFake.createLatinOnlyCoverage(),
                PACK_DECLARED_DEFAULT);

            assertThat(resolver.resolveFont(StarsectorFont.VANILLA_INSIGNIA_42, List.of(LOCALISED_NAME)))
                .isEqualTo(FaceResolver.LAST_RESORT_FONT);
        }

        @Test
        void answersTheLastResortWhereNoFaceOnTheWalkLoads() {
            // Never no face: the last resort is answered even where it will not load itself, drawing nothing
            // - as the face asked for would have.
            var resolver = createLocalisedResolverWithUnloadable(
                StarsectorFont.VANILLA_INSIGNIA_42,
                StarsectorFont.VANILLA_INSIGNIA_25,
                StarsectorFont.VANILLA_INSIGNIA_21,
                StarsectorFont.VANILLA_INSIGNIA_15);

            assertThat(resolver.resolveFont(StarsectorFont.VANILLA_INSIGNIA_42, List.of("Hegemony")))
                .isEqualTo(FaceResolver.LAST_RESORT_FONT);
        }
    }

    @Nested
    class ListFallbackWalk {

        @Test
        void stepsDownTheFamilyAndTriesEachFaceOnce() {
            // The family's smallest cut is both the vanilla declared default and the last resort here, so it
            // is tried once, not three times.
            assertThat(createLocalisedResolver().listFallbackWalk(StarsectorFont.VANILLA_INSIGNIA_42))
                .containsExactly(
                    StarsectorFont.VANILLA_INSIGNIA_42,
                    StarsectorFont.VANILLA_INSIGNIA_25,
                    StarsectorFont.VANILLA_INSIGNIA_21,
                    StarsectorFont.VANILLA_INSIGNIA_15);
        }

        @Test
        void goesStraightToTheDefaultsFromAFaceWithNoSmallerCut() {

            assertThat(createLocalisedResolver().listFallbackWalk(StarsectorFont.VANILLA_ORBITRON_20AA))
                .containsExactly(StarsectorFont.VANILLA_ORBITRON_20AA, StarsectorFont.VANILLA_INSIGNIA_15);
        }

        @Test
        void triesTheDeclaredDefaultBeforeTheLastResort() {
            // The declared face is the install's choice and is tried as such; KMLib's own face comes after it
            // whatever the setting names.
            assertThat(createPackDeclaredResolver(16d).listFallbackWalk(StarsectorFont.VANILLA_VICTOR_10))
                .containsExactly(
                    StarsectorFont.VANILLA_VICTOR_10,
                    PACK_DECLARED_DEFAULT,
                    FaceResolver.LAST_RESORT_FONT);
        }

        @Test
        void isTheLastResortAloneWhereItIsAskedFor() {

            assertThat(createLocalisedResolver().listFallbackWalk(StarsectorFont.VANILLA_INSIGNIA_15))
                .containsExactly(StarsectorFont.VANILLA_INSIGNIA_15);
        }
    }
}
