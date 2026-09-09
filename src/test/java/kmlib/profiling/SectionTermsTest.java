package kmlib.profiling;

import kmlib.profiling.budget.ProfileBudget;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins that the terms a section states start from one default and replace one term at a time, so a
 * section names only what it changes and two terms stated together keep each other.
 */
final class SectionTermsTest {

    private static final long ONE_MILLISECOND_IN_NANOS = 1_000_000L;

    @Nested
    class Default {

        @Test
        void statesNothing() {

            assertThat(SectionTerms.DEFAULT.level())
                .isEqualTo(ProfileLevel.COARSE);
            assertThat(SectionTerms.DEFAULT.budget())
                .isSameAs(ProfileBudget.NO_BUDGET);
            assertThat(SectionTerms.DEFAULT.callLogThreshold())
                .isSameAs(CallLogThreshold.NO_LOGGING);
        }
    }

    @Nested
    class WithLevel {

        @Test
        void replacesTheLevelAndKeepsTheRest() {

            var budget = ProfileBudget.allowingDurationPerCall(() -> ONE_MILLISECOND_IN_NANOS);
            var terms = SectionTerms.DEFAULT.withBudget(budget).withLevel(ProfileLevel.FINE);

            assertThat(terms.level())
                .isEqualTo(ProfileLevel.FINE);
            assertThat(terms.budget())
                .isSameAs(budget);
        }
    }

    @Nested
    class WithBudget {

        @Test
        void replacesTheBudgetAndKeepsTheRest() {

            var budget = ProfileBudget.allowingDurationPerCall(() -> ONE_MILLISECOND_IN_NANOS);
            var terms = SectionTerms.DEFAULT
                .withCallLogThreshold(CallLogThreshold.LOGGING_EVERY_CALL)
                .withBudget(budget);

            assertThat(terms.budget())
                .isSameAs(budget);
            assertThat(terms.callLogThreshold())
                .isSameAs(CallLogThreshold.LOGGING_EVERY_CALL);
        }
    }

    @Nested
    class WithCallLogThreshold {

        @Test
        void replacesTheThresholdAndKeepsTheRest() {

            var terms = SectionTerms.DEFAULT
                .withLevel(ProfileLevel.FINE)
                .withCallLogThreshold(CallLogThreshold.LOGGING_EVERY_CALL);

            assertThat(terms.callLogThreshold())
                .isSameAs(CallLogThreshold.LOGGING_EVERY_CALL);
            assertThat(terms.level())
                .isEqualTo(ProfileLevel.FINE);
        }

        @Test
        void leavesTheTermsItWasCalledOnUnchanged() {
            // A value, so the default a hundred sections start from cannot be moved by one of them.
            SectionTerms.DEFAULT.withCallLogThreshold(CallLogThreshold.LOGGING_EVERY_CALL);

            assertThat(SectionTerms.DEFAULT.callLogThreshold())
                .isSameAs(CallLogThreshold.NO_LOGGING);
        }
    }
}
