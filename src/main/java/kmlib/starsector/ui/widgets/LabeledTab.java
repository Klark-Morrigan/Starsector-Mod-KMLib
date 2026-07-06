package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

/**
 * One laid-out tab in a {@link TabStrip}: the label it shows and the box it occupies. The
 * label is kept alongside the geometry so the consumer draws the same text the width was
 * snapped to, and the box hit-tests a pointer with no extra conversion (UI origin is
 * bottom-left, which {@link Rectangle} already matches).
 *
 * @param text   the tab's label, exactly as measured when the box was sized
 * @param bounds the tab's footprint in UI coordinates
 */
public record LabeledTab(String text, Rectangle bounds) {
}
