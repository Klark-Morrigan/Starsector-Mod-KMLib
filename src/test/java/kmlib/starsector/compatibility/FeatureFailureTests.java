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
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Covers how a feature failure fills the slots of the lines it composes, and which runs of each
 * line it names as standing out.
 *
 * <p>The templates are stand-ins that expose their slots. {@code FeatureFailureIntegrationTests}
 * composes the notice out of the shipped wording.
 */
final class FeatureFailureTests {

    private static final String MOD_ID = CompatibilityFailureFixture.MAP_OVERLAY_MOD_ID;

    @AfterEach
    void clearSettings() {

        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class Constructor {

        @Test
        void refusesAFailureWithNoCause() {

            // A feature is switched off because something was thrown, and the trace is what finds
            // the fault.
            assertThatNullPointerException()
                .isThrownBy(() -> new FeatureFailure(CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER, null));
        }

        @Test
        void refusesAFailureWithNoConsumer() {

            assertThatNullPointerException()
                .isThrownBy(() -> new FeatureFailure(null, CompatibilityFailureFixture.FEATURE_CAUSE));
        }
    }

    @Nested
    class DescribeForPlayer {

        @BeforeEach
        void installSlotTemplates() {

            CompatibilitySlotTemplates.installSlotTemplates();
        }

        @Test
        void laysOutEveryLineInReadingOrder() {

            assertThat(CompatibilityFailureFixture.createFeatureFailure().describeForPlayer())
                .isEqualTo("feature-title{" + MOD_ID + "|erred}"
                    + "\n\ndiagnose-feature{" + MOD_ID + "|report-to " + MOD_ID + " dev}"
                    + "\n\nmod{" + MOD_ID + "}"
                    + "\nfeature{" + MOD_ID + ":map-overlay}"
                    + "\neffect{" + CompatibilityFailureFixture.LOST_FEATURE + "}"
                    + "\nnoeffect{" + CompatibilityFailureFixture.UNAFFECTED_FEATURE + "}"
                    + "\n\nseelog{starsector.log}");
        }
    }

    @Nested
    class DescribeHeadingForPlayer {

        @BeforeEach
        void installSlotTemplates() {

            CompatibilitySlotTemplates.installSlotTemplates();
        }

        @Test
        void bringsTheModForwardAndWarnsOnTheFailurePhrase() {

            assertThat(CompatibilityFailureFixture.createFeatureFailure().describeHeadingForPlayer().emphasisedRuns())
                .extracting(EmphasisedRun::runText, EmphasisedRun::emphasis)
                .containsExactly(
                    tuple(MOD_ID, Emphasis.HIGHLIGHT),
                    tuple("erred", Emphasis.WARNING));
        }
    }

    @Nested
    class DescribeDiagnosisForPlayer {

        @BeforeEach
        void installSlotTemplates() {

            CompatibilitySlotTemplates.installSlotTemplates();
        }

        @Test
        void bringsTheModToReportToForwardInsideTheWarning() {

            // The mod is named twice, once as the one at fault and once as where the report goes,
            // and each is brought forward on its own occurrence.
            var diagnosis = CompatibilityFailureFixture.createFeatureFailure().describeDiagnosisForPlayer();

            assertThat(diagnosis)
                .hasSize(1);
            assertThat(diagnosis.get(0).emphasisedRuns())
                .extracting(EmphasisedRun::runText, EmphasisedRun::emphasis)
                .containsExactly(
                    tuple(MOD_ID, Emphasis.HIGHLIGHT),
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
        void warnsOnWhatIsLostAndSetsAtEaseOnWhatIsNot() {

            assertThat(CompatibilityFailureFixture.createFeatureFailure().describeRowsForPlayer())
                .extracting(row -> row.emphasisedRuns().get(0).emphasis())
                .containsExactly(
                    Emphasis.HIGHLIGHT,
                    Emphasis.HIGHLIGHT,
                    Emphasis.WARNING,
                    Emphasis.REASSURANCE);
        }

        @Test
        void leavesOutTheNoEffectRowWhereTheConsumerSaidNothingAboutOne() {

            var consumer = new CompatibilityConsumer(
                MOD_ID,
                CompatibilityFailureFixture.MAP_OVERLAY_FEATURE_KEY,
                CompatibilityFailureFixture.LOST_FEATURE);

            assertThat(new FeatureFailure(consumer, CompatibilityFailureFixture.FEATURE_CAUSE).describeRowsForPlayer())
                .extracting(CompatibilityNoticeLine::lineText)
                .containsExactly(
                    "mod{" + MOD_ID + "}",
                    "feature{" + MOD_ID + ":map-overlay}",
                    "effect{" + CompatibilityFailureFixture.LOST_FEATURE + "}");
        }
    }

    @Nested
    class DescribeForLog {

        @Test
        void laysOutEveryReadingAsItsOwnRow() {

            // No settings installed: the block is written from literals.
            assertThat(CompatibilityFailureFixture.createFeatureFailure().describeForLog())
                .isEqualTo(MOD_ID + " ran into an error and switched one of its features off."
                    + "\n    Mod:           " + MOD_ID
                    + "\n    Feature:       " + MOD_ID + ":map-overlay"
                    + "\n    Effect:        " + CompatibilityFailureFixture.LOST_FEATURE
                    + "\n    No effect:     " + CompatibilityFailureFixture.UNAFFECTED_FEATURE);
        }
    }
}
