package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.VerticalTableSpecs;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.layout.ControlStripLayout.StripMeasurement;
import kmlib.testfixtures.starsector.ui.font.LineWidthMeasurerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the capped strip: {@link CappedStripLayout#capBodyHeight} shrinks the body only by what the flex
 * list can give up (never below one row), and {@link CappedStripLayout#layoutCappedControls} pins the
 * header from the top, pins the footer to the bottom, and scrolls the flex list between them. Framing
 * hangs the body from a FIXED top edge (as the tab panel frames it from the tab-row bottom), so a shorter
 * capped body rises from the bottom while the header stays put - the property the assertions lean on.
 */
final class CappedStripLayoutTest {
    private static final float WIDTH_PER_CHAR = 10f;
    private static final float TOLERANCE = 0.01f;
    private static final float BODY_LEFT_X = 100f;
    // The body's top edge, fixed across caps: the tab panel hangs the body from the tab-row bottom, so a
    // shorter capped body rises from the bottom rather than dropping its top.
    private static final float BODY_TOP_Y = 400f;
    private static final int FLEX_OPTION_COUNT = 4;

    private final LineWidthMeasurer measurerFake = new LineWidthMeasurerFake(WIDTH_PER_CHAR);

    // A header checkbox, a four-option scrolling list, and a footer checkbox - the shape the political
    // map's picker takes under the alliances view (a recede control pins below the list). Row heights
    // come out [20, 80, 20]: one control row for each checkbox, four for the list.
    private List<ControlSpec> headerFlexFooterStrip() {
        return List.of(ControlSpec.Checkbox.lit("H", false, ControlAction.NONE),
                scrollingList(FLEX_OPTION_COUNT), ControlSpec.Checkbox.lit("F", false,
                        ControlAction.NONE));
    }

    private static ControlSpec scrollingList(int optionCount) {
        var labels = new ArrayList<String>();
        var icons = new ArrayList<String>();
        for (var index = 0; index < optionCount; index++) {
            labels.add("Opt" + index);
            icons.add(null);
        }
        return VerticalTableSpecs.buildIconList(labels, icons, ControlSpec.NO_SELECTION,
                ControlAction.NONE).asScrolling();
    }

    @Nested
    class FindScrollingIndex {

        @Test
        void findScrollingIndexReturnsTheMarkedControlsIndex() {
            var index = CappedStripLayout.findScrollingIndex(headerFlexFooterStrip());
            assertThat(index).isEqualTo(1);
        }

        @Test
        void findScrollingIndexReturnsNoFlexRegionWhenNoneScroll() {
            var specs = List.<ControlSpec>of(ControlSpec.Checkbox.lit("A", false, ControlAction.NONE),
                    ControlSpec.Checkbox.lit("B", false, ControlAction.NONE));
            assertThat(CappedStripLayout.findScrollingIndex(specs))
                    .isEqualTo(CappedStripLayout.NO_FLEX_REGION);
        }
    }

    @Nested
    class CapBodyHeight {

        @Test
        void capBodyHeightKeepsTheNaturalHeightWhenTheStripFits() {
            var strip = measure(headerFlexFooterStrip());
            // A cap well above the natural height leaves it untouched, so the strip draws at full size.
            assertThat(CappedStripLayout.capBodyHeight(strip, 1, strip.bodyHeight() + 100f))
                    .isCloseTo(strip.bodyHeight(), within(TOLERANCE));
        }

        @Test
        void capBodyHeightKeepsTheNaturalHeightWhenNoControlScrolls() {
            var specs = List.<ControlSpec>of(ControlSpec.Checkbox.lit("A", false, ControlAction.NONE),
                    ControlSpec.Checkbox.lit("B", false, ControlAction.NONE));
            var strip = measure(specs);
            // With no flex region there is nothing to shrink, so a tight cap cannot reduce the body -
            // the strip keeps its natural height (and would overflow the box, as it does today).
            assertThat(CappedStripLayout.capBodyHeight(strip, CappedStripLayout.NO_FLEX_REGION, 10f))
                    .isCloseTo(strip.bodyHeight(), within(TOLERANCE));
        }

        @Test
        void capBodyHeightShrinksTheBodyToTheCapByTakingItFromTheList() {
            var strip = measure(headerFlexFooterStrip());
            // The overshoot comes entirely off the flex list (header and footer keep their height), so
            // the capped body is exactly the requested cap while the list still holds more than one row.
            var cap = strip.bodyHeight() - 40f;
            assertThat(CappedStripLayout.capBodyHeight(strip, 1, cap)).isCloseTo(cap, within(TOLERANCE));
        }

        @Test
        void capBodyHeightFloorsTheShrinkAtOneListRow() {
            var strip = measure(headerFlexFooterStrip());
            // A cap tighter than "header + footer + one row" cannot shrink the list to nothing: the body
            // floors at that minimum (16 inset + 8 gaps + 20 header + 20 footer + 20 one row = 84) rather
            // than collapsing the list.
            var flooredHeight = 2f * ControlStripLayout.BODY_PADDING + 2f * ControlStripLayout.ROW_GAP
                    + 3f * ControlStripLayout.CONTROL_ROW_HEIGHT;
            assertThat(CappedStripLayout.capBodyHeight(strip, 1, 10f))
                    .isCloseTo(flooredHeight, within(TOLERANCE));
        }
    }

    @Nested
    class LayoutCappedControls {

        @Test
        void layoutCappedControlsMatchesThePlainStackWhenNothingScrolls() {
            var specs = List.<ControlSpec>of(ControlSpec.Checkbox.lit("A", false, ControlAction.NONE),
                    ControlSpec.Checkbox.lit("B", false, ControlAction.NONE));
            var strip = measure(specs);
            var body = frameBody(strip.bodyHeight(), strip.bodyWidth());
            var capped = CappedStripLayout.layoutCappedControls(body, specs, strip.rowHeights(),
                    strip.rowWidths(), CappedStripLayout.NO_FLEX_REGION, 0f, measurerFake);
            var plain = ControlStripLayout.layoutControls(body, specs, strip.rowHeights(),
                    strip.rowWidths(), measurerFake);
            // A strip with no flex region pins whole, so the capped placement is the plain stack with no
            // viewport and no overflow.
            assertThat(capped.controls()).hasSameSizeAs(plain);
            for (var index = 0; index < plain.size(); index++) {
                assertThat(capped.controls().get(index).bounds())
                        .isEqualTo(plain.get(index).bounds());
            }
            assertThat(capped.scrollOverflow()).isZero();
            assertThat(capped.isScrollbarNeeded()).isFalse();
        }

        @Test
        void layoutCappedControlsLeavesNoOverflowWhenTheBodyIsNaturalHeight() {
            var specs = headerFlexFooterStrip();
            var strip = measure(specs);
            var body = frameBody(strip.bodyHeight(), strip.bodyWidth());
            var capped = CappedStripLayout.layoutCappedControls(body, specs, strip.rowHeights(),
                    strip.rowWidths(), 1, 0f, measurerFake);
            // At full height the list fills its viewport exactly - no overflow, no scrollbar, and the
            // viewport is as tall as the list's natural rows.
            assertThat(capped.scrollOverflow()).isCloseTo(0f, within(TOLERANCE));
            assertThat(capped.isScrollbarNeeded()).isFalse();
            assertThat(capped.flexViewport().height())
                    .isCloseTo(strip.rowHeights().get(1), within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsPinsTheHeaderInPlaceWhenTheBodyIsCapped() {
            var specs = headerFlexFooterStrip();
            var strip = measure(specs);
            var naturalHeader = firstControlBounds(specs, strip, strip.bodyHeight());
            var cappedHeader = firstControlBounds(specs, strip, strip.bodyHeight() - 40f);
            // The header hangs from the fixed top edge, so shrinking the body (which rises from the
            // bottom) leaves the header exactly where it was - only the list gives up room.
            assertThat(cappedHeader.y()).isCloseTo(naturalHeader.y(), within(TOLERANCE));
            assertThat(cappedHeader.height()).isCloseTo(naturalHeader.height(), within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsPinsTheFooterToTheBodyBottom() {
            var specs = headerFlexFooterStrip();
            var strip = measure(specs);
            var cappedHeight = strip.bodyHeight() - 40f;
            var body = frameBody(cappedHeight, strip.bodyWidth());
            var capped = CappedStripLayout.layoutCappedControls(body, specs, strip.rowHeights(),
                    strip.rowWidths(), 1, 0f, measurerFake);
            // The footer (last control) sits one inset above the body's bottom edge, flush at the bottom
            // rather than trailing the scrolled list.
            var footer = capped.controls().get(capped.controls().size() - 1).bounds();
            assertThat(footer.y()).isCloseTo(body.y() + ControlStripLayout.BODY_PADDING,
                    within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsReportsTheOverflowTakenFromTheList() {
            var specs = headerFlexFooterStrip();
            var strip = measure(specs);
            var body = frameBody(strip.bodyHeight() - 40f, strip.bodyWidth());
            var capped = CappedStripLayout.layoutCappedControls(body, specs, strip.rowHeights(),
                    strip.rowWidths(), 1, 0f, measurerFake);
            // The 40px the body lost all came off the list, so the list overruns its viewport by exactly
            // that much and a scrollbar is due.
            assertThat(capped.scrollOverflow()).isCloseTo(40f, within(TOLERANCE));
            assertThat(capped.isScrollbarNeeded()).isTrue();
        }

        @Test
        void layoutCappedControlsClampsAScrollPastTheBottomToTheOverflow() {
            var specs = headerFlexFooterStrip();
            var strip = measure(specs);
            var body = frameBody(strip.bodyHeight() - 40f, strip.bodyWidth());
            // A request far past the last row settles at the overflow, so the list stops with its bottom
            // row flush against the viewport bottom rather than scrolling into blank space.
            var capped = CappedStripLayout.layoutCappedControls(body, specs, strip.rowHeights(),
                    strip.rowWidths(), 1, 9999f, measurerFake);
            assertThat(capped.scrollOffset()).isCloseTo(40f, within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsClampsANegativeScrollToTheTop() {
            var specs = headerFlexFooterStrip();
            var strip = measure(specs);
            var body = frameBody(strip.bodyHeight() - 40f, strip.bodyWidth());
            var capped = CappedStripLayout.layoutCappedControls(body, specs, strip.rowHeights(),
                    strip.rowWidths(), 1, -50f, measurerFake);
            assertThat(capped.scrollOffset()).isZero();
        }

        @Test
        void layoutCappedControlsShiftsTheListUpAsItScrolls() {
            var specs = headerFlexFooterStrip();
            var strip = measure(specs);
            var body = frameBody(strip.bodyHeight() - 40f, strip.bodyWidth());
            var atTop = flexBounds(CappedStripLayout.layoutCappedControls(body, specs,
                    strip.rowHeights(), strip.rowWidths(), 1, 0f, measurerFake));
            var scrolled = flexBounds(CappedStripLayout.layoutCappedControls(body, specs,
                    strip.rowHeights(), strip.rowWidths(), 1, 40f, measurerFake));
            // Scrolling down slides the list's full-height content upward (UI y grows up) by the offset,
            // so the lower rows come into the viewport while the content keeps its natural height.
            assertThat(scrolled.y()).isCloseTo(atTop.y() + 40f, within(TOLERANCE));
            assertThat(scrolled.height()).isCloseTo(atTop.height(), within(TOLERANCE));
            assertThat(scrolled.height()).isCloseTo(strip.rowHeights().get(1), within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsAlignsTheBottomRowToTheViewportWhenFullyScrolled() {
            var specs = headerFlexFooterStrip();
            var strip = measure(specs);
            var body = frameBody(strip.bodyHeight() - 40f, strip.bodyWidth());
            var capped = CappedStripLayout.layoutCappedControls(body, specs, strip.rowHeights(),
                    strip.rowWidths(), 1, 40f, measurerFake);
            // Fully scrolled, the list's bottom edge meets the viewport's bottom edge, so the last row is
            // the one flush at the bottom of the scroll region.
            var list = flexBounds(capped);
            assertThat(list.y()).isCloseTo(capped.flexViewport().y(), within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsFlattensAPinnedHeaderSideBySideIntoItsChildren() {
            // A side-by-side group heads the block above the scrolling list - the political map's picker
            // shape, where a selector sits beside a related block over the list. The pinned header expands
            // the group into its two children (the left control, then the right), each side by side above
            // the flex list, so the capped path flattens a group as the plain stack does.
            var left = ControlSpec.Checkbox.lit("L", false, ControlAction.NONE);
            var right = ControlSpec.Checkbox.lit("RR", false, ControlAction.NONE);
            var specs = List.<ControlSpec>of(new ControlSpec.SideBySide(List.of(left), List.of(right)),
                    scrollingList(FLEX_OPTION_COUNT));
            var strip = measure(specs);
            var body = frameBody(strip.bodyHeight(), strip.bodyWidth());
            var capped = CappedStripLayout.layoutCappedControls(body, specs, strip.rowHeights(),
                    strip.rowWidths(), 1, 0f, measurerFake);

            // The group's two children pin ahead of the list, side by side (the right one to the right of
            // the left), and the scrolling list follows.
            assertThat(capped.controls()).hasSize(3);
            assertThat(capped.controls().get(0).spec()).isEqualTo(left);
            assertThat(capped.controls().get(1).spec()).isEqualTo(right);
            assertThat(capped.controls().get(1).bounds().x())
                    .isGreaterThan(capped.controls().get(0).bounds().x());
            assertThat(capped.controls().get(2).spec()).isInstanceOf(ControlSpec.VerticalTable.class);
        }

        @Test
        void layoutCappedControlsFillsTheFlexListToTheBodyContentWidth() {
            // A header wider than the list makes the body wider than the list's own rows. The flex list,
            // as the strip's main region, spreads to the body's content width rather than leaving a
            // gutter of dead space between it and the scrollbar pinned at the body's right edge.
            var wideHeader = ControlSpec.Checkbox.lit("HeaderWiderThanTheList", false,
                    ControlAction.NONE);
            var specs = List.<ControlSpec>of(wideHeader, scrollingList(FLEX_OPTION_COUNT));
            var strip = measure(specs);
            var body = frameBody(strip.bodyHeight(), strip.bodyWidth());
            var capped = CappedStripLayout.layoutCappedControls(body, specs, strip.rowHeights(),
                    strip.rowWidths(), 1, 0f, measurerFake);
            var list = flexBounds(capped);
            var contentWidth = body.width() - 2f * ControlStripLayout.BODY_PADDING;
            // The list and its viewport both fill the content width, and the fill only ever grows the
            // list - it is wider than its own measured row width, never shrunk below it.
            assertThat(list.width()).isCloseTo(contentWidth, within(TOLERANCE));
            assertThat(list.width()).isGreaterThan(strip.rowWidths().get(1));
            assertThat(capped.flexViewport().width()).isCloseTo(contentWidth, within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsSpansAPinnedHeaderDividerAcrossTheFullBody() {
            // A rule heads the block above the scrolling list - the political map's picker shape. The
            // pinned header divider spans the whole framed body (edge to edge inside the border inset),
            // not the padded content column, so the capped path spans dividers as the plain stack does.
            var specs = List.<ControlSpec>of(new ControlSpec.Divider(), scrollingList(FLEX_OPTION_COUNT));
            var strip = measure(specs);
            var body = frameBody(strip.bodyHeight(), strip.bodyWidth());
            var capped = CappedStripLayout.layoutCappedControls(body, specs, strip.rowHeights(),
                    strip.rowWidths(), 1, 0f, measurerFake);
            var divider = capped.controls().get(0).bounds();
            assertThat(divider.x()).isCloseTo(body.x(), within(TOLERANCE));
            assertThat(divider.width()).isCloseTo(body.width(), within(TOLERANCE));
        }
    }

    // The flex list's laid-out bounds - the second control, between the header and the footer.
    private static Rectangle flexBounds(CappedStripLayout.CappedStripPlacement placement) {
        return placement.controls().get(1).bounds();
    }

    // The first control's bounds after laying the strip out in a body of the given height, for comparing
    // the header's position across a natural and a capped body.
    private Rectangle firstControlBounds(List<ControlSpec> specs, StripMeasurement strip, float height) {
        var body = frameBody(height, strip.bodyWidth());
        return CappedStripLayout.layoutCappedControls(body, specs, strip.rowHeights(),
                strip.rowWidths(), 1, 0f, measurerFake).controls().get(0).bounds();
    }

    private StripMeasurement measure(List<ControlSpec> specs) {
        return ControlStripLayout.measureStrip(specs, measurerFake);
    }

    // Frames a body of the given height hanging from the fixed top edge, so a shorter body rises from the
    // bottom the way the tab panel frames it beneath the tab row.
    private static Rectangle frameBody(float height, float width) {
        return new Rectangle(BODY_LEFT_X, BODY_TOP_Y - height, width, height);
    }
}
