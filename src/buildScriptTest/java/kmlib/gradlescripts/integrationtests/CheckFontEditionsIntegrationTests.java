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
 * Exercises the font-edition gate against a real Gradle build and local repositories standing in for the
 * upstream localisations: an edition branch shipping one of two faces, over a folder of the game's own
 * descriptors.
 *
 * <p>What the gate has to get right is what a player's install would hold - the edition's descriptor
 * where it ships one and the game's own where it does not - and that every way upstream can part from the
 * lock fails the build naming the edition, the face and both SHAs, since a gate that passed on a moved
 * pack would report an edition as checked when it was not. Editions are the lock's by name rather than by
 * where they are published, so two repositories sharing a branch name stay two editions.
 */
final class CheckFontEditionsIntegrationTests {

    private static final String GATE_SCRIPT =
        new File("gradle/tasks/checks/check-font-editions.gradle").getAbsolutePath();

    private static final String EDITION_NAME = "alpha";
    private static final String BRANCH = "font-alpha";
    private static final String FONTS_PATH = "localization/graphics/fonts";
    private static final String LANGUAGE = "zh-Hans";

    // "Hegemony" (U+9738 U+4E3B): the edition's probe text, in a script a lock rewrite has to keep as is.
    private static final String PROBE_TEXT = "霸主";

    // Two faces: the edition ships the first and leaves the second to the game.
    private static final String SHIPPED_FACE = "faceA";
    private static final String ABSENT_FACE = "faceB";

    private static final String EDITION_DESCRIPTOR = "info face=\"Alpha\" aa=4";
    private static final String VANILLA_SHIPPED_DESCRIPTOR = "info face=\"Vanilla A\" aa=4";
    private static final String VANILLA_ABSENT_DESCRIPTOR = "info face=\"Vanilla B\" aa=1";

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

    // Commits a descriptor onto the edition branch of an upstream.
    private static void commitDescriptor(Path upstreamDirectory, String face, String content) throws IOException {

        var descriptor = upstreamDirectory.resolve(FONTS_PATH).resolve(face + ".fnt");

        Files.createDirectories(descriptor.getParent());
        Files.writeString(descriptor, content);

        runGit(upstreamDirectory, "add", "--all");
        runGit(upstreamDirectory, "commit", "--quiet", "-m", "Descriptor " + face);
    }

    // The blob SHA an upstream holds for a face on the edition branch.
    private static String readBlobSha(Path upstreamDirectory, String face) throws IOException {
        return runGit(upstreamDirectory, "rev-parse", BRANCH + ":" + FONTS_PATH + "/" + face + ".fnt");
    }

    // An upstream whose edition branch ships the first face, as the given descriptor.
    private static Path createUpstream(Path upstreamDirectory, String shippedDescriptor) throws IOException {

        Files.createDirectories(upstreamDirectory);

        runGit(upstreamDirectory, "init", "--quiet");
        runGit(upstreamDirectory, "checkout", "--quiet", "-b", BRANCH);

        // A partial clone reads its blobs by SHA when it first needs them, which the stand-in has to
        // allow as the real host does.
        runGit(upstreamDirectory, "config", "uploadpack.allowFilter", "true");
        runGit(upstreamDirectory, "config", "uploadpack.allowAnySHA1InWant", "true");

        commitDescriptor(upstreamDirectory, SHIPPED_FACE, shippedDescriptor);

        return upstreamDirectory;
    }

    /**
     * An upstream whose edition branch ships the first face, the game's descriptors for both, and a build
     * applying the gate with those two faces as the enum's.
     */
    private static Workspace writeWorkspace(Path root) throws IOException {

        var upstreamDirectory = createUpstream(root.resolve("upstream"), EDITION_DESCRIPTOR);
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

    // One edition's lock entry, its faces written as the JSON members they are.
    private static String writeEditionEntry(String name, Path upstreamDirectory, String faceMembers) {

        return String.join(
            " ",
            "{",
            "\"name\": \"" + name + "\",",
            "\"repository\": \"" + upstreamDirectory.toUri() + "\",",
            "\"branch\": \"" + BRANCH + "\",",
            "\"fontsPath\": \"" + FONTS_PATH + "\",",
            "\"language\": \"" + LANGUAGE + "\",",
            "\"faces\": { " + faceMembers + " }",
            "}");
    }

    // The faces as an upstream's current head holds them.
    private static String writeMatchingFaces(Path upstreamDirectory) throws IOException {
        return "\"" + SHIPPED_FACE + "\": \"" + readBlobSha(upstreamDirectory, SHIPPED_FACE) + "\", \""
            + ABSENT_FACE + "\": null";
    }

    // A lock stating the editions' language's probe text beside the editions.
    private static void writeLock(Workspace workspace, String... editionEntries) throws IOException {
        writeLockProbing(workspace, "\"" + LANGUAGE + "\": { \"text\": \"" + PROBE_TEXT + "\" }", editionEntries);
    }

    private static void writeLockProbing(Workspace workspace, String probeTextMembers, String... editionEntries)
            throws IOException {

        Files.writeString(
            workspace.projectDirectory().resolve("font-editions.lock.json"),
            "{ \"probeTexts\": { " + probeTextMembers + " }, \"editions\": [ "
                + String.join(", ", editionEntries) + " ] }",
            StandardCharsets.UTF_8);
    }

    // The lock as it stands for the one upstream's current head.
    private static void writeMatchingLock(Workspace workspace) throws IOException {

        writeLock(
            workspace,
            writeEditionEntry(
                EDITION_NAME,
                workspace.upstreamDirectory(),
                writeMatchingFaces(workspace.upstreamDirectory())));
    }

    private static Path resolveEditionFonts(Workspace workspace, String editionName) {

        return workspace
            .projectDirectory()
            .resolve("build/font-editions/roots/" + editionName + "/starsector-core/graphics/fonts");
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

            var editionFonts = resolveEditionFonts(workspace, EDITION_NAME);

            assertThat(editionFonts.resolve(SHIPPED_FACE + ".fnt"))
                .hasContent(EDITION_DESCRIPTOR);
            assertThat(editionFonts.resolve(ABSENT_FACE + ".fnt"))
                .hasContent(VANILLA_ABSENT_DESCRIPTOR);
        }

        @Test
        void marksTheEditionRootWithTheEditionsNameAndLanguageAsAnInstalledEditionIsMarked(@TempDir Path root)
                throws IOException {

            var workspace = writeWorkspace(root);

            writeMatchingLock(workspace);
            createRunner(workspace, "checkFontEditions").build();

            var markerFile = workspace.projectDirectory()
                .resolve("build/font-editions/roots/" + EDITION_NAME + "/starsector-core/localization_version.json");

            assertThat(markerFile)
                .content()
                .contains("\"branch\":\"" + EDITION_NAME + "\"")
                .contains("\"language\":\"" + LANGUAGE + "\"");
        }

        @Test
        void failsWhenAnEditionsLanguageHasNoProbeText(@TempDir Path root) throws IOException {
            // The suites could not tell whether the edition's faces draw its script, and would pass it.
            var workspace = writeWorkspace(root);

            writeLockProbing(
                workspace,
                "",
                writeEditionEntry(
                    EDITION_NAME,
                    workspace.upstreamDirectory(),
                    writeMatchingFaces(workspace.upstreamDirectory())));

            assertThat(createRunner(workspace, "checkFontEditions").buildAndFail().getOutput())
                .contains("no probe text for " + LANGUAGE + ", the language of edition '" + EDITION_NAME + "'");
        }

        @Test
        void laysEachEditionUnderItsOwnNameWhereTwoRepositoriesShareABranchName(@TempDir Path root)
                throws IOException {
            // Two localisations publishing on a branch of the same name: keyed by where they are
            // published, the second would overwrite the first.
            var workspace = writeWorkspace(root);
            var betaDescriptor = "info face=\"Beta\" aa=4";
            var betaUpstream = createUpstream(root.resolve("beta-upstream"), betaDescriptor);

            writeLock(
                workspace,
                writeEditionEntry(
                    EDITION_NAME,
                    workspace.upstreamDirectory(),
                    writeMatchingFaces(workspace.upstreamDirectory())),
                writeEditionEntry("beta", betaUpstream, writeMatchingFaces(betaUpstream)));
            createRunner(workspace, "checkFontEditions").build();

            assertThat(resolveEditionFonts(workspace, EDITION_NAME).resolve(SHIPPED_FACE + ".fnt"))
                .hasContent(EDITION_DESCRIPTOR);
            assertThat(resolveEditionFonts(workspace, "beta").resolve(SHIPPED_FACE + ".fnt"))
                .hasContent(betaDescriptor);
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
                .contains(EDITION_NAME + " " + SHIPPED_FACE + ": locked " + lockedSha + ", upstream " + upstreamSha)
                .contains("gradlew writeFontEditionsLock");
        }

        @Test
        void failsWhenUpstreamStartsShippingAFaceTheLockHoldsAbsent(@TempDir Path root) throws IOException {
            // The case the stated absence exists for: a face KM falls through starting to ship upstream.
            var workspace = writeWorkspace(root);

            writeMatchingLock(workspace);
            commitDescriptor(workspace.upstreamDirectory(), ABSENT_FACE, "info face=\"Alpha B\" aa=4");

            assertThat(createRunner(workspace, "checkFontEditions").buildAndFail().getOutput())
                .contains(EDITION_NAME + " " + ABSENT_FACE + ": locked absent, upstream "
                    + readBlobSha(workspace.upstreamDirectory(), ABSENT_FACE));
        }

        @Test
        void failsWhenTheLockNamesOtherFacesThanTheEnum(@TempDir Path root) throws IOException {

            var workspace = writeWorkspace(root);

            writeLock(
                workspace,
                writeEditionEntry(
                    EDITION_NAME,
                    workspace.upstreamDirectory(),
                    "\"" + SHIPPED_FACE + "\": \""
                        + readBlobSha(workspace.upstreamDirectory(), SHIPPED_FACE) + "\""));

            assertThat(createRunner(workspace, "checkFontEditions").buildAndFail().getOutput())
                .contains(EDITION_NAME + ": the lock names faces [" + SHIPPED_FACE + "], the enum ["
                    + SHIPPED_FACE + ", " + ABSENT_FACE + "]");
        }

        @Test
        void failsWhenTwoEditionsShareAName(@TempDir Path root) throws IOException {
            // Both would be laid into one root, and the second would silently stand for the first.
            var workspace = writeWorkspace(root);
            var entry = writeEditionEntry(
                EDITION_NAME,
                workspace.upstreamDirectory(),
                writeMatchingFaces(workspace.upstreamDirectory()));

            writeLock(workspace, entry, entry);

            assertThat(createRunner(workspace, "checkFontEditions").buildAndFail().getOutput())
                .contains("two editions are named '" + EDITION_NAME + "'");
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

        @Test
        void failsWhereAFaceTheEditionDoesNotShipIsMissingFromTheGameToo(@TempDir Path root)
                throws IOException {
            // The root would lack the face, and the suites would read the edition as one that cannot load it.
            var workspace = writeWorkspace(root);
            writeMatchingLock(workspace);

            Files.delete(workspace.vanillaFontsDirectory().resolve(ABSENT_FACE + ".fnt"));

            assertThat(createRunner(workspace, "checkFontEditions").buildAndFail().getOutput())
                .contains(EDITION_NAME + " ships no " + ABSENT_FACE + ".fnt and the game's own is missing at");
        }

        @Test
        void failsNamingWhatAnEditionDoesNotState(@TempDir Path root) throws IOException {

            var workspace = writeWorkspace(root);
            var entryWithoutBranch = writeEditionEntry(
                    EDITION_NAME,
                    workspace.upstreamDirectory(),
                    writeMatchingFaces(workspace.upstreamDirectory()))
                .replace("\"branch\": \"" + BRANCH + "\",", "");

            writeLock(workspace, entryWithoutBranch);

            assertThat(createRunner(workspace, "checkFontEditions").buildAndFail().getOutput())
                .contains("an edition states no branch");
        }

        @Test
        void failsWhereAnEditionsNameIsNoPlainFolderName(@TempDir Path root) throws IOException {
            // The name becomes the edition's folder under build/, which a path could lead out of.
            var workspace = writeWorkspace(root);

            writeLock(
                workspace,
                writeEditionEntry(
                    "../escape",
                    workspace.upstreamDirectory(),
                    writeMatchingFaces(workspace.upstreamDirectory())));

            assertThat(createRunner(workspace, "checkFontEditions").buildAndFail().getOutput())
                .contains("edition name '../escape' is not a plain folder name");
        }

        @Test
        void failsWhereTheLockStatesNoEditionsList(@TempDir Path root) throws IOException {

            var workspace = writeWorkspace(root);

            Files.writeString(
                workspace.projectDirectory().resolve("font-editions.lock.json"),
                "{ \"probeTexts\": {} }");

            assertThat(createRunner(workspace, "checkFontEditions").buildAndFail().getOutput())
                .contains("font-editions.lock.json states no editions list");
        }
    }

    @Nested
    final class WriteFontEditionsLock {

        @Test
        void writesEachFacesBlobShaOrAnAbsenceFromTheBranchHead(@TempDir Path root) throws IOException {

            var workspace = writeWorkspace(root);

            writeLock(workspace, writeEditionEntry(EDITION_NAME, workspace.upstreamDirectory(), ""));
            createRunner(workspace, "writeFontEditionsLock").build();

            assertThat(workspace.projectDirectory().resolve("font-editions.lock.json"))
                .content()
                .contains("\"" + SHIPPED_FACE + "\": \""
                    + readBlobSha(workspace.upstreamDirectory(), SHIPPED_FACE) + "\"")
                .contains("\"" + ABSENT_FACE + "\": null");
        }

        @Test
        void keepsEachEditionsNameAndWhereItIsPublished(@TempDir Path root) throws IOException {
            // Which editions there are, and where each is published, are the lock author's: a rewrite
            // touches the faces alone.
            var workspace = writeWorkspace(root);

            writeLock(workspace, writeEditionEntry(EDITION_NAME, workspace.upstreamDirectory(), ""));
            createRunner(workspace, "writeFontEditionsLock").build();

            assertThat(workspace.projectDirectory().resolve("font-editions.lock.json"))
                .content()
                .contains("\"name\": \"" + EDITION_NAME + "\"")
                .contains("\"repository\": \"" + workspace.upstreamDirectory().toUri() + "\"")
                .contains("\"branch\": \"" + BRANCH + "\"")
                .contains("\"fontsPath\": \"" + FONTS_PATH + "\"")
                .contains("\"language\": \"" + LANGUAGE + "\"");
        }

        @Test
        void keepsTheProbeTextsInTheirOwnScript(@TempDir Path root) throws IOException {
            // Escaped, the probe text would still parse, but the lock would no longer read as the text it
            // probes with.
            var workspace = writeWorkspace(root);

            writeLock(workspace, writeEditionEntry(EDITION_NAME, workspace.upstreamDirectory(), ""));
            createRunner(workspace, "writeFontEditionsLock").build();

            assertThat(workspace.projectDirectory().resolve("font-editions.lock.json"))
                .content(StandardCharsets.UTF_8)
                .contains("\"text\": \"" + PROBE_TEXT + "\"");
        }
    }
}
