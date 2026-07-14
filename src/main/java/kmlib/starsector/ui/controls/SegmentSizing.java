package kmlib.starsector.ui.controls;

/**
 * How a horizontal segmented control (a horizontal {@link RadioAlignment#HORIZONTAL} radio or a
 * {@link ControlKind#TABS} row) sizes its segments across the row. It is the width rule both share,
 * lifted off {@code kind} so a radio and a tabs row can each be uniform or snapped and one measurement
 * path serves both. {@code kind} still names the renderer (a radio's framed cells versus a tab strip's
 * black band); this decides only how wide each segment is.
 *
 * <p>It refines a horizontal segmented control only. A vertical radio's wrapped columns are uniform by
 * construction (each column the widest option plus padding), so the sizing is inert for them, and a
 * non-segmented kind (checkbox, toggle, label, divider) has no segments to size - {@link #UNIFORM} is
 * the sole value those carry.
 */
public enum SegmentSizing {
    /**
     * Every segment shares one width - the widest label plus the padding - so the segments read as
     * even cells (an option pair such as Short/Full). The default for every control, and the only
     * value a vertical radio or a non-segmented kind carries.
     */
    UNIFORM,

    /**
     * Each segment snaps to its own label plus the padding, floored at a minimum, so a short label
     * does not carry a wide empty cell and a long one is never clipped (the Sector/System tab strip).
     */
    SNAPPED
}
