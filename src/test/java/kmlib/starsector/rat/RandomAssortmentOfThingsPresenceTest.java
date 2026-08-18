package kmlib.starsector.rat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

            ModEnabledScopes.runWithModEnabled(true, () ->
                assertThat(RandomAssortmentOfThingsPresence.isModEnabled())
                    .isTrue());
        }

        @Test
        void reportsNotEnabledWhileTheModManagerSaysSo() {

            ModEnabledScopes.runWithModEnabled(false, () ->
                assertThat(RandomAssortmentOfThingsPresence.isModEnabled())
                    .isFalse());
        }

        @Test
        void reportsNotEnabledBeforeTheGameSettingsAreUp() {

            ModEnabledScopes.runWithoutGameSettings(() ->
                assertThat(RandomAssortmentOfThingsPresence.isModEnabled())
                    .isFalse());
        }

        @Test
        void reportsNotEnabledWhileTheSettingsCarryNoModManager() {

            ModEnabledScopes.runWithoutModManager(() ->
                assertThat(RandomAssortmentOfThingsPresence.isModEnabled())
                    .isFalse());
        }
    }
}
