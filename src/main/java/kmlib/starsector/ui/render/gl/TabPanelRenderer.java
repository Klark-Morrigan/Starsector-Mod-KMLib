package kmlib.starsector.ui.render.gl;

import kmlib.starsector.ui.widgets.TabPanelPlacement;

import java.awt.Color;

/**
 * Raw-GL paint for a {@link TabPanelPlacement}: the panel's chrome - the outer frame and the tab
 * header - scaled by one opacity so the frame, border, tab washes, dividers, underline, and labels
 * fade as a unit. The layout geometry lives on the substrate-independent
 * {@link kmlib.starsector.ui.widgets.TabPanel}; this composes the {@link BorderedBoxRenderer} frame
 * and the {@link VanillaTabStripRenderer} header, exercised in-engine.
 *
 * <p>The body is not drawn here; the consumer paints it into {@link TabPanelPlacement#body()}. No GL
 * state is pushed or restored, so the consumer wraps this and its own body draw in one attribute save.
 */
public final class TabPanelRenderer {
    private TabPanelRenderer() {
    }

    /**
     * Paints the panel's chrome - the outer frame and the tab header - scaling every draw by one
     * {@code opacity}. The body is the consumer's to draw.
     *
     * @param placement     the laid-out panel
     * @param borderWidth   the frame's border thickness; 0 draws no border
     * @param fill          the panel's backdrop colour
     * @param border        the frame's edge colour
     * @param selectedIndex the active tab's index, or a value outside the row to light none
     * @param hoveredIndex  the hovered tab's index, or a value outside the row
     * @param colors        the tab palette (see {@link VanillaTabColors#mapTabs})
     * @param fontBasename  the {@code graphics/fonts} basename the tab labels draw in
     * @param fontSize      the tab label font size
     * @param opacity       overall alpha, 0..1, applied to every quad and text colour
     */
    public static void render(TabPanelPlacement placement, float borderWidth, Color fill,
            Color border, int selectedIndex, int hoveredIndex, VanillaTabColors colors,
            String fontBasename, double fontSize, float opacity) {
        BorderedBoxRenderer.render(placement.box(), borderWidth, fill, border, opacity);
        VanillaTabStripRenderer.render(placement.tabs(), selectedIndex, hoveredIndex, colors,
                fontBasename, fontSize, opacity);
    }
}
