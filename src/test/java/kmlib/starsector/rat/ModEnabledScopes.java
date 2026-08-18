package kmlib.starsector.rat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Answers whether Random Assortment of Things is enabled for the length of one body - the gate
 * every read in this package stands behind, reached through {@code Global} and so standable-in for
 * only within a scope.
 *
 * <p>Shared rather than written out per suite because each read here opens by settling the same
 * hop, and a suite spelling it out itself would be free to stub a different mod id than the one
 * the class under test asks for and still pass. The id is a literal rather than the production
 * constant, so a rename of that constant is something a case still notices.
 *
 * <p>The two half-built states are scopes of their own rather than arguments, because what they
 * stand for is the game not being up yet rather than a mod being off - a distinction a boolean
 * would flatten at the call site.
 */
final class ModEnabledScopes {

    private static final String RAT_MOD_ID = "assortment_of_things";

    private ModEnabledScopes() {
    }

    /**
     * Runs body with the mod set readable and the mod enabled or not, as asked.
     *
     * @param isModEnabled what the mod manager reports for Random Assortment of Things
     * @param body         the case to run inside the scope
     */
    static void runWithModEnabled(boolean isModEnabled, Runnable body) {

        try (var globalMock = mockStatic(Global.class)) {

            var settingsMock = mock(SettingsAPI.class);
            var modManagerMock = mock(ModManagerAPI.class);

            globalMock
                .when(Global::getSettings)
                .thenReturn(settingsMock);

            when(settingsMock.getModManager())
                .thenReturn(modManagerMock);

            when(modManagerMock.isModEnabled(RAT_MOD_ID))
                .thenReturn(isModEnabled);

            body.run();
        }
    }

    /**
     * Runs body with no settings at all - a read taken before the game has stood them up.
     *
     * @param body the case to run inside the scope
     */
    static void runWithoutGameSettings(Runnable body) {

        try (var globalMock = mockStatic(Global.class)) {

            globalMock
                .when(Global::getSettings)
                .thenReturn(null);

            body.run();
        }
    }

    /**
     * Runs body with settings up but no mod manager on them - the half-built state between the
     * two, and the one a guard is easiest to leave out of.
     *
     * @param body the case to run inside the scope
     */
    static void runWithoutModManager(Runnable body) {

        try (var globalMock = mockStatic(Global.class)) {

            var settingsMock = mock(SettingsAPI.class);

            globalMock
                .when(Global::getSettings)
                .thenReturn(settingsMock);

            when(settingsMock.getModManager())
                .thenReturn(null);

            body.run();
        }
    }
}
