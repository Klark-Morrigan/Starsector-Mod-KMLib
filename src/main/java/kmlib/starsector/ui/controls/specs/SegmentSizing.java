package kmlib.starsector.ui.controls.specs;

/**
 * How a horizontal segmented control (a {@link HorizontalRadioSpec} or a {@link
 * TabsSpec} row) sizes its segments across the row. It is the width rule both share, so one
 * measurement path serves both. The variant still names the renderer (a radio's framed cells versus a
 * tab strip's black band); this decides only how wide each segment is.
 *
 * <p>Only a horizontal segmented control carries a segment-sizing choice. A {@link
 * VerticalTableSpec}'s wrapped columns are uniform by construction (each column the widest option
 * plus padding), so a vertical table has nothing to choose.
 *
 * <p>The two controls that do choose, choose differently. A horizontal radio picks between the measured
 * rules - even cells or ragged ones - and never states a width of its own. A tabs row picks between
 * measuring at all: a row stacked in a body snaps to its labels, while a header row takes whichever box
 * its {@link kmlib.starsector.ui.widgets.tabs.style.TabStyle} states, which for a row copying vanilla's
 * map tabs is a fixed one.
 */
public enum SegmentSizing {

    /**
     * Every segment shares one width - the widest label plus the padding - so the segments read as
     * even cells (an option pair such as Short/Full). The default a horizontal radio takes.
     */
    UNIFORM,

    /**
     * Each segment snaps to its own label plus the padding, floored at a minimum, so a short label
     * does not carry a wide empty cell and a long one is never clipped.
     */
    SNAPPED,

    /**
     * Every segment takes one stated width whatever its label measures, so the row's geometry is a
     * property of the chrome rather than of its text (the vanilla Sector/System tab strip, whose tabs
     * are a fixed box the label is centred in). Unlike {@link #UNIFORM}, which is still measured - it
     * asks the labels how wide the widest is - this asks them nothing, so a renamed label moves no tab
     * and a label wider than the box overruns it rather than growing it.
     */
    FIXED
}
