package kmlib.starsector.ui.sound;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins what a cue may be built out of. A pair this small carries only two faults - a factory whose name
 * and content have drifted apart, and a value the engine has no meaning for reaching the sound player -
 * and both fail where nothing on screen shows them: a wrong volume is heard, at best, as a panel that
 * feels off.
 */
final class UiSoundCueTest {

    private static final float QUIETER_VOLUME = 0.25f;

    @Nested
    class CreateAtFullVolume {

        @Test
        void createAtFullVolumeLeavesTheEnginesOwnLevelUnscaled() {
            // The engine multiplies its configured volume by what it is passed, so "vanilla's own
            // balance" is a scale of exactly one; anything else here would quieten every default look.
            var soundCue = UiSoundCue.createAtFullVolume(StarsectorUiSound.BUTTON_PRESSED);

            assertThat(soundCue.sound())
                .isEqualTo(StarsectorUiSound.BUTTON_PRESSED);
            assertThat(soundCue.volume())
                .isEqualTo(1f);
        }
    }

    @Nested
    class Constructor {

        @Test
        void constructorCarriesAQuietenedVolumeThrough() {
            // The whole point of the type: a look that names a role and a level of its own gets both,
            // rather than the level being dropped somewhere between the scheme and the player.
            var soundCue = new UiSoundCue(StarsectorUiSound.BUTTON_MOUSEOVER, QUIETER_VOLUME);

            assertThat(soundCue.volume())
                .isEqualTo(QUIETER_VOLUME);
        }

        @Test
        void constructorRejectsACueWithNoRole() {
            // A silent moment is a null cue, stated by the scheme, so a cue holding no role is a look
            // that meant to sound and left out what with - which would otherwise surface as a null id
            // inside a draw-time play call.
            assertThatThrownBy(() -> new UiSoundCue(null, UiSoundCue.FULL_VOLUME))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("sound");
        }

        @Test
        void constructorRejectsANegativeVolume() {
            // Below zero the engine has no meaning for the value and simply plays whatever it makes of
            // it, so the fault would be as quiet as a wrong id - caught where the value is written.
            assertThatThrownBy(() -> new UiSoundCue(StarsectorUiSound.BUTTON_PRESSED, -0.5f))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volume");
        }
    }
}
