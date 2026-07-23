package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.easing.Easing;
import kmlib.math.ranges.Ranges;

/**
 * Holds a tab panel's collapse animation: how far the body is collapsed and which way it is heading, so the
 * render pass can drive the collapse forward frame by frame and the input pass can flip it on the handle.
 * Transient per-session UI state - no persistence, no stored keys - held beside the consumer's scroll
 * offset, so a save neither carries it nor needs a migration when it changes.
 *
 * <p>The state is one linear {@code progress} parameter and a direction. Progress steps linearly in time
 * because a linear parameter is what an eased curve composes cleanly on top of: {@link
 * #getCollapseFraction} eases it on read through {@link Easing#easeInOut}, so reversing the direction
 * part-way leaves the fraction continuous rather than snapping. Feeding the eased value to the layout is
 * what turns a constant-rate advance into a collapse that accelerates off the start and settles into the
 * end. The docked and expanded end states are read off the progress rather than tracked alongside it, so
 * there is one source of truth for where the animation sits.
 */
public final class TabPanelCollapse {
    /**
     * How long a full collapse or expand takes, in seconds. Fixed, so every collapse runs at the same pace
     * regardless of frame rate: {@link #advanceByElapsedTime} steps progress by the elapsed fraction of
     * this duration. Public so a consumer pacing frames and a test driving progress share the one figure.
     */
    public static final float DURATION_SECONDS = 0.25f;

    // Linear animation parameter in [0, 1]: 0 fully expanded, 1 fully docked. Stepped linearly by elapsed
    // time and eased only on read, so the eased fraction stays continuous when the direction reverses.
    private float progress;
    // Which end the animation is heading for: true steps progress toward docked (1), false toward expanded
    // (0). A settled panel keeps its last direction, so the next toggle sends it the other way.
    private boolean isCollapsing;

    /**
     * Flips the direction, so the handle both starts a collapse and reverses one. From expanded or still
     * collapsing it heads for docked; from docked or still expanding it heads back to expanded. Progress is
     * left where it is, so a reversal mid-flight eases on from the current fraction rather than restarting.
     */
    public void toggleCollapse() {
        isCollapsing = !isCollapsing;
    }

    /**
     * Steps the collapse toward its current direction's end by the elapsed fraction of {@link
     * #DURATION_SECONDS}, clamped so it settles exactly at the end rather than overshooting. A frame spent
     * already settled at that end leaves the state unchanged, so a render loop can call this every frame
     * unconditionally.
     *
     * @param elapsedSeconds real time since the last frame; a full {@link #DURATION_SECONDS} completes the
     *                       animation in one step
     */
    public void advanceByElapsedTime(float elapsedSeconds) {
        var step = elapsedSeconds / DURATION_SECONDS;
        var stepped = isCollapsing ? progress + step : progress - step;
        progress = Ranges.clampToUnit(stepped);
    }

    /**
     * @return how far the body is collapsed horizontally for the layout, eased so the motion accelerates
     *         off the start and settles into the end: 0 fully expanded, 1 fully docked
     */
    public float getCollapseFraction() {
        return Easing.easeInOut(progress);
    }

    /**
     * @return true once the body has fully collapsed to the docked rail, so the renderer can draw the rail
     *         and the flipped handle; false at any partial collapse
     */
    public boolean isDocked() {
        return progress >= 1f;
    }

    /**
     * @return true while a step would still move the collapse - heading for an end it has not reached - so
     *         a consumer can keep pumping frames while it animates and rest once it settles; false when it
     *         sits at the end its direction points to
     */
    public boolean isAnimating() {
        return isCollapsing ? progress < 1f : progress > 0f;
    }
}
