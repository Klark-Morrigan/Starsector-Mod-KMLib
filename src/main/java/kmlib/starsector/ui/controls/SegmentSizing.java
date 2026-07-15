package kmlib.starsector.ui.controls;

/**
 * How a horizontal segmented control (a {@link ControlSpec.HorizontalRadio} or a {@link
 * ControlSpec.Tabs} row) sizes its segments across the row. It is the width rule both share, so one
 * measurement path serves both. The variant still names the renderer (a radio's framed cells versus a
 * tab strip's black band); this decides only how wide each segment is.
 *
 * <p>Only a horizontal segmented control carries a segment-sizing choice. A {@link
 * ControlSpec.VerticalTable}'s wrapped columns are uniform by construction (each column the widest option
 * plus padding), and a tabs row always snaps, so {@link #UNIFORM} versus {@link #SNAPPED} is a real
 * choice on a horizontal radio alone.
 */
public enum SegmentSizing {
    /**
     * Every segment shares one width - the widest label plus the padding - so the segments read as
     * even cells (an option pair such as Short/Full). The default a horizontal radio takes.
     */
    UNIFORM,

    /**
     * Each segment snaps to its own label plus the padding, floored at a minimum, so a short label
     * does not carry a wide empty cell and a long one is never clipped (the Sector/System tab strip).
     */
    SNAPPED
}
