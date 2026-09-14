package kmlib.profiling;

import kmlib.profiling.budget.ProfileBudget;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Pins that a section is an identity rather than a string: one name resolves to one instance
 * however often it is asked for, which is what lets the profiler find a row by comparing
 * references and what keeps a block opened by name and one opened by section on a single row.
 */
final class ProfileSectionTest {

    private static final String SECTION_NAME = "test.profileSection.render";

    private static final long ONE_MILLISECOND_IN_NANOS = 1_000_000L;

    @Nested
    class RegisterSection {

        @Test
        void returnsTheSameSectionForTheSameName() {

            var section = ProfileSection.registerSection(SECTION_NAME);

            assertThat(ProfileSection.registerSection(SECTION_NAME))
                .isSameAs(section);
        }

        @Test
        void returnsDistinctSectionsForDistinctNames() {

            assertThat(ProfileSection.registerSection("test.profileSection.build"))
                .isNotSameAs(ProfileSection.registerSection("test.profileSection.draw"));
        }

        @Test
        void keepsTheNameItWasRegisteredUnder() {

            assertThat(ProfileSection.registerSection(SECTION_NAME).getName())
                .isEqualTo(SECTION_NAME);
        }

        @Test
        void refusesAnUnnamedSection() {

            assertThatNullPointerException()
                .isThrownBy(() -> ProfileSection.registerSection(null));
        }

        @Test
        void keepsTheBudgetTheNameWasFirstRegisteredWith() {
            // A section states what its calls are allowed once, beside the constant holding it, so
            // a later resolve of the name gets the bound rather than replacing it.
            var budget = ProfileBudget.allowingDurationPerCall(() -> ONE_MILLISECOND_IN_NANOS);
            var section = ProfileSection.registerSection(
                "test.profileSection.bounded",
                SectionTerms.DEFAULT.withBudget(budget));

            assertThat(ProfileSection.registerSection("test.profileSection.bounded"))
                .isSameAs(section);
            assertThat(section.getBudget())
                .isSameAs(budget);
        }

        @Test
        void keepsTheLevelTheNameWasFirstRegisteredWith() {
            // Stated once beside the constant holding the section, like the bound above: a later
            // resolve of the name is a call site asking for the row, not one redeclaring it.
            var section = ProfileSection.registerSection(
                "test.profileSection.perItem",
                SectionTerms.DEFAULT.withLevel(ProfileLevel.FINE));

            assertThat(ProfileSection.registerSection("test.profileSection.perItem").getLevel())
                .isEqualTo(ProfileLevel.FINE);
            assertThat(section.getLevel())
                .isEqualTo(ProfileLevel.FINE);
        }

        @Test
        void keepsTheCallLogThresholdTheNameWasFirstRegisteredWith() {
            // Stated once like the two above, so a site opening the section does not have to
            // repeat - or contradict - what makes one of its calls worth a line.
            var section = ProfileSection.registerSection(
                "test.profileSection.logged",
                SectionTerms.DEFAULT.withCallLogThreshold(CallLogThreshold.LOGGING_EVERY_CALL));

            assertThat(ProfileSection.registerSection("test.profileSection.logged"))
                .isSameAs(section);
            assertThat(section.getCallLogThreshold())
                .isSameAs(CallLogThreshold.LOGGING_EVERY_CALL);
        }
    }

    @Nested
    class GetLevel {

        @Test
        void answersCoarseForASectionThatStatedNoLevel() {
            // The beats and passes a frame is made of, which is nearly every section: they are
            // timed by any capture that is running at all.
            assertThat(ProfileSection.registerSection(SECTION_NAME).getLevel())
                .isEqualTo(ProfileLevel.COARSE);
        }
    }

    @Nested
    class GetBudget {

        @Test
        void answersTheSharedNothingForASectionThatStatedNoBound() {
            // Most sections, and the answer a caller tells by reference: a close on a per-frame path
            // costs one comparison rather than a check nobody stated.
            assertThat(ProfileSection.registerSection(SECTION_NAME).getBudget())
                .isSameAs(ProfileBudget.NO_BUDGET);
        }
    }

    @Nested
    class GetCallLogThreshold {

        @Test
        void answersTheSharedNothingForASectionThatStatedNoThreshold() {
            // Most sections: their calls are read in the report, and a line per call of every
            // section would bury the few passes somebody registered to follow.
            assertThat(ProfileSection.registerSection(SECTION_NAME).getCallLogThreshold())
                .isSameAs(CallLogThreshold.NO_LOGGING);
        }
    }
}
