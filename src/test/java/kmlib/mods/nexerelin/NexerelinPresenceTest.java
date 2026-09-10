package kmlib.mods.nexerelin;

import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.testfixtures.starsector.settings.StubbedModIds.NEXERELIN;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the gate every routine in this package stands behind. What is this class's own, rather than
 * the shared read beneath it, is the id: the cases name the mod through a spelling declared apart
 * from the production one, so an id that stops matching the mod's own fails here rather than
 * silently turning every deferral into a decline nobody notices.
 */
final class NexerelinPresenceTest {

    @Nested
    class IsModEnabled {

        @Test
        void reportsEnabledWhileTheModManagerSaysSo() {

            ModStateScopes.runWithModEnabled(NEXERELIN, true, () ->
                assertThat(NexerelinPresence.isModEnabled())
                    .isTrue());
        }

        @Test
        void reportsNotEnabledWhileTheModManagerSaysSo() {

            ModStateScopes.runWithModEnabled(NEXERELIN, false, () ->
                assertThat(NexerelinPresence.isModEnabled())
                    .isFalse());
        }

        @Test
        void reportsNotEnabledBeforeTheGameSettingsAreUp() {

            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(NexerelinPresence.isModEnabled())
                    .isFalse());
        }
    }
}
