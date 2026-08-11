package kmlib.starsector.ui.render.gl.tabs;

import kmlib.starsector.ui.render.gl.GlStateGuard;
import kmlib.starsector.ui.render.gl.UiScissor;
import kmlib.starsector.ui.render.gl.controls.ControlRenderer;
import kmlib.starsector.ui.render.gl.panel.NotchRenderer;
import kmlib.starsector.ui.render.gl.panel.NotchState;
import kmlib.starsector.ui.render.gl.panel.PanelRenderer;
import kmlib.starsector.ui.render.gl.style.WidgetStyle;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.tabs.TabInteractionSources;
import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;

/**
 * Raw-GL paint for a whole {@link TabPanelPlacement}: it delegates the body - the one bordered frame, the
 * body controls, and the scrollbar - to {@link PanelRenderer} verbatim, then paints the tabs header
 * control (via {@link ControlRenderer}) on its own backdrop above that frame, and paints the collapse
 * handle (via {@link NotchRenderer}) protruding past the frame's right edge. A tab panel is a panel with a
 * tab row standing on it plus a handle, so its paint is the panel's paint plus the row plus the notch; the
 * single frame is the body placement's box, so there is one border, drawn once by the delegate. A bodyless
 * panel has no box and no notch (nothing to frame, nothing to collapse), so the frame and the handle are
 * both skipped and it paints as its tab row alone.
 *
 * <p>Nothing is laid under the row: its tabs are opaque surfaces of their own (see {@link
 * kmlib.starsector.ui.widgets.tabs.style.TabPalette#createMapTabPalette}), so the row reads the same wherever it
 * stands - over the panel, over a bodyless tab's bare screen, or over whatever the panel floats on. A
 * backdrop drawn here instead would make the row's look a property of the panel rather than of the tabs,
 * and every other consumer of the strip would have to supply one to get the same tabs.
 *
 * <p>While collapsing, the body is clipped to the shrinking box and the row to the {@code
 * drawnHeaderBand} the layout narrowed with it, so the panel reads as a horizontal wipe toward its anchored
 * edge and, fully docked, reduces to the border-only rail the collapsed box already draws - the rail is the
 * clipped box, not a second draw. At rest that band is the whole row, so a tab row wider than the body still
 * overhangs the frame. The notch draws last and unclipped, past the frame's right edge, so the handle stays
 * reachable even once the body has wiped away to the rail.
 *
 * <p>The header control draws its own immediate-mode GL, so it is bracketed in a {@link
 * GlStateGuard#bracket} state save like {@link PanelRenderer} brackets its own draw. Drawn after the body
 * so the row sits over the frame's top border rather than under it. GL passthrough exercised in-engine like
 * the other draw helpers.
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
     * then the tabs header above them, then the collapse handle past the right edge. The body and handle
     * fade by {@code opacity}; the header is opaque chrome and paints at full alpha regardless (see {@code
     * HEADER_OPACITY}). Must run with a current GL context. The {@code border} names which frame edges to
     * stroke, so a panel flush against another's edge can drop the border there; the header, the notch,
     * and the collapse clip are unaffected.
     *
     * @param placement       the laid-out tab panel to draw
     * @param style           how the panel looks (fill, frame colour, accents, body font, and the tab
     *                        style for the header)
     * @param border          the outer border width and which edges to stroke; a zero width draws no border
     * @param tabInteractions what each header tab is currently showing - how far onto the hovered shade it
     *                        has faded and what pulse it carries - resolved by whoever owns the panel's live
     *                        state, since this pass reads no cursor and holds no timing
     * @param notchState      how far the body is collapsed (0 lays out full and unclipped, 1 docks to the
     *                        rail, and it orients the notch's chevron) and how far the handle has lit under
     *                        the pointer, resolved by the same owner for the same reason
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

        // A bodyless panel frames nothing: it lays out no box, so there is no frame, no fill, and no fold -
        // the row above is the whole panel. Asked of the placement rather than inferred from the absent
        // handle, so what is skipped here is skipped for the reason it is skipped.
        if (placement.hasBody()) {
            drawFramedBody(placement, style, border, notchState, opacity);
        }
        drawHeaderBand(placement, style, tabInteractions);

        // The handle draws last and unclipped, over the map beyond the frame's right edge, so it stays
        // reachable to expand the panel even when the body has wiped away to the docked rail. A bodyless
        // panel has no handle, so there is nothing to draw here.
        if (placement.notch() != null) {
            NotchRenderer.render(
                placement.notch(),
                style,
                border.width(),
                notchState,
                opacity);
        }
    }

    // The framed body: the one border, the body controls, and the scrollbar, self-bracketed in their own
    // GL-state save. Only the border's edges are stroked, so a panel flush against another's edge drops the
    // border there. Clipped to the box while the fold runs, border included, so the frame narrows with the
    // fold and rides its shrinking right edge down to the docked rail.
    private static void drawFramedBody(
            TabPanelPlacement placement,
            WidgetStyle style,
            BoxBorder border,
            NotchState notchState,
            float opacity) {

        // Named once and run either way, so the clipped and unclipped paths cannot drift apart in
        // what they draw - only in whether the clip is around it.
        Runnable drawBody = () -> PanelRenderer.render(placement.body(), style, border, opacity);

        if (notchState.isFolding()) {
            UiScissor.runClippedTo(placement.body().box(), drawBody);
        } else {
            drawBody.run();
        }
    }

    // The tab row, drawn after the body so it stands over the frame rather than under it, and clipped to
    // the band the layout says is on screen: at rest that is the whole row, and while the fold runs it is
    // what the fold has not yet wiped, so the row narrows with the frame beneath it. The row's raw GL needs
    // the same state save the body's draw takes.
    private static void drawHeaderBand(
            TabPanelPlacement placement,
            WidgetStyle style,
            TabInteractionSources tabInteractions) {

        // Clipped to what the chrome paints rather than to the band alone: a chrome whose buttons lay their
        // borders on the panel's own lines reaches a hairline outside the band, and a clip cut to the band
        // would drop exactly those borders. The fold still wipes the row, the reach travelling with the
        // narrowing band.
        UiScissor.runClippedTo(
            TabChromeRenderer.computePaintedRegionFor(
                style.tabStyle().chrome(),
                placement.drawnHeaderBand(),
                style.tabStyle().resolveTabHeight()),
            () -> GlStateGuard.bracket(() -> ControlRenderer.render(
                placement.tabsHeader(),
                style,
                HEADER_OPACITY,
                tabInteractions)));
    }
}
