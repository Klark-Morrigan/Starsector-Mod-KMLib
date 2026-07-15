package kmlib.starsector.ui.render.gl;

import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;

import org.lwjgl.opengl.GL11;

/**
 * Raw-GL paint for a whole {@link TabPanelPlacement}: it delegates the body - the one bordered frame, the
 * body controls, and the scrollbar - to {@link PanelRenderer} verbatim, then overlays the tabs header
 * control (via {@link ControlRenderer}) on the frame's top band. A tab panel is a panel plus a header, so
 * its paint is the panel's paint plus one control drawn on top; the single frame is the body placement's
 * whole-footprint box, so there is one border, drawn once by the delegate.
 *
 * <p>The header control draws its own immediate-mode GL, so it is bracketed in a {@code glPushAttrib}/
 * {@code glPopAttrib} state save like {@link PanelRenderer} brackets its own draw. Drawn after the body so
 * the header sits over the frame fill of the top band. GL passthrough exercised in-engine like the other
 * draw helpers.
 */
public final class TabPanelRenderer {
    private TabPanelRenderer() {
    }

    /**
     * Draws the tab panel: the bordered frame, body controls, and scrollbar via {@link PanelRenderer},
     * then the tabs header on top, all faded by {@code opacity}. Must run with a current GL context.
     *
     * @param placement   the laid-out tab panel to draw
     * @param style       how the panel looks (fill, accents, body font, and the tab style for the header)
     * @param borderWidth the outer border thickness; 0 draws no border
     * @param opacity     overall alpha, 0..1, fading the whole panel
     */
    public static void render(TabPanelPlacement placement, WidgetStyle style, float borderWidth,
            float opacity) {
        // The body carries the whole-footprint box, so this draws the one frame, the body controls, and
        // the scrollbar - self-bracketed in its own GL-state save.
        PanelRenderer.render(placement.body(), style, borderWidth, opacity);
        // The header control's raw GL needs the same state save; bracket it here. Drawn after the body so
        // it sits over the frame fill of the top band.
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        ControlRenderer.render(placement.tabsHeader(), style, opacity);
        GL11.glPopAttrib();
    }
}
