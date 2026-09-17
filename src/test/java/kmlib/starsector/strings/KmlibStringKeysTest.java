package kmlib.starsector.strings;

import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the one thing the category-bound accessors can get wrong: which category they bind.
 *
 * <p>A holder forwarding the wrong one would answer {@code [REDACTED]} for every key KMLib has,
 * everywhere, and read as missing wording rather than as a wrong lookup - so the category is
 * asserted as the literal the strings file declares rather than through the constant, which would
 * agree with the holder however either was edited.
 */
final class KmlibStringKeysTest {

    // What the settings object was asked for, filled by the resolver each case installs.
    private final String[] requestedCategoryAndKey = new String[2];

    @AfterEach
    void clearSettings() {

        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class Get {

        @Test
        void bindsKmlibsCategoryToTheRequestedKey() {

            installRecordingStrings("Jump-point");

            var value = KmlibStringKeys.get(KmlibStringKeys.JUMP_POINT_LABEL);

            assertThat(requestedCategoryAndKey)
                .containsExactly("kmlib", "jump_point_label");
            assertThat(value)
                .isEqualTo("Jump-point");
        }
    }

    @Nested
    class Format {

        @Test
        void bindsKmlibsCategoryAndFillsTheTemplate() {

            installRecordingStrings("%s version mismatch");

            var value = KmlibStringKeys.format(
                KmlibStringKeys.COMPATIBILITY_NOTICE_TITLE,
                "Fast Rendering");

            assertThat(requestedCategoryAndKey)
                .containsExactly("kmlib", "compatibility_notice_title");
            assertThat(value)
                .isEqualTo("Fast Rendering version mismatch");
        }
    }

    // A settings object answering every lookup with one wording and recording what it was asked
    // for, so a case reads both halves of the delegation off the same call.
    private void installRecordingStrings(String wording) {

        StarsectorSettingsFake.installSettings((category, key) -> {
            requestedCategoryAndKey[0] = category;
            requestedCategoryAndKey[1] = key;
            return wording;
        });
    }
}
