package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.geometry.Rectangle;

/**
 * One laid-out {@link VanillaTabStrip} tab: its {@link VanillaTabContent} and the box it
 * occupies. The content rides along with the geometry so the strip's paint draws the same label
 * and shortcut the box was sized to, and the box hit-tests a pointer directly.
 *
 * @param content the tab's label and optional shortcut
 * @param bounds  the tab's footprint in UI coordinates
 */
public record VanillaTab(VanillaTabContent content, Rectangle bounds) {
}
