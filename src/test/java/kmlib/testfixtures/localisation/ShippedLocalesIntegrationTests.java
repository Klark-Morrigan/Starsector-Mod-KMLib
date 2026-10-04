package kmlib.testfixtures.localisation;

import com.fs.starfarer.api.Global;

import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reads KMLib's own {@code localisation/} directory, the one these fixtures find from the module a suite
 * runs in.
 */
final class ShippedLocalesIntegrationTests {

    @AfterEach
    void clearSettings() {
        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class ListLocaleTags {

        @Test
        void everyLocaleTheManifestDeclaresIsListedSorted() {

            assertThat(ShippedLocales.listLocaleTags())
                .containsExactly("en", "zh-hans");
        }
    }

    @Nested
    class InstallLocaleStrings {

        @Test
        void theGameAnswersWithTheLocalesOwnWording() {

            ShippedLocales.installLocaleStrings("en");

            assertThat(Global.getSettings().getString("kmlib", "compatibility_notice_title_error"))
                .isEqualTo("Error integrating");
        }

        @Test
        void aLocaleTheManifestDoesNotDeclareIsRefused() {

            assertThatThrownBy(() -> ShippedLocales.installLocaleStrings("fr"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The manifest declares no locale fr");
        }
    }
}
