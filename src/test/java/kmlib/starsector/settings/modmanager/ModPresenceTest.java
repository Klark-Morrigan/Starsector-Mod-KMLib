package kmlib.starsector.settings.modmanager;

import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the read every optional-mod gate in the library stands behind, and in particular that it
 * answers rather than throws while the game is only part-built. Each gate calls it before touching
 * anything of the mod it is asking about, so a throw here would surface as a crash on the paths
 * whose whole purpose is to make those mods optional - which is the half of this that the hop
 * itself does not make obvious.
 */
final class ModPresenceTest {

    // A mod id of no significance: what is being pinned is the manner of asking, which is the same
    // whichever mod a caller names.
    private static final String MOD_ID = "some_mod";

    @Nested
    class IsModEnabled {

        @Test
        void reportsEnabledWhileTheModManagerSaysSo() {

            ModStateScopes.runWithModEnabled(MOD_ID, true, () ->
                assertThat(ModPresence.isModEnabled(MOD_ID))
                    .isTrue());
        }

        @Test
        void reportsNotEnabledWhileTheModManagerSaysSo() {

            ModStateScopes.runWithModEnabled(MOD_ID, false, () ->
                assertThat(ModPresence.isModEnabled(MOD_ID))
                    .isFalse());
        }

        @Test
        void reportsNotEnabledForAModTheGameWasNeverGiven() {
            // The id is what a caller brings, so a caller naming a mod this install does not have
            // is the ordinary case rather than an error.
            ModStateScopes.runWithModEnabled(MOD_ID, true, () ->
                assertThat(ModPresence.isModEnabled("a_mod_this_install_does_not_have"))
                    .isFalse());
        }

        @Test
        void reportsNotEnabledBeforeTheGameSettingsAreUp() {
            // A read taken outside a running game - a class being initialised, a plugin's earliest
            // hook - which has nothing to ask and must not throw for it.
            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(ModPresence.isModEnabled(MOD_ID))
                    .isFalse());
        }

        @Test
        void reportsNotEnabledWhileTheSettingsCarryNoModManager() {
            // The half-built state between a game that is up and one that is not, and the one a
            // gate written out by hand is easiest to leave out of.
            ModStateScopes.runWithoutModManager(() ->
                assertThat(ModPresence.isModEnabled(MOD_ID))
                    .isFalse());
        }

        @Test
        void reportsNotEnabledWhenNoModIsNamed() {

            ModStateScopes.runWithModEnabled(MOD_ID, true, () ->
                assertThat(ModPresence.isModEnabled(null))
                    .isFalse());
        }
    }
}
