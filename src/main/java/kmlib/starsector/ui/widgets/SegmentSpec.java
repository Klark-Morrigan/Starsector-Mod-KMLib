package kmlib.starsector.ui.widgets;

import kmlib.starsector.ui.controls.SegmentSizing;

/**
 * How to size a row of segments: the four inputs the width rule reads, bundled because they always
 * travel together. The measure pass and the placement pass both need all four, and a host builds one
 * per control type - a horizontal radio's body-size uniform cells, a tab strip's face-size snapped
 * tabs - then threads that one policy through the whole sizing chain ({@link
 * kmlib.starsector.ui.layout.ControlStripLayout} to {@link VanillaTabStrip} / {@link TabStrip} to
 * {@link HorizontalSegments}) rather than re-bundling four loose scalars at each hop. It is the sizing
 * counterpart to the host's {@link kmlib.starsector.ui.controls.ControlSpec}: that describes what a
 * control is, this describes how wide its segments come out.
 *
 * @param padding  slack added past each measured label so text does not touch the segment edges
 * @param minWidth the narrowest a segment may be, so a short label still gives a clickable cell
 * @param fontSize the size the labels are measured (and later drawn) at
 * @param sizing   whether the segments share one width (uniform) or each snaps to its own label
 */
public record SegmentSpec(float padding, float minWidth, double fontSize, SegmentSizing sizing) {
}
