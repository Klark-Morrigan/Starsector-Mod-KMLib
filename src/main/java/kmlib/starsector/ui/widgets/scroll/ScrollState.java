package kmlib.starsector.ui.widgets.scroll;

/**
 * The scroll position of a scrollable region: how far, in pixels, its content is scrolled from the top.
 * It is a raw request, not the drawn offset - the layout, which knows the region's real overflow, is the
 * clamp authority - so a consumer holds one instance per scrolling region, nudges it as the wheel turns or
 * a drag moves, and calls {@link #clampTo} each frame once the overflow is known to settle the stored
 * value back into range. That way a wheel past the bottom, or a region that shrank, cannot leave the
 * stored request drifting far outside what can be scrolled.
 *
 * <p>Ephemeral runtime state, not persisted: a scrolling region starts at the top, and the position is not
 * worth serialising.
 */
public final class ScrollState {
    // The requested scroll offset in pixels from the content's top; settled into range each frame by
    // clampTo, so it never drifts unboundedly past what the region can scroll.
    private float offset;

    /**
     * @return the current scroll offset request in pixels, which the layout clamps to the region's
     *         overflow when it places the content
     */
    public float getOffset() {
        return offset;
    }

    /**
     * Nudges the scroll offset by {@code delta} pixels (positive scrolls toward the bottom). Not clamped
     * here - {@link #clampTo} settles it into range once the frame's layout knows the overflow.
     *
     * @param delta the pixels to add to the offset
     */
    public void scrollBy(float delta) {
        offset += delta;
    }

    /**
     * Sets the scroll offset outright, for a scrollbar drag that maps the pointer straight to a position
     * rather than nudging by a delta. Not clamped here - {@link #clampTo} settles it each frame - though a
     * drag resolves an already-in-range offset, so the set value is normally valid.
     *
     * @param newOffset the offset in pixels to jump to
     */
    public void setOffset(float newOffset) {
        offset = newOffset;
    }

    /**
     * Confines the stored offset to {@code [0, overflow]}, called each frame once the layout has resolved
     * how far the content overruns its viewport, so the stored request tracks what can be scrolled rather
     * than drifting past it.
     *
     * @param overflow how far the content overruns its viewport, 0 when it fits
     */
    public void clampTo(float overflow) {
        offset = Math.max(0f, Math.min(offset, overflow));
    }
}
