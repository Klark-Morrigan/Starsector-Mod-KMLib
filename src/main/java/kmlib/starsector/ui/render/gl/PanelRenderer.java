package kmlib.starsector.ui.render.gl;

import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.starsector.ui.widgets.scroll.PanelScrollbars;

/**
 * Raw-GL paint for a whole headerless {@link PanelPlacement}: the bordered frame (via {@link
 * BorderedBoxRenderer}), then each body control (via {@link ControlRenderer}), then the scrollbar when
 * the body is capped. It composes the frame, the per-control paint, and the scrollbar into one panel
 * draw, so a host renders its whole panel with one call and stays out of the per-widget GL. The look
 * rides on a {@link WidgetStyle}; the per-frame border width and opacity are parameters. A {@link
 * TabPanelRenderer} reuses this for the body and overlays a tabs header on the top band.
 *
 * <p>Brackets the draw in one {@link GlStateGuard#bracket} state save - the map chrome and tooltips draw
 * after a UI-overlay pass, so any enable / colour / blend state the panel touches must be restored - and
 * the whole draw shares that one save. The scrolling control (the one marked {@link
 * kmlib.starsector.ui.controls.ControlSpec.VerticalTable#scrolls()}) draws clipped to its viewport, so
 * its rows that scroll past the top slide out under a pinned control rather than overpainting it. GL
 * passthrough exercised in-engine like the other draw helpers.
 */
public final class PanelRenderer {
    private PanelRenderer() {
    }

    /**
     * Draws the panel: the bordered frame, the body controls, and the scrollbar when the body is capped,
     * all faded by {@code opacity}. Must run with a current GL context. The {@code border} names which
     * frame edges to stroke, so a panel flush against another's edge can drop the border there; the body
     * controls and scrollbar are unaffected.
     *
     * @param placement the laid-out panel to draw
     * @param style     how the panel looks (fill, accents, fonts)
     * @param border    the outer border width and which edges to stroke; a zero width draws no border
     * @param opacity   overall alpha, 0..1, fading the whole panel
     */
    public static void render(
            PanelPlacement placement,
            WidgetStyle style,
            BoxBorder border,
            float opacity) {
        // Belt-and-suspenders around the raw GL: the map chrome and tooltips draw after a UI-overlay
        // pass, so any state the panel touches must be restored. The whole draw shares this one save.
        GlStateGuard.bracket(() -> {
            BorderedBoxRenderer.render(
                    placement.box(),
                    border,
                    new UiElementPaint(style.panelFill(), opacity),
                    new UiElementPaint(style.accent(), opacity));
            drawBodyControls(placement, style, opacity);
        });
    }

    // Draws each body control in the lit state its spec carries, then - when the body is capped - the
    // scrollbar for its scrolling control. The one control marked as the scroll region draws clipped to
    // its viewport; every other control draws unclipped in its pinned place.
    private static void drawBodyControls(
            PanelPlacement placement,
            WidgetStyle style,
            float opacity) {

        for (var control : placement.bodyControls()) {
            if (control.spec() instanceof ControlSpec.VerticalTable table && table.scrolls()) {
                
                // The scroll clip replaces any outer clip (a raw GL scissor is absolute), so it is
                // intersected with the box first: when a collapsing tab panel narrows the box, the list
                // stays inside that shrinking frame and wipes with it rather than escaping to full width.
                // Uncollapsed the viewport already sits within the box, so the intersection is a no-op.
                UiScissor.push(placement
                        .flexViewport()
                        .intersectWith(placement.box()));

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

    private static void drawControl(Control control, WidgetStyle style, float opacity) {
        ControlRenderer.render(control, style, opacity);
    }

    // The scrollbar for the capped body: track and thumb come from the placement's scroll region - the
    // same geometry the input listener hit-tests for a drag - so what is drawn and what a drag grabs
    // cannot drift.
    private static void drawScrollbar(PanelPlacement placement, WidgetStyle style, float opacity) {
        var track = PanelScrollbars.computeTrack(placement);
        var thumb = PanelScrollbars.computeThumb(placement);
        ScrollbarRenderer.render(
                track,
                thumb,
                style.accent(),
                opacity);
    }
}
