package kmlib.starsector.ui.font;

import kmlib.testfixtures.starsector.ui.font.FontDescriptorHeader;
import kmlib.testfixtures.starsector.ui.font.InstalledFonts;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Holds every install this machine was handed to what KM text needs of its atlases: the build's own, and
 * any others {@code -PfontInstallRoots} names - which is how a machine holding the core localisation's
 * editions checks each of them. They are checked where they are installed rather than recorded and
 * checked elsewhere, since a recording goes on passing after the localisation publishes a pack that no
 * longer matches it. A CI runner carries the game's JARs and no fonts, so this suite skips there: every
 * install it checks is on a machine holding the full game.
 *
 * <p>Two things are asked of every install. That each face KM text may draw in is present in a shape
 * LazyLib and the game both load. And that a map label - the one category whose preferred atlas no
 * edition replaces - lands on a face that draws a localised faction name, which is the row of question
 * marks the automatic choice exists to end, settled here against real atlases rather than posed ones.
 *
 * <p>Walks the enum, never the fonts folder: an install running a localisation's dynamic-font pipeline
 * writes generated descriptors and a subfolder of its own into that folder, and a listing would take them
 * for faces KM offers.
 *
 * <p>Skips where it was handed no install carrying the game's fonts.
 */
class InstalledFontsIntegrationTest {

    // The installs to check, as the build hands them.
    private static final String STARSECTOR_ROOT_PROPERTY = "kmlib.starsectorRoot";
    private static final String INSTALL_ROOTS_PROPERTY = "kmlib.fontInstallRoots";

    // Where an install's font descriptors live, whose presence is what makes a root one this suite can
    // check. The core folder alone is not enough: a CI runner carries the game's JARs under it and nothing
    // else, and a suite taking that for an install would fail on every face it lacks.
    private static final String FONTS_DIRECTORY = "starsector-core/graphics/fonts";

    // "Hegemony" (U+9738 U+4E3B), a faction name as a localised install draws it.
    private static final String LOCALISED_NAME = "霸主";

    // The first code point of the CJK blocks, from the radicals supplement up. Below it are the Latin,
    // Greek and symbol ranges a localisation leaves to the game's own glyphs.
    private static final int FIRST_CJK_CODE_POINT = 0x2E80;

    // A map label's offer: the high-resolution atlas preferred, the larger insignia cut behind it.
    private static final FaceOffer MAP_LABEL_OFFER = FaceOffer.createOfferFallingBackTo(
        StarsectorFont.VANILLA_INSIGNIA_42,
        StarsectorFont.VANILLA_INSIGNIA_25);

    @Nested
    class ReadInstall {

        @Test
        void readInstallFindsEveryFaceOnEveryInstallInAShapeLazyLibAndTheGameBothLoad() {
            for (var installedFonts : readEveryHandedInstall()) {

                var edition = installedFonts.describeEdition();

                for (var font : StarsectorFont.values()) {
                    var face = installedFonts.faceByFont().get(font);

                    assertThat(face)
                        .as("%s on %s", font.getBasename(), edition)
                        .isNotNull();
                    assertThat(face.headerTokenCount())
                        .as("header token count of %s on %s", font.getBasename(), edition)
                        .isEqualTo(FontDescriptorHeader.LAZYFONT_HEADER_TOKEN_COUNT);
                    assertThat(face.pageCount())
                        .as("page count of %s on %s", font.getBasename(), edition)
                        .isEqualTo(1);
                    assertThat(face.lineHeight())
                        .as("line height of %s on %s", font.getBasename(), edition)
                        .isPositive();
                }
            }
        }

        @Test
        void readInstallFindsTheLargerInsigniaHoldingEveryCjkGlyphTheBodyInsigniaHolds() {
            // What makes the larger cut a sound fallback for text the body face draws. Not every glyph:
            // on several editions it lacks a Greek mu and the euro sign the body cut carries, both below
            // the CJK blocks and neither drawn in a faction name.
            for (var installedFonts : readEveryHandedInstall()) {

                var largerGlyphIds = installedFonts.faceByFont().get(StarsectorFont.VANILLA_INSIGNIA_25).glyphIds();
                var bodyGlyphIds = installedFonts.faceByFont().get(StarsectorFont.VANILLA_INSIGNIA_15).glyphIds();
                var missingGlyphIds = largerGlyphIds.listIdsMissingFrom(bodyGlyphIds);

                assertThat(missingGlyphIds.idRanges())
                    .as("glyphs %s's insignia15LTaa holds and insignia25LTaa lacks: %s",
                        installedFonts.describeEdition(), missingGlyphIds.formatRanges())
                    .allSatisfy(idRange -> assertThat(idRange.lastId()).isLessThan(FIRST_CJK_CODE_POINT));
            }
        }
    }

    @Nested
    class ResolveFont {

        @Test
        void resolveFontKeepsTheHighResolutionAtlasForALatinNameOnEveryInstall() {
            for (var installedFonts : readEveryHandedInstall()) {

                assertThat(createResolverOver(installedFonts)
                        .resolveFont(MAP_LABEL_OFFER, FaceChoice.AUTO_FACE, List.of("Hegemony")))
                    .as("the map label face for a Latin name on %s", installedFonts.describeEdition())
                    .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_42);
            }
        }

        @Test
        void resolveFontFallsToTheLargerInsigniaForALocalisedNameOnEveryLocalisedInstall() {
            // The high-resolution atlas is the one no edition replaces, so it draws the name as question
            // marks; the larger insignia cut holds the name on every edition.
            var localisedInstalls = readEveryHandedInstall().stream()
                .filter(installedFonts -> !installedFonts.edition().equals(InstalledFonts.VANILLA_EDITION))
                .toList();
            assumeFalse(localisedInstalls.isEmpty(), "No localised install was handed");

            for (var installedFonts : localisedInstalls) {

                assertThat(createResolverOver(installedFonts)
                        .resolveFont(MAP_LABEL_OFFER, FaceChoice.AUTO_FACE, List.of(LOCALISED_NAME)))
                    .as("the map label face for a localised name on %s", installedFonts.describeEdition())
                    .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
            }
        }
    }

    private static FaceResolver createResolverOver(InstalledFonts installedFonts) {
        return new FaceResolver(installedFonts.createLineHeightReader(), installedFonts.createGlyphCoverageReader());
    }

    // Every install handed, the build's own first, each once and each actually holding an install - or a
    // skip where there is none.
    private static List<InstalledFonts> readEveryHandedInstall() {

        var namedRoots = new ArrayList<String>();

        namedRoots.add(System.getProperty(STARSECTOR_ROOT_PROPERTY, ""));
        namedRoots.addAll(List.of(System.getProperty(INSTALL_ROOTS_PROPERTY, "").split(File.pathSeparator)));

        var installRoots = namedRoots.stream()
            .filter(root -> !root.isBlank())
            .map(root -> Path.of(root).toAbsolutePath().normalize())
            .filter(root -> Files.isDirectory(root.resolve(FONTS_DIRECTORY)))
            .distinct()
            .toList();
        assumeFalse(installRoots.isEmpty(), "No install carrying the game's fonts was handed");

        return installRoots.stream()
            .map(InstalledFonts::readInstall)
            .toList();
    }
}
