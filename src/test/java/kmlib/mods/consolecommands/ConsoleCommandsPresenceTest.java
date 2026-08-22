package kmlib.mods.consolecommands;

import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.testfixtures.starsector.settings.StubbedModIds.CONSOLE_COMMANDS;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the gate every read of this mod stands behind, across the series rather than in one mod:
 * what stands behind it is a read that resolves an {@code org.lazywizard.console} type, so a gate
 * that threw, or that answered on an id the mod does not have, would surface as a crash or as a
 * permanently absent console on the path whose whole purpose is to make the mod optional.
 *
 * <p>The cases name the mod through a spelling declared apart from the production one, so a Console
 * Commands release renaming its id fails here rather than silently turning every read into a
 * decline nobody notices.
 */
final class ConsoleCommandsPresenceTest {

    @Nested
    class IsModEnabled {

        @Test
        void reports_enabled_while_the_mod_manager_says_so() {

            ModStateScopes.runWithModEnabled(CONSOLE_COMMANDS, true, () ->
                assertThat(ConsoleCommandsPresence.isModEnabled())
                    .isTrue());
        }

        @Test
        void reports_not_enabled_while_the_mod_manager_says_so() {

            ModStateScopes.runWithModEnabled(CONSOLE_COMMANDS, false, () ->
                assertThat(ConsoleCommandsPresence.isModEnabled())
                    .isFalse());
        }

        @Test
        void reports_not_enabled_before_the_game_settings_are_up() {

            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(ConsoleCommandsPresence.isModEnabled())
                    .isFalse());
        }

        @Test
        void reports_not_enabled_while_the_settings_carry_no_mod_manager() {

            ModStateScopes.runWithoutModManager(() ->
                assertThat(ConsoleCommandsPresence.isModEnabled())
                    .isFalse());
        }
    }
}
