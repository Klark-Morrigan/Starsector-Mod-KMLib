package kmlib.starsector.compatibility;

import kmlib.starsector.compatibility.CompatibilityNoticeLine.EmphasisedRun;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;
import kmlib.testfixtures.starsector.strings.ShippedStrings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Composes the notice a player reads out of the wording KMLib actually ships.
 *
 * <p>The one check the unit suite cannot make: that the shipped templates and the order the record
 * fills them in still agree. A template whose slots were reordered in {@code strings.json} would
 * pass every case written against a copy of it and reach a player with the versions swapped.
 *
 * <p>The other is that every run a line names as standing out is actually in that line, and in the
 * order given. The engine matches each run from where the last one ended, so a run the wording no
 * longer contains, or one listed before a run that precedes it, silently tints nothing.
 */
final class CompatibilityFailureIntegrationTest {

    private static final String MOD_ID = CompatibilityFailureFixture.MAP_OVERLAY_MOD_ID;

    @BeforeEach
    void installShippedStrings() {

        var stringsByKey = ShippedStrings.readStringsByKey();

        StarsectorSettingsFake.installSettings((category, key) -> stringsByKey.get(key));
    }

    @AfterEach
    void clearSettings() {

        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class DescribeForPlayer {

        @Test
        void readsAsTheNoticeWhereTheInstallIsAheadOfTheBuild() {

            // Both versions arrive with the prefix their subjects self-report, and neither reaches
            // the player with it: the label already says a version is what follows, and the two
            // versions come from two places that need not agree about carrying one.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1");

            assertThat(failure.describeForPlayer())
                .isEqualTo("Error integrating " + MOD_ID + " with Fast Rendering."
                    + "\n\nYour Fast Rendering carries changes to public contracts " + MOD_ID
                    + " depends on and you can either downgrade Fast Rendering to 0.8.8"
                    + " or wait for a " + MOD_ID + " update."
                    + "\n\n    Mod:           " + MOD_ID
                    + "\n    Integration:   " + MOD_ID + ":map-overlay"
                    + "\n    Targeted:      0.8.8"
                    + "\n    Detected:      0.9.1"
                    + "\n    Broken:        " + CompatibilityFailureFixture.BROKEN_DETAIL
                    + "\n    Failed while:  " + CompatibilityFailureFixture.FAILURE_SITE
                    + "\n    Effect:        " + CompatibilityFailureFixture.LOST_FEATURE
                    + "\n    No effect:     " + CompatibilityFailureFixture.UNAFFECTED_FEATURE
                    + "\n\nSee starsector.log for more details.");
        }

        @Test
        void readsAsTheNoticeWhereTheInstallIsBehindTheBuild() {

            // One line, because there is one thing to say: the version was read and it is behind.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.9.1", "v0.8.8");

            assertThat(failure.describeForPlayer())
                .contains("\n\nYour Fast Rendering is too old and needs to be updated"
                    + " to at least 0.9.1.\n\n");
        }

        @Test
        void readsAsTheTwoCasesWhereTheInstalledVersionCouldNotBeRead() {

            // A lead and the two cases under it, because both directions are live at once and a
            // sentence carrying both reads as one tangled claim.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", null);

            assertThat(failure.describeForPlayer())
                .contains("\n\nThe likely cause is that your Fast Rendering is either:"
                    + "\n- too old and needs to be updated to at least 0.8.8,"
                    + "\n- or it's new and carries changes to public contracts " + MOD_ID
                    + " depends on and you can either downgrade Fast Rendering to 0.8.8"
                    + " or wait for a " + MOD_ID + " update.\n\n")
                .contains("\n    Detected:      unknown");
        }

        @Test
        void readsAsTheNoticeWhereNeitherVersionWasRead() {

            // No diagnosis at all: every one names the release to move to, and there is none.
            var failure = CompatibilityFailureFixture.createFailure();

            assertThat(failure.describeForPlayer())
                .isEqualTo("Error integrating " + MOD_ID + " with Fast Rendering."
                    + "\n\n    Mod:           " + MOD_ID
                    + "\n    Integration:   " + MOD_ID + ":map-overlay"
                    + "\n    Targeted:      unknown"
                    + "\n    Detected:      unknown"
                    + "\n    Broken:        " + CompatibilityFailureFixture.BROKEN_DETAIL
                    + "\n    Failed while:  " + CompatibilityFailureFixture.FAILURE_SITE
                    + "\n    Effect:        " + CompatibilityFailureFixture.LOST_FEATURE
                    + "\n    No effect:     " + CompatibilityFailureFixture.UNAFFECTED_FEATURE
                    + "\n\nSee starsector.log for more details.");
        }
    }

    @Nested
    class EmphasisedRuns {

        @Test
        void everyRunOfEveryLineIsFoundInOrderInTheShippedWording() {

            // Walked over all four shapes a notice takes, because which runs a line names differs
            // by shape and a run that went missing tints nothing rather than failing.
            assertEveryRunIsFoundInOrder(
                CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1"));
            assertEveryRunIsFoundInOrder(
                CompatibilityFailureFixture.createFailureBetweenVersions("v0.9.1", "v0.8.8"));
            assertEveryRunIsFoundInOrder(
                CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", null));
            assertEveryRunIsFoundInOrder(CompatibilityFailureFixture.createFailure());
        }

        // Walks a notice's lines the way the engine walks a line's runs: each searched from where
        // the last one ended, which is what makes a repeated name land on its own occurrence.
        private void assertEveryRunIsFoundInOrder(CompatibilityFailure failure) {

            var lines = new ArrayList<CompatibilityNoticeLine>();
            lines.add(failure.describeHeadingForPlayer());
            lines.addAll(failure.describeDiagnosisForPlayer());
            lines.addAll(failure.describeRowsForPlayer());
            lines.add(failure.describeClosingForPlayer());

            for (var line : lines) {
                assertRunsAreFoundInOrder(line);
            }
        }

        private void assertRunsAreFoundInOrder(CompatibilityNoticeLine line) {

            var searchedFrom = 0;

            for (EmphasisedRun run : line.emphasisedRuns()) {
                var foundAt = line.lineText().indexOf(run.runText(), searchedFrom);

                assertThat(foundAt)
                    .as("run '%s' in line '%s'", run.runText(), line.lineText())
                    .isNotNegative();

                searchedFrom = foundAt + run.runText().length();
            }
        }
    }
}
