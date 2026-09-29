package kmlib.gradlescripts.integrationtests;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the font-edition gate against a real Gradle build and a local repository standing in for the
 * upstream localisation: one edition branch shipping one of two faces, over a folder of the game's own
 * descriptors.
 *
 * <p>What the gate has to get right is what a player's install would hold - the edition's descriptor
 * where it ships one and the game's own where it does not - and that every way upstream can part from the
 * lock fails the build naming the edition, the face and both SHAs, since a gate that passed on a moved
 * pack would report an edition as checked when it was not.
 */
final class CheckFontEditionsIntegrationTests {

    private static final String GATE_SCRIPT =
        new File("gradle/tasks/checks/check-font-editions.gradle").getAbsolutePath();

    private static final String EDITION = "font-alpha";
    private static final String FONTS_PATH = "localization/graphics/fonts";

    // Two faces: the edition ships the first and leaves the second to the game.
    private static final String SHIPPED_FACE = "faceA";
    private static final String ABSENT_FACE = "faceB";

    private static final String EDITION_DESCRIPTOR = "info face=\"Alpha\" aa=4";
    private static final String VANILLA_SHIPPED_DESCRIPTOR = "info face=\"Vanilla A\" aa=4";
    private static final String VANILLA_ABSENT_DESCRIPTOR = "info face=\"Vanilla B\" aa=1";

    private static final String EDITION_FONTS_PATH = "build/font-editions/roots/" + EDITION
        + "/starsector-core/graphics/fonts/";

    /** The pieces a case poses: the upstream repository, the game's descriptors, and the build. */
    private record Workspace(
        Path upstreamDirectory,
        Path vanillaFontsDirectory,
        Path projectDirectory) {
    }

    // Runs git in a directory, failing the case with git's own reason rather than a bare exit code.
    private static String runGit(Path directory, String... arguments) throws IOException {

        var command = new ArrayList<>(List.of(
            "git",
            "-C",
            directory.toString(),
            "-c",
            "user.name=Upstream",
            "-c",
            "user.email=upstream@example.invalid"));

        command.addAll(List.of(arguments));

        var process = new ProcessBuilder(command)
            .redirectErrorStream(true)
            .start();

        var output = new String(
            process.getInputStream().readAllBytes(),
            StandardCharsets.UTF_8);

        try {

            if (process.waitFor() != 0) {
                throw new IllegalStateException("git " + String.join(" ", arguments) + " failed: " + output);
            }
        } catch (InterruptedException interrupted) {

            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
        return output.trim();
    }

    // Commits a descriptor onto the edition branch upstream.
    private static void commitDescriptor(Path upstreamDirectory, String face, String content) throws IOException {

        var descriptor = upstreamDirectory.resolve(FONTS_PATH).resolve(face + ".fnt");

        Files.createDirectories(descriptor.getParent());
        Files.writeString(descriptor, content);

        runGit(upstreamDirectory, "add", "--all");
        runGit(upstreamDirectory, "commit", "--quiet", "-m", "Descriptor " + face);
    }

    // The blob SHA upstream holds for a face on the edition branch.
    private static String readBlobSha(Path upstreamDirectory, String face) throws IOException {
        return runGit(upstreamDirectory, "rev-parse", EDITION + ":" + FONTS_PATH + "/" + face + ".fnt");
    }

    /**
     * An upstream whose edition branch ships the first face, the game's descriptors for both, and a build
     * applying the gate with those two faces as the enum's.
     */
    private static Workspace writeWorkspace(Path root) throws IOException {

        var upstreamDirectory = Files.createDirectories(root.resolve("upstream"));

        runGit(upstreamDirectory, "init", "--quiet");
        runGit(upstreamDirectory, "checkout", "--quiet", "-b", EDITION);

        // A partial clone reads its blobs by SHA when it first needs them, which the stand-in has to
        // allow as the real host does.
        runGit(upstreamDirectory, "config", "uploadpack.allowFilter", "true");
        runGit(upstreamDirectory, "config", "uploadpack.allowAnySHA1InWant", "true");

        commitDescriptor(upstreamDirectory, SHIPPED_FACE, EDITION_DESCRIPTOR);

        var vanillaFontsDirectory = Files.createDirectories(root.resolve("vanilla-fonts"));

        Files.writeString(vanillaFontsDirectory.resolve(SHIPPED_FACE + ".fnt"), VANILLA_SHIPPED_DESCRIPTOR);
        Files.writeString(vanillaFontsDirectory.resolve(ABSENT_FACE + ".fnt"), VANILLA_ABSENT_DESCRIPTOR);

        var projectDirectory = Files.createDirectories(root.resolve("kmlib"));

        Files.writeString(projectDirectory.resolve("settings.gradle"), "rootProject.name = 'kmlib'");
        Files.writeString(
            projectDirectory.resolve("build.gradle"),
            String.join(
                "\n",
                "ext.fontEditionsLockFile = file('font-editions.lock.json')",
                "ext.vanillaFontsDirectory = file('" + toGradlePath(vanillaFontsDirectory) + "')",
                "ext.fontEditionFaces = { -> ['" + SHIPPED_FACE + "', '" + ABSENT_FACE + "'] }",
                "apply from: '" + toGradlePath(Path.of(GATE_SCRIPT)) + "'"));

        return new Workspace(upstreamDirectory, vanillaFontsDirectory, projectDirectory);
    }

    // The lock over the stand-in upstream, the edition's faces written as the JSON members they are.
    private static void writeLock(Workspace workspace, String faceMembers) throws IOException {

        Files.writeString(
            workspace.projectDirectory().resolve("font-editions.lock.json"),
            String.join(
                "\n",
                "{",
                "  \"repository\": \"" + workspace.upstreamDirectory().toUri() + "\",",
                "  \"fontsPath\": \"" + FONTS_PATH + "\",",
                "  \"editions\": { \"" + EDITION + "\": { " + faceMembers + " } }",
                "}"));
    }

    // The lock as it stands for the upstream's current head.
    private static void writeMatchingLock(Workspace workspace) throws IOException {
        writeLock(workspace, "\"" + SHIPPED_FACE + "\": \"" + readBlobSha(workspace.upstreamDirectory(), SHIPPED_FACE)
            + "\", \"" + ABSENT_FACE + "\": null");
    }

    private static String toGradlePath(Path path) {
        return path.toAbsolutePath().toString().replace('\\', '/');
    }

    private static GradleRunner createRunner(Workspace workspace, String taskName) {

        return GradleRunner
            .create()
            .withProjectDir(workspace.projectDirectory().toFile())
            .withArguments(taskName);
    }

    @Nested
    final class CheckFontEditions {

        @Test
        void laysTheEditionsDescriptorWhereItShipsOneAndTheGamesOwnWhereItDoesNot(@TempDir Path root)
                throws IOException {

            var workspace = writeWorkspace(root);

            writeMatchingLock(workspace);
            createRunner(workspace, "checkFontEditions").build();

            var editionFonts = workspace.projectDirectory().resolve(EDITION_FONTS_PATH);

            assertThat(editionFonts.resolve(SHIPPED_FACE + ".fnt"))
                .hasContent(EDITION_DESCRIPTOR);
            assertThat(editionFonts.resolve(ABSENT_FACE + ".fnt"))
                .hasContent(VANILLA_ABSENT_DESCRIPTOR);
        }

        @Test
        void marksTheEditionRootWithTheBranchAsAnInstalledEditionIs(@TempDir Path root) throws IOException {

            var workspace = writeWorkspace(root);

            writeMatchingLock(workspace);
            createRunner(workspace, "checkFontEditions").build();

            assertThat(workspace.projectDirectory()
                    .resolve("build/font-editions/roots/" + EDITION + "/starsector-core/localization_version.json"))
                .content()
                .contains("\"branch\":\"" + EDITION + "\"");
        }

        @Test
        void failsNamingTheEditionTheFaceAndBothShasWhenUpstreamMovedAShippedFace(@TempDir Path root)
                throws IOException {

            var workspace = writeWorkspace(root);
            writeMatchingLock(workspace);

            var lockedSha = readBlobSha(workspace.upstreamDirectory(), SHIPPED_FACE);
            commitDescriptor(workspace.upstreamDirectory(), SHIPPED_FACE, EDITION_DESCRIPTOR + " moved");

            var upstreamSha = readBlobSha(workspace.upstreamDirectory(), SHIPPED_FACE);

            assertThat(createRunner(workspace, "checkFontEditions").buildAndFail().getOutput())
                .contains(EDITION + " " + SHIPPED_FACE + ": locked " + lockedSha + ", upstream " + upstreamSha)
                .contains("gradlew writeFontEditionsLock");
        }

        @Test
        void failsWhenUpstreamStartsShippingAFaceTheLockHoldsAbsent(@TempDir Path root) throws IOException {
            // The case the stated absence exists for: a face KM falls through starting to ship upstream.
            var workspace = writeWorkspace(root);

            writeMatchingLock(workspace);
            commitDescriptor(workspace.upstreamDirectory(), ABSENT_FACE, "info face=\"Alpha B\" aa=4");

            assertThat(createRunner(workspace, "checkFontEditions").buildAndFail().getOutput())
                .contains(EDITION + " " + ABSENT_FACE + ": locked absent, upstream "
                    + readBlobSha(workspace.upstreamDirectory(), ABSENT_FACE));
        }

        @Test
        void failsWhenTheLockNamesOtherFacesThanTheEnum(@TempDir Path root) throws IOException {

            var workspace = writeWorkspace(root);
            writeLock(workspace, "\"" + SHIPPED_FACE + "\": \""
                + readBlobSha(workspace.upstreamDirectory(), SHIPPED_FACE) + "\"");

            assertThat(createRunner(workspace, "checkFontEditions").buildAndFail().getOutput())
                .contains(EDITION + ": the lock names faces [" + SHIPPED_FACE + "], the enum ["
                    + SHIPPED_FACE + ", " + ABSENT_FACE + "]");
        }

        @Test
        void failsWhereTheGameCarriesNoFontsToLayTheEditionOver(@TempDir Path root) throws IOException {
            // A runner without the game's descriptors would otherwise check nothing and pass.
            var workspace = writeWorkspace(root);
            writeMatchingLock(workspace);

            Files.delete(workspace.vanillaFontsDirectory().resolve(SHIPPED_FACE + ".fnt"));
            Files.delete(workspace.vanillaFontsDirectory().resolve(ABSENT_FACE + ".fnt"));
            Files.delete(workspace.vanillaFontsDirectory());

            assertThat(createRunner(workspace, "checkFontEditions").buildAndFail().getOutput())
                .contains("No game fonts at");
        }
    }

    @Nested
    final class WriteFontEditionsLock {

        @Test
        void writesEachFacesBlobShaOrAnAbsenceFromTheBranchHead(@TempDir Path root) throws IOException {

            var workspace = writeWorkspace(root);

            writeLock(workspace, "");
            createRunner(workspace, "writeFontEditionsLock").build();

            assertThat(workspace.projectDirectory().resolve("font-editions.lock.json"))
                .content()
                .contains("\"" + SHIPPED_FACE + "\": \"" + readBlobSha(workspace.upstreamDirectory(), SHIPPED_FACE) + "\"")
                .contains("\"" + ABSENT_FACE + "\": null");
        }

        @Test
        void keepsTheRepositoryAndTheEditionsTheLockNames(@TempDir Path root) throws IOException {

            var workspace = writeWorkspace(root);

            writeLock(workspace, "");
            createRunner(workspace, "writeFontEditionsLock").build();

            assertThat(workspace.projectDirectory().resolve("font-editions.lock.json"))
                .content()
                .contains("\"repository\": \"" + workspace.upstreamDirectory().toUri() + "\"")
                .contains("\"" + EDITION + "\": {");
        }
    }
}
