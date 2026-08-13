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
    class CreateIfAudible {

        @Test
        void createIfAudibleBindsTheRoleToTheLevelItWasHandedIn() {
            // The ordinary case: a level somebody set, above silence, reaching the player bound to the
            // role it was set for rather than to whatever a call site paired with it.
            var soundCue = UiSoundCue.createIfAudible(StarsectorUiSound.BUTTON_MOUSEOVER, QUIETER_VOLUME);

            assertThat(soundCue)
                .isEqualTo(new UiSoundCue(StarsectorUiSound.BUTTON_MOUSEOVER, QUIETER_VOLUME));
        }

        @Test
        void createIfAudibleNamesNoCueAtAllAtSilence() {
            // The whole reason the factory exists: a slider at the bottom composes a moment that is not
            // named, not one named at nothing. The boundary is at silence rather than below it, a level
            // arriving from a stored double having no business turning on how the narrowing landed.
            assertThat(UiSoundCue.createIfAudible(StarsectorUiSound.BUTTON_MOUSEOVER, 0f))
                .isNull();
        }

        @Test
        void createIfAudibleNamesNoCueBelowSilenceRatherThanRefusing() {
            // Where this parts from the constructor, and deliberately: the level here is the player's,
            // so one that arrives under zero is a moment nobody wants heard rather than a look that
            // stated a volume it had no meaning for - answered by a quiet panel, not by a throw out of
            // the composition every frame rebuilds.
            assertThat(UiSoundCue.createIfAudible(StarsectorUiSound.BUTTON_PRESSED, -0.5f))
                .isNull();
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
        void constructorAcceptsAVolumeOfNothing() {
            // The guard stops below zero rather than at it, so a host composing a cue straight from a
            // slider the player dragged to the bottom gets a silent cue rather than a throw. That it is
            // legal is not that it is right: silence is a null cue, and a cue at nothing is a sound still
            // played - which is why the boundary is pinned rather than left to whichever way it was read.
            var soundCue = new UiSoundCue(StarsectorUiSound.BUTTON_MOUSEOVER, 0f);

            assertThat(soundCue.volume())
                .isEqualTo(0f);
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
