package kmlib.starsector.ui.font.installed;

import kmlib.starsector.ui.font.StarsectorFont;

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
 * editions checks each of them. A CI runner carries the game's JARs and no fonts, so this suite skips
 * there: every install it checks is one holding the full game.
 *
 * <p>Two things are asked of every install, over the faces KM draws in and the cuts they fall back to -
 * the enum, which names both. That each is present in a shape LazyLib and the game both load, with the
 * smoothing the enum states for it. And that
 * every face KM may ask for settles, walking down its family to the install's own default, on one that
 * draws a localised faction name - which is the row of question marks the walk exists to end, settled
 * here against real atlases rather than posed ones.
 *
 * <p>Walks the enum, never the fonts folder: an install running a localisation's dynamic-font pipeline
 * writes generated descriptors and a subfolder of its own into that folder, and a listing would take them
 * for faces KM draws in.
 *
 * <p>Skips where it was handed no install carrying the game's fonts.
 */
class InstalledFontsIntegrationTests {

    // The installs to check, as the build hands them.
    private static final String STARSECTOR_ROOT_PROPERTY = "kmlib.starsectorRoot";
    private static final String INSTALL_ROOTS_PROPERTY = "kmlib.fontInstallRoots";

    // Where an install's font descriptors live, whose presence is what makes a root one this suite can
    // check. The core folder alone is not enough: a CI runner carries the game's JARs under it and nothing
    // else, and a suite taking that for an install would fail on every face it lacks.
    private static final String FONTS_DIRECTORY = "starsector-core/graphics/fonts";

    // "Hegemony" (U+9738 U+4E3B), a faction name as a localised install draws it.
    private static final String LOCALISED_NAME = "霸主";

    @Nested
    class ReadInstall {

        @Test
        void findsEveryFaceOnEveryInstallInAShapeLazyLibAndTheGameBothLoad() {
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

                    // The enum states each face's smoothing rather than reading it, so an edition turning
                    // a face pixel-exact, or the reverse, would draw it through the wrong filter unseen.
                    assertThat(face.smoothing())
                        .as("smoothing of %s on %s", font.getBasename(), edition)
                        .isEqualTo(font.getSmoothing());
                }
            }
        }
    }

    @Nested
    class ResolveFont {

        @Test
        void keepsEveryFaceForALatinNameOnEveryInstall() {
            // Nothing a Latin name needs is missing from any face on any install, so no walk moves.
            for (var installedFonts : readEveryHandedInstall()) {

                var resolver = installedFonts.createFaceResolver();

                for (var font : StarsectorFont.values()) {
                    assertThat(resolver.resolveFont(font, List.of("Hegemony")))
                        .as("the face %s settles on for a Latin name on %s",
                            font.getBasename(), installedFonts.describeEdition())
                        .isEqualTo(font);
                }
            }
        }

        @Test
        void settlesEveryFaceOnOneDrawingALocalisedNameOnEveryLocalisedInstall() {
            // Wherever the walk ends, the face it ends on has to hold the name: a walk running out onto a
            // default that lacks it would draw the same question marks it set out to avoid.
            for (var installedFonts : readEveryLocalisedInstall()) {

                var resolver = installedFonts.createFaceResolver();
                var coverage = installedFonts.createGlyphCoverageReader();

                for (var font : StarsectorFont.values()) {
                    var settledAtlas = resolver.resolveFont(font, List.of(LOCALISED_NAME));

                    assertThat(coverage.coversText(settledAtlas, LOCALISED_NAME))
                        .as("%s, which %s settles on for a localised name on %s, holds the name",
                            settledAtlas.resolvePath(), font.getBasename(), installedFonts.describeEdition())
                        .isTrue();
                }
            }
        }

        @Test
        void stepsTheHighResolutionAtlasDownOneCutForALocalisedNameOnEveryLocalisedInstall() {
            // The one face no edition replaces, and the case the walk exists for: the next cut down holds
            // the script on every edition, so a map label drops no further than it has to.
            for (var installedFonts : readEveryLocalisedInstall()) {

                assertThat(installedFonts.createFaceResolver()
                        .resolveFont(StarsectorFont.VANILLA_INSIGNIA_42, List.of(LOCALISED_NAME)))
                    .as("the face insignia42LTaa settles on for a localised name on %s",
                        installedFonts.describeEdition())
                    .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
            }
        }
    }

    // Every handed install a core localisation has been laid over - or a skip where there is none.
    private static List<InstalledFonts> readEveryLocalisedInstall() {

        var localisedInstalls = readEveryHandedInstall().stream()
            .filter(installedFonts -> !installedFonts.edition().equals(InstalledFonts.VANILLA_EDITION))
            .toList();
        assumeFalse(localisedInstalls.isEmpty(), "No localised install was handed");

        return localisedInstalls;
    }

    // Every install handed, the build's own first, each once and each actually carrying the game's fonts -
    // or a skip where there is none.
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
            .map(InstalledFontsReader::readInstall)
            .toList();
    }
}
