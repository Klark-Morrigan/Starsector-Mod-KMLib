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
 */
final class ModIntegrationTest {

    private static final String SUBJECT_MOD_ID = "assortment_of_things";

    private static final String SUBJECT_MOD_NAME = "Random Assortment of Things";

    private static final String SUBJECT_MOD_VERSION = "1.9.2";

    private static final String FAILURE_SITE = "installing the integration at start-up";

    private static final CompatibilityConsumer CONSUMER = CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER;

    private static final ModIntegration INTEGRATION =
        new ModIntegration(SUBJECT_MOD_ID, SUBJECT_MOD_NAME, CONSUMER);

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

                var failure = INTEGRATION.composeFailure(FAILURE_SITE, installationFailure);

                assertThat(failure.consumer())
                    .isEqualTo(CONSUMER);
                assertThat(failure.cause())
                    .isSameAs(installationFailure);
            });
        }

        @Test
        void refusesToComposeAFailureFromNothingThrown() {

            assertThatNullPointerException()
                .isThrownBy(() -> INTEGRATION.composeFailure(FAILURE_SITE, null));
        }
    }

    // The representative composition, against a throwable whose description every case that reads
    // the detail is asserted on.
    private static CompatibilityFailure composeFailure() {

        return INTEGRATION.composeFailure(
            FAILURE_SITE,
            new IllegalStateException("routes already registered"));
    }
}
