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
        void createVanillaSoundSchemeNamesTheEnginesOwnButtonRoles() {

            var soundScheme = UiSoundScheme.createVanillaSoundScheme();

            assertThat(soundScheme.pressSound())
                .isEqualTo(StarsectorUiSound.BUTTON_PRESSED);
            assertThat(soundScheme.pointerArrivalSound())
                .isEqualTo(StarsectorUiSound.BUTTON_MOUSEOVER);
        }
    }

    @Nested
    class CreateSilentSoundScheme {

        @Test
        void createSilentSoundSchemeNamesNoRoleAtAll() {
            // A null role is what a consumer skips, so a "silent" scheme that named a role anywhere would
            // be a panel that still answered where its look said it would not.
            var soundScheme = UiSoundScheme.createSilentSoundScheme();

            assertThat(soundScheme.pressSound())
                .isNull();
            assertThat(soundScheme.pointerArrivalSound())
                .isNull();
        }
    }
}
