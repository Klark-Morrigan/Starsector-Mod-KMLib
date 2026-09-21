package kmlib.starsector.compatibility;

import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;
import kmlib.testfixtures.starsector.compatibility.CompatibilitySlotTemplates;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers how a failure fills the slots of the two sentences it composes: which value lands in which
 * slot, and what stands in a version slot nothing could fill.
 *
 * <p>The player-facing templates are stand-ins that expose their slots rather than copies of the
 * shipped wording: a copy agrees with the code however the shipped file is edited, so it is
 * {@code CompatibilityFailureIntegrationTest} that composes the sentence a player reads.
 */
final class CompatibilityFailureTest {

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
        void laysOutTheHeadingAndEveryRow() {

            // The whole modal as one value: it is a block, so which rows there are and what order
            // they come in is as much the subject as what lands in each.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1");

            assertThat(failure.describeForPlayer())
                .isEqualTo("title[Fast Rendering] seelog[]"
                    + "\n\nmod[" + CompatibilityFailureFixture.MAP_OVERLAY_MOD_ID + "]"
                    + "\nbuilt[0.8.8]"
                    + "\ninstalled[0.9.1]"
                    + "\neffect[" + CompatibilityFailureFixture.LOST_FEATURE + "]"
                    + "\nnoeffect[" + CompatibilityFailureFixture.UNAFFECTED_FEATURE + "]");
        }

        @Test
        void leavesOutTheNoEffectRowWhereTheConsumerSaidNothingAboutOne() {

            // A row reading "No effect: -" claims less than no row at all and takes as much of the
            // player's eye, so a consumer with nothing to add gets a block one row shorter.
            var failure = CompatibilityFailureFixture.createFailureLosing(
                CompatibilityFailureFixture.LOST_FEATURE);

            assertThat(failure.describeForPlayer())
                .doesNotContain("noeffect[");
        }

        @Test
        void standsTheUnknownWordingInForAnAbsentInstalledVersion() {

            // The same one-word stand-in both version rows take. A row that explained instead of
            // answering would be the longest line in a panel that does not grow to hold it.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", null);

            assertThat(failure.describeForPlayer())
                .contains("installed[?]")
                .doesNotContain("null");
        }

        @Test
        void standsTheUnknownWordingInForAnAbsentBuiltAgainstVersion() {

            // A blank is absent too, so a version that arrives as an empty string reaches the
            // player as the unknown wording rather than as a hole in the row.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("", "v0.9.1");

            assertThat(failure.describeForPlayer())
                .contains("built[?]");
        }

        @Test
        void namesTheModByItsIdAloneWhereTheGameListsNoSuchMod() {

            // The mod row is resolved against the game's mod manager at report time, and the fake
            // settings a case runs under list no mods at all - which is also what an install sees
            // for an ID naming nothing. The ID stands for itself rather than being dressed as a
            // name, so a misspelled or invented one is visible in the report.
            var failure = CompatibilityFailureFixture.createFailure();

            assertThat(failure.describeForPlayer())
                .contains("mod[" + CompatibilityFailureFixture.MAP_OVERLAY_MOD_ID + "]");
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
                .isEqualTo("Fast Rendering version mismatch."
                    + "\n    Built against: 0.8.8"
                    + "\n    Installed:     0.9.1"
                    + "\n    Failed while:  resolving the binding"
                    + "\n    Broken:        GLCommand.run"
                    + " (ClassNotFoundException: com.genir.renderer.bridge.interfaces.GLCommand)"
                    + "\n    Mod:           " + CompatibilityFailureFixture.MAP_OVERLAY_MOD_ID
                    + "\n    Consumer:      " + CompatibilityFailureFixture.MAP_OVERLAY_MOD_ID
                    + ":map-overlay"
                    + "\n    Effect:        "
                    + "Sector map overlays will not respond to the cursor this session.");
        }

        @Test
        void namesAnUnreadVersionAsUnknownRatherThanAsNull() {

            var failure = CompatibilityFailureFixture.createFailure();

            assertThat(failure.describeForLog())
                .contains("\n    Built against: an unknown version")
                .contains("\n    Installed:     an unknown version");
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
