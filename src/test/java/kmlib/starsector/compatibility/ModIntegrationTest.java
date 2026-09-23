package kmlib.starsector.compatibility;

import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;
import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Covers what a failed integration with another mod is reported as: which third party it names,
 * which of the two versions it can state, and what it puts where a probe's findings would go.
 *
 * <p>The version pair is the case worth the most. A mod integration is the mirror of a binding to a
 * renderer patch - nothing was compiled against it, so there is no built-against release, while the
 * installed one is published - and a composition filling those the way the other one does would
 * report a mismatch between two versions it never read.
 *
 * <p>Filing that report is covered apart from composing it, because the two answer different
 * questions: which slot each part lands in, and which consumer the composition runs against once
 * the record has decided what the report is filed under.
 */
final class ModIntegrationTest {

    private static final String SUBJECT_MOD_ID = CompatibilityFailureFixture.INTEGRATED_MOD_ID;

    private static final String SUBJECT_MOD_NAME = CompatibilityFailureFixture.INTEGRATED_MOD_NAME;

    private static final String SUBJECT_MOD_VERSION = "1.9.2";

    // Whatever the guard that caught the failure called itself. Deliberately not the phrase
    // WiringSteps files under: this composes the site it is handed and reads none of it, so
    // borrowing the production wording would imply a coupling that is not here.
    private static final String FAILURE_SITE = "catching it somewhere";

    private static final CompatibilityConsumer CONSUMER = CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER;

    private static final ModIntegration INTEGRATION = CompatibilityFailureFixture.MOD_INTEGRATION;

    @Nested
    class Construction {

        @Test
        void refusesAnIntegrationWithNoModId() {

            assertThatIllegalArgumentException()
                .isThrownBy(() -> new ModIntegration(" ", SUBJECT_MOD_NAME, CONSUMER));
        }

        @Test
        void refusesAnIntegrationWithNoModName() {

            assertThatIllegalArgumentException()
                .isThrownBy(() -> new ModIntegration(SUBJECT_MOD_ID, " ", CONSUMER));
        }

        @Test
        void refusesAnIntegrationWithNoConsumer() {

            assertThatNullPointerException()
                .isThrownBy(() -> new ModIntegration(SUBJECT_MOD_ID, SUBJECT_MOD_NAME, null));
        }
    }

    @Nested
    class ComposeFailure {

        @Test
        void namesTheThirdPartyRatherThanItsId() {
            // The name leads the modal's heading, so a report falling back to the ID would head
            // itself with a string no player has seen in their mod list.
            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(composeFailure().subject().name())
                    .isEqualTo(SUBJECT_MOD_NAME));
        }

        @Test
        void statesTheInstalledVersionTheModDeclares() {

            ModStateScopes.runWithModVersioned(SUBJECT_MOD_ID, SUBJECT_MOD_NAME, SUBJECT_MOD_VERSION, () ->
                assertThat(composeFailure().subject().installedVersion())
                    .isEqualTo(SUBJECT_MOD_VERSION));
        }

        @Test
        void statesNoBuiltAgainstVersionAtAll() {
            // Nothing was compiled against the mod, so there is no release a build could have
            // stamped - and a slot filled anyway would state a mismatch nobody measured.
            ModStateScopes.runWithModVersioned(SUBJECT_MOD_ID, SUBJECT_MOD_NAME, SUBJECT_MOD_VERSION, () ->
                assertThat(composeFailure().subject().builtAgainstVersion())
                    .isNull());
        }

        @Test
        void leavesTheInstalledVersionUnreadWhereTheGameCannotAnswer() {
            // A report composed before the mod set is readable still has to compose, which is what
            // an unknown version is for.
            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(composeFailure().subject().hasInstalledVersion())
                    .isFalse());
        }

        @Test
        void reportsWhatWasThrownAsTheDetailThatBroke() {
            // No member to point at: the step called into the mod and the mod refused, so the
            // throwable's own description is the whole of the finding.
            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(composeFailure().breakage().brokenDetail())
                    .isEqualTo("java.lang.IllegalStateException: routes already registered"));
        }

        @Test
        void filesUnderTheSiteTheGuardNamed() {

            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(composeFailure().breakage().failureSite())
                    .isEqualTo(FAILURE_SITE));
        }

        @Test
        void carriesTheConsumerAndWhatWasThrown() {

            ModStateScopes.runWithoutGameSettings(() -> {
                var installationFailure = new IllegalStateException("routes already registered");

                var failure = INTEGRATION.composeFailure(CONSUMER, FAILURE_SITE, installationFailure);

                assertThat(failure.consumer())
                    .isEqualTo(CONSUMER);
                assertThat(failure.cause())
                    .isSameAs(installationFailure);
            });
        }

        @Test
        void carriesTheConsumerItWasHandedRatherThanItsOwn() {
            // The record hands back the integration's consumer under a numbered key where it found
            // that key reused, and only the record knows whether it did - so the composition takes
            // the consumer rather than reading its own.
            ModStateScopes.runWithoutGameSettings(() -> {
                var failure = INTEGRATION.composeFailure(
                    CONSUMER.resolveConsumerAtPosition(2),
                    FAILURE_SITE,
                    new IllegalStateException("routes already registered"));

                assertThat(failure.consumer().consumerKey())
                    .isEqualTo("map-mod:map-overlay-2");
            });
        }

        @Test
        void refusesToComposeAFailureForNoConsumer() {

            assertThatNullPointerException()
                .isThrownBy(() -> INTEGRATION.composeFailure(
                    null,
                    FAILURE_SITE,
                    new IllegalStateException("routes already registered")));
        }

        @Test
        void refusesToComposeAFailureFromNothingThrown() {

            assertThatNullPointerException()
                .isThrownBy(() -> INTEGRATION.composeFailure(CONSUMER, FAILURE_SITE, null));
        }
    }

    @Nested
    class RecordFailure {

        private final CompatibilityFailures failureRecord = new CompatibilityFailures();

        @Test
        void recordsOneFailureUnderTheThirdPartysOwnModId() {

            ModStateScopes.runWithoutGameSettings(() -> {
                INTEGRATION.recordFailure(
                    failureRecord,
                    FAILURE_SITE,
                    new IllegalStateException("routes already registered"));

                var failure = failureRecord.takeNextUnreported();

                assertThat(failure.subject().name())
                    .isEqualTo(SUBJECT_MOD_NAME);
                assertThat(failure.consumer())
                    .isEqualTo(CONSUMER);
                assertThat(failureRecord.takeNextUnreported())
                    .isNull();
            });
        }

        @Test
        void filesASecondFeatureUnderOneKeyAsANumberedOne() {
            // The hand-off nothing else here would catch. Composed against this integration's own
            // consumer rather than the one the record settled on, a second feature the wiring mod
            // filed under one key would report under the first's key and read as that feature
            // failing twice.
            ModStateScopes.runWithoutGameSettings(() -> {
                var secondFeatureUnderOneKey = new ModIntegration(
                    SUBJECT_MOD_ID,
                    SUBJECT_MOD_NAME,
                    new CompatibilityConsumer(
                        CONSUMER.modId(),
                        CONSUMER.featureKey(),
                        "A second feature of the wiring mod stopped working."));

                INTEGRATION.recordFailure(
                    failureRecord,
                    FAILURE_SITE,
                    new IllegalStateException("routes already registered"));
                secondFeatureUnderOneKey.recordFailure(
                    failureRecord,
                    FAILURE_SITE,
                    new IllegalStateException("routes already registered"));

                failureRecord.takeNextUnreported();

                assertThat(failureRecord.takeNextUnreported().consumer().consumerKey())
                    .isEqualTo("map-mod:map-overlay-2");
            });
        }

        @Test
        void refusesToRecordWithNowhereToRecordInto() {

            assertThatNullPointerException()
                .isThrownBy(() -> INTEGRATION.recordFailure(
                    null,
                    FAILURE_SITE,
                    new IllegalStateException("routes already registered")));
        }
    }

    // The representative composition, against a throwable whose description every case that reads
    // the detail is asserted on.
    private static CompatibilityFailure composeFailure() {

        return INTEGRATION.composeFailure(
            CONSUMER,
            FAILURE_SITE,
            new IllegalStateException("routes already registered"));
    }
}
