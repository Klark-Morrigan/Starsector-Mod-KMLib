package kmlib.starsector.ui.render.gl;

import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.input.UiCursor;
import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.starsector.ui.widgets.PanelScrollbars;
import kmlib.starsector.ui.widgets.TabPanel;

import org.lwjgl.opengl.GL11;

/**
 * Raw-GL paint for a whole {@link PanelPlacement}: the bordered frame and vanilla-styled tab strip (via
 * {@link TabPanelRenderer}), then each body control (via {@link ControlRenderer}), then the scrollbar when
 * the body is capped. It composes the chrome, the per-control paint, and the scrollbar into one panel
 * draw, so a host renders its whole panel with one call and stays out of the per-widget GL. The look
 * rides on a {@link PanelStyle}; the per-frame border width, selected tab, and opacity are parameters.
 *
 * <p>Brackets the draw in one {@code glPushAttrib}/{@code glPopAttrib} - the map chrome and tooltips draw
 * after a UI-overlay pass, so any enable / colour / blend state the panel touches must be restored - and
 * the whole draw shares that one save. The scrolling control (the one marked {@link
 * kmlib.starsector.ui.controls.ControlSpec#scrolls()}) draws clipped to its viewport, so its rows that
 * scroll past the top slide out under the pinned header rather than overpainting it. Hover is read from
 * the cursor here, so the tab under the pointer lights without an input event. GL passthrough exercised
 * in-engine like the other draw helpers.
 */
public final class PanelRenderer {
    private PanelRenderer() {
    }

    /**
     * Draws the panel: the frame and tab strip, the body controls, and the scrollbar when the body is
     * capped, all faded by {@code opacity}. Must run with a current GL context.
     *
     * @param placement     the laid-out panel to draw
     * @param style         how the panel looks (fill, accents, tab colours, fonts)
     * @param borderWidth   the outer border thickness; 0 draws no border
     * @param selectedIndex the lit tab's index, in tab order
     * @param opacity       overall alpha, 0..1, fading the whole panel
     */
    public static void render(PanelPlacement placement, PanelStyle style, float borderWidth,
            int selectedIndex, float opacity) {
        // Belt-and-suspenders around the raw GL: the map chrome and tooltips draw after a UI-overlay
        // pass, so any state the panel touches must be restored. The whole draw shares this one save.
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        // Hover reads the cursor in UI coords - the same space the tab rects live in - so the tab under
        // the pointer lights without an input event.
        var hoveredIndex = TabPanel.findTabIndexAt(placement.panel(), UiCursor.getUiX(),
                UiCursor.getUiY());
        TabPanelRenderer.render(placement.panel(), borderWidth, style.panelFill(), style.accent(),
                selectedIndex, hoveredIndex, style.tabColors(), style.tabFont(), style.tabFontSize(),
                opacity);
        drawBodyControls(placement, style, opacity);
        GL11.glPopAttrib();
    }

    // Draws each body control in the lit state its spec carries, then - when the body is capped - the
    // scrollbar for its scrolling control. The one control marked as the scroll region draws clipped to
    // its viewport; every other control draws unclipped in its pinned place.
    private static void drawBodyControls(PanelPlacement placement, PanelStyle style, float opacity) {
        for (var control : placement.bodyControls()) {
            if (control.spec().scrolls()) {
                UiScissor.push(placement.flexViewport());
                drawControl(control, style, opacity);
                UiScissor.pop();
            } else {
                drawControl(control, style, opacity);
            }
        }
        if (placement.isScrollbarNeeded()) {
            drawScrollbar(placement, style, opacity);
        }
    }

    private static void drawControl(Control control, PanelStyle style, float opacity) {
        ControlRenderer.render(control, style, opacity);
    }

    // The scrollbar for the capped body: track and thumb come from the placement's scroll region - the
    // same geometry the input listener hit-tests for a drag - so what is drawn and what a drag grabs
    // cannot drift.
    private static void drawScrollbar(PanelPlacement placement, PanelStyle style, float opacity) {
        var track = PanelScrollbars.computeTrack(placement);
        var thumb = PanelScrollbars.computeThumb(placement);
        ScrollbarRenderer.render(track, thumb, style.accent(), opacity);
    }
}
