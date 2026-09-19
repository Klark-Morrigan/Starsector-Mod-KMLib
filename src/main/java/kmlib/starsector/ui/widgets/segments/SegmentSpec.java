package kmlib.starsector.ui.widgets.segments;

import kmlib.starsector.ui.controls.specs.SegmentSizing;

/**
 * How to size and space a row of segments: the inputs the width rule reads, bundled because they always
 * travel together. The measure pass and the placement pass both need them, and a host builds one per
 * control type - a horizontal radio's body-size uniform cells, a tab strip's fixed vanilla boxes - then
 * threads that one policy through the whole sizing chain ({@link
 * kmlib.starsector.ui.layout.ControlStripLayout} to {@link kmlib.starsector.ui.widgets.tabs.VanillaTabStrip}
 * / {@link kmlib.starsector.ui.widgets.tabs.TabStrip} to
 * {@link HorizontalSegments}) rather than re-bundling loose scalars at each hop. It is the sizing
 * counterpart to the host's {@link kmlib.starsector.ui.controls.ControlSpec}: that describes what a
 * control is, this describes how wide its segments come out and how far apart they stand.
 *
 * <p>Which fields matter depends on the sizing. {@link SegmentSizing#FIXED} reads {@code fixedWidth}
 * alone and asks the labels nothing; the measured sizings read {@code padding}, {@code minWidth} and
 * {@code fontSize} and ignore {@code fixedWidth}. Rather than split the record per sizing, the unused
 * field is left at zero - one spec threading one chain is worth more than a type per width rule.
 *
 * @param padding      slack added past each measured label so text does not touch the segment edges;
 *                     unread under {@link SegmentSizing#FIXED}
 * @param minWidth     the narrowest a segment may be, so a short label still gives a clickable cell;
 *                     unread under {@link SegmentSizing#FIXED}
 * @param fontSize     the size the labels are measured (and later drawn) at
 * @param sizing       whether the segments share one measured width (uniform), each snap to their own
 *                     label (snapped), or all take one stated width (fixed)
 * @param fixedWidth   the width every segment takes under {@link SegmentSizing#FIXED}; unread otherwise
 * @param neighbourGap the empty channel left between two neighbouring segments, taken between them
 *                     rather than out of either, so the row grows by one gap per pair. Zero abuts them,
 *                     which is what a row ruling its seams with a divider wants
 */
public record SegmentSpec(
    float padding,
    float minWidth,
    double fontSize,
    SegmentSizing sizing,
    float fixedWidth,
    float neighbourGap) {

    /**
     * A measured row whose segments abut: no fixed width and no gap between neighbours. The shape every
     * caller that predates fixed boxes wants, so a radio row and a body tabs row keep saying what they
     * always said.
     *
     * @param padding  slack added past each measured label
     * @param minWidth the narrowest a segment may be
     * @param fontSize the size the labels are measured at
     * @param sizing   the measured width rule to apply
     */
    public SegmentSpec(float padding, float minWidth, double fontSize, SegmentSizing sizing) {
        this(padding, minWidth, fontSize, sizing, 0f, 0f);
    }

    /**
     * Floors the two dimensions at zero, so a caller handed a negative box or gap lays a collapsed row
     * rather than segments that run backwards through their neighbours.
     */
    public SegmentSpec {
        fixedWidth = Math.max(0f, fixedWidth);
        neighbourGap = Math.max(0f, neighbourGap);
    }
}
