package kmlib.starsector.ui.sound;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins what a scheme answers each moment with. Three faults live here and none of them shows on screen: a
 * factory whose name and content have drifted apart, a kind of arrival wired to the wrong level, and a
 * silent look that turns out to sound after all.
 */
final class UiSoundSchemeTest {

    private static final float LOUD_VOLUME = 0.9f;
    private static final float MIDDLING_VOLUME = 0.6f;
    private static final float QUIET_VOLUME = 0.1f;

    @Nested
    class Constructor {

        @Test
        void constructorTakesTheLibrarysOwnArrivalBalanceForALookThatStatesNone() {
            // Spelt as literals rather than read off the constants they come from, so a default nudged in
            // the source is a decision this case reports rather than one it agrees with silently. The
            // ratio is the point: a listed item is quieter than either thing the player aims at.
            var soundScheme = new UiSoundScheme(
                UiSoundCue.createAtFullVolume(StarsectorUiSound.BUTTON_PRESSED),
                StarsectorUiSound.BUTTON_MOUSEOVER);

            assertThat(soundScheme.panelChromeArrivalVolume())
                .isEqualTo(0.5f);
            assertThat(soundScheme.singleOptionControlArrivalVolume())
                .isEqualTo(0.5f);
            assertThat(soundScheme.listedItemArrivalVolume())
                .isEqualTo(0.25f);
        }

        @Test
        void constructorRejectsANegativeArrivalVolume() {
            // Caught where the look is composed rather than where the cue is built: a scheme is composed
            // once, and a cue is built on the frame the pointer first reaches something - so the same bad
            // number reaches the player as a crash mid-hover if it is left that late.
            assertThatThrownBy(() -> new UiSoundScheme(
                    null,
                    StarsectorUiSound.BUTTON_MOUSEOVER,
                    LOUD_VOLUME,
                    MIDDLING_VOLUME,
                    -0.5f))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volume");
        }
    }

    @Nested
    class CreateVanillaSoundScheme {

        @Test
        void createVanillaSoundSchemeNamesTheEnginesOwnButtonRoles() {
            // A vanilla-looking control that answered with samples of ours would stop matching the chrome
            // around it, whatever it was mixed at.
            var soundScheme = UiSoundScheme.createVanillaSoundScheme();

            assertThat(soundScheme.pressCue())
                .isEqualTo(new UiSoundCue(StarsectorUiSound.BUTTON_PRESSED, 1f));
            assertThat(soundScheme.pointerArrivalSound())
                .isEqualTo(StarsectorUiSound.BUTTON_MOUSEOVER);
        }

        @Test
        void createVanillaSoundSchemeLeavesThePressAtTheEnginesOwnLevel() {
            // The press is one act the player asked for, so nothing about a KM panel's density argues for
            // quietening it - the reason the arrivals are scaled does not reach this moment.
            assertThat(UiSoundScheme.createVanillaSoundScheme().pressCue().volume())
                .isEqualTo(UiSoundCue.FULL_VOLUME);
        }

        @Test
        void createVanillaSoundSchemeQuietensTheArrivalsToAPanelsOwnDensity() {
            // The reversal this scheme carries: vanilla's mouseover level is right for a screen with a
            // handful of hit targets and chatters across a column of them, so "looks vanilla" stops
            // meaning "mixed like vanilla" for the one moment a sweep repeats.
            assertThat(UiSoundScheme.createVanillaSoundScheme()
                    .resolvePointerArrivalCueFor(PointerArrivalTarget.PANEL_CHROME)
                    .volume())
                .isLessThan(UiSoundCue.FULL_VOLUME);
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
            assertThat(soundScheme.pointerArrivalSound())
                .isNull();
        }
    }

    @Nested
    class ResolvePointerArrivalCueFor {

        @Test
        void resolvePointerArrivalCueForAnswersEachKindAtTheLevelItWasBuiltWith() {
            // Three distinct volumes rather than the defaults, because a scheme agreeing with the numbers
            // the library ships would pass whether or not the kind was ever read - and the fault worth
            // catching is a kind wired to a neighbour's level, which no default set can show.
            var soundScheme = buildSchemeWithDistinctVolumes();

            assertThat(soundScheme.resolvePointerArrivalCueFor(PointerArrivalTarget.PANEL_CHROME))
                .isEqualTo(new UiSoundCue(StarsectorUiSound.BUTTON_MOUSEOVER, LOUD_VOLUME));
            assertThat(soundScheme.resolvePointerArrivalCueFor(PointerArrivalTarget.SINGLE_OPTION_CONTROL))
                .isEqualTo(new UiSoundCue(StarsectorUiSound.BUTTON_MOUSEOVER, MIDDLING_VOLUME));
            assertThat(soundScheme.resolvePointerArrivalCueFor(PointerArrivalTarget.LISTED_ITEM))
                .isEqualTo(new UiSoundCue(StarsectorUiSound.BUTTON_MOUSEOVER, QUIET_VOLUME));
        }

        @Test
        void resolvePointerArrivalCueForAnswersNothingForALookThatStaysSilentUnderThePointer() {
            // Silence is stated once, for the moment rather than per kind: a look with no arrival sound
            // has no kind of thing it would sound for, so every kind comes back empty and the caller's one
            // null check covers all three.
            var soundScheme = UiSoundScheme.createSilentSoundScheme();

            assertThat(soundScheme.resolvePointerArrivalCueFor(PointerArrivalTarget.PANEL_CHROME))
                .isNull();
            assertThat(soundScheme.resolvePointerArrivalCueFor(PointerArrivalTarget.SINGLE_OPTION_CONTROL))
                .isNull();
            assertThat(soundScheme.resolvePointerArrivalCueFor(PointerArrivalTarget.LISTED_ITEM))
                .isNull();
        }

        // A look whose three arrival levels are all different, so which kind resolved which can be told
        // apart at all - the whole subject of these cases.
        private static UiSoundScheme buildSchemeWithDistinctVolumes() {
            return new UiSoundScheme(
                UiSoundCue.createAtFullVolume(StarsectorUiSound.BUTTON_PRESSED),
                StarsectorUiSound.BUTTON_MOUSEOVER,
                LOUD_VOLUME,
                MIDDLING_VOLUME,
                QUIET_VOLUME);
        }
    }
}
