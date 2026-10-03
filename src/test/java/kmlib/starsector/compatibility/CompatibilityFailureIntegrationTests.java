package kmlib.starsector.compatibility;

import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;
import kmlib.testfixtures.starsector.strings.ShippedStrings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Composes the notice a player reads out of the wording KMLib actually ships.
 *
 * <p>The one check the unit suite cannot make: that the shipped templates and the order the record
 * fills them in still agree. A template whose slots were reordered in {@code strings.json} would
 * pass every case written against a copy of it and reach a player with the versions swapped.
 *
 * <p>The other is that every run a line names as standing out is one the engine highlights, in every
 * locale KMLib ships. A run the wording no longer contains, or one beside a character the engine does
 * not highlight against, silently tints nothing.
 */
final class CompatibilityFailureIntegrationTests {

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
        void readsAsARequestToReportWhereTheTwoVersionsNameOneRelease() {

            // No version to move to, so the line asks for the one thing left to do. The rows
            // either side of it show one version twice, which alone reads as though nothing is
            // wrong.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "0.8.8");

            assertThat(failure.describeForPlayer())
                .contains("\n\nYour Fast Rendering reports the version this integration was built"
                    + " for, so it's better to report this issue to the " + MOD_ID + " developer.\n\n")
                .contains("\n    Targeted:      0.8.8")
                .contains("\n    Detected:      0.8.8");
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
    class LogBlockAgainstNoticeRows {

        @Test
        void carryTheSameReadingsInTheSameOrder() {

            // The two blocks are written apart on purpose - the log from literals, so it holds on
            // a path where the game's settings may not be up, and the notice from the strings file
            // so its wording can be edited - and nothing but this holds them to the same readings
            // in the same order. A row added to one alone is what it catches.
            //
            // Compared by the values rather than by the rendered rows, so that a translated label
            // or a re-padded column is not a failure. Every row names exactly one run, and that
            // run is its value.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1");

            assertThat(failure.describeForLog())
                .containsSubsequence(readNoticeRowValues(failure));
        }

        @Test
        void carryTheSameNumberOfRows() {

            // The order check above passes over a row the log gained and the notice did not, the
            // values it looks for still being there in order. This is that other direction.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1");

            assertThat(countLogRows(failure))
                .isEqualTo(failure.describeRowsForPlayer().size());
        }

        @Test
        void dropTheSameRowWhereTheConsumerSaidNothingAboutWhatStillWorks() {

            // The one row either block may carry or leave out, and so the one most likely to be
            // dropped on a single side.
            var failure = CompatibilityFailureFixture.createFailureLosing(
                CompatibilityFailureFixture.LOST_FEATURE);

            assertThat(countLogRows(failure))
                .isEqualTo(failure.describeRowsForPlayer().size());
        }

        // The value of each notice row, in reading order.
        private String[] readNoticeRowValues(CompatibilityFailure failure) {

            return failure.describeRowsForPlayer()
                .stream()
                .map(row -> row.emphasisedRuns().get(0).runText())
                .toArray(String[]::new);
        }

        // The block's rows, which is every line of it but the heading.
        private int countLogRows(CompatibilityFailure failure) {

            return failure.describeForLog().split("\n").length - 1;
        }
    }

    @Nested
    class EmphasisedRuns {

        @ParameterizedTest
        @MethodSource("kmlib.starsector.compatibility.NoticeLineHighlights#listLocaleTags")
        void everyRunOfEveryLineHighlightsInEveryLocale(String localeTag) {

            // Walked over all five shapes a notice takes, because which runs a line names differs by
            // shape. The engine leaves a run plain, with no error, when the wording lacks it or a
            // character beside it is neither whitespace nor ASCII punctuation.
            NoticeLineHighlights.installLocaleStrings(localeTag);

            var lines = new ArrayList<CompatibilityNoticeLine>();

            for (var failure : List.of(
                    CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1"),
                    CompatibilityFailureFixture.createFailureBetweenVersions("v0.9.1", "v0.8.8"),
                    CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", null),
                    CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "0.8.8"),
                    CompatibilityFailureFixture.createFailure())) {

                lines.add(failure.describeHeadingForPlayer());
                lines.addAll(failure.describeDiagnosisForPlayer());
                lines.addAll(failure.describeRowsForPlayer());
                lines.add(failure.describeClosingForPlayer());
            }
            assertThat(NoticeLineHighlights.findUnhighlightedRuns(lines))
                .isEmpty();
        }
    }
}
