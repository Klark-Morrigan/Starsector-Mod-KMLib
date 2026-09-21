package kmlib.starsector.compatibility;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the one rule the subject holds: a blank version is an absent one, whichever side it is on.
 */
final class CompatibilitySubjectTest {

    private static final String UNKNOWN_WORDING = "?";

    @Nested
    class DescribeBuiltAgainstVersion {

        @Test
        void answersTheVersionWithoutThePrefixTheSubjectSelfReportsItWith() {

            // The label a version is shown under already says it is one, so the letter is noise -
            // and the two versions in a report come from two places that need not agree about
            // carrying it, which would show one with and one without.
            var subject = new CompatibilitySubject("Fast Rendering", "v0.8.8", null);

            assertThat(subject.describeBuiltAgainstVersion(UNKNOWN_WORDING))
                .isEqualTo("0.8.8");
        }

        @Test
        void keepsALeadingLetterThatOpensTheVersionItself() {

            // Stripped only where a digit follows, so a version that genuinely begins with a letter
            // is reported as its author spells it rather than beheaded.
            var subject = new CompatibilitySubject("Fast Rendering", "vanguard-3", null);

            assertThat(subject.describeBuiltAgainstVersion(UNKNOWN_WORDING))
                .isEqualTo("vanguard-3");
        }

        @Test
        void answersTheUnknownWordingForABlank() {

            // A stub build stamps a sentinel rather than a version; whichever value reaches here as
            // blank is a slot nothing filled, not a version that happens to be empty.
            var subject = new CompatibilitySubject("Fast Rendering", " ", null);

            assertThat(subject.describeBuiltAgainstVersion(UNKNOWN_WORDING))
                .isEqualTo(UNKNOWN_WORDING);
        }
    }

    @Nested
    class DescribeInstalledVersion {

        @Test
        void answersTheVersionWhereItWasRead() {

            var subject = new CompatibilitySubject("Fast Rendering", null, "v0.9.1");

            assertThat(subject.describeInstalledVersion(UNKNOWN_WORDING))
                .isEqualTo("0.9.1");
        }

        @Test
        void answersTheUnknownWordingForNull() {

            var subject = new CompatibilitySubject("Fast Rendering", null, null);

            assertThat(subject.describeInstalledVersion(UNKNOWN_WORDING))
                .isEqualTo(UNKNOWN_WORDING);
        }
    }

    @Nested
    class HasInstalledVersion {

        @Test
        void isTrueWhereTheVersionHasText() {

            assertThat(new CompatibilitySubject("Fast Rendering", null, "v0.9.1").hasInstalledVersion())
                .isTrue();
        }

        @Test
        void isFalseForABlank() {

            assertThat(new CompatibilitySubject("Fast Rendering", null, "").hasInstalledVersion())
                .isFalse();
        }
    }
}
