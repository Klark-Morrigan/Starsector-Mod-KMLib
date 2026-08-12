package kmlib.starsector.ui.input;

/**
 * What the pointer is on this frame, over one tab panel's hoverable parts: which header tab, which cell of
 * which body control, and whether the collapse handle. One reading of the cursor resolved into one value,
 * so every motion and every moment the panel answers with is charged against the same pointer rather than
 * against hit-tests taken a line apart.
 *
 * <p>It travels as a value because the parts are answered together everywhere they are answered at all -
 * fades stepped, arrivals detected - and readings threaded loose through those calls are readings that can
 * be passed crossed over, which would light the handle for a tab.
 *
 * <p>A frame's reading, not a latch. It is resolved against the placement the panel is actually being
 * drawn at rather than remembered from the last pointer event, which is what keeps it honest when the
 * panel moves under a still cursor: a scroll, a fold, or a relayout otherwise leaves a part answering for
 * a pointer no longer over it.
 *
 * @param tabIndex        the header tab the pointer is on, in row order, or null when it is on none -
 *                        which is also what a panel not presenting its tabs reports, whatever is laid
 *                        out under the cursor
 * @param bodyCellSlot    the body cell the pointer is on, or null when it is on none - which is also what
 *                        a point outside the box the body is drawn inside reports, whatever is laid out
 *                        under the cursor
 * @param isNotchHovered  whether the pointer is on the collapse handle
 */
public record TabPanelHover(
    Integer tabIndex,
    BodyCellSlot bodyCellSlot,
    boolean isNotchHovered) {

    /**
     * The pointer on none of the panel's parts - what a frame with the cursor away from the panel
     * reports, and the reading a consumer stepping a panel by hand wants when it is exercising
     * something other than hover.
     */
    public static final TabPanelHover NOTHING_HOVERED = new TabPanelHover(null, null, false);
}
