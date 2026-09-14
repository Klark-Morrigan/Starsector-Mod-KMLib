package kmlib.starsector.ui.input;

import kmlib.starsector.ui.sound.PointerArrivalTarget;
import kmlib.starsector.ui.sound.UiSoundPlayer;
import kmlib.starsector.ui.sound.UiSoundScheme;

/**
 * What a panel sounds like in answer to a moment, as one thing a controller holds: where its interface sounds
 * go, and what each moment it detects sounds like. The two are always read together - a role resolved without
 * somewhere to play it is nothing, and a player with no scheme has nothing to play - and pairing them here is
 * what lets a controller name the moment alone.
 *
 * <p>It is the line the whole family is built along: a controller owns the <em>moments</em>, being what
 * detects them, and owns none of the <em>choices</em>. The scheme comes in from the host with the rest of the
 * look, because how a press sounds is a property of how the panel presents itself.
 *
 * <p>Held as a seam rather than reached for directly because a sound leaves no trace in the panel's state:
 * every other answer to an input can be read back off a fraction or an offset, and this one can only be
 * observed by recording that it was asked for.
 *
 * <p>One of these covers a whole tab panel, its header and its body alike, so a panel is silenced in one
 * place rather than by visiting every part that ever named a sound - and a body sounding by the library's
 * default look while its header sounded by the host's, which is one panel presenting itself two ways, cannot
 * be arranged by accident.
 */
final class PanelSounds {

    // Where this panel's interface sounds go.
    private final UiSoundPlayer soundPlayer;

    // Which role each moment answers on, and how loudly.
    private final UiSoundScheme soundScheme;

    PanelSounds(UiSoundPlayer soundPlayer, UiSoundScheme soundScheme) {
        this.soundPlayer = soundPlayer;
        this.soundScheme = soundScheme;
    }

    /** Answers a control that has just been pressed, at whatever a press sounds like under this look. */
    void soundPress() {
        soundPlayer.playCueIfPresent(soundScheme.pressCue());
    }

    /**
     * Answers a list that has just moved under the wheel - a moment beside the arrivals rather than one of
     * them: nobody got anywhere, the content moved instead.
     */
    void soundListScroll() {
        soundPlayer.playCueIfPresent(soundScheme.listScrollCue());
    }

    /**
     * Answers the pointer reaching something, at the level that kind of thing is owed. Role and level are
     * taken together, which is the point of a cue: resolved apart, a moment could sound at a level meant for
     * another kind and nothing on screen would show it.
     *
     * @param arrivalTarget the kind of thing the pointer reached
     */
    void soundPointerArrivalAt(PointerArrivalTarget arrivalTarget) {
        soundPlayer.playCueIfPresent(soundScheme.resolvePointerArrivalCueFor(arrivalTarget));
    }
}
