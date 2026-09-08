package kmlib.profiling;

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
            var section = ProfileSection.registerSection("test.profileSection.bounded", budget);

            assertThat(ProfileSection.registerSection("test.profileSection.bounded"))
                .isSameAs(section);
            assertThat(section.getBudget())
                .isSameAs(budget);
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
}
