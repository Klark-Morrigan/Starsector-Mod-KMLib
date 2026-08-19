package kmlib.starsector.settings;

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
        void reports_enabled_while_the_mod_manager_says_so() {

            ModStateScopes.runWithModEnabled(MOD_ID, true, () ->
                assertThat(ModPresence.isModEnabled(MOD_ID))
                    .isTrue());
        }

        @Test
        void reports_not_enabled_while_the_mod_manager_says_so() {

            ModStateScopes.runWithModEnabled(MOD_ID, false, () ->
                assertThat(ModPresence.isModEnabled(MOD_ID))
                    .isFalse());
        }

        @Test
        void reports_not_enabled_for_a_mod_the_game_was_never_given() {
            // The id is what a caller brings, so a caller naming a mod this install does not have
            // is the ordinary case rather than an error.
            ModStateScopes.runWithModEnabled(MOD_ID, true, () ->
                assertThat(ModPresence.isModEnabled("a_mod_this_install_does_not_have"))
                    .isFalse());
        }

        @Test
        void reports_not_enabled_before_the_game_settings_are_up() {
            // A read taken outside a running game - a class being initialised, a plugin's earliest
            // hook - which has nothing to ask and must not throw for it.
            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(ModPresence.isModEnabled(MOD_ID))
                    .isFalse());
        }

        @Test
        void reports_not_enabled_while_the_settings_carry_no_mod_manager() {
            // The half-built state between a game that is up and one that is not, and the one a
            // gate written out by hand is easiest to leave out of.
            ModStateScopes.runWithoutModManager(() ->
                assertThat(ModPresence.isModEnabled(MOD_ID))
                    .isFalse());
        }

        @Test
        void reports_not_enabled_when_no_mod_is_named() {

            ModStateScopes.runWithModEnabled(MOD_ID, true, () ->
                assertThat(ModPresence.isModEnabled(null))
                    .isFalse());
        }
    }
}
