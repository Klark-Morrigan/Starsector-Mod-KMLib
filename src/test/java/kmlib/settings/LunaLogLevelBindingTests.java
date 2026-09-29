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
 * Pins that a bound mod's level follows its LunaLib field - applied once at the bind and again on
 * each change to that mod's settings - and what setting it does to log4j: the level reaches the whole
 * subtree through inheritance, whitespace is tolerated, an unset or unrecognised name falls back to
 * the library default, and loggers outside the subtree are left alone.
 *
 * <p>LunaLib's store is stood in by a static mock, and its mod set as up, since a running game is
 * what stands either up. The loggers are log4j's real ones, since that inheritance is exactly what
 * the binding relies on; each case binds a root of its own because log4j loggers are process-global.
 */
final class LunaLogLevelBindingTests {

    private static final String MOD_ID = "kmlibtest_bound";
    private static final String FIELD_ID = "kmlibtest_logLevel";

    private static final String LUNALIB_MOD_ID = "lunalib";

    @Nested
    class BindLogLevel {

        @Test
        void appliesTheStoredLevelAtOnce() {

            bindWithStoredLevel("kmlibtest_bound_at_once", "DEBUG");

            assertThat(Logger.getLogger("kmlibtest_bound_at_once").getLevel())
                .isEqualTo(Level.DEBUG);
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
        void setsTheLevelOnEveryLoggerBeneathTheRoot() {

            var descendant = Logger.getLogger("kmlibtest_bound_inherited.child.grandchild");

            bindWithStoredLevel("kmlibtest_bound_inherited", "DEBUG");

            assertThat(descendant.getEffectiveLevel())
                .isEqualTo(Level.DEBUG);
        }

        @Test
        void toleratesWhitespaceAroundTheStoredName() {

            bindWithStoredLevel("kmlibtest_bound_padded", "  ERROR  ");

            assertThat(Logger.getLogger("kmlibtest_bound_padded").getLevel())
                .isEqualTo(Level.ERROR);
        }

        @Test
        void appliesTheLibraryDefaultWhereTheFieldIsUnset() {
            // LunaLib's null for a field with no stored value, as on a first run before the player
            // has opened the settings.
            bindWithStoredLevel("kmlibtest_bound_unset", null);

            assertThat(Logger.getLogger("kmlibtest_bound_unset").getLevel())
                .isEqualTo(Level.WARN);
        }

        @Test
        void appliesTheLibraryDefaultWhereTheStoredNameIsNoLevel() {

            bindWithStoredLevel("kmlibtest_bound_unrecognised", "nonsense");

            assertThat(Logger.getLogger("kmlibtest_bound_unrecognised").getLevel())
                .isEqualTo(Level.WARN);
        }

        @Test
        void leavesLoggersOutsideTheSubtreeAlone() {

            var sibling = Logger.getLogger("kmlibtest_bound_sibling_outside");
            var siblingBefore = sibling.getEffectiveLevel();

            bindWithStoredLevel("kmlibtest_bound_subtree", "OFF");

            assertThat(sibling.getEffectiveLevel())
                .isEqualTo(siblingBefore);
        }
    }

    @Nested
    class DefaultLevel {

        @Test
        void isWarn() {
            // Pins the shared fallback every binding applies where no level is set, so mods do not
            // restate a default of their own.
            assertThat(LunaLogLevelBinding.DEFAULT_LEVEL)
                .isEqualTo(Level.WARN);
        }
    }

    // Binds the root with LunaLib holding the given level name for the field, or nothing for null.
    private static void bindWithStoredLevel(String loggerRoot, String storedLevelName) {

        ModStateScopes.runWithModEnabled(LUNALIB_MOD_ID, true, () -> {
            try (var lunaSettingsMock = mockStatic(LunaSettings.class)) {
                lunaSettingsMock
                    .when(() -> LunaSettings.getString(MOD_ID, FIELD_ID))
                    .thenReturn(storedLevelName);

                LunaLogLevelBinding.bindLogLevel(MOD_ID, loggerRoot, FIELD_ID);
            }
        });
    }
}
