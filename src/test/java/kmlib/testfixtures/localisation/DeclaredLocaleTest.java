package kmlib.testfixtures.localisation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two named ways a locale declaration is built. The shape rules the constructor holds are read
 * through a manifest file in {@link LocaleManifestTest}, which is where a declaration comes from.
 */
final class DeclaredLocaleTest {

    private static final URI CORE_LOCALISATION = URI.create("https://github.com/example/localisation");

    @Nested
    class CreateLocaleWithCoreLocalisation {

        @Test
        void theLocaleNamesTheProjectSupplyingItsGlyphs() {

            var locale = DeclaredLocale.createLocaleWithCoreLocalisation("zh-hans", "Name", CORE_LOCALISATION);

            assertThat(locale.localeTag())
                .isEqualTo("zh-hans");
            assertThat(locale.displayName())
                .isEqualTo("Name");
            assertThat(locale.coreLocalisation())
                .contains(CORE_LOCALISATION);
        }
    }

    @Nested
    class CreateLocaleWithoutCoreLocalisation {

        @Test
        void theLocaleNamesNoProject() {

            var locale = DeclaredLocale.createLocaleWithoutCoreLocalisation("en", "English");

            assertThat(locale.localeTag())
                .isEqualTo("en");
            assertThat(locale.displayName())
                .isEqualTo("English");
            assertThat(locale.coreLocalisation())
                .isEmpty();
        }
    }
}
