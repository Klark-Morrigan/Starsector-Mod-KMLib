package kmlib.starsector.ui.sound;

import kmlib.testfixtures.starsector.ui.sound.UiSoundPlayerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what {@link UiSoundPlayer} does with a sound that may not be there - the one piece of behaviour
 * the port has rather than delegates. It is pinned here rather than at each control that resolves a role
 * and plays it, so a control's own cases can be about which moments it detects rather than each
 * restating what an absent sound does.
 */
final class UiSoundPlayerTest {

    private final UiSoundPlayerFake soundPlayerFake = new UiSoundPlayerFake();

    @Nested
    class PlaySoundIfPresent {

        @Test
        void playSoundIfPresentPlaysTheSoundItWasGiven() {
            
            soundPlayerFake.playSoundIfPresent(StarsectorUiSound.BUTTON_PRESSED);

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_PRESSED);
        }

        @Test
        void playSoundIfPresentPlaysNothingWhenThereIsNoSound() {
            // Nothing reaches the implementation at all, which is the point of the guard sitting on the
            // port: a recording player must not log a sound the player never made.
            soundPlayerFake.playSoundIfPresent(null);

            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }
    }
}
