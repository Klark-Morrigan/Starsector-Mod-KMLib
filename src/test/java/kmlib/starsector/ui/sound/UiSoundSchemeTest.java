package kmlib.starsector.ui.sound;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two schemes a host builds through by name: what "sounds like a vanilla control" resolves to,
 * and that a silent scheme names nothing at all. Both are what a caller reads the factory's name and
 * expects, and a factory whose name and content have drifted apart is the one fault a value this small
 * can carry.
 */
final class UiSoundSchemeTest {

    @Nested
    class CreateVanillaSoundScheme {

        @Test
        void createVanillaSoundSchemeNamesTheEnginesOwnButtonRolesUnscaled() {
            // A vanilla-looking control that sounded at a level of ours would stop matching the chrome
            // around it, so both cues have to leave the engine's own balance alone.
            var soundScheme = UiSoundScheme.createVanillaSoundScheme();

            assertThat(soundScheme.pressCue())
                .isEqualTo(new UiSoundCue(StarsectorUiSound.BUTTON_PRESSED, 1f));
            assertThat(soundScheme.pointerArrivalCue())
                .isEqualTo(new UiSoundCue(StarsectorUiSound.BUTTON_MOUSEOVER, 1f));
        }
    }

    @Nested
    class CreateSilentSoundScheme {

        @Test
        void createSilentSoundSchemeNamesNoCueAtAll() {
            // A null cue is what a consumer skips, so a "silent" scheme that named a cue anywhere would
            // be a panel that still answered where its look said it would not - and a cue at zero volume
            // would be exactly that, a sound played at nothing rather than a moment left quiet.
            var soundScheme = UiSoundScheme.createSilentSoundScheme();

            assertThat(soundScheme.pressCue())
                .isNull();
            assertThat(soundScheme.pointerArrivalCue())
                .isNull();
        }
    }
}
