package kmlib.starsector.ui.render.gl.panel;

import kmlib.starsector.ui.controls.BodyInteractionSources;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlInteractionSources;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.render.gl.GlStateGuard;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiScissor;
import kmlib.starsector.ui.render.gl.controls.ControlRenderer;
import kmlib.starsector.ui.render.gl.style.WidgetStyle;
import kmlib.starsector.ui.render.gl.tabs.TabPanelRenderer;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.PanelAlpha;
import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.starsector.ui.widgets.scroll.PanelScrollbars;

/**
 * Raw-GL paint for a whole headerless {@link PanelPlacement}: the bordered frame (via {@link
 * BorderedBoxRenderer}), then each body control (via {@link ControlRenderer}), then the scrollbar when
 * the body is capped. It composes the frame, the per-control paint, and the scrollbar into one panel
 * draw, so a host renders its whole panel with one call and stays out of the per-widget GL. The look
 * rides on a {@link WidgetStyle}; the per-frame border width and alpha are parameters. A {@link
 * TabPanelRenderer} reuses this for the body and overlays a tabs header on the top band.
 *
 * <p>Brackets the draw in one {@link GlStateGuard#bracket} state save - the map chrome and tooltips draw
 * after a UI-overlay pass, so any enable / colour / blend state the panel touches must be restored - and
 * the whole draw shares that one save. The scrolling control (the one marked {@link
 * kmlib.starsector.ui.controls.ControlSpec.VerticalTable#scrolls()}) draws clipped to its viewport, so
 * its rows that scroll past the top slide out under a pinned control rather than overpainting it. GL
 * passthrough run only in-engine like the other draw helpers.
 */
public final class PanelRenderer {
    private PanelRenderer() {
    }

    /**
     * Draws the panel: the bordered frame, the body controls at whatever point of their hover fades they
     * stand, and the scrollbar when the body is capped, all faded by {@code alpha}. Must run with a
     * current GL context. The {@code border} names which frame edges to stroke, so a panel flush against
     * another's edge can drop the border there; the body controls and scrollbar are unaffected.
     *
     * <p>A consumer drawing a panel without an animator behind it passes {@link
     * BodyInteractionSources#RESTING} and every control paints the settled look its spec names. Named at the
     * call site rather than offered as a shorter overload, so a panel drawn with no hover says as much where
     * it is drawn.
     *
     * @param placement        the laid-out panel to draw
     * @param style            how the panel looks (fill, frame colour, accents, fonts)
     * @param border           the outer border width and which edges to stroke; a zero width draws no border
     * @param bodyInteractions how far onto its hovered look, and how far through its press lift, each cell of
     *                         each body control stands, resolved by whoever owns the panel's live state,
     *                         since this pass reads no cursor and holds no timing
     * @param alpha            the panel's two alpha channels - the body's own translucency and how much
     *                         of the panel is on screen - which its chrome and its words take separately
     */
    public static void render(
            PanelPlacement placement,
            WidgetStyle style,
            BoxBorder border,
            BodyInteractionSources bodyInteractions,
            PanelAlpha alpha) {
        // Belt-and-suspenders around the raw GL: the map chrome and tooltips draw after a UI-overlay
        // pass, so any state the panel touches must be restored. The whole draw shares this one save.
        GlStateGuard.bracket(() -> {
            // The frame strokes in the style's own border colour rather than its accent, so a host whose
            // surrounding chrome is a different colour can match it without dragging its controls along.
            BorderedBoxRenderer.render(
                placement.box(),
                border,
                new UiElementPaint(style.boxColours().fill(), alpha.resolveBodyAlpha()),
                new UiElementPaint(style.boxColours().border(), alpha.resolveBodyAlpha()));
            drawBodyControls(placement, style, bodyInteractions, alpha);
        });
    }

    // Draws each body control in the lit state its spec carries and at whatever point of its hover fades and
    // press lifts its cells stand, then - when the body is capped - the scrollbar for its scrolling control.
    // The one control marked as the scroll region draws clipped to its viewport; every other control draws
    // unclipped in its pinned place.
    //
    // Walked by index rather than over the list because the index is half of what a fade is held against:
    // a hover belongs to the place in the strip, so the walk that draws a control is what binds its
    // position and hands the widget below only the cell it is painting. It is the same numbering the hit
    // resolver walks, which is what makes the control that lights the control a press would land on.
    private static void drawBodyControls(
            PanelPlacement placement,
            WidgetStyle style,
            BodyInteractionSources bodyInteractions,
            PanelAlpha alpha) {

        var bodyControls = placement.bodyControls();

        for (var controlIndex = 0; controlIndex < bodyControls.size(); controlIndex++) {

            var control = bodyControls.get(controlIndex);

            // Both of the control's channels bound in one step, so the walk cannot pick its hover from one
            // strip position and its press from another.
            var interactions = bodyInteractions.resolveControlInteractionSourcesAt(controlIndex);

            if (control.spec() instanceof ControlSpec.VerticalTable table && table.scrolls()) {

                // The scroll clip replaces any outer clip (a raw GL scissor is absolute), so it is
                // intersected with the box first: when a collapsing tab panel narrows the box, the list
                // stays inside that shrinking frame and wipes with it rather than escaping to full width.
                // Uncollapsed the viewport already sits within the box, so the intersection is a no-op.
                UiScissor.runClippedTo(
                    placement.flexViewport().intersectWith(placement.box()),
                    () -> drawControl(control, style, interactions, alpha));
            } else {
                drawControl(control, style, interactions, alpha);
            }
        }
        if (placement.isScrollbarDrawn()) {
            drawScrollbar(placement, style, alpha);
        }
    }

    private static void drawControl(
            Control control,
            WidgetStyle style,
            ControlInteractionSources interactions,
            PanelAlpha alpha) {

        ControlRenderer.render(control, style, alpha, interactions);
    }

    // The scrollbar for the capped body: track and thumb come from the placement's scroll region - the
    // same geometry the input listener hit-tests for a drag - so what is drawn and what a drag grabs
    // cannot drift.
    private static void drawScrollbar(PanelPlacement placement, WidgetStyle style, PanelAlpha alpha) {
        var track = PanelScrollbars.computeTrack(placement);
        var thumb = PanelScrollbars.computeThumb(placement);
        ScrollbarRenderer.render(
            track,
            thumb,
            style.accentColours().base(),
            alpha.resolveBodyAlpha());
    }
}
