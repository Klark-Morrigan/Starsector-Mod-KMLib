package kmlib.settings;

import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;
import kmlib.testfixtures.starsector.settings.StubbedModIds;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins which report a failed LunaLib binding files under: the library's own, and another mod's.
 *
 * <p>The installation itself binds through {@code LunaLogLevelBinding} and is pinned there.
 */
final class KmlibLunaSettingsTest {

    @Nested
    class DescribeLunaLibIntegration {

        @AfterEach
        void clearSettings() {

            StarsectorSettingsFake.clearSettings();
        }

        @Test
        void namesLunaLibAndTheSettingsFeatureTheBindingsCost() {
            // A transposition composes as plausibly as the right pairing, and reaches a player as a
            // report naming a mod that was working or a loss that did not happen.
            StarsectorSettingsFake.installSettings((category, key) -> "the sentence for " + key);

            var integration = KmlibLunaSettings.describeLunaLibIntegration();

            assertThat(integration.subjectModId())
                .isEqualTo(StubbedModIds.LUNALIB);
            assertThat(integration.subjectModName())
                .isEqualTo("LunaLib");
            assertThat(integration.consumer().consumerKey())
                .isEqualTo("kmlib:lunalib-settings");
            assertThat(integration.consumer().lostFeature())
                .isEqualTo("the sentence for compatibility_lost_lunalib_settings");
            assertThat(integration.consumer().unaffectedFeature())
                .isEqualTo("the sentence for compatibility_unaffected_lunalib_settings");
        }

        @Test
        void pairsLunaLibWithTheConsumerItWasGiven() {
            // The form another mod reports its own LunaLib bindings through: LunaLib as the subject,
            // and that mod's consumer carried as it was handed in.
            var consumer = CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER;
            var integration = KmlibLunaSettings.describeLunaLibIntegration(consumer);

            assertThat(integration.subjectModId())
                .isEqualTo(StubbedModIds.LUNALIB);
            assertThat(integration.subjectModName())
                .isEqualTo("LunaLib");
            assertThat(integration.consumer())
                .isSameAs(consumer);
        }
    }
}
