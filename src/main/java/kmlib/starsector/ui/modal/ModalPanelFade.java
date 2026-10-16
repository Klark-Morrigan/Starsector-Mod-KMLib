package kmlib.starsector.ui.modal;

import kmlib.animation.TraverseDurations;
import kmlib.animation.TraverseFraction;

import java.util.Objects;

/**
 * Whether a {@link ModalPanel} holds the screen, and how far onto it the panel stands - two answers
 * that part company for the length of a fade.
 *
 * <p>The panel fades because everything else that claims the screen does. A prompt a core screen
 * raises dissolves in and out over its own curve; a panel that arrived whole beside that would snap
 * on the frame it opened, and the difference reads as a fault rather than as a style.
 *
 * <p><b>Raised is crisp; the fraction is the paint's alone.</b> Input has to go the instant the
 * panel appears and come back the instant it is dismissed, while the paint runs on for the length
 * of the fade. The panel outliving the press is what makes the split load-bearing rather than tidy: a
 * panel still on screen after the press must claim nothing, or it eats the click that follows it.
 * That is why the flag is held here rather than handed to the travel each frame - it is an answer in
 * its own right, and the one input reads.
 *
 * <p>Kept out of the panel it paces so the clock can be checked without a running game. Kept inside a
 * frame hook, it could be checked only by opening the panel and watching it.
 */
final class ModalPanelFade {

    // How long the way in and the way out take.
    private final TraverseDurations durations;

    // How far onto the screen the panel is painted.
    private final TraverseFraction fadeProgress = new TraverseFraction();

    // Whether the panel holds the screen, which is what input reads. Moves on the press, never on
    // the fade.
    private boolean isRaised;

    /**
     * @param durations how long the rise and the fall each take
     */
    public ModalPanelFade(TraverseDurations durations) {
        this.durations = Objects.requireNonNull(durations, "durations");
    }

    /**
     * Steps the fade toward the end the panel is heading for, at that direction's own pace. A frame
     * spent already at that end leaves it unchanged, so a frame hook can call this unconditionally.
     *
     * @param elapsedSeconds real time since the last frame
     */
    public void advanceFade(float elapsedSeconds) {
        fadeProgress.advanceTowardEnd(isRaised, elapsedSeconds, durations);
    }

    /**
     * Hands the screen back while leaving the paint to fall from wherever it stands.
     */
    public void dismissPanel() {
        isRaised = false;
    }

    /**
     * Takes the panel down and its paint with it, in one step. For a panel that has lost the screen
     * it stood over: there is nothing left to fade against, and a fraction left part-way up would be
     * the first thing the next open painted before winding down.
     */
    public void dropFade() {

        isRaised = false;
        fadeProgress.dropToRest();
    }

    /**
     * @return whether the panel holds the screen - true from the frame it is raised and false from
     *         the press that dismisses it, whatever the paint is doing
     */
    public boolean isPanelRaised() {
        return isRaised;
    }

    /**
     * Whether there is nothing left to show: the panel is down and its fall has run out. What tells a
     * panel it can come off the screen, a panel still fading being one the player is watching leave.
     *
     * @return true once the panel is dismissed and its fade sits exactly at down
     */
    public boolean isSettledDown() {

        return !isRaised
            && fadeProgress.hasSettledAtRest();
    }

    /**
     * Takes the screen. The paint follows over the next frames from wherever it stands, which is
     * nothing on a first open and part-way down on a reopen during the fall.
     */
    public void raisePanel() {
        isRaised = true;
    }

    /**
     * @return how far onto the screen the panel's paint stands, eased so the travel accelerates off
     *         the start and settles into the end: 0 nothing showing, 1 fully in place
     */
    public float resolveFadeFraction() {
        return fadeProgress.getEasedValue();
    }
}
