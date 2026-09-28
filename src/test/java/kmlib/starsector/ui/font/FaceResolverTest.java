package kmlib.starsector.ui.font;

import kmlib.testfixtures.starsector.ui.font.FaceLineHeightReaderFake;
import kmlib.testfixtures.starsector.ui.font.GlyphCoverageReaderFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how a category's face is settled from the player's pick and the install: the automatic choice
 * keeping the preferred face until the text needs a glyph it lacks, taking the one fallback where that
 * face holds the text, and never landing on a face that will not load while one that does is offered.
 *
 * <p>The install is posed as vanilla or as a localised one: vanilla atlases hold Latin-1 alone, while a
 * localised install replaces the two smaller insignia atlases with ones holding its script and leaves the
 * high-resolution one untouched - the arrangement that drew KMU's map labels as question marks.
 */
class FaceResolverTest {

    // "Hegemony" (U+9738 U+4E3B), a faction name as a localised install draws it.
    private static final String LOCALISED_NAME = "霸主";

    // What the reader states for a face the install cannot load.
    private static final double UNLOADABLE = 0d;

    // A map label's offer: the high-resolution atlas preferred, the larger insignia cut behind it.
    private static final FaceOffer LABEL_OFFER = FaceOffer.createOfferFallingBackTo(
        StarsectorFont.VANILLA_INSIGNIA_42,
        StarsectorFont.VANILLA_INSIGNIA_25);

    // A category with no fallback, whose preferred face is one a localised install replaces.
    private static final FaceOffer BODY_OFFER = FaceOffer.createOfferWithoutFallback(
        StarsectorFont.VANILLA_INSIGNIA_15);

    private static GlyphCoverageReaderFake createLocalisedCoverage() {
        return GlyphCoverageReaderFake.createLatinOnlyCoverage()
            .coveringEveryCharacter(StarsectorFont.VANILLA_INSIGNIA_25)
            .coveringEveryCharacter(StarsectorFont.VANILLA_INSIGNIA_15);
    }

    private static FaceResolver createLocalisedResolver() {
        return new FaceResolver(FaceLineHeightReaderFake.createVanillaLineHeights(), createLocalisedCoverage());
    }

    private static FaceResolver createResolverWithUnloadable(StarsectorFont... unloadableFonts) {

        var lineHeightsFake = FaceLineHeightReaderFake.createVanillaLineHeights();
        for (var unloadableFont : unloadableFonts) {
            lineHeightsFake = lineHeightsFake.answeringLineHeight(unloadableFont, UNLOADABLE);
        }
        return new FaceResolver(lineHeightsFake, createLocalisedCoverage());
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
        void resolveFontTakesTheFallbackWhenThePreferredAtlasLacksAGlyphTheFallbackHolds() {
            // The row of question marks, answered.
            assertThat(createLocalisedResolver().resolveFont(
                    LABEL_OFFER,
                    FaceChoice.AUTO_FACE,
                    List.of(LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
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
        void resolveFontKeepsThePreferredFaceWhenTheFallbackLacksTheTextToo() {
            // A localised build on a vanilla install: no atlas holds the script, and gaps in the face
            // meant for the job read no worse than the same gaps in another.
            var resolver = new FaceResolver(
                FaceLineHeightReaderFake.createVanillaLineHeights(),
                GlyphCoverageReaderFake.createLatinOnlyCoverage());

            assertThat(resolver.resolveFont(LABEL_OFFER, FaceChoice.AUTO_FACE, List.of(LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_42);
        }

        @Test
        void resolveFontKeepsThePreferredFaceOfACategoryWithNoFallback() {

            var resolver = new FaceResolver(
                FaceLineHeightReaderFake.createVanillaLineHeights(),
                GlyphCoverageReaderFake.createLatinOnlyCoverage());

            assertThat(resolver.resolveFont(BODY_OFFER, FaceChoice.AUTO_FACE, List.of(LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_15);
        }

        @Test
        void resolveFontPassesOverACoveringFallbackThatWillNotLoad() {

            var resolver = createResolverWithUnloadable(StarsectorFont.VANILLA_INSIGNIA_25);

            assertThat(resolver.resolveFont(LABEL_OFFER, FaceChoice.AUTO_FACE, List.of(LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_42);
        }

        @Test
        void resolveFontTakesTheFallbackWhenThePreferredFaceWillNotLoadEvenForTextItWouldCover() {
            // A face that will not load draws nothing, however little it would have had to draw.
            var resolver = createResolverWithUnloadable(StarsectorFont.VANILLA_INSIGNIA_42);

            assertThat(resolver.resolveFont(LABEL_OFFER, FaceChoice.AUTO_FACE, List.of("Hegemony")))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }

        @Test
        void resolveFontAnswersThePreferredFaceWhenNeitherFaceLoads() {
            // Never no face: the category keeps drawing in what it always drew in, which draws nothing -
            // as it did before any of this was read.
            var resolver = createResolverWithUnloadable(
                StarsectorFont.VANILLA_INSIGNIA_42,
                StarsectorFont.VANILLA_INSIGNIA_25);

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
        void resolveFontHonoursANamedFaceOutsideTheCategorysOffer() {
            // Every face is on every Radio, so a pick is not held to where the automatic choice may go.
            var namedChoice = new FaceChoice.NamedFace(StarsectorFont.VANILLA_VICTOR_10);

            assertThat(createLocalisedResolver().resolveFont(LABEL_OFFER, namedChoice, List.of("Hegemony")))
                .isEqualTo(StarsectorFont.VANILLA_VICTOR_10);
        }

        @Test
        void resolveFontResolvesANamedFaceThatWillNotLoadAsTheAutomaticChoice() {

            var resolver = createResolverWithUnloadable(StarsectorFont.VANILLA_VICTOR_10);
            var namedChoice = new FaceChoice.NamedFace(StarsectorFont.VANILLA_VICTOR_10);

            assertThat(resolver.resolveFont(LABEL_OFFER, namedChoice, List.of(LOCALISED_NAME)))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }
    }
}
