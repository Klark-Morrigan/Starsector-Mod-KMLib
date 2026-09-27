package kmlib.settings;

import kmlib.testfixtures.starsector.settings.ModStateScopes;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * Pins that a bound mod's level follows its LunaLib field: applied once at the bind, and again on
 * each change to that mod's settings.
 *
 * <p>LunaLib's store is stood in by a static mock, and its mod set as up, since a running game is
 * what stands either up. The loggers are log4j's real ones, each case on a root of its own because
 * log4j loggers are process-global.
 */
final class LunaLogLevelBindingTest {

    private static final String MOD_ID = "kmlibtest_bound";
    private static final String FIELD_ID = "kmlibtest_logLevel";

    private static final String LUNALIB_MOD_ID = "lunalib";

    @Nested
    class BindLogLevel {

        @Test
        void appliesTheStoredLevelAtOnce() {

            ModStateScopes.runWithModEnabled(LUNALIB_MOD_ID, true, () -> {
                try (var lunaSettingsMock = mockStatic(LunaSettings.class)) {
                    lunaSettingsMock
                        .when(() -> LunaSettings.getString(MOD_ID, FIELD_ID))
                        .thenReturn("DEBUG");

                    LunaLogLevelBinding.bindLogLevel(MOD_ID, "kmlibtest_bound_at_once", FIELD_ID);

                    assertThat(Logger.getLogger("kmlibtest_bound_at_once").getLevel())
                        .isEqualTo(Level.DEBUG);
                }
            });
        }

        @Test
        void appliesTheLevelAgainOnAChangeToTheModsSettings() {
            // The point of the binding: the player retunes from the settings screen with no reload.
            ModStateScopes.runWithModEnabled(LUNALIB_MOD_ID, true, () -> {
                try (var lunaSettingsMock = mockStatic(LunaSettings.class)) {
                    lunaSettingsMock
                        .when(() -> LunaSettings.getString(MOD_ID, FIELD_ID))
                        .thenReturn("DEBUG", "ERROR");

                    LunaLogLevelBinding.bindLogLevel(MOD_ID, "kmlibtest_bound_on_change", FIELD_ID);

                    var listenerCaptor = ArgumentCaptor.forClass(LunaSettingsListener.class);

                    lunaSettingsMock.verify(() -> LunaSettings.addSettingsListener(listenerCaptor.capture()));
                    listenerCaptor.getValue().settingsChanged(MOD_ID);

                    assertThat(Logger.getLogger("kmlibtest_bound_on_change").getLevel())
                        .isEqualTo(Level.ERROR);
                }
            });
        }

        @Test
        void appliesTheLibraryDefaultWhereTheFieldIsUnset() {
            // LunaLib's null for a field with no stored value, as on a first run before the player
            // has opened the settings.
            ModStateScopes.runWithModEnabled(LUNALIB_MOD_ID, true, () -> {
                try (var lunaSettingsMock = mockStatic(LunaSettings.class)) {
                    LunaLogLevelBinding.bindLogLevel(MOD_ID, "kmlibtest_bound_unset", FIELD_ID);

                    assertThat(Logger.getLogger("kmlibtest_bound_unset").getLevel())
                        .isEqualTo(Level.WARN);
                }
            });
        }
    }
}
