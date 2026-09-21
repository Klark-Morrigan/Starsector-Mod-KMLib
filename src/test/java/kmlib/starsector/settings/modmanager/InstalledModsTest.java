package kmlib.starsector.settings.modmanager;

import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins that a caller composing a report can ask the game a mod's name without the asking becoming
 * the thing that fails.
 *
 * <p>The reason is the same one {@link ModPresence} beside it exists for, and sharper here: the
 * caller is already reporting that something broke, so a throw on the way to naming who it was about
 * would lose the whole report. Every state the game can be in on the way down to a mod spec answers
 * instead - no settings, settings carrying no mod manager, a manager listing no such mod.
 */
final class InstalledModsTest {

    private static final String MOD_ID = "some_mod";

    private static final String MOD_NAME = "Some Mod";

    @Nested
    class ReadModName {

        @Test
        void answersTheNameTheGameHoldsForThatMod() {

            ModStateScopes.runWithModNamed(MOD_ID, MOD_NAME, () ->
                assertThat(InstalledMods.readModName(MOD_ID))
                    .isEqualTo(MOD_NAME));
        }

        @Test
        void answersNothingForAModTheGameListsNoSpecFor() {

            // What an ID naming no installed mod meets, invented or merely misspelled - and the
            // branch a report's fallback to the bare ID stands on.
            ModStateScopes.runWithModNamed(MOD_ID, MOD_NAME, () ->
                assertThat(InstalledMods.readModName("a_mod_this_install_does_not_have"))
                    .isNull());
        }

        @Test
        void answersNothingBeforeTheGameSettingsAreUp() {

            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(InstalledMods.readModName(MOD_ID))
                    .isNull());
        }

        @Test
        void answersNothingWhileTheSettingsCarryNoModManager() {

            // The half-built state between a game that is up and one that is not, and the one a
            // guard written out by hand is easiest to leave out of.
            ModStateScopes.runWithoutModManager(() ->
                assertThat(InstalledMods.readModName(MOD_ID))
                    .isNull());
        }

        @Test
        void answersNothingWhenNoModIsNamed() {

            ModStateScopes.runWithModNamed(MOD_ID, MOD_NAME, () ->
                assertThat(InstalledMods.readModName(null))
                    .isNull());
        }
    }
}
