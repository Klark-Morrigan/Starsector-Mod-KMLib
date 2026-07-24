package kmlib.starsector.ui.render.gl;

import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;

import java.util.Set;

/**
 * Raw-GL paint for a whole {@link TabPanelPlacement}: it delegates the body - the one bordered frame, the
 * body controls, and the scrollbar - to {@link PanelRenderer} verbatim, then overlays the tabs header
 * control (via {@link ControlRenderer}) on the frame's top band, and paints the collapse handle (via {@link
 * NotchRenderer}) protruding past the frame's right edge. A tab panel is a panel plus a header plus a
 * handle, so its paint is the panel's paint plus one control drawn on top plus the notch; the single frame
 * is the body placement's whole-footprint box, so there is one border, drawn once by the delegate. A
 * bodyless panel carries no notch (nothing to collapse), so the handle and the collapse clip are both
 * skipped and it paints as its bordered tab row alone.
 *
 * <p>While collapsing, the body and header are clipped to the shrinking box, so the panel reads as a
 * horizontal wipe toward its anchored edge and, fully docked, reduces to the border-only rail the collapsed
 * box already draws - the rail is the clipped box, not a second draw. Fully expanded there is no clip, so a
 * tab row wider than the body still overhangs the frame. The notch draws last and unclipped, past the
 * frame's right edge, so the handle stays reachable even once the body has wiped away to the rail.
 *
 * <p>The header control draws its own immediate-mode GL, so it is bracketed in a {@link
 * GlStateGuard#bracket} state save like {@link PanelRenderer} brackets its own draw. Drawn after the body
 * so the header sits over the frame fill of the top band. GL passthrough exercised in-engine like the
 * other draw helpers.
 */
public final class TabPanelRenderer {
    private TabPanelRenderer() {
    }

    /**
     * Draws the tab panel: the bordered frame, body controls, and scrollbar via {@link PanelRenderer},
     * then the tabs header on top, then the collapse handle past the right edge, all faded by {@code
     * opacity}. Must run with a current GL context.
     *
     * @param placement   the laid-out tab panel to draw
     * @param style       how the panel looks (fill, accents, body font, and the tab style for the header)
     * @param borderWidth the outer border thickness; 0 draws no border
     * @param notchState  how far the body is collapsed (0 lays out full and unclipped, 1 docks to the rail,
     *                    and it orients the notch's chevron) and whether the handle is hovered
     * @param opacity     overall alpha, 0..1, fading the whole panel
     */
    public static void render(
            TabPanelPlacement placement,
            WidgetStyle style,
            float borderWidth,
            NotchState notchState,
            float opacity) {
        render(placement, style, borderWidth, notchState, BoxEdge.ALL, opacity);
    }

    /**
     * Draws the tab panel as {@link #render(TabPanelPlacement, WidgetStyle, float, NotchState, float)}
     * does, but strokes only the {@code borderEdges} of the frame, leaving the omitted sides open so a
     * panel flush against another's edge can drop the border there. The header, the notch, and the
     * collapse clip are unaffected.
     *
     * @param placement   the laid-out tab panel to draw
     * @param style       how the panel looks (fill, accents, body font, and the tab style for the header)
     * @param borderWidth the outer border thickness; 0 draws no border
     * @param notchState  how far the body is collapsed (0 lays out full and unclipped, 1 docks to the rail,
     *                    and it orients the notch's chevron) and whether the handle is hovered
     * @param borderEdges which of the frame's four edges to stroke; the rest are left open
     * @param opacity     overall alpha, 0..1, fading the whole panel
     */
    public static void render(
            TabPanelPlacement placement,
            WidgetStyle style,
            float borderWidth,
            NotchState notchState,
            Set<BoxEdge> borderEdges,
            float opacity) {
        var box = placement.body().box();
        
        // A null notch marks a bodyless, non-collapsible panel (nothing to fold): it draws no handle and
        // never clips, so a stale docked fraction left in the controller by another tab cannot scissor its
        // tab row down to the border box and hide it with no handle to bring it back.
        var notch = placement.notch();

        // Any collapse clips the body and header to the box, so the frame narrows the visible content with
        // it and the docked state shows only the rail. Fully expanded there is no clip, leaving a wide tab
        // row free to overhang the frame.
        var isCollapsing = notch != null && notchState.collapseFraction() > 0f;
        if (isCollapsing) {
            UiScissor.push(box);
        }

        // The body carries the whole-footprint box, so this draws the one frame, the body controls, and
        // the scrollbar - self-bracketed in its own GL-state save. Only the requested edges of the frame
        // are stroked, so a panel flush against another's edge drops the border there.
        PanelRenderer.render(placement.body(), style, borderWidth, borderEdges, opacity);

        // The header control's raw GL needs the same state save; bracket it here. Drawn after the body so
        // it sits over the frame fill of the top band.
        GlStateGuard.bracket(() -> ControlRenderer.render(placement.tabsHeader(), style, opacity));
        if (isCollapsing) {
            UiScissor.pop();
        }

        // The handle draws last and unclipped, over the map beyond the frame's right edge, so it stays
        // reachable to expand the panel even when the body has wiped away to the docked rail. A bodyless
        // panel has no handle, so there is nothing to draw here.
        if (notch != null) {
            NotchRenderer.render(notch, style, borderWidth, notchState, opacity);
        }
    }
}
