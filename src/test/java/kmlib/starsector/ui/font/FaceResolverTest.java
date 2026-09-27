package kmlib.starsector.ui.font;

import kmlib.testfixtures.starsector.ui.font.FaceLineHeightReaderFake;
import kmlib.testfixtures.starsector.ui.font.GlyphCoverageReaderFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how a category's face is settled from the player's pick and the install: the automatic choice
 * keeping the preferred face until the text needs another, falling to the largest face that covers it by
 * the installed line height, and never landing on a face that will not load while one that does is
 * offered.
 *
 * <p>The install is posed as vanilla or as a localised one: vanilla atlases hold Latin-1 alone, while a
 * localised install replaces the two smaller insignia atlases with taller ones holding its script and
 * leaves the high-resolution one untouched - the arrangement that drew KMU's map labels as question marks.
 */
class FaceResolverTest {

    // "Hegemony" (U+9738 U+4E3B), a faction name as a localised install draws it.
    private static final String LOCALISED_NAME = "霸主";

    // The line heights a localised install's replaced atlases state - its master edition's.
    private static final double LOCALISED_INSIGNIA_25_LINE_HEIGHT = 25d;
    private static final double LOCALISED_INSIGNIA_15_LINE_HEIGHT = 17d;

    // What the reader states for a face the install cannot load.
    private static final double UNLOADABLE = 0d;

    // A map label's offer: the high-resolution atlas preferred, the two insignia cuts behind it, the
    // larger listed first.
    private static final FaceOffer LABEL_OFFER = new FaceOffer(
        StarsectorFont.VANILLA_INSIGNIA_42,
        List.of(StarsectorFont.VANILLA_INSIGNIA_25, StarsectorFont.VANILLA_INSIGNIA_15));

    private static FaceLineHeightReaderFake createLocalisedLineHeights() {

        return FaceLineHeightReaderFake.createVanillaLineHeights()
            .answeringLineHeight(StarsectorFont.VANILLA_INSIGNIA_25, LOCALISED_INSIGNIA_25_LINE_HEIGHT)
            .answeringLineHeight(StarsectorFont.VANILLA_INSIGNIA_15, LOCALISED_INSIGNIA_15_LINE_HEIGHT);
    }

    private static GlyphCoverageReaderFake createLocalisedCoverage() {

        return GlyphCoverageReaderFake.createLatinOnlyCoverage()
            .coveringEveryCharacter(StarsectorFont.VANILLA_INSIGNIA_25)
            .coveringEveryCharacter(StarsectorFont.VANILLA_INSIGNIA_15);
    }

    private static FaceResolver createLocalisedResolver() {
        return new FaceResolver(createLocalisedLineHeights(), createLocalisedCoverage());
    }

    @Nested
    class ResolveFont {

        @Test
        void resolveFontKeepsThePreferredFaceWhenItsAtlasCoversTheText() {
            // An English build's names on a localised install: the high-resolution atlas holds them, so
            // nothing moves the category off it.
            assertThat(createLocalisedResolver().resolveFont(LABEL_OFFER, FaceChoice.AUTO_FACE, List.of("Hegemony")))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_42);
        }

        @Test
        void resolveFontFallsToTheLargestCoveringFaceWhenThePreferredAtlasLacksAGlyph() {
            // The row of question marks, answered: the preferred atlas lacks the script, and of the two
            // that hold it the one rasterised larger survives being stretched better.
            assertThat(createLocalisedResolver().resolveFont(
                    LABEL_OFFER,
                    FaceChoice.AUTO_FACE,
                    List.of(LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }

        @Test
        void resolveFontWeighsTheFacesByTheLineHeightTheirInstalledAtlasesState() {
            // An edition whose larger cut came out shorter than its smaller one: largest means the atlas
            // installed, not the size the basename suggests.
            var lineHeightsFake = createLocalisedLineHeights()
                .answeringLineHeight(StarsectorFont.VANILLA_INSIGNIA_25, 14d);

            var resolver = new FaceResolver(lineHeightsFake, createLocalisedCoverage());

            assertThat(resolver.resolveFont(LABEL_OFFER, FaceChoice.AUTO_FACE, List.of(LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_15);
        }

        @Test
        void resolveFontFallsWhenAnyOneOfTheTextsNeedsAGlyphThePreferredAtlasLacks() {
            // A category draws every name at once, so one localised name among Latin ones moves them all.
            assertThat(createLocalisedResolver().resolveFont(
                    LABEL_OFFER,
                    FaceChoice.AUTO_FACE,
                    List.of("Hegemony", LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }

        @Test
        void resolveFontPassesOverACoveringFaceThatWillNotLoad() {

            var lineHeightsFake = createLocalisedLineHeights()
                .answeringLineHeight(StarsectorFont.VANILLA_INSIGNIA_25, UNLOADABLE);

            var resolver = new FaceResolver(lineHeightsFake, createLocalisedCoverage());

            assertThat(resolver.resolveFont(LABEL_OFFER, FaceChoice.AUTO_FACE, List.of(LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_15);
        }

        @Test
        void resolveFontLeavesAPreferredFaceThatWillNotLoadEvenForTextItWouldCover() {
            // A face that will not load draws nothing, however little it would have had to draw.
            var lineHeightsFake = createLocalisedLineHeights()
                .answeringLineHeight(StarsectorFont.VANILLA_INSIGNIA_42, UNLOADABLE);

            var resolver = new FaceResolver(lineHeightsFake, createLocalisedCoverage());

            assertThat(resolver.resolveFont(LABEL_OFFER, FaceChoice.AUTO_FACE, List.of("Hegemony")))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }

        @Test
        void resolveFontKeepsThePreferredFaceWhenNoOfferedFaceCoversTheText() {
            // A localised build on a vanilla install: every atlas lacks the script, and gaps in the face
            // meant for the job read no worse than the same gaps in another.
            var resolver = new FaceResolver(
                FaceLineHeightReaderFake.createVanillaLineHeights(),
                GlyphCoverageReaderFake.createLatinOnlyCoverage());

            assertThat(resolver.resolveFont(LABEL_OFFER, FaceChoice.AUTO_FACE, List.of(LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_42);
        }

        @Test
        void resolveFontTakesTheFirstLoadableFaceWhenNothingCoversAndThePreferredWillNotLoad() {

            var lineHeightsFake = FaceLineHeightReaderFake.createVanillaLineHeights()
                .answeringLineHeight(StarsectorFont.VANILLA_INSIGNIA_42, UNLOADABLE);

            var resolver = new FaceResolver(lineHeightsFake, GlyphCoverageReaderFake.createLatinOnlyCoverage());

            assertThat(resolver.resolveFont(LABEL_OFFER, FaceChoice.AUTO_FACE, List.of(LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }

        @Test
        void resolveFontAnswersThePreferredFaceWhenNoOfferedFaceLoads() {
            // Never no face: the category keeps drawing in what it always drew in, which draws nothing -
            // as it did before any of this was read.
            var lineHeightsFake = FaceLineHeightReaderFake.createVanillaLineHeights()
                .answeringLineHeight(StarsectorFont.VANILLA_INSIGNIA_42, UNLOADABLE)
                .answeringLineHeight(StarsectorFont.VANILLA_INSIGNIA_25, UNLOADABLE)
                .answeringLineHeight(StarsectorFont.VANILLA_INSIGNIA_15, UNLOADABLE);

            var resolver = new FaceResolver(lineHeightsFake, createLocalisedCoverage());

            assertThat(resolver.resolveFont(LABEL_OFFER, FaceChoice.AUTO_FACE, List.of("Hegemony")))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_42);
        }

        @Test
        void resolveFontHonoursANamedFaceEvenWhereItsAtlasLacksAGlyph() {
            // The player's pick is theirs: the probe steers only the automatic choice.
            var namedChoice = new FaceChoice.NamedFace(StarsectorFont.VANILLA_INSIGNIA_42);

            assertThat(createLocalisedResolver().resolveFont(LABEL_OFFER, namedChoice, List.of(LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_42);
        }

        @Test
        void resolveFontResolvesANamedFaceThatWillNotLoadAsTheAutomaticChoice() {

            var lineHeightsFake = createLocalisedLineHeights()
                .answeringLineHeight(StarsectorFont.VANILLA_INSIGNIA_15, UNLOADABLE);

            var resolver = new FaceResolver(lineHeightsFake, createLocalisedCoverage());
            var namedChoice = new FaceChoice.NamedFace(StarsectorFont.VANILLA_INSIGNIA_15);

            assertThat(resolver.resolveFont(LABEL_OFFER, namedChoice, List.of(LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }

        @Test
        void resolveFontResolvesANamedFaceTheCategoryDoesNotOfferAsTheAutomaticChoice() {
            // A stored pick outliving the option that offered it must not pin the category to a face
            // wrong for its job.
            var namedChoice = new FaceChoice.NamedFace(StarsectorFont.VANILLA_VICTOR_10);

            assertThat(createLocalisedResolver().resolveFont(LABEL_OFFER, namedChoice, List.of("Hegemony")))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_42);
        }
    }
}
