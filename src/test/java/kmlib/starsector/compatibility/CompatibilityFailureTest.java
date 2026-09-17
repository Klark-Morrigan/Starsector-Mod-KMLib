package kmlib.starsector.compatibility;

import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the two sentences a failure composes, and the version slots either of them may have to
 * write without.
 *
 * <p>The templates are spelled here as the shipped file spells them rather than read out of it, so
 * a case states the whole sentence it expects instead of agreeing with whatever wording the file
 * happens to carry. What holds the two together is
 * {@code KmlibStringKeysIntegrationTest}, which walks the file against the keys.
 */
final class CompatibilityFailureTest {

    private static final String SUBJECT_NAME = "Fast Rendering";

    private static final String BUILT_AGAINST_VERSION = "v0.8.8";

    private static final String INSTALLED_VERSION = "v0.9.1";

    private static final String LOST_FEATURE = "Sector map overlays will not respond to the cursor this session.";

    private static final String BROKEN_DETAIL = "com.genir.renderer.bridge.interfaces.GLCommand is absent";

    @AfterEach
    void clearSettings() {

        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class DescribeForPlayer {

        @Test
        void namesBothVersionsWhereTheInstalledOneWasRead() {

            installShippedTemplates();

            var failure = new CompatibilityFailure(
                SUBJECT_NAME,
                BUILT_AGAINST_VERSION,
                INSTALLED_VERSION,
                LOST_FEATURE,
                BROKEN_DETAIL,
                new NoClassDefFoundError(BROKEN_DETAIL));

            assertThat(failure.describeForPlayer())
                .isEqualTo("Fast Rendering version mismatch"
                    + "\n\nKMLib was built against Fast Rendering v0.8.8, and this install has v0.9.1."
                    + "\n\nSector map overlays will not respond to the cursor this session."
                    + " Everything else, including your save, is unaffected."
                    + " See starsector.log for which part is mismatched.");
        }

        @Test
        void saysTheInstalledVersionCouldNotBeReadWhereItIsAbsent() {

            installShippedTemplates();

            var failure = new CompatibilityFailure(
                SUBJECT_NAME,
                BUILT_AGAINST_VERSION,
                null,
                LOST_FEATURE,
                BROKEN_DETAIL,
                null);

            assertThat(failure.describeForPlayer())
                .contains("KMLib was built against Fast Rendering v0.8.8,"
                    + " and this install's version could not be read.")
                .doesNotContain("null");
        }

        @Test
        void rendersAnAbsentBuiltAgainstVersionAsTheUnknownParenthetical() {

            // What a build that compiled against the bridge stubs stamps, reaching the player as a
            // version slot nothing can fill.
            installShippedTemplates();

            var failure = new CompatibilityFailure(
                SUBJECT_NAME,
                "",
                INSTALLED_VERSION,
                LOST_FEATURE,
                BROKEN_DETAIL,
                null);

            assertThat(failure.describeForPlayer())
                .contains("KMLib was built against Fast Rendering (version unknown),"
                    + " and this install has v0.9.1.");
        }
    }

    @Nested
    class DescribeForLog {

        @Test
        void namesBothVersionsAndTheBrokenDetail() {

            // No settings installed: the log line is written from literals, so it holds on a path
            // where the game's settings may not be up yet.
            var failure = new CompatibilityFailure(
                SUBJECT_NAME,
                BUILT_AGAINST_VERSION,
                INSTALLED_VERSION,
                LOST_FEATURE,
                BROKEN_DETAIL,
                null);

            assertThat(failure.describeForLog())
                .isEqualTo("Fast Rendering compatibility failure."
                    + " KMLib was built against v0.8.8, and this install reports v0.9.1."
                    + " Broken: com.genir.renderer.bridge.interfaces.GLCommand is absent");
        }

        @Test
        void namesAnUnreadVersionAsUnknownRatherThanAsNull() {

            var failure = new CompatibilityFailure(
                SUBJECT_NAME,
                null,
                null,
                LOST_FEATURE,
                BROKEN_DETAIL,
                null);

            assertThat(failure.describeForLog())
                .isEqualTo("Fast Rendering compatibility failure."
                    + " KMLib was built against an unknown version,"
                    + " and this install reports an unknown version."
                    + " Broken: com.genir.renderer.bridge.interfaces.GLCommand is absent");
        }
    }

    // The wording KMLib ships, keyed as data/strings/strings.json keys it, answered for KMLib's
    // category alone so a lookup against the wrong category reads as missing wording.
    private void installShippedTemplates() {

        var templatesByKey = Map.of(
            "compatibility_notice_title",
            "%s version mismatch",
            "compatibility_notice_built_against",
            "KMLib was built against %s %s, and this install has %s.",
            "compatibility_notice_built_against_unreadable",
            "KMLib was built against %s %s, and this install's version could not be read."
                + " It is either newer and carries breaking changes, or too old for this build.",
            "compatibility_notice_consequence",
            "%s Everything else, including your save, is unaffected."
                + " See starsector.log for which part is mismatched.",
            "compatibility_notice_version_unknown",
            "(version unknown)");

        StarsectorSettingsFake.installSettings(
            (category, key) -> "kmlib".equals(category) ? templatesByKey.get(key) : null);
    }
}
