package kmlib.starsector.rat;

import kmlib.testfixtures.starsector.settings.ModStateScopes;

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

    // Stated as a literal rather than read off the class under test, so a rename of that constant
    // is a failing case here rather than a pair of readers agreeing with each other about an id
    // the game does not have.
    private static final String RAT_MOD_ID = "assortment_of_things";

    @Nested
    class IsModEnabled {

        @Test
        void reportsEnabledWhileTheModManagerSaysSo() {

            ModStateScopes.runWithModEnabled(RAT_MOD_ID, true, () ->
                assertThat(RandomAssortmentOfThingsPresence.isModEnabled())
                    .isTrue());
        }

        @Test
        void reportsNotEnabledWhileTheModManagerSaysSo() {

            ModStateScopes.runWithModEnabled(RAT_MOD_ID, false, () ->
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
