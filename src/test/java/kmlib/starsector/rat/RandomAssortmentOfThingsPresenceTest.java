package kmlib.starsector.rat;

import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.testfixtures.starsector.settings.StubbedModIds.RANDOM_ASSORTMENT_OF_THINGS;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the gate every read of this mod stands behind, and in particular that it answers rather
 * than throws while the game is only part-built. Both readers here call it before touching
 * anything of the mod's, so a throw from it would surface as a crash on a path whose whole purpose
 * is to make the mod optional.
 */
final class RandomAssortmentOfThingsPresenceTest {

    @Nested
    class IsModEnabled {

        @Test
        void reportsEnabledWhileTheModManagerSaysSo() {

            ModStateScopes.runWithModEnabled(RANDOM_ASSORTMENT_OF_THINGS, true, () ->
                assertThat(RandomAssortmentOfThingsPresence.isModEnabled())
                    .isTrue());
        }

        @Test
        void reportsNotEnabledWhileTheModManagerSaysSo() {

            ModStateScopes.runWithModEnabled(RANDOM_ASSORTMENT_OF_THINGS, false, () ->
                assertThat(RandomAssortmentOfThingsPresence.isModEnabled())
                    .isFalse());
        }

        @Test
        void reportsNotEnabledBeforeTheGameSettingsAreUp() {

            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(RandomAssortmentOfThingsPresence.isModEnabled())
                    .isFalse());
        }

        @Test
        void reportsNotEnabledWhileTheSettingsCarryNoModManager() {

            ModStateScopes.runWithoutModManager(() ->
                assertThat(RandomAssortmentOfThingsPresence.isModEnabled())
                    .isFalse());
        }
    }
}
