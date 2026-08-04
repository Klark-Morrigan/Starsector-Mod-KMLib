package kmlib.starsector.ui.render.gl;

import kmlib.starsector.ui.widgets.BorderedBox;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.tabs.TabInteractionSources;
import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;

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
 * <p>While collapsing, the body is clipped to the shrinking box and the header to that box's INTERIOR
 * (inset past the border), so the panel reads as a horizontal wipe toward its anchored edge and, fully
 * docked, reduces to the border-only rail the collapsed box already draws - the rail is the clipped box,
 * not a second draw. The header clips to the interior rather than the whole box because it lays out at the
 * panel's full width; clipped to the box its opaque tab fill would wipe right up onto the right border and
 * paint over it across the header band, leaving the border showing over the body but covered over the
 * header - a step at the header/body seam. Fully expanded there is no clip, so a tab row wider than the
 * body still overhangs the frame. The notch draws last and unclipped, past the frame's right edge, so the
 * handle stays reachable even once the body has wiped away to the rail.
 *
 * <p>The header control draws its own immediate-mode GL, so it is bracketed in a {@link
 * GlStateGuard#bracket} state save like {@link PanelRenderer} brackets its own draw. Drawn after the body
 * so the header sits over the frame fill of the top band. GL passthrough exercised in-engine like the
 * other draw helpers.
 */
public final class TabPanelRenderer {
    // The tabs header is opaque chrome, not part of the translucent body: it paints at full alpha
    // regardless of the body's opacity, so the tab row reads as a solid cap over a see-through panel
    // rather than fading with it. The body's translucency comes from its panelFill's own alpha and the
    // opacity parameter; the header opts out of both. This is the seam where the tab chrome decouples
    // from the panel's global opacity - the strip renderer still fades faithfully by whatever alpha it
    // is handed, it is simply handed full alpha here.
    private static final float HEADER_OPACITY = 1f;

    private TabPanelRenderer() {
    }

    /**
     * Draws the tab panel: the bordered frame, body controls, and scrollbar via {@link PanelRenderer},
     * then the tabs header on top, then the collapse handle past the right edge. The body and handle fade
     * by {@code opacity}; the header is opaque chrome and paints at full alpha regardless (see {@code
     * HEADER_OPACITY}). Must run with a current GL context. The {@code border} names which frame edges to
     * stroke, so a panel flush against another's edge can drop the border there; the header, the notch,
     * and the collapse clip are unaffected.
     *
     * @param placement       the laid-out tab panel to draw
     * @param style           how the panel looks (fill, accents, body font, and the tab style for the
     *                        header)
     * @param border          the outer border width and which edges to stroke; a zero width draws no border
     * @param tabInteractions what each header tab is currently showing - how far onto the hovered shade it
     *                        has faded and what pulse it carries - resolved by whoever owns the panel's live
     *                        state, since this pass reads no cursor and holds no timing
     * @param notchState      how far the body is collapsed (0 lays out full and unclipped, 1 docks to the
     *                        rail, and it orients the notch's chevron) and whether the handle is hovered
     * @param opacity         overall alpha, 0..1, fading the body and the collapse handle; the tabs header
     *                        ignores it and paints opaque
     */
    public static void render(
            TabPanelPlacement placement,
            WidgetStyle style,
            BoxBorder border,
            TabInteractionSources tabInteractions,
            NotchState notchState,
            float opacity) {

        var box = placement.body().box();

        // A null notch marks a bodyless, non-collapsible panel (nothing to fold): it draws no handle and
        // never clips, so a stale docked fraction left in the controller by another tab cannot scissor its
        // tab row down to the border box and hide it with no handle to bring it back.
        var notch = placement.notch();

        // Any collapse narrows the visible content so the docked state shows only the rail; fully expanded
        // there is no clip, leaving a wide tab row free to overhang the frame.
        var isCollapsing = notch != null && notchState.collapseFraction() > 0f;

        // The body carries the whole-footprint box, so this draws the one frame, the body controls, and
        // the scrollbar - self-bracketed in its own GL-state save. Only the border's edges are stroked, so
        // a panel flush against another's edge drops the border there. Clipped to the box during collapse,
        // border included, so the frame narrows with the fold and rides its shrinking right edge.
        if (isCollapsing) {
            UiScissor.push(box);
        }
        PanelRenderer.render(placement.body(), style, border, opacity);
        if (isCollapsing) {
            UiScissor.pop();
        }

        // The header control's raw GL needs the same state save; bracket it here. Drawn after the body so
        // it sits over the frame FILL of the top band. During collapse it is clipped to the box INTERIOR
        // (past the border), not the whole box: the header lays out at the panel's full width, so a
        // full-box clip would wipe its fill onto the right border and cover it across the header band while
        // the body keeps it - the step at the header/body seam. Interior-clipped, its wipe stops at the
        // border's inner edge and the right border reads continuously past the header. The interior insets
        // only the STROKED edges, so a dropped edge (the intel panel's left) keeps the header flush there
        // rather than clipping a border-width strip off a side that has no border to protect.
        if (isCollapsing) {
            UiScissor.push(BorderedBox.computeContentBounds(box, border));
        }

        // Tab headers:
        GlStateGuard.bracket(() -> ControlRenderer.render(
            placement.tabsHeader(),
            style,
            HEADER_OPACITY,
            tabInteractions));

        if (isCollapsing) {
            UiScissor.pop();
        }

        // The handle draws last and unclipped, over the map beyond the frame's right edge, so it stays
        // reachable to expand the panel even when the body has wiped away to the docked rail. A bodyless
        // panel has no handle, so there is nothing to draw here.
        if (notch != null) {
            NotchRenderer.render(
                notch,
                style,
                border.width(),
                notchState,
                opacity);
        }
    }
}
