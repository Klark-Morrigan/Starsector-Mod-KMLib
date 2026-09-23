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
    class HasBuiltAgainstVersion {

        @Test
        void isTrueWhereTheBuildStampedOne() {

            assertThat(new CompatibilitySubject("Fast Rendering", "v0.8.8", null).hasBuiltAgainstVersion())
                .isTrue();
        }

        @Test
        void isFalseForABlank() {

            assertThat(new CompatibilitySubject("Fast Rendering", " ", null).hasBuiltAgainstVersion())
                .isFalse();
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

    @Nested
    class ResolveInstalledVersionRelation {

        @Test
        void readsAnInstallBehindTheBuildAsOlder() {

            assertThat(relationBetween("v0.9.1", "v0.8.9"))
                .isEqualTo(CompatibilitySubject.VersionRelation.OLDER);
        }

        @Test
        void readsAnInstallAheadOfTheBuildAsNewer() {

            assertThat(relationBetween("v0.8.9", "v0.9.1"))
                .isEqualTo(CompatibilitySubject.VersionRelation.NEWER);
        }

        @Test
        void readsTheSameReleaseAsSameWhateverThePrefix() {

            // The two come from two places that need not agree about carrying the letter.
            assertThat(relationBetween("v0.8.9", "0.8.9"))
                .isEqualTo(CompatibilitySubject.VersionRelation.SAME);
        }

        @Test
        void readsAMissingTrailingSegmentAsZero() {

            // 1.2 and 1.2.0 are one release, not two.
            assertThat(relationBetween("1.2", "1.2.0"))
                .isEqualTo(CompatibilitySubject.VersionRelation.SAME);
        }

        @Test
        void comparesSegmentsAsNumbersRatherThanAsText() {

            // Ten is after nine, which a character comparison gets the other way round.
            assertThat(relationBetween("0.9", "0.10"))
                .isEqualTo(CompatibilitySubject.VersionRelation.NEWER);
        }

        @Test
        void answersUnknownWhereTheInstalledVersionWasNotRead() {

            assertThat(relationBetween("v0.8.9", null))
                .isEqualTo(CompatibilitySubject.VersionRelation.UNKNOWN);
        }

        @Test
        void answersUnknownWhereTheBuildStampedNoVersion() {

            assertThat(relationBetween(null, "v0.9.1"))
                .isEqualTo(CompatibilitySubject.VersionRelation.UNKNOWN);
        }

        @Test
        void answersUnknownWhereAVersionCarriesNoNumber() {

            // A sentinel or a word in the slot is not a version to compare by.
            assertThat(relationBetween("unknown", "v0.9.1"))
                .isEqualTo(CompatibilitySubject.VersionRelation.UNKNOWN);
        }

        private CompatibilitySubject.VersionRelation relationBetween(
                String builtAgainstVersion,
                String installedVersion) {

            return new CompatibilitySubject("Fast Rendering", builtAgainstVersion, installedVersion)
                .resolveInstalledVersionRelation();
        }
    }
}
