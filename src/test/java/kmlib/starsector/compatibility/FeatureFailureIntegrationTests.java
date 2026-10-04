package kmlib.starsector.compatibility;

import kmlib.testfixtures.localisation.ShippedLocales;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;
import kmlib.testfixtures.starsector.strings.ShippedStrings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Composes a feature failure's notice out of the wording KMLib actually ships.
 *
 * <p>Checks what the unit suite cannot: that the shipped templates and the order they are filled in
 * still agree, and that every run a line names as standing out is in that line in the order given.
 */
final class FeatureFailureIntegrationTests {

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
        void readsAsTheNotice() {

            assertThat(CompatibilityFailureFixture.createFeatureFailure().describeForPlayer())
                .isEqualTo(MOD_ID + " ran into an error and switched one of its features off."
                    + "\n\nThis is a fault in " + MOD_ID + " itself rather than a clash with another mod,"
                    + " so it's better to report this issue to the " + MOD_ID + " developer."
                    + "\n\n    Mod:           " + MOD_ID
                    + "\n    Feature:       " + MOD_ID + ":map-overlay"
                    + "\n    Effect:        " + CompatibilityFailureFixture.LOST_FEATURE
                    + "\n    No effect:     " + CompatibilityFailureFixture.UNAFFECTED_FEATURE
                    + "\n\nSee starsector.log for more details.");
        }
    }

    @Nested
    class LogBlockAgainstNoticeRows {

        @Test
        void carryTheSameReadingsInTheSameOrder() {

            // Written apart on purpose - the log from literals, the notice from the strings file -
            // so nothing but this holds the two to the same rows.
            var failure = CompatibilityFailureFixture.createFeatureFailure();

            assertThat(failure.describeForLog())
                .containsSubsequence(NoticeReadings.readNoticeRowValues(failure));
        }

        @Test
        void carryTheSameNumberOfRows() {

            var failure = CompatibilityFailureFixture.createFeatureFailure();

            assertThat(NoticeReadings.countLogRows(failure))
                .isEqualTo(failure.describeRowsForPlayer().size());
        }
    }

    @Nested
    class EmphasisedRuns {

        @ParameterizedTest
        @MethodSource("kmlib.testfixtures.localisation.ShippedLocales#listLocaleTags")
        void everyRunOfEveryLineHighlightsInEveryLocale(String localeTag) {

            // The engine leaves a run plain, with no error, when the wording lacks it or a character
            // beside it is neither whitespace nor ASCII punctuation - the case for most Chinese text.
            ShippedLocales.installLocaleStrings(localeTag);

            var lines = NoticeReadings.listNoticeLines(CompatibilityFailureFixture.createFeatureFailure());

            assertThat(NoticeLineHighlights.findUnhighlightedRuns(lines))
                .isEmpty();
        }
    }
}
