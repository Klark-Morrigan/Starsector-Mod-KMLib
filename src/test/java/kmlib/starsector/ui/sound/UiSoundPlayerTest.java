package kmlib.starsector.ui.sound;

import kmlib.testfixtures.starsector.ui.sound.UiSoundPlayerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what {@link UiSoundPlayer} does with a cue that may not be there - the one piece of behaviour the
 * port has rather than delegates. It is pinned here rather than at each control that resolves a cue and
 * plays it, so a control's own cases can be about which moments it detects rather than each restating
 * what an absent cue does.
 */
final class UiSoundPlayerTest {

    private static final float QUIETER_VOLUME = 0.25f;

    private final UiSoundPlayerFake soundPlayerFake = new UiSoundPlayerFake();

    @Nested
    class PlayCueIfPresent {

        @Test
        void playCueIfPresentPlaysTheCueItWasGivenAtTheVolumeItNames() {
            // Role and volume both reach the player, which is what the pair exists for: a seam that
            // carried only the role would leave every volume to be applied by whoever plays it.
            var soundCue = new UiSoundCue(StarsectorUiSound.BUTTON_MOUSEOVER, QUIETER_VOLUME);

            soundPlayerFake.playCueIfPresent(soundCue);

            assertThat(soundPlayerFake.getPlayedCues())
                .containsExactly(new UiSoundCue(StarsectorUiSound.BUTTON_MOUSEOVER, 0.25f));
        }

        @Test
        void playCueIfPresentPlaysNothingWhenThereIsNoCue() {
            // Nothing reaches the implementation at all, which is the point of the guard sitting on the
            // port: a recording player must not log a sound the player never made.
            soundPlayerFake.playCueIfPresent(null);

            assertThat(soundPlayerFake.getPlayedCues())
                .isEmpty();
        }
    }
}
