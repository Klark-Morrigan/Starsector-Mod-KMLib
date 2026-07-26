package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;
import kmlib.math.ranges.Ranges;
import kmlib.starsector.ui.color.StarsectorUiColor;

/**
 * Raw-GL paint for the collapse handle: the notch protruding past the panel's right border edge, the
 * chevron inside it whose orientation tracks the collapse fraction, and the hover lighting. It draws the
 * one rect the layout exposes and the input pass hit-tests, so the drawn handle is the clickable one. The
 * GL passthrough (over {@link UiFill}), exercised in-engine like the other draw helpers; the chevron
 * orientation and the border floor are pure computations split out ({@link #computeChevronArms}, {@link
 * #computeNotchBorder}) so the glyph's contract is unit-testable without a GL context.
 *
 * <p>The chevron reads as one glyph rotating through the collapse: left-pointing at full expansion (the
 * collapse cue), straightening to a plain vertical line at the midpoint, and flipped to point right once
 * docked (the expand cue). Its outline is stroked one pixel thinner than the frame ({@code max(1,
 * borderWidth - 1)}) so the handle reads as a lighter appendage of the border rather than a second frame,
 * and it paints in the highlight gold ({@link StarsectorUiColor#VANILLA_HIGHLIGHT_GOLD}) rather than the
 * style's accent, so the actionable glyph stands apart from the chrome carrying it.
 */
public final class NotchRenderer {
    // Hover wash: a translucent accent overlay lighting the notch when the pointer is over it, so the
    // handle answers the hover without a second colour in the style bundle.
    private static final float HOVER_WASH_ALPHA = 0.35f;
    // The chevron's footprint inside the notch: how far the arms inset from the notch's top and bottom
    // edges, and how far the apex swings off centre at each end of the collapse.
    private static final float CHEVRON_INSET = 6f;
    private static final float CHEVRON_APEX_TRAVEL = 4f;
    // The notch strokes one pixel thinner than the frame, floored so a hairline frame still leaves an
    // edge to trace.
    private static final float MIN_NOTCH_BORDER = 1f;

    private NotchRenderer() {
    }

    /**
     * Draws the notch: a panel-fill backdrop (accent-washed on hover), the three outer edges stroked one
     * pixel thinner than the frame, and the fraction-oriented chevron in the highlight gold, all faded by
     * {@code opacity}. Must
     * run with a current GL context, like any immediate-mode GL call.
     *
     * @param notch       the collapse-handle rect on the box's right border edge, in UI coordinates
     * @param style       the panel look (the fill and accent the handle's backdrop and frame draw in;
     *                    the chevron takes the highlight gold instead)
     * @param borderWidth the frame's border thickness; the notch strokes one pixel thinner, floored at 1
     * @param state       how far the body is collapsed (orienting the chevron) and whether the handle is hovered
     * @param opacity     overall alpha, 0..1, fading the handle with the panel
     */
    public static void render(
            Rectangle notch,
            WidgetStyle style,
            float borderWidth,
            NotchState state,
            float opacity) {

        // Solid backdrop so the handle reads as panel chrome protruding over the map, not a floating glyph.
        var notchPaint = new UiElementPaint(style.panelFill(), opacity);
        UiFill.renderQuad(notch, notchPaint);

        if (state.isHovered()) {
            // Light the whole notch face on hover, so the handle answers the pointer as one lit unit.
            var hoverPaint = new UiElementPaint(style.accent(), opacity * HOVER_WASH_ALPHA);
            UiFill.renderQuad(notch, hoverPaint);
        }

        var notchBorder = computeNotchBorder(borderWidth);
        strokeOuterEdges(notch, notchBorder, new UiElementPaint(style.accent(), opacity));

        // The chevron paints in the highlight gold rather than the panel accent, so the direction cue
        // separates from the chrome it sits in instead of reading as more frame. Hover is already answered
        // by the wash lighting the whole notch face, so the glyph holds one colour across both states.
        drawChevron(
                computeChevronArms(notch, state.collapseFraction()),
                notchBorder,
                new UiElementPaint(StarsectorUiColor.VANILLA_HIGHLIGHT_GOLD.resolve(), opacity));
    }

    /**
     * @param borderWidth the frame's border thickness
     * @return the notch outline thickness: one pixel thinner than the frame, floored at one so a hairline
     *         frame still leaves an edge to trace
     */
    static float computeNotchBorder(float borderWidth) {
        return Math.max(MIN_NOTCH_BORDER, borderWidth - 1f);
    }

    /**
     * Places the chevron's two arms inside {@code notch} for a collapse fraction: the apex swings from the
     * left of the ends (the collapse cue) through coincident with them at the midpoint (a straight vertical
     * line) to the right of them (the expand cue), while the ends swing the opposite way, so the glyph
     * reads as one chevron rotating rather than two shapes swapping. The fraction is clamped, so an
     * overshooting animation value never inverts the glyph.
     *
     * @param notch            the handle rect the chevron sits within, in UI coordinates
     * @param collapseFraction how far the body is collapsed, 0 fully expanded to 1 fully docked
     * @return the arms' shared end x, apex x, and the three ys the polyline runs through
     */
    static ChevronArms computeChevronArms(Rectangle notch, float collapseFraction) {
        var fraction = Ranges.clampToUnit(collapseFraction);
        var centreX = notch.computeCenterX();
        var midY = notch.computeCenterY();
        var topY = notch.y() + notch.height() - CHEVRON_INSET;
        var bottomY = notch.y() + CHEVRON_INSET;
        // Half the travel each way off centre: negative before the midpoint (apex left, opening right),
        // zero at it (a straight line), positive after (apex right, opening left).
        var swing = (fraction - 0.5f) * 2f * CHEVRON_APEX_TRAVEL;
        return new ChevronArms(
                centreX - swing,
                centreX + swing,
                topY,
                midY,
                bottomY);
    }

    // Strokes the notch's three outer edges - top, right, bottom - leaving the left open where it meets the
    // box's right border, so the handle reads as a protrusion merging with the frame, not a boxed-in second
    // frame doubling the border along the shared edge.
    private static void strokeOuterEdges(Rectangle notch, float thickness, UiElementPaint paint) {

        var top = notch.y() + notch.height();
        var right = notch.x() + notch.width();

        var topEdge = new Rectangle(
                notch.x(),
                top - thickness,
                notch.width(),
                thickness);
        var rightEdge = new Rectangle(
                right - thickness,
                notch.y(),
                thickness,
                notch.height());
        var bottomEdge = new Rectangle(
                notch.x(),
                notch.y(),
                notch.width(),
                thickness);

        UiFill.renderQuad(topEdge, paint);
        UiFill.renderQuad(rightEdge, paint);
        UiFill.renderQuad(bottomEdge, paint);
    }

    // Strokes the two arms of the chevron as thick segments meeting at the apex: the top end down to the
    // apex, then the apex down to the bottom end.
    private static void drawChevron(ChevronArms arms, float thickness, UiElementPaint paint) {
        strokeArm(arms.endsX(), arms.topY(), arms.apexX(), arms.midY(), thickness, paint);
        strokeArm(arms.apexX(), arms.midY(), arms.endsX(), arms.bottomY(), thickness, paint);
    }

    // Draws a thick line segment from (x1, y1) to (x2, y2) as two triangles offset perpendicular to the
    // segment by half the thickness, since the fill primitive draws triangles, not strokes. A zero-length
    // segment (the straightened chevron's degenerate arms never reach it, but guard anyway) draws nothing.
    private static void strokeArm(
            float x1,
            float y1,
            float x2,
            float y2,
            float thickness,
            UiElementPaint paint) {

        var deltaX = x2 - x1;
        var deltaY = y2 - y1;
        var length = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (length <= 0f) {
            return;
        }
        var half = thickness / 2f;
        
        // Perpendicular to the segment, scaled to half the thickness: offsets each endpoint to both sides.
        var offsetX = -deltaY / length * half;
        var offsetY = deltaX / length * half;

        UiFill.renderTriangle(
                new float[] {
                        x1 + offsetX, y1 + offsetY,
                        x1 - offsetX, y1 - offsetY,
                        x2 - offsetX, y2 - offsetY,
                },
                paint);

        UiFill.renderTriangle(
                new float[] {
                        x1 + offsetX, y1 + offsetY,
                        x2 - offsetX, y2 - offsetY,
                        x2 + offsetX, y2 + offsetY,
                },
                paint);
    }
}
