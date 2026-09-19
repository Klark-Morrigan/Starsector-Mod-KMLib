package kmlib.starsector.ui.render.gl.tabs;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.BodyInteractionSources;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlInteractionSources;
import kmlib.starsector.ui.render.gl.GlStateGuard;
import kmlib.starsector.ui.render.gl.UiScissor;
import kmlib.starsector.ui.render.gl.UiSprite;
import kmlib.starsector.ui.render.gl.controls.ControlRenderer;
import kmlib.starsector.ui.render.gl.panel.NotchRenderer;
import kmlib.starsector.ui.render.gl.panel.NotchState;
import kmlib.starsector.ui.render.gl.panel.PanelRenderer;
import kmlib.starsector.ui.render.gl.style.WidgetStyle;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.PanelAlpha;
import kmlib.starsector.ui.widgets.tabs.BandButtonPlacement;
import kmlib.starsector.ui.widgets.tabs.TabInteractionSources;
import kmlib.starsector.ui.widgets.tabs.TabPanelInteractionSources;
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
 * <p>The panel's own band button, where it flies one, is painted with the tabs as one piece of chrome: same
 * clip, same state save, same alpha, and its hover fraction taken off the same reading of the pointer. What
 * it does not share is the tab style: it carries its own, so a button stands beside a strip of tabs looking
 * like the button it is. It carries no lift either, being a single cell that acts on the press rather than
 * holding one.
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
 * so the row sits over the frame's top border rather than under it. GL passthrough run only in-engine like
 * the other draw helpers.
 */
public final class TabPanelRenderer {

    private TabPanelRenderer() {
    }

    /**
     * Draws the tab panel: the bordered frame, body controls, and scrollbar via {@link PanelRenderer},
     * then the tabs header above them, then the collapse handle past the right edge. Each surface takes
     * its alpha from {@code alpha}, which settles for itself which of the two channels a surface honours -
     * the header being the one that stands opaque on the body while still going with it as the panel
     * fades. Must run with a current GL context. The {@code border} names which frame edges to
     * stroke, so a panel flush against another's edge can drop the border there; the header, the notch,
     * and the collapse clip are unaffected.
     *
     * @param placement    the laid-out tab panel to draw
     * @param style        how the panel looks (fill, frame colour, accents, body font, and the tab
     *                     style for the header)
     * @param border       the outer border width and which edges to stroke; a zero width draws no border
     * @param interactions what the pointer is doing to the panel - what each header tab is showing, and how
     *                     far each body cell has lit and how far through a press it stands - resolved by
     *                     whoever owns the panel's live state
     *                     against this same placement, since this pass reads no cursor and holds no timing.
     *                     One value rather than a channel per half, so the row and the strip beneath it
     *                     cannot be drawn from two readings of the pointer
     * @param notchState   how far the body is collapsed (0 lays out full and unclipped, 1 docks to the
     *                     rail, and it orients the notch's chevron) and how far the handle has lit under
     *                     the pointer, resolved by the same owner for the same reason
     * @param alpha        the panel's two alphas - how see-through its body is meant to be, and how far
     *                     through arriving or leaving the whole panel stands - which each surface below
     *                     reads the channel it honours from
     */
    public static void render(
            TabPanelPlacement placement,
            WidgetStyle style,
            BoxBorder border,
            TabPanelInteractionSources interactions,
            NotchState notchState,
            PanelAlpha alpha) {

        // A bodyless panel frames nothing: it lays out no box, so there is no frame, no fill, and no fold -
        // the row above is the whole panel. Asked of the placement rather than inferred from the absent
        // handle, so what is skipped here is skipped for the reason it is skipped.
        if (placement.hasBody()) {
            drawFramedBody(placement, style, border, interactions.bodyControls(), notchState, alpha);
        }
        drawHeaderBand(placement, style, interactions, alpha);

        // The handle draws last and unclipped, over the map beyond the frame's right edge, so it stays
        // reachable to expand the panel even when the body has wiped away to the docked rail. A bodyless
        // panel has no handle, so there is nothing to draw here.
        if (placement.notch() != null) {
            NotchRenderer.render(
                placement.notch(),
                style,
                border.width(),
                notchState,
                alpha.resolveBodyAlpha());
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
            BodyInteractionSources bodyInteractions,
            NotchState notchState,
            PanelAlpha alpha) {

        // Named once and run either way, so the clipped and unclipped paths cannot drift apart in
        // what they draw - only in whether the clip is around it.
        Runnable drawBody = () -> PanelRenderer.render(
            placement.body(),
            style,
            border,
            bodyInteractions,
            alpha);

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
            TabPanelInteractionSources interactions,
            PanelAlpha alpha) {

        // Clipped to what the chrome paints rather than to the band alone: a chrome whose buttons lay their
        // borders on the panel's own lines reaches a hairline outside the band, and a clip cut to the band
        // would drop exactly those borders. The fold still wipes the row, the reach travelling with the
        // narrowing band.
        UiScissor.runClippedTo(
            computeBandClip(placement, style),
            () -> GlStateGuard.bracket(() -> {

                drawBandControl(
                    placement.tabsHeader(),
                    style,
                    interactions.headerTabs(),
                    alpha);

                // The panel's own button, drawn in the same bracket and after the tabs so the two are one
                // piece of chrome under one state save, but in its own tab style - it wears a button's
                // chrome beside whatever the tabs wear, which is the whole of what makes it read as a
                // button. A panel asked for none simply has nothing here.
                var bandButton = placement.bandButton();
                if (bandButton != null) {
                    drawBandControl(
                        bandButton.control(),
                        style.withTabStyle(bandButton.style()),
                        interactions.resolveBandButtonSources(),
                        alpha);
                    drawBandButtonIcon(bandButton, interactions.bandButtonHover(), alpha);
                }
            }));
    }

    // The screen the band is allowed to write to: what the tabs' own chrome paints over the drawn band,
    // widened to what the button's chrome paints over the button where there is one. Two readings rather
    // than one because the two wear different chromes and each reaches past its own boxes by its own
    // margin - a clip cut to the tabs' reach alone would shave the frame off a raised button standing
    // beside a seamless strip.
    private static Rectangle computeBandClip(TabPanelPlacement placement, WidgetStyle style) {

        var tabsClip = TabChromeRenderer.computePaintedRegionFor(
            style.tabStyle().chrome(),
            placement.drawnHeaderBand(),
            style.tabStyle().resolveTabHeight());

        var bandButton = placement.bandButton();
        if (bandButton == null) {
            return tabsClip;
        }
        // Over the button's own box rather than over the whole band: its chrome's reach is measured from
        // what it actually paints, and the band it stands in is mostly somebody else's tabs.
        return tabsClip.unionWith(TabChromeRenderer.computePaintedRegionFor(
            bandButton.style().chrome(),
            bandButton.control().bounds(),
            bandButton.style().resolveTabHeight()));
    }

    // The mark a band button carries in place of a word, drawn over the chrome that was just laid under it
    // and sized to the tab box the layout reserved for it - the box being as wide as the image asked to be,
    // so the picture fills it rather than being letterboxed inside it.
    //
    // Washed by the button's own fade, which is the whole of what shows the pointer here: the mark fills its
    // box, so the fill lighting beneath it is covered by the very thing it would be lighting for. Which
    // shade that is belongs to the placement, this pass spending only the fraction the panel reported.
    //
    // The chrome channel, like the row's own words: the button stands opaque on a see-through body and goes
    // with the panel as it arrives and leaves.
    private static void drawBandButtonIcon(
            BandButtonPlacement bandButton,
            float hoverFraction,
            PanelAlpha alpha) {

        var icon = bandButton.icon();
        if (icon == null) {
            return;
        }
        UiSprite.renderImage(
            icon.spritePath(),
            bandButton.computeIconBox(),
            alpha.resolveChromeAlpha(),
            bandButton.resolveIconTint(hoverFraction));
    }

    // One control of the header band, drawn as the chrome it is. Both the tabs and the button beside them go
    // through it, so the two cannot come to be drawn at different alphas or with the body's cell wash
    // reaching one of them.
    private static void drawBandControl(
            Control bandControl,
            WidgetStyle style,
            TabInteractionSources tabInteractions,
            PanelAlpha alpha) {

        ControlRenderer.render(
            bandControl,
            style,
            // The chrome channel throughout: the row stands opaque on a see-through body, so it
            // takes none of the body's opacity - but it goes with the panel as that panel arrives or
            // leaves. Handed on as the pair rather than as the one number, so the words inside it
            // keep telling body paint from chrome paint the way every other control's do.
            alpha.withOpaqueBody(),
            tabInteractions,
            // The row answers the pointer and a press through its own palette - a tab meets a shade
            // rather than taking the body's cell wash - so it is drawn with those channels at rest.
            ControlInteractionSources.RESTING);
    }
}
