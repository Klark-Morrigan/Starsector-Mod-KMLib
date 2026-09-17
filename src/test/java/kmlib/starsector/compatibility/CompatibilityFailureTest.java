package kmlib.starsector.compatibility;

import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers how a failure fills the slots of the two sentences it composes: which value lands in which
 * slot, and what stands in a version slot nothing could fill.
 *
 * <p>The player-facing templates are stand-ins that expose their slots rather than copies of the
 * shipped wording: a copy agrees with the code however the shipped file is edited, so it is
 * {@code CompatibilityFailureIntegrationTest} that composes the sentence a player reads.
 */
final class CompatibilityFailureTest {

    private static final String LOST_FEATURE = "Overlays will not respond to the cursor this session.";

    private static final String BROKEN_DETAIL = "GLCommand is absent";

    @AfterEach
    void clearSettings() {

        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class DescribeForPlayer {

        @BeforeEach
        void installSlotTemplates() {

            // Each template names its key and lists its slots in order, so an assertion reads which
            // value was put where without knowing any wording.
            var templatesByKey = Map.of(
                "compatibility_notice_title", "title[%s]",
                "compatibility_notice_built_against", "built[%s|%s|%s]",
                "compatibility_notice_built_against_unreadable", "unreadable[%s|%s]",
                "compatibility_notice_consequence", "consequence[%s]",
                "compatibility_notice_version_unknown", "?");

            StarsectorSettingsFake.installSettings((category, key) -> templatesByKey.get(key));
        }

        @Test
        void fillsBothVersionsWhereTheInstalledOneWasRead() {

            var failure = createFailure("v0.8.8", "v0.9.1");

            assertThat(failure.describeForPlayer())
                .isEqualTo("title[Fast Rendering]"
                    + "\n\nbuilt[Fast Rendering|v0.8.8|v0.9.1]"
                    + "\n\nconsequence[" + LOST_FEATURE + "]");
        }

        @Test
        void switchesToTheUnreadableWordingWhereTheInstalledVersionIsAbsent() {

            var failure = createFailure("v0.8.8", null);

            assertThat(failure.describeForPlayer())
                .contains("unreadable[Fast Rendering|v0.8.8]")
                .doesNotContain("null");
        }

        @Test
        void standsTheUnknownWordingInForAnAbsentBuiltAgainstVersion() {

            // A blank is absent too, so a version that arrives as an empty string reaches the
            // player as the unknown wording rather than as a hole in the sentence.
            var failure = createFailure("", "v0.9.1");

            assertThat(failure.describeForPlayer())
                .contains("built[Fast Rendering|?|v0.9.1]");
        }
    }

    @Nested
    class DescribeForLog {

        @Test
        void namesBothVersionsAndTheBrokenDetail() {

            // No settings installed: the log line is written from literals, so it holds on a path
            // where the game's settings may not be up yet.
            var failure = createFailure("v0.8.8", "v0.9.1");

            assertThat(failure.describeForLog())
                .isEqualTo("Fast Rendering compatibility failure."
                    + " KMLib was built against v0.8.8, and this install reports v0.9.1."
                    + " Broken: GLCommand is absent");
        }

        @Test
        void namesAnUnreadVersionAsUnknownRatherThanAsNull() {

            var failure = createFailure(null, null);

            assertThat(failure.describeForLog())
                .isEqualTo("Fast Rendering compatibility failure."
                    + " KMLib was built against an unknown version,"
                    + " and this install reports an unknown version."
                    + " Broken: GLCommand is absent");
        }
    }

    // The versions are what the cases vary; everything else names one representative failure.
    private static CompatibilityFailure createFailure(String builtAgainstVersion, String installedVersion) {

        return new CompatibilityFailure(
            new CompatibilitySubject("Fast Rendering", builtAgainstVersion, installedVersion),
            LOST_FEATURE,
            BROKEN_DETAIL,
            null);
    }
}
