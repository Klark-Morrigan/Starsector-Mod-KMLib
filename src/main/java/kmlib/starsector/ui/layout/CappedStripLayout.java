package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.layout.ControlStripLayout.StripMeasurement;

import java.util.ArrayList;
import java.util.List;

/**
 * Caps a {@link ControlStripLayout} strip to a maximum body height by letting ONE control - the strip's
 * scrolling flex region, the control whose {@link ControlSpec.VerticalTable#scrolls()} is set - give up its height and
 * scroll its own rows. The controls before the flex region pin from the body top exactly as an uncapped
 * strip places them; the controls after it pin to the body bottom; the flex region takes the room left
 * between and, when its natural rows overrun that room, scrolls within it. So a long list stays reachable
 * inside a bounded box while the header and footer controls around it never move.
 *
 * <p>As the strip's main region, the flex region also fills the body's content width rather than its own
 * narrower row width - it flexes horizontally into the leftover width just as it flexes vertically into
 * the leftover height. The body is as wide as the widest control, so a flex list narrower than the widest
 * header row spreads to the frame instead of leaving a gutter of dead space between it and the scrollbar.
 *
 * <p>Pure geometry in UI coordinates (origin bottom-left, y grows up), split into the same measure-then-
 * place shape as the strip it caps: {@link #capBodyHeight} shrinks the measured body height for the host
 * to frame its chrome around, then {@link #layoutCappedControls} places the controls inside the framed
 * body - the header stacked from the top, the footer pinned to the bottom, the flex list at its full
 * natural height shifted by the clamped scroll offset, plus the viewport the host clips the flex draw to.
 * When nothing overflows (the strip fits under the cap, or carries no flex region) the placement is
 * identical to the plain {@link ControlStripLayout} stack, so the cap is inert until it bites.
 */
public final class CappedStripLayout {
    /** {@link #findScrollingIndex} returns this when no control in the strip scrolls. */
    public static final int NO_FLEX_REGION = -1;

    // A zero rectangle for the flex viewport when the strip has no scrolling region, so a host reads a
    // non-null viewport uniformly and simply finds nothing to clip.
    private static final Rectangle NO_VIEWPORT = new Rectangle(0f, 0f, 0f, 0f);

    private CappedStripLayout() {
    }

    /**
     * The index of the strip's scrolling flex control - the one control whose {@link
     * ControlSpec#scrolls()} is set - or {@link #NO_FLEX_REGION} when none scrolls. At most one control
     * scrolls, so the first match is the flex region; a strip with none is never capped. Resolved once
     * by the host and handed to both {@link #capBodyHeight} and {@link #layoutCappedControls}, so the
     * two agree on which control gives up its height.
     *
     * @param specs the strip's controls, top to bottom
     * @return the scrolling control's index, or {@link #NO_FLEX_REGION}
     */
    public static int findScrollingIndex(List<ControlSpec> specs) {
        for (var index = 0; index < specs.size(); index++) {
            if (specs.get(index) instanceof ControlSpec.VerticalTable table && table.scrolls()) {
                return index;
            }
        }
        return NO_FLEX_REGION;
    }

    /**
     * The body height to frame the strip at under {@code maxBodyHeight}: the natural measured height when
     * the strip already fits or carries no flex region, otherwise the height with the flex region shrunk
     * by the overshoot. The flex region gives up its height only down to one row, so the list never
     * vanishes; a cap tighter than "header + footer + one row" leaves the body a hair over the cap rather
     * than a zero-height list. Only the flex region shrinks - the header and footer keep their measured
     * height - so the shrink is exactly the height the flex list must then scroll.
     *
     * @param strip         the measured strip, from {@link ControlStripLayout#measureStrip}
     * @param flexIndex     the scrolling control's index, from {@link #findScrollingIndex} ({@link
     *                      #NO_FLEX_REGION} for a strip that cannot scroll and so is never capped)
     * @param maxBodyHeight the most the framed body may stand
     * @return the body height to frame, capped only when the strip overflows and can scroll
     */
    public static float capBodyHeight(
            StripMeasurement strip,
            int flexIndex,
            float maxBodyHeight) {

        var naturalHeight = strip.bodyHeight();
        if (flexIndex == NO_FLEX_REGION || naturalHeight <= maxBodyHeight) {
            return naturalHeight;
        }
        var overshoot = naturalHeight - maxBodyHeight;

        // Keep at least one row of the list, so a very tight cap shows a one-row scrolling list rather
        // than shrinking it to nothing.
        var flexNatural = strip.rowHeights().get(flexIndex);
        var maxShrink = Math.max(0f, flexNatural - ControlStripLayout.CONTROL_ROW_HEIGHT);

        return naturalHeight - Math.min(overshoot, maxShrink);
    }

    /**
     * Places the strip's controls inside the framed body, scrolling the flex region: the header stacks
     * from the top inset (unchanged from an uncapped strip), the footer pins to the bottom inset as a
     * block, and the flex list draws at its full natural height shifted up by the clamped scroll offset
     * so its overrun rows slide out under the header. Also returns the flex viewport the host clips the
     * list draw to, the clamped scroll offset, and the scroll overflow (zero when the list fits). With no
     * flex region the placement is the plain {@link ControlStripLayout} stack and the viewport is empty.
     *
     * <p>The body rectangle must carry the height {@link #capBodyHeight} returned for the same strip, so
     * the flex viewport lands one row or taller; a body framed smaller than "header + footer + one row"
     * leaves the flex region nothing to occupy.
     *
     * @param body            the framed body rectangle, sized via {@link #capBodyHeight}
     * @param specs           the controls to place, top to bottom (must match the measured specs)
     * @param rowHeights      the measured row heights, from {@link StripMeasurement#rowHeights()}
     * @param rowWidths       the measured row widths, from {@link StripMeasurement#rowWidths()}
     * @param flexIndex       the scrolling control's index, from {@link #findScrollingIndex}
     * @param rawScrollOffset the requested scroll offset in pixels; clamped to the available overflow
     * @param measurer        measures each label's rendered width, for snapping a tabs row's segments
     * @return the laid-out controls, the flex viewport, the clamped offset, and the overflow
     */
    public static CappedStripPlacement layoutCappedControls(
            Rectangle body,
            List<ControlSpec> specs,
            List<Float> rowHeights,
            List<Float> rowWidths,
            int flexIndex,
            float rawScrollOffset,
            LineWidthMeasurer measurer) {

        if (specs.isEmpty()) {
            return new CappedStripPlacement(List.of(), NO_VIEWPORT, 0f, 0f);
        }
        if (flexIndex == NO_FLEX_REGION) {
            // Nothing scrolls, so the strip pins whole - byte-for-byte the plain stacked layout.
            var pinned = ControlStripLayout.layoutControls(body, specs, rowHeights, rowWidths, measurer);
            return new CappedStripPlacement(pinned, NO_VIEWPORT, 0f, 0f);
        }
        var originX = body.x() + ControlStripLayout.BODY_PADDING;
        var topInsetY = body.y() + body.height() - ControlStripLayout.BODY_PADDING;
        var bottomInsetY = body.y() + ControlStripLayout.BODY_PADDING;
        var gap = ControlStripLayout.ROW_GAP;

        // Header: the controls above the flex row, stacked from the top inset. A capped strip only
        // shrinks the flex region, so the header lands exactly where an uncapped strip would place it.
        var headerSpecs = specs.subList(0, flexIndex);
        var headerHeights = rowHeights.subList(0, flexIndex);
        var headerWidths = rowWidths.subList(0, flexIndex);

        // Span the header's dividers to the full body before pinning, so a section rule above the flex
        // list reaches the frame the same as an uncapped strip's does.
        var headerRows = ControlStripLayout.spanDividerRowsToBody(
            headerSpecs,
            RowStack.layoutRows(originX, topInsetY, gap, headerHeights, headerWidths),
            body);

        // The flex viewport's top edge is where the flex row starts: one gap below the last header row,
        // read straight off the laid-out header rather than re-summing their heights. With no header the
        // flex row heads the strip at the top inset.
        var flexViewportTopY = headerRows.isEmpty()
            ? topInsetY
            : headerRows.get(headerRows.size() - 1).y() - gap;

        // Footer: the controls below the flex row, pinned to the bottom inset as a block, so they never
        // scroll and always sit flush at the body bottom regardless of the list's length.
        var footerSpecs = specs.subList(flexIndex + 1, specs.size());
        var footerHeights = rowHeights.subList(flexIndex + 1, rowHeights.size());
        var footerWidths = rowWidths.subList(flexIndex + 1, rowWidths.size());
        var footerCount = footerHeights.size();
        var footerTopY = bottomInsetY + ControlStripLayout.measureStackedHeight(footerHeights);

        // Span the footer's dividers to the full body too, so a rule beneath the flex list spans edge to
        // edge like the header's.
        var footerRows = ControlStripLayout.spanDividerRowsToBody(
            footerSpecs,
            RowStack.layoutRows(originX, footerTopY, gap, footerHeights, footerWidths),
            body);

        // The viewport's bottom edge sits one gap above the footer block, or at the bottom inset when
        // there is no footer.
        var flexViewportBottomY = footerCount == 0 ? bottomInsetY : footerTopY + gap;
        var flexViewportHeight = Math.max(0f, flexViewportTopY - flexViewportBottomY);
        var flexNatural = rowHeights.get(flexIndex);

        // The flex region fills the body's content width, not just its own row width: it is the strip's
        // main region, so it flexes horizontally into the leftover width the same way it flexes
        // vertically into the leftover height. The body is as wide as the widest control, so this only
        // ever grows the list (a list already as wide as the body is unchanged), spreading its columns to
        // the frame and right-aligning each row's trailing value against the panel edge by the scrollbar.
        var flexWidth = Math.max(
            rowWidths.get(flexIndex),
            body.width() - 2f * ControlStripLayout.BODY_PADDING);
        var overflow = Math.max(0f, flexNatural - flexViewportHeight);
        var scrollOffset = clamp(rawScrollOffset, overflow);

        // The list draws at its full natural height, shifted up by the clamped scroll so the rows past
        // the viewport top slide out under the header; the host clips the draw to the viewport.
        var flexContentTopY = flexViewportTopY + scrollOffset;
        var flexBounds = new Rectangle(originX, flexContentTopY - flexNatural, flexWidth, flexNatural);
        var flexViewport = new Rectangle(originX, flexViewportBottomY, flexWidth, flexViewportHeight);

        // Assemble in strip order: the pinned header, the scrolled flex list, then the pinned footer -
        // each run turned into controls through the shared zip so segments split identically everywhere.
        var controls = new ArrayList<Control>(specs.size());
        controls.addAll(ControlStripLayout.toControls(headerSpecs, headerRows, measurer));
        controls.add(ControlStripLayout.toControl(specs.get(flexIndex), flexBounds, measurer));
        controls.addAll(ControlStripLayout.toControls(footerSpecs, footerRows, measurer));

        return new CappedStripPlacement(
            List.copyOf(controls),
            flexViewport,
            scrollOffset,
            overflow);
    }

    /**
     * Measures, caps, and places a scrollable control strip into a body region hung from {@code bodyTopY}
     * down to at most {@code maxBodyHeight}: the shared body composition a plain panel and a tab panel
     * both frame (the plain panel wraps it in a border, the tab panel hangs it beneath a header), so the
     * two size and lay their body the same way and only their outer framing differs. The body is as wide
     * as the measured strip and as tall as the capped height, left-aligned at {@code originX}, and the
     * strip's one scrolling control gives up the overshoot past {@code maxBodyHeight}.
     *
     * @param originX         the body's left edge (the content inset), in UI coordinates
     * @param bodyTopY        the body's top edge, in UI coordinates (below a header, or the content top)
     * @param maxBodyHeight   the most the body may stand before its scrolling control caps
     * @param bodyControls    the body controls, top to bottom (empty for no body)
     * @param measurer        measures each label's rendered width for text snapping
     * @param rawScrollOffset the requested scroll offset for the scrolling control; clamped to its overflow
     * @return the framed body rectangle and the capped, placed controls inside it
     */
    public static BodyStrip layoutBodyStrip(
            float originX,
            float bodyTopY,
            float maxBodyHeight,
            List<ControlSpec> bodyControls,
            LineWidthMeasurer measurer,
            float rawScrollOffset) {

        var strip = ControlStripLayout.measureStrip(bodyControls, measurer);
        var flexIndex = findScrollingIndex(bodyControls);
        var bodyHeight = capBodyHeight(strip, flexIndex, maxBodyHeight);
        var bounds = new Rectangle(originX, bodyTopY - bodyHeight, strip.bodyWidth(), bodyHeight);

        var placement = layoutCappedControls(
            bounds,
            bodyControls,
            strip.rowHeights(),
            strip.rowWidths(),
            flexIndex,
            rawScrollOffset,
            measurer);

        return new BodyStrip(bounds, placement);
    }

    // Confines a requested offset to the scrollable range: 0 when the list fits (overflow 0) or the
    // request runs past the top, up to the overflow when it runs past the bottom.
    private static float clamp(float requested, float overflow) {
        if (requested < 0f) {
            return 0f;
        }
        return Math.min(requested, overflow);
    }

    /**
     * A capped strip's placement: the laid-out controls (header pinned top, flex list scrolled, footer
     * pinned bottom), the flex viewport the host clips the list draw to, the scroll offset actually
     * applied (the request clamped to the overflow), and the scroll overflow - how far past the viewport
     * the list's rows run, zero when it fits. The viewport is a zero rectangle when the strip has no
     * scrolling region, in which case the offset and overflow are zero and the controls are the plain
     * stack.
     *
     * @param controls       the laid-out controls, top to bottom
     * @param flexViewport   the clip rectangle for the flex list, zero-size when nothing scrolls
     * @param scrollOffset   the applied scroll offset in pixels, clamped to {@code scrollOverflow}
     * @param scrollOverflow how far the flex list overruns its viewport, zero when it fits
     */
    public record CappedStripPlacement(
            List<Control> controls,
            Rectangle flexViewport,
            float scrollOffset,
            float scrollOverflow) {
        /**
         * @return whether the flex list overruns its viewport, so the host draws a scrollbar and scrolls
         *         on a wheel event
         */
        public boolean isScrollbarNeeded() {
            return scrollOverflow > 0f;
        }
    }

    /**
     * A laid-out body strip: the framed body {@code bounds} and the capped, scrolled controls {@code
     * placement} inside it, returned together by {@link #layoutBodyStrip} so a panel frames its chrome
     * around one value.
     *
     * @param bounds    the framed body rectangle, in UI coordinates
     * @param placement the capped controls, flex viewport, and scroll geometry inside the body
     */
    public record BodyStrip(
        Rectangle bounds,
        CappedStripPlacement placement) {
    }
}
