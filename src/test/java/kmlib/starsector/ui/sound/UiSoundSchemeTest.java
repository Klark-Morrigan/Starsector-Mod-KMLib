package kmlib.starsector.ui.sound;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins what a scheme answers each moment with. Three faults live here and none of them shows on screen: a
 * factory whose name and content have drifted apart, an arrival resolved without the level its kind names,
 * and a silent look that turns out to sound after all. What the levels themselves are is pinned beside
 * {@link PointerArrivalVolumes}, which owns them.
 */
final class UiSoundSchemeTest {

    private static final float LOUD_VOLUME = 0.9f;
    private static final float MIDDLING_VOLUME = 0.6f;
    private static final float QUIET_VOLUME = 0.1f;

    @Nested
    class Constructor {

        @Test
        void constructorTakesTheLibrarysOwnArrivalBalanceForALookThatStatesNone() {
            // Which balance the short form picks, not what its numbers are - a host saying nothing about
            // volume gets the library's tuning rather than silence or the engine's own level. The numbers
            // are pinned as literals where they are declared.
            var soundScheme = new UiSoundScheme(
                UiSoundCue.createAtFullVolume(StarsectorUiSound.BUTTON_PRESSED),
                StarsectorUiSound.BUTTON_MOUSEOVER,
                UiSoundCue.createAtFullVolume(StarsectorUiSound.LIST_SCROLLED));

            assertThat(soundScheme.pointerArrivalVolumes())
                .isEqualTo(PointerArrivalVolumes.createDefaultVolumes());
        }

        @Test
        void constructorRejectsALookWithNoArrivalBalanceAtAll() {
            // A scheme naming a role it cannot resolve a level for is an omission rather than an intent -
            // silence is stated by naming no role - and one that would otherwise throw on the frame the
            // pointer first reached something rather than where the look was composed.
            assertThatThrownBy(() -> new UiSoundScheme(
                    UiSoundCue.createAtFullVolume(StarsectorUiSound.BUTTON_PRESSED),
                    StarsectorUiSound.BUTTON_MOUSEOVER,
                    null,
                    UiSoundCue.createAtFullVolume(StarsectorUiSound.LIST_SCROLLED)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("pointerArrivalVolumes");
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
        void createVanillaSoundSchemeNamesTheEnginesOwnScrollingRoleForAList() {
            // The engine keeps several IDs over the one sample and this is the one named for scrolling, so
            // a list wearing vanilla's look takes it. A wrong ID is looked up by name at play time and
            // fails in silence, which is why the choice is pinned rather than left to the ear.
            assertThat(UiSoundScheme.createVanillaSoundScheme().listScrollCue().sound())
                .isEqualTo(StarsectorUiSound.LIST_SCROLLED);
        }

        @Test
        void createVanillaSoundSchemeQuietensTheWheelToAPanelsOwnDensity() {
            // The wheel lands among the panel's arrivals rather than on a screen of its own, so it is
            // scaled on the same argument they are - a list ticking at the engine's own level over a strip
            // whose every other moment was quietened reads as the one part that was missed.
            assertThat(UiSoundScheme.createVanillaSoundScheme().listScrollCue().volume())
                .isLessThan(UiSoundCue.FULL_VOLUME);
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
            assertThat(soundScheme.listScrollCue())
                .isNull();
        }
    }

    @Nested
    class ResolvePointerArrivalCueFor {

        @Test
        void resolvePointerArrivalCueForPairsTheOneRoleWithTheKindsOwnLevel() {
            // The join this type makes: one sample throughout, and the level the balance names for whatever
            // was reached. Distinct numbers per kind, so a scheme reaching past the kind it was asked about
            // is visible rather than hidden behind three equal defaults.
            var soundScheme = new UiSoundScheme(
                UiSoundCue.createAtFullVolume(StarsectorUiSound.BUTTON_PRESSED),
                StarsectorUiSound.BUTTON_MOUSEOVER,
                new PointerArrivalVolumes(LOUD_VOLUME, MIDDLING_VOLUME, QUIET_VOLUME),
                UiSoundCue.createAtFullVolume(StarsectorUiSound.LIST_SCROLLED));

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
    }
}
