package kmlib.starsector.ui.input;

import kmlib.starsector.ui.sound.PointerArrivalTarget;
import kmlib.starsector.ui.sound.PointerArrivalVolumes;
import kmlib.starsector.ui.sound.StarsectorUiSound;
import kmlib.starsector.ui.sound.UiSoundCue;
import kmlib.starsector.ui.sound.UiSoundScheme;
import kmlib.testfixtures.starsector.ui.sound.UiSoundPlayerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins which moment reaches which of a look's cues. Every moment here is one line of delegation, and the
 * fault worth catching is not that a line is missing but that two of them are crossed: a panel whose wheel
 * confirms presses and whose presses report scrolling compiles clean and is wrong only to the ear.
 *
 * <p>Run against a scheme naming a different role for each of the three, so a crossed pair shows as the wrong
 * role rather than as a sound that happens to match.
 */
final class PanelSoundsTest {

    // Distinct levels per kind, so what an arrival sounded at says which kind it was resolved for. Literals
    // rather than the library's own balance, which spreads no further than the defaults happen to differ.
    private static final PointerArrivalVolumes ARRIVAL_VOLUMES =
        new PointerArrivalVolumes(0.9f, 0.6f, 0.3f);

    // One role per moment. The engine's own ids stand in for three distinguishable roles; nothing here reads
    // what any of them sounds like.
    private static final UiSoundCue PRESS_CUE =
        UiSoundCue.createAtFullVolume(StarsectorUiSound.BUTTON_PRESSED);

    private static final UiSoundCue LIST_SCROLL_CUE =
        new UiSoundCue(StarsectorUiSound.LIST_SCROLLED, 0.5f);

    private static final UiSoundScheme SCHEME = new UiSoundScheme(
        PRESS_CUE,
        StarsectorUiSound.BUTTON_MOUSEOVER,
        ARRIVAL_VOLUMES,
        LIST_SCROLL_CUE);

    private final UiSoundPlayerFake soundPlayerFake = new UiSoundPlayerFake();

    private final PanelSounds sounds = new PanelSounds(soundPlayerFake, SCHEME);

    @Nested
    class SoundPress {

        @Test
        void soundPressPlaysTheLooksPressCue() {

            sounds.soundPress();

            assertThat(soundPlayerFake.getPlayedCues())
                .containsExactly(PRESS_CUE);
        }
    }

    @Nested
    class SoundListScroll {

        @Test
        void soundListScrollPlaysTheLooksListScrollCue() {

            sounds.soundListScroll();

            assertThat(soundPlayerFake.getPlayedCues())
                .containsExactly(LIST_SCROLL_CUE);
        }
    }

    @Nested
    class SoundPointerArrivalAt {

        @Test
        void soundPointerArrivalAtPlaysTheArrivalRoleAtTheLevelThatKindIsOwed() {
            // The one moment answering at more than one level, so what reaches the player is the look's
            // arrival role bound to the volume its kind carries - the pair, never the role alone.
            sounds.soundPointerArrivalAt(PointerArrivalTarget.PANEL_CHROME);

            assertThat(soundPlayerFake.getPlayedCues())
                .containsExactly(new UiSoundCue(StarsectorUiSound.BUTTON_MOUSEOVER, 0.9f));
        }

        @Test
        void soundPointerArrivalAtTakesItsLevelFromTheKindReached() {
            // A second kind, so the level is shown to follow what was reached rather than being one arrival
            // volume the look happens to hold.
            sounds.soundPointerArrivalAt(PointerArrivalTarget.SINGLE_OPTION_CONTROL);

            assertThat(soundPlayerFake.getPlayedCues())
                .containsExactly(new UiSoundCue(StarsectorUiSound.BUTTON_MOUSEOVER, 0.6f));
        }

        @Test
        void soundPointerArrivalAtStaysSilentUnderALookThatNamesNoArrivalRole() {
            // Silence is something a look states, so a scheme with no arrival role plays nothing rather than
            // reaching the player with a cue at no volume - a sound played at nothing is still a sound
            // played, and reads as wiring that half worked.
            var silentSounds = new PanelSounds(
                soundPlayerFake,
                new UiSoundScheme(PRESS_CUE, null, ARRIVAL_VOLUMES, LIST_SCROLL_CUE));

            silentSounds.soundPointerArrivalAt(PointerArrivalTarget.PANEL_CHROME);

            assertThat(soundPlayerFake.getPlayedCues())
                .isEmpty();
        }
    }
}
