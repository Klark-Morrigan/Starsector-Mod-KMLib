package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.font.StripTextMeasurers;
import kmlib.starsector.ui.layout.ControlStripLayout.StripMeasurement;
import kmlib.starsector.ui.widgets.scroll.ScrollbarThickness;

import java.util.ArrayList;
import java.util.List;

/**
 * Caps a {@link ControlStripLayout} strip to a maximum body height by letting ONE control - the strip's
 * scrolling flex region, the {@link ControlSpec.ScrollingSection} a host put a run inside - give up its
 * height and scroll that run. The controls before the section pin from the body top exactly as an
 * uncapped strip places them; the controls after it pin to the body bottom; the section takes the room
 * left between and, when its run overruns that room, scrolls within it. So a long list stays
 * reachable inside a bounded box while the header and footer controls around it never move.
 *
 * <p>As the strip's main region, the flex region also fills the body's content width rather than its own
 * narrower row width - it flexes horizontally into the leftover width just as it flexes vertically into
 * the leftover height. The body is as wide as the widest control, so a flex list narrower than the widest
 * header row spreads to the frame instead of leaving a strip of dead space between it and the scrollbar.
 *
 * <p>Across, the body reserves the gutter its scrollbar stands in, growing rightward by whatever the bar
 * overruns the padding by ({@link #measureGutterExcess}) so the controls keep the width they measured to.
 * Reserved on the strip HAVING a scrolling region rather than on that region currently overrunning: a list
 * crossing the overflow threshold is an ordinary thing, and a body that changed width when it did would
 * read as the panel twitching. A strip with no scrolling control reserves nothing, having no bar to reserve
 * for.
 *
 * <p>Pure geometry in UI coordinates (origin bottom-left, y grows up), split into the same measure-then-
 * place shape as the strip it caps: {@link #capBodyHeight} shrinks the measured body height for the host
 * to frame its chrome around, then {@link #layoutCappedControls} places the controls inside the framed
 * body - the header stacked from the top, the footer pinned to the bottom, the flex list at its full
 * natural height shifted by the clamped scroll offset, plus the viewport the host clips the flex draw to.
 * When nothing overflows (the strip fits under the cap, or carries no flex region) the placement is
 * identical to the plain {@link ControlStripLayout} stack, so the cap is inert until it bites.
 *
 * <p>{@link #layoutBodyStrip} is the whole of that sequence and the only entry a host outside this package
 * needs; the phases it runs stay reachable within the package for the cases that pin them one at a time.
 */
public final class CappedStripLayout {
    /** {@link #findScrollingIndex} returns this when no control in the strip scrolls. */
    static final int NO_FLEX_REGION = -1;

    // A zero rectangle for the flex viewport when the strip has no scrolling region, so a host reads a
    // non-null viewport uniformly and simply finds nothing to clip.
    private static final Rectangle NO_VIEWPORT = new Rectangle(0f, 0f, 0f, 0f);

    private CappedStripLayout() {
    }

    /**
     * Measures, caps, and places a scrollable control strip into the room it was given: the shared body
     * composition a plain panel and a tab panel both frame (the plain panel wraps it in a border, the tab
     * panel hangs it beneath a header), so the two size and lay their body the same way and only their
     * outer framing differs. The body is as wide as the measured strip plus whatever gutter its bar needs
     * past the padding, as tall as the capped height, left-aligned at the room's content edge, and the
     * strip's one scrolling control gives up the overshoot past the room's height.
     *
     * @param room            where the body's content starts and how far down it may run
     * @param thickness       how wide the scrolling control's bar draws; a bar wider than the padding
     *                        already holds clear widens the body by the excess
     * @param bodyControls    the body controls, top to bottom (empty for no body)
     * @param measurers       measure each label's rendered width for text snapping
     * @param rawScrollOffset the requested scroll offset for the scrolling control; clamped to its overflow
     * @return the framed body rectangle and the capped, placed controls inside it
     */
    public static BodyStrip layoutBodyStrip(
            BodyRoom room,
            ScrollbarThickness thickness,
            List<ControlSpec> bodyControls,
            StripTextMeasurers measurers,
            float rawScrollOffset) {

        var strip = measureStrip(bodyControls, measurers);
        var bodyHeight = capBodyHeight(strip, room.maxHeight());

        // Grow rightward for a bar the padding cannot swallow, which is the direction a panel already
        // grows: the list keeps its measured width and its left edge, and the frame moves out around it.
        var bodyWidth = strip.measurement().bodyWidth() + measureGutterExcess(strip, thickness);
        var bounds = new Rectangle(
            room.contentX(),
            room.contentTopY() - bodyHeight,
            bodyWidth,
            bodyHeight);

        return new BodyStrip(
            bounds,
            layoutCappedControls(bounds, strip, thickness, rawScrollOffset, measurers));
    }

    /**
     * Measures a strip and resolves which of its controls scrolls, as the one value the phases below take.
     * Pairs the measurement with the specs it was taken of and the flex index read off those same specs,
     * so no caller can hand a phase a measurement of one strip and the controls of another.
     *
     * @param specs     the strip's controls, top to bottom
     * @param measurers measure each label's rendered width for text snapping
     * @return the specs, their measurement, and the scrolling control's index
     */
    static MeasuredStrip measureStrip(List<ControlSpec> specs, StripTextMeasurers measurers) {
        return new MeasuredStrip(
            specs,
            ControlStripLayout.measureStrip(specs, measurers),
            findScrollingIndex(specs));
    }

    /**
     * The index of the strip's {@link ControlSpec.ScrollingSection}, or {@link #NO_FLEX_REGION} when the
     * strip has none. At most one section scrolls - two would each need the leftover height the other is
     * claiming - so the first is taken as the flex region; a strip with none is never capped.
     *
     * @param specs the strip's controls, top to bottom
     * @return the scrolling control's index, or {@link #NO_FLEX_REGION}
     */
    static int findScrollingIndex(List<ControlSpec> specs) {
        for (var index = 0; index < specs.size(); index++) {
            if (specs.get(index) instanceof ControlSpec.ScrollingSection) {
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
     * @param strip         the measured strip, from {@link #measureStrip}
     * @param maxBodyHeight the most the framed body may stand
     * @return the body height to frame, capped only when the strip overflows and can scroll
     */
    static float capBodyHeight(MeasuredStrip strip, float maxBodyHeight) {

        var naturalHeight = strip.measurement().bodyHeight();
        if (!strip.hasScrollingRegion() || naturalHeight <= maxBodyHeight) {
            return naturalHeight;
        }
        var overshoot = naturalHeight - maxBodyHeight;

        // Keep at least one row of the list, so a very tight cap shows a one-row scrolling list rather
        // than shrinking it to nothing.
        var maxShrink = Math.max(0f, strip.measureFlexRowHeight() - ControlStripLayout.CONTROL_ROW_HEIGHT);

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
     * @param body            the framed body rectangle, sized via {@link #capBodyHeight} and widened for
     *                        the scrollbar gutter via {@link #layoutBodyStrip}
     * @param strip           the measured strip, from {@link #measureStrip}
     * @param thickness       how wide the scrollbar draws, deciding how much of the body's right side the
     *                        flex region leaves clear for it
     * @param rawScrollOffset the requested scroll offset in pixels; clamped to the available overflow
     * @param measurers       measure each label's rendered width, for snapping a tabs row's segments
     * @return the laid-out controls, the flex viewport, the clamped offset, and the overflow
     */
    static CappedStripPlacement layoutCappedControls(
            Rectangle body,
            MeasuredStrip strip,
            ScrollbarThickness thickness,
            float rawScrollOffset,
            StripTextMeasurers measurers) {

        var specs = strip.specs();
        if (specs.isEmpty()) {
            return new CappedStripPlacement(List.of(), NO_VIEWPORT, 0f, 0f);
        }
        if (!strip.hasScrollingRegion()) {
            // Nothing scrolls, so the strip pins whole - byte-for-byte the plain stacked layout.
            var pinned = ControlStripLayout.layoutControls(
                body,
                specs,
                strip.measurement(),
                measurers);

            return new CappedStripPlacement(pinned, NO_VIEWPORT, 0f, 0f);
        }
        var flexIndex = strip.flexIndex();
        var originX = body.x() + ControlStripLayout.BODY_PADDING;
        var topInsetY = body.y() + body.height() - ControlStripLayout.BODY_PADDING;
        var bottomInsetY = body.y() + ControlStripLayout.BODY_PADDING;

        // Header: the controls above the flex row, stacked from the top inset. A capped strip only
        // shrinks the flex region, so the header lands exactly where an uncapped strip would place it.
        var header = strip.sliceRows(0, flexIndex);
        var headerRows = layoutPinnedRows(header, originX, topInsetY, body);

        // Footer: the controls below the flex row, pinned to the bottom inset as a block, so they never
        // scroll and always sit flush at the body bottom regardless of the list's length.
        var footer = strip.sliceRows(flexIndex + 1, specs.size());
        var footerTopY = bottomInsetY + ControlStripLayout.measureStackedHeight(footer.rowHeights());
        var footerRows = layoutPinnedRows(footer, originX, footerTopY, body);

        var flexViewportTopY = resolveFlexViewportTopY(headerRows, topInsetY);
        var flexViewportBottomY = resolveFlexViewportBottomY(footer, footerTopY, bottomInsetY);
        var flexViewportHeight = Math.max(0f, flexViewportTopY - flexViewportBottomY);
        var flexNatural = strip.measureFlexRowHeight();
        var flexWidth = measureFlexWidth(body, strip, thickness);
        var overflow = Math.max(0f, flexNatural - flexViewportHeight);
        var scrollOffset = clamp(rawScrollOffset, overflow);

        // The list draws at its full natural height, shifted up by the clamped scroll so the rows past
        // the viewport top slide out under the header; the host clips the draw to the viewport.
        var flexContentTopY = flexViewportTopY + scrollOffset;
        var flexBounds = new Rectangle(originX, flexContentTopY - flexNatural, flexWidth, flexNatural);
        var flexViewport = new Rectangle(originX, flexViewportBottomY, flexWidth, flexViewportHeight);

        // Assemble in strip order: the pinned header, the scrolled run, then the pinned footer - each
        // turned into controls through the shared zip so segments split identically everywhere. The
        // section itself never reaches the renderer or the input listener; what does is the run inside it,
        // laid at its scrolled position and marked so both clip against the viewport below.
        var section = (ControlSpec.ScrollingSection) specs.get(flexIndex);
        var controls = new ArrayList<Control>(specs.size());
        controls.addAll(ControlStripLayout.toControls(header.specs(), headerRows, measurers));
        controls.addAll(ControlStripLayout.layoutScrolledColumn(section.controls(), flexBounds, measurers));
        controls.addAll(ControlStripLayout.toControls(footer.specs(), footerRows, measurers));

        return new CappedStripPlacement(
            List.copyOf(controls),
            flexViewport,
            scrollOffset,
            overflow);
    }

    // The flex viewport's top edge, which is where the flex row starts: one gap below the last header row,
    // read straight off the laid-out header rather than re-summing their heights. With no header the flex
    // row heads the strip at the top inset.
    private static float resolveFlexViewportTopY(List<Rectangle> headerRows, float topInsetY) {
        if (headerRows.isEmpty()) {
            return topInsetY;
        }
        return headerRows.get(headerRows.size() - 1).y() - ControlStripLayout.ROW_GAP;
    }

    // The flex viewport's bottom edge: one gap above the footer block, or at the bottom inset when there
    // is no footer. Measured off the footer's own top rather than off the body, so the list stops where
    // the pinned block actually starts.
    private static float resolveFlexViewportBottomY(
            RowRun footer,
            float footerTopY,
            float bottomInsetY) {

        if (footer.isEmpty()) {
            return bottomInsetY;
        }
        return footerTopY + ControlStripLayout.ROW_GAP;
    }

    // One pinned run of the strip - the header above the flex region or the footer below it - laid from
    // topY with its dividers spanned to the framed body, so a rule on either side of the list reaches the
    // frame the way an uncapped strip's does. The two runs differ only in where they hang from, so they
    // are placed through one path rather than through two copies of it.
    private static List<Rectangle> layoutPinnedRows(
            RowRun run,
            float originX,
            float topY,
            Rectangle body) {

        return ControlStripLayout.spanDividerRowsToBody(
            run.specs(),
            RowStack.layoutRows(originX, topY, ControlStripLayout.ROW_GAP, run.rowHeights(), run.rowWidths()),
            body);
    }

    // How wide the flex region lays out inside the framed body. It fills the body's content width rather
    // than its own row width: as the strip's main region it flexes horizontally into the leftover width
    // the same way it flexes vertically into the leftover height, spreading its columns to the frame and
    // right-aligning each row's trailing value against the panel edge by the scrollbar. The body is as
    // wide as the widest control, so this only ever grows the list - a list already that wide is
    // unchanged.
    //
    // The right side is held clear by the padding plus the same excess layoutBodyStrip widened the body
    // by, so the two cancel and the list comes out at its measured content width at every thickness: the
    // growth is absorbed by the box, not taken out of the content.
    private static float measureFlexWidth(
            Rectangle body,
            MeasuredStrip strip,
            ScrollbarThickness thickness) {

        var reservedRight = ControlStripLayout.BODY_PADDING + measureGutterExcess(strip, thickness);

        return Math.max(
            strip.measureFlexRowWidth(),
            body.width() - ControlStripLayout.BODY_PADDING - reservedRight);
    }

    // How much wider than a plain strip a body has to stand to hold its scrollbar: the gutter the bar
    // occupies, less the padding the body already insets its controls by. Zero for a strip with nothing to
    // scroll (no bar to reserve for) and zero for any bar the padding already covers, which is why the
    // default thickness costs the body nothing.
    private static float measureGutterExcess(MeasuredStrip strip, ScrollbarThickness thickness) {
        if (!strip.hasScrollingRegion()) {
            return 0f;
        }
        return Math.max(0f, thickness.computeGutterWidth() - ControlStripLayout.BODY_PADDING);
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
     * A measured control strip: its controls, the measurement taken of them, and which one scrolls. The
     * three travel together because they are one reading of one strip - a measurement taken of other
     * specs, or an index found in them, describes a strip that is not being laid out. Holding them apart
     * also put two same-typed row lists side by side in every signature that carried them, with nothing
     * to catch a caller passing the widths where the heights go.
     *
     * @param specs       the strip's controls, top to bottom
     * @param measurement the rows measured for those controls
     * @param flexIndex   the scrolling control's index, or {@link #NO_FLEX_REGION} when none scrolls
     */
    record MeasuredStrip(
        List<ControlSpec> specs,
        StripMeasurement measurement,
        int flexIndex) {

        /**
         * @return whether one of the controls scrolls, and so whether the strip caps, reserves a
         *         scrollbar gutter, and has a flex region to read at all
         */
        boolean hasScrollingRegion() {
            return flexIndex != NO_FLEX_REGION;
        }

        /** @return the measured heights of the strip's rows, top to bottom */
        List<Float> rowHeights() {
            return measurement.rowHeights();
        }

        /** @return the measured widths of the strip's rows, top to bottom */
        List<Float> rowWidths() {
            return measurement.rowWidths();
        }

        /** @return the flex region's natural height - every row it holds, before any of it is given up */
        float measureFlexRowHeight() {
            return measurement.rowHeights().get(flexIndex);
        }

        /** @return the flex region's own measured width, before it flexes into the body's content width */
        float measureFlexRowWidth() {
            return measurement.rowWidths().get(flexIndex);
        }

        /**
         * One run of the strip, sliced at the same bounds across all three lists so the specs and their
         * two measurements cannot come apart.
         *
         * @param fromIndex the first control in the run
         * @param toIndex   one past the last control in the run
         * @return the run's controls and their measured rows
         */
        RowRun sliceRows(int fromIndex, int toIndex) {
            return new RowRun(
                specs.subList(fromIndex, toIndex),
                measurement.rowHeights().subList(fromIndex, toIndex),
                measurement.rowWidths().subList(fromIndex, toIndex));
        }
    }

    /**
     * A contiguous run of a strip's controls with the rows measured for them - the header pinned above the
     * flex region, or the footer pinned below it.
     *
     * @param specs      the run's controls, top to bottom
     * @param rowHeights the measured heights of those controls' rows
     * @param rowWidths  the measured widths of those controls' rows
     */
    record RowRun(
        List<ControlSpec> specs,
        List<Float> rowHeights,
        List<Float> rowWidths) {

        /** @return whether the run holds no controls, so nothing is pinned on that side of the list */
        boolean isEmpty() {
            return specs.isEmpty();
        }
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
