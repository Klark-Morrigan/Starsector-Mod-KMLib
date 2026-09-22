package kmlib.starsector.compatibility;

import kmlib.starsector.compatibility.CompatibilityNoticeLine.Emphasis;
import kmlib.starsector.compatibility.CompatibilityNoticeLine.EmphasisedRun;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;
import kmlib.testfixtures.starsector.compatibility.CompatibilitySlotTemplates;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Covers how a failure fills the slots of the lines it composes: which value lands in which slot,
 * which diagnosis the two versions pick, and which runs of a line it names as standing out.
 *
 * <p>The runs are worth as much as the wording. The engine matches each from where the last one
 * ended, so a run listed out of order tints the wrong words or nothing at all - and the cases below
 * pin the order as much as the membership.
 *
 * <p>The player-facing templates are stand-ins that expose their slots rather than copies of the
 * shipped wording: a copy agrees with the code however the shipped file is edited, so it is
 * {@code CompatibilityFailureIntegrationTest} that composes the notice a player reads.
 */
final class CompatibilityFailureTest {

    private static final String MOD_ID = CompatibilityFailureFixture.MAP_OVERLAY_MOD_ID;

    @AfterEach
    void clearSettings() {

        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class DescribeForPlayer {

        @BeforeEach
        void installSlotTemplates() {

            CompatibilitySlotTemplates.installSlotTemplates();
        }

        @Test
        void laysOutEveryLineInReadingOrder() {

            // The whole notice as one value: it is a sequence of lines, so which there are and what
            // order they come in is as much the subject as what lands in each.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1");

            assertThat(failure.describeForPlayer())
                .isEqualTo("title{failed|" + MOD_ID + "|Fast Rendering}"
                    + "\n\ndiagnose-newer{Fast Rendering|" + MOD_ID
                    + "|do-choose{Fast Rendering|0.8.8|" + MOD_ID + "}}"
                    + "\n\nmod{" + MOD_ID + "}"
                    + "\nintegration{" + MOD_ID + ":map-overlay}"
                    + "\ntargeted{0.8.8}"
                    + "\ndetected{0.9.1}"
                    + "\nbroken{" + CompatibilityFailureFixture.BROKEN_DETAIL + "}"
                    + "\nfailedwhile{" + CompatibilityFailureFixture.FAILURE_SITE + "}"
                    + "\neffect{" + CompatibilityFailureFixture.LOST_FEATURE + "}"
                    + "\nnoeffect{" + CompatibilityFailureFixture.UNAFFECTED_FEATURE + "}"
                    + "\n\nseelog{starsector.log}");
        }

        @Test
        void leavesOutTheNoEffectRowWhereTheConsumerSaidNothingAboutOne() {

            // A row reading "No effect: -" claims less than no row at all and takes as much of the
            // player's eye, so a consumer with nothing to add gets a block one row shorter.
            var failure = CompatibilityFailureFixture.createFailureLosing(
                CompatibilityFailureFixture.LOST_FEATURE);

            assertThat(failure.describeForPlayer())
                .doesNotContain("noeffect{");
        }

        @Test
        void standsTheUnknownWordingInForAnAbsentInstalledVersion() {

            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", null);

            assertThat(failure.describeForPlayer())
                .contains("detected{?}")
                .doesNotContain("null");
        }

        @Test
        void standsTheUnknownWordingInForAnAbsentBuiltAgainstVersion() {

            // A blank is absent too, so a version that arrives as an empty string reaches the
            // player as the unknown wording rather than as a hole in the row.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("", "v0.9.1");

            assertThat(failure.describeForPlayer())
                .contains("targeted{?}");
        }

        @Test
        void namesTheModByItsIdAloneWhereTheGameListsNoSuchMod() {

            // The mod row is resolved against the game's mod manager at report time, and the fake
            // settings a case runs under list no mods at all - which is also what an install sees
            // for an ID naming nothing. The ID stands for itself rather than being dressed as a
            // name, so a misspelled or invented one is visible in the report.
            var failure = CompatibilityFailureFixture.createFailure();

            assertThat(failure.describeForPlayer())
                .contains("mod{" + MOD_ID + "}");
        }
    }

    @Nested
    class DescribeHeadingForPlayer {

        @BeforeEach
        void installSlotTemplates() {

            CompatibilitySlotTemplates.installSlotTemplates();
        }

        @Test
        void warnsOnTheFailurePhraseAndBringsBothNamesForward() {

            var failure = CompatibilityFailureFixture.createFailure();

            assertThat(failure.describeHeadingForPlayer().emphasisedRuns())
                .extracting(EmphasisedRun::runText, EmphasisedRun::emphasis)
                .containsExactly(
                    tuple("failed", Emphasis.WARNING),
                    tuple(MOD_ID, Emphasis.HIGHLIGHT),
                    tuple("Fast Rendering", Emphasis.HIGHLIGHT));
        }
    }

    @Nested
    class DescribeDiagnosisForPlayer {

        @BeforeEach
        void installSlotTemplates() {

            CompatibilitySlotTemplates.installSlotTemplates();
        }

        @Test
        void advisesAnUpdateWhereTheInstallIsBehindTheBuild() {

            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.9.1", "v0.8.8");

            assertThat(failure.describeDiagnosisForPlayer().lineText())
                .isEqualTo("diagnose-older{Fast Rendering|do-update|0.9.1}");
        }

        @Test
        void warnsOnTheUpdatePhraseAloneWhereTheInstallIsBehind() {

            // The names and the version are brought forward; only the instruction warns. Listed in
            // the order they appear, which is what the engine matches them in.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.9.1", "v0.8.8");

            assertThat(failure.describeDiagnosisForPlayer().emphasisedRuns())
                .extracting(EmphasisedRun::runText, EmphasisedRun::emphasis)
                .containsExactly(
                    tuple("Fast Rendering", Emphasis.HIGHLIGHT),
                    tuple("do-update", Emphasis.WARNING),
                    tuple("0.9.1", Emphasis.HIGHLIGHT));
        }

        @Test
        void advisesADowngradeOrAWaitWhereTheInstallIsAheadOfTheBuild() {

            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1");

            assertThat(failure.describeDiagnosisForPlayer().lineText())
                .isEqualTo("diagnose-newer{Fast Rendering|" + MOD_ID
                    + "|do-choose{Fast Rendering|0.8.8|" + MOD_ID + "}}");
        }

        @Test
        void warnsOnTheWholeChoiceWhereTheInstallIsAhead() {

            // The choice warns as one run, values and all, so nothing inside it is brought forward
            // separately. The names that appear both before it and inside it are listed once each,
            // in order, which is what keeps the engine matching the early ones first.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1");

            assertThat(failure.describeDiagnosisForPlayer().emphasisedRuns())
                .extracting(EmphasisedRun::runText, EmphasisedRun::emphasis)
                .containsExactly(
                    tuple("Fast Rendering", Emphasis.HIGHLIGHT),
                    tuple(MOD_ID, Emphasis.HIGHLIGHT),
                    tuple("do-choose{Fast Rendering|0.8.8|" + MOD_ID + "}", Emphasis.WARNING));
        }

        @Test
        void advisesBothWaysWhereTheInstalledVersionCouldNotBeRead() {

            // Both directions in one sentence: with one version unread, nothing said of either
            // alone would be true of both cases.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", null);

            assertThat(failure.describeDiagnosisForPlayer().lineText())
                .isEqualTo("diagnose-unknown{Fast Rendering|do-update|0.8.8|" + MOD_ID
                    + "|do-choose{Fast Rendering|0.8.8|" + MOD_ID + "}}");
        }

        @Test
        void warnsOnBothInstructionsWhereTheInstalledVersionCouldNotBeRead() {

            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", null);

            assertThat(failure.describeDiagnosisForPlayer().emphasisedRuns())
                .extracting(EmphasisedRun::emphasis)
                .containsExactly(
                    Emphasis.HIGHLIGHT,
                    Emphasis.WARNING,
                    Emphasis.HIGHLIGHT,
                    Emphasis.HIGHLIGHT,
                    Emphasis.WARNING);
        }

        @Test
        void advisesNothingWhereTheBuildStampedNoTarget() {

            // Every sentence names the release to move to, and there is none to name.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions(null, "v0.9.1");

            assertThat(failure.describeDiagnosisForPlayer())
                .isNull();
        }

        @Test
        void advisesNothingWhereTheTwoNameOneRelease() {

            // Neither direction is true, and a sentence saying one would be a guess dressed as
            // advice. The rows still show the two versions, which is the whole of what is known.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "0.8.8");

            assertThat(failure.describeDiagnosisForPlayer())
                .isNull();
        }
    }

    @Nested
    class DescribeRowsForPlayer {

        @BeforeEach
        void installSlotTemplates() {

            CompatibilitySlotTemplates.installSlotTemplates();
        }

        @Test
        void answersEachRowInReadingOrder() {

            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1");

            assertThat(failure.describeRowsForPlayer())
                .extracting(CompatibilityNoticeLine::lineText)
                .containsExactly(
                    "mod{" + MOD_ID + "}",
                    "integration{" + MOD_ID + ":map-overlay}",
                    "targeted{0.8.8}",
                    "detected{0.9.1}",
                    "broken{" + CompatibilityFailureFixture.BROKEN_DETAIL + "}",
                    "failedwhile{" + CompatibilityFailureFixture.FAILURE_SITE + "}",
                    "effect{" + CompatibilityFailureFixture.LOST_FEATURE + "}",
                    "noeffect{" + CompatibilityFailureFixture.UNAFFECTED_FEATURE + "}");
        }

        @Test
        void bringsEachRowsValueForwardAndWarnsOnNone() {

            // A row's label says what it is and every notice carries the same ones; what differs
            // between two notices is the value, so that is the only run of a row that stands out.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1");

            assertThat(failure.describeRowsForPlayer())
                .allSatisfy(row -> assertThat(row.emphasisedRuns())
                    .extracting(EmphasisedRun::emphasis)
                    .containsExactly(Emphasis.HIGHLIGHT));
        }

        @Test
        void leavesOutTheNoEffectRowWhereTheConsumerSaidNothingAboutOne() {

            var failure = CompatibilityFailureFixture.createFailureLosing(
                CompatibilityFailureFixture.LOST_FEATURE);

            assertThat(failure.describeRowsForPlayer())
                .extracting(CompatibilityNoticeLine::lineText)
                .noneMatch(row -> row.startsWith("noeffect{"));
        }
    }

    @Nested
    class DescribeClosingForPlayer {

        @BeforeEach
        void installSlotTemplates() {

            CompatibilitySlotTemplates.installSlotTemplates();
        }

        @Test
        void bringsTheLogsOwnFileNameForward() {

            // The file name is not wording and is not translated, so it is supplied rather than
            // read - and it is the one run of the closing line worth the player's eye.
            var failure = CompatibilityFailureFixture.createFailure();

            assertThat(failure.describeClosingForPlayer().emphasisedRuns())
                .extracting(EmphasisedRun::runText, EmphasisedRun::emphasis)
                .containsExactly(tuple("starsector.log", Emphasis.HIGHLIGHT));
        }
    }

    @Nested
    class DescribeForLog {

        @Test
        void laysOutEveryReadingAReportIsWrittenFromAsItsOwnRow() {

            // The whole block, asserted as one value rather than row by row: what a reader of two
            // logs compares is the shape as much as the values, so a row that moved, lost its
            // label or lost its column is a change to be seen here.
            //
            // No settings installed: the block is written from literals, so it holds on a path
            // where the game's settings may not be up yet.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1");

            assertThat(failure.describeForLog())
                .isEqualTo("Error integrating " + MOD_ID + " with Fast Rendering."
                    + "\n    Mod:           " + MOD_ID
                    + "\n    Integration:   " + MOD_ID + ":map-overlay"
                    + "\n    Targeted:      0.8.8"
                    + "\n    Detected:      0.9.1"
                    + "\n    Broken:        GLCommand.run"
                    + " (ClassNotFoundException: com.genir.renderer.bridge.interfaces.GLCommand)"
                    + "\n    Failed while:  resolving the binding"
                    + "\n    Effect:        " + CompatibilityFailureFixture.LOST_FEATURE
                    + "\n    No effect:     " + CompatibilityFailureFixture.UNAFFECTED_FEATURE);
        }

        @Test
        void namesAnUnreadVersionAsUnknownRatherThanAsNull() {

            var failure = CompatibilityFailureFixture.createFailure();

            assertThat(failure.describeForLog())
                .contains("\n    Targeted:      unknown")
                .contains("\n    Detected:      unknown");
        }

        @Test
        void leavesOutTheNoEffectRowWhereTheConsumerSaidNothingAboutOne() {

            var failure = CompatibilityFailureFixture.createFailureLosing(
                CompatibilityFailureFixture.LOST_FEATURE);

            assertThat(failure.describeForLog())
                .doesNotContain("No effect:");
        }

        @Test
        void namesTheGuardThatCaughtTheBinding() {

            // The one reading that tells a release which moved a member apart from one that
            // declares it and refuses the call: both reach the log as a broken binding, and only
            // the site says which guard met it.
            var failure = CompatibilityFailureFixture.createFailureCaughtWhile(
                "calling the bridge from the game thread");

            assertThat(failure.describeForLog())
                .contains("\n    Failed while:  calling the bridge from the game thread");
        }
    }
}
