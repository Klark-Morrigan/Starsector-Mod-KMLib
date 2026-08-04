package kmlib.starsector.ui.widgets.tabs;

import kmlib.animation.EasedFraction;

/**
 * Holds a tab panel's collapse animation: how far the body is collapsed and which way it is heading, so the
 * render pass can drive the collapse forward frame by frame and the input pass can flip it on the handle.
 * Transient per-session UI state - no persistence, no stored keys - held beside the consumer's scroll
 * offset, so a save neither carries it nor needs a migration when it changes.
 *
 * <p>What is about folding a panel lives here - the direction the handle flips, the docked and expanded end
 * states read off the animation rather than tracked alongside it, and the pace a fold runs at. The position
 * between those two ends is an {@link EasedFraction}, so this fold eases exactly as every other animation on
 * the panel does rather than on a curve written beside theirs. Feeding the eased value to the layout is what
 * turns a constant-rate advance into a collapse that accelerates off the start and settles into the end.
 */
public final class TabPanelCollapse {
    /**
     * The pace a full collapse or expand runs at, in seconds, when a consumer offers no control over it.
     * The holder no longer owns the duration - {@link #advanceByElapsedTime} takes it per frame - so this
     * is only the recommended default: a consumer's control defaults here so an untouched setting keeps the
     * original pace, and it anchors the tests that pin that pace.
     */
    public static final float DEFAULT_DURATION_SECONDS = 0.25f;

    // The two ends the fold travels between, named so the direction below reads as a destination rather
    // than as a bare bound.
    private static final float DOCKED_PROGRESS = 1f;
    private static final float EXPANDED_PROGRESS = 0f;

    // Where the fold currently stands between the two ends above: 0 fully expanded, 1 fully docked.
    private EasedFraction collapseProgress = new EasedFraction();

    // Which end the animation is heading for: true steps toward docked, false toward expanded. A settled
    // panel keeps its last direction, so the next toggle sends it the other way.
    private boolean isCollapsing;

    /**
     * A collapse seeded fully docked and idle, for a panel that opens collapsed - the rail already showing
     * and the body reclaimed - rather than at the expanded default. The next handle toggle reverses it
     * toward expanded, so a docked start animates open exactly as an expanded start animates shut.
     *
     * @return a collapse already settled at the docked end
     */
    public static TabPanelCollapse createDocked() {
        var collapse = new TabPanelCollapse();

        // Seed the position at the docked end and aim it there, so the eased read reports a full fraction
        // and the next toggle reverses it toward expanded - the same end state a full collapse settles
        // into, reached without stepping through the animation.
        collapse.collapseProgress = EasedFraction.createAtValue(DOCKED_PROGRESS);
        collapse.isCollapsing = true;

        return collapse;
    }

    /**
     * Flips the direction, so the handle both starts a collapse and reverses one. From expanded or still
     * collapsing it heads for docked; from docked or still expanding it heads back to expanded. The position
     * is left where it is, so a reversal mid-flight eases on from the current fraction rather than
     * restarting.
     */
    public void toggleCollapse() {
        isCollapsing = !isCollapsing;
    }

    /**
     * Steps the collapse toward its current direction's end by the elapsed fraction of {@code
     * durationSeconds}, clamped so it settles exactly at the end rather than overshooting. A frame spent
     * already settled at that end leaves the state unchanged, so a render loop can call this every frame
     * unconditionally. The duration is a per-frame input rather than a holder constant so a consumer can
     * expose it as a setting; a non-positive duration means "no animation" and snaps straight to the target
     * end in this one step.
     *
     * @param elapsedSeconds  real time since the last frame; a full {@code durationSeconds} completes the
     *                        animation in one step
     * @param durationSeconds how long a full collapse or expand should take; zero or less snaps instantly
     */
    public void advanceByElapsedTime(float elapsedSeconds, float durationSeconds) {
        collapseProgress.advanceTowardTarget(resolveTargetProgress(), elapsedSeconds, durationSeconds);
    }

    /**
     * @return how far the body is collapsed horizontally, for the layout to size the body by: 0 fully
     *         expanded, 1 fully docked
     */
    public float getCollapseFraction() {
        return collapseProgress.getEasedValue();
    }

    /**
     * @return true once the body has fully collapsed to the docked rail, so the renderer can draw the rail
     *         and the flipped handle; false at any partial collapse
     */
    public boolean isDocked() {
        return collapseProgress.hasReachedTarget(DOCKED_PROGRESS);
    }

    /**
     * @return true while a step would still move the collapse - heading for an end it has not reached - so
     *         a consumer can keep pumping frames while it animates and rest once it settles; false when it
     *         sits at the end its direction points to
     */
    public boolean isAnimating() {
        return !collapseProgress.hasReachedTarget(resolveTargetProgress());
    }

    /**
     * @return true only when the body sits idle at the fully expanded end - at that end and not aimed back
     *         toward the dock - so a consumer can gate expanded-only input such as tab hotkeys, which must
     *         stay inert while the panel is docked, docking, or undocking. The direction guard is what
     *         separates the settled-expanded state from the first frame of a fresh collapse, where the
     *         position is still expanded but the panel is already heading for the dock; false in every
     *         non-expanded state
     */
    public boolean isFullyExpanded() {
        return collapseProgress.hasReachedTarget(EXPANDED_PROGRESS) && !isCollapsing;
    }

    /**
     * @return the end the current direction is heading for, which is what both the per-frame advance and the
     *         animating check measure against - one rule for "where this fold is going", so the two cannot
     *         disagree about whether it has arrived
     */
    private float resolveTargetProgress() {
        return isCollapsing
            ? DOCKED_PROGRESS
            : EXPANDED_PROGRESS;
    }
}
