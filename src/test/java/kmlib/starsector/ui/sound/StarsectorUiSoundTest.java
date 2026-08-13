package kmlib.starsector.ui.sound;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the ids the enum exists to stop anyone typing by hand. An id is looked up by name at play time,
 * so a wrong one is not an error anywhere - it is a control that answers nothing, found by a player
 * wondering why one widget went quiet. Nothing else in the suite can catch it either: every assertion
 * about a moment carries the enum constant, which is equal to itself however it is spelled underneath.
 */
class StarsectorUiSoundTest {

    @Nested
    class GetSoundId {

        // The id each role names, as it is declared in starsector-core/data/config/sounds.json. Asserted
        // against literals rather than derived, a value under test agreeing with any typo in itself - and
        // exhaustive over the enum, so a role added without a spelling pinned here fails to compile.
        @ParameterizedTest
        @EnumSource(StarsectorUiSound.class)
        void getSoundIdNamesTheEnginesOwnIdForTheRole(StarsectorUiSound sound) {

            var expected = switch (sound) {
                case BUTTON_PRESSED -> "ui_button_pressed";
                case BUTTON_MOUSEOVER -> "ui_button_mouseover";
                case LIST_SCROLLED -> "ui_number_scrolling";
                // The one id whose file and name part ways: the sample is ui_type.ogg, and the ids the
                // engine keeps over it are this, ui_number_scrolling above, and ui_typer_buzz. Spelling
                // it the way the file is spelled is the plausible mistake, and a silent one.
                case TEXT_TYPED -> "ui_typer_type";
            };

            assertThat(sound.getSoundId())
                .isEqualTo(expected);
        }

        @Test
        void getSoundIdIsDistinctForEveryRole() {
            // Two roles sharing an id is the copy-paste that adding one invites, and it survives the
            // pinning above only as a pair of literals that happen to match - so it is asked separately.
            // Sharing a *sample* is not sharing an id: the scroll and the typed tick are both ui_type.ogg
            // under two names, and it is the names that have to differ.
            var soundIds = Arrays.stream(StarsectorUiSound.values())
                .map(StarsectorUiSound::getSoundId)
                .toList();

            assertThat(soundIds)
                .doesNotHaveDuplicates();
        }
    }
}
