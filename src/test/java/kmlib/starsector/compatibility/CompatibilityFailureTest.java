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
                    + "\n\ndiagnose-newer{Fast Rendering|is-changed|" + MOD_ID + "|needs-it"
                    + "|down Fast Rendering to 0.8.8 or-wait " + MOD_ID + " up}"
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
        void advisesAnUpdateOnOneLineWhereTheInstallIsBehindTheBuild() {

            // One line, because the version was read and there is one thing to say.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.9.1", "v0.8.8");

            assertThat(failure.describeDiagnosisForPlayer())
                .extracting(CompatibilityNoticeLine::lineText)
                .containsExactly("diagnose-older{Fast Rendering|is-old|do-update{0.9.1}}");
        }

        @Test
        void warnsOnTheStateAndTheInstructionWhereTheInstallIsBehind() {

            // What the install is warns as well as what to do about it, so a player scanning for
            // the trouble finds it without reading the sentence. The version is inside the warned
            // instruction rather than brought forward beside it, that being one thing to act on.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.9.1", "v0.8.8");

            assertThat(failure.describeDiagnosisForPlayer().get(0).emphasisedRuns())
                .extracting(EmphasisedRun::runText, EmphasisedRun::emphasis)
                .containsExactly(
                    tuple("Fast Rendering", Emphasis.HIGHLIGHT),
                    tuple("is-old", Emphasis.WARNING),
                    tuple("do-update{0.9.1}", Emphasis.WARNING));
        }

        @Test
        void advisesADowngradeOrAWaitOnOneLineWhereTheInstallIsAhead() {

            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1");

            assertThat(failure.describeDiagnosisForPlayer())
                .extracting(CompatibilityNoticeLine::lineText)
                .containsExactly("diagnose-newer{Fast Rendering|is-changed|" + MOD_ID + "|needs-it"
                    + "|down Fast Rendering to 0.8.8 or-wait " + MOD_ID + " up}");
        }

        @Test
        void bringsTheModsInsideTheChoiceForwardAndWarnsOnTheWordsAround() {

            // The choice names both mods again, and a mod is brought forward wherever it is named
            // rather than disappearing into the colour of the instruction holding it. The version
            // stays inside the warning: it is what the player is told to move to, not a party to
            // the mismatch, so it is folded into the wording around it rather than kept apart.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1");

            assertThat(failure.describeDiagnosisForPlayer().get(0).emphasisedRuns())
                .extracting(EmphasisedRun::runText, EmphasisedRun::emphasis)
                .containsExactly(
                    tuple("Fast Rendering", Emphasis.HIGHLIGHT),
                    tuple("is-changed", Emphasis.WARNING),
                    tuple(MOD_ID, Emphasis.HIGHLIGHT),
                    tuple("needs-it", Emphasis.WARNING),
                    tuple("down", Emphasis.WARNING),
                    tuple("Fast Rendering", Emphasis.HIGHLIGHT),
                    tuple("to 0.8.8 or-wait", Emphasis.WARNING),
                    tuple(MOD_ID, Emphasis.HIGHLIGHT),
                    tuple("up", Emphasis.WARNING));
        }

        @Test
        void advisesBothCasesUnderALeadWhereTheInstalledVersionCouldNotBeRead() {

            // Three lines: both directions are live at once, and a sentence carrying both reads as
            // one tangled claim where a lead and two cases read as a choice.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", null);

            assertThat(failure.describeDiagnosisForPlayer())
                .extracting(CompatibilityNoticeLine::lineText)
                .containsExactly(
                    "diagnose-unknown{Fast Rendering}",
                    "case-older{is-old|do-update{0.8.8}}",
                    "case-newer{is-new|" + MOD_ID + "|needs-it|down Fast Rendering to 0.8.8 or-wait "
                        + MOD_ID + " up}");
        }

        @Test
        void warnsOnEachCaseAndBringsEveryModNamedForward() {

            // The second case names three mods: the one it is about, and the two the choice inside
            // it names again. All three are brought forward, whichever run they sit in.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", null);
            var diagnosis = failure.describeDiagnosisForPlayer();

            assertThat(diagnosis.get(0).emphasisedRuns())
                .extracting(EmphasisedRun::emphasis)
                .containsExactly(Emphasis.HIGHLIGHT);
            assertThat(diagnosis.get(1).emphasisedRuns())
                .extracting(EmphasisedRun::emphasis)
                .containsExactly(Emphasis.WARNING, Emphasis.WARNING);
            assertThat(diagnosis.get(2).emphasisedRuns())
                .extracting(EmphasisedRun::runText, EmphasisedRun::emphasis)
                .containsExactly(
                    tuple("is-new", Emphasis.WARNING),
                    tuple(MOD_ID, Emphasis.HIGHLIGHT),
                    tuple("needs-it", Emphasis.WARNING),
                    tuple("down", Emphasis.WARNING),
                    tuple("Fast Rendering", Emphasis.HIGHLIGHT),
                    tuple("to 0.8.8 or-wait", Emphasis.WARNING),
                    tuple(MOD_ID, Emphasis.HIGHLIGHT),
                    tuple("up", Emphasis.WARNING));
        }

        @Test
        void advisesNothingWhereTheBuildStampedNoTarget() {

            // Every sentence names the release to move to, and there is none to name.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions(null, "v0.9.1");

            assertThat(failure.describeDiagnosisForPlayer())
                .isEmpty();
        }

        @Test
        void asksForAReportOnOneLineWhereTheTwoNameOneRelease() {

            // No version to move to, so the instruction is the one thing left to do. Without the
            // line the rows show two matching versions and read as though nothing is wrong.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "0.8.8");

            assertThat(failure.describeDiagnosisForPlayer())
                .extracting(CompatibilityNoticeLine::lineText)
                .containsExactly("diagnose-same{Fast Rendering|report-to " + MOD_ID + " dev}");
        }

        @Test
        void bringsTheModToReportToForwardInsideTheWarningWhereTheTwoNameOneRelease() {

            // The mod the report goes to is brought forward inside the instruction as it is
            // everywhere else, rather than disappearing into the colour of the warning holding it.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "0.8.8");

            assertThat(failure.describeDiagnosisForPlayer().get(0).emphasisedRuns())
                .extracting(EmphasisedRun::runText, EmphasisedRun::emphasis)
                .containsExactly(
                    tuple("Fast Rendering", Emphasis.HIGHLIGHT),
                    tuple("report-to", Emphasis.WARNING),
                    tuple(MOD_ID, Emphasis.HIGHLIGHT),
                    tuple("dev", Emphasis.WARNING));
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
        void standsExactlyOneRunOutPerRow() {

            // A row's label says what it is and every notice carries the same ones; what differs
            // between two notices is the value, so that is the only run of a row that stands out.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1");

            assertThat(failure.describeRowsForPlayer())
                .allSatisfy(row -> assertThat(row.emphasisedRuns()).hasSize(1));
        }

        @Test
        void warnsOnWhatIsLostAndSetsAtEaseOnWhatIsNot() {

            // The two rows a player actually weighs. Read as one gold list, the good news and the
            // bad look alike and the eye has to read both to tell which is which.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1");
            var rows = failure.describeRowsForPlayer();

            assertThat(rows)
                .extracting(row -> row.emphasisedRuns().get(0).emphasis())
                .containsExactly(
                    Emphasis.HIGHLIGHT,
                    Emphasis.HIGHLIGHT,
                    Emphasis.HIGHLIGHT,
                    Emphasis.HIGHLIGHT,
                    Emphasis.HIGHLIGHT,
                    Emphasis.HIGHLIGHT,
                    Emphasis.WARNING,
                    Emphasis.REASSURANCE);
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
