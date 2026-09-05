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
    }
}
