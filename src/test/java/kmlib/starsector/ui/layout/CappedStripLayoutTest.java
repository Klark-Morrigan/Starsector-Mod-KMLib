package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.LabelledControlSpecs;
import kmlib.starsector.ui.controls.VerticalTableSpecs;
import kmlib.starsector.ui.controls.specs.ControlAction;
import kmlib.starsector.ui.controls.specs.ControlSpec;
import kmlib.starsector.ui.controls.specs.DividerSpec;
import kmlib.starsector.ui.controls.specs.ScrollingSectionSpec;
import kmlib.starsector.ui.controls.specs.SideBySideSpec;
import kmlib.starsector.ui.controls.specs.VerticalTableSpec;
import kmlib.starsector.ui.font.StripTextMeasurers;
import kmlib.starsector.ui.layout.CappedStripLayout.CappedStripPlacement;
import kmlib.starsector.ui.layout.CappedStripLayout.MeasuredStrip;
import kmlib.starsector.ui.widgets.scroll.ScrollbarThickness;
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
 *
 * <p>{@link CappedStripLayout#layoutBodyStrip} adds the across dimension: the body widens for a scrollbar
 * too fat for the padding to hold clear, and nothing inside it moves when it does.
 */
final class CappedStripLayoutTest {

    private static final float WIDTH_PER_CHAR = 10f;
    private static final float TOLERANCE = 0.01f;
    private static final float BODY_LEFT_X = 100f;

    // The body's top edge, fixed across caps: the tab panel hangs the body from the tab-row bottom, so a
    // shorter capped body rises from the bottom rather than dropping its top.
    private static final float BODY_TOP_Y = 400f;
    private static final int FLEX_OPTION_COUNT = 4;

    // A cap far above any strip these cases build, so a body-width assertion is never also measuring a
    // height cap.
    private static final float UNCAPPED_BODY_HEIGHT = 1000f;

    // A bar the body padding still swallows whole (2 + 3 margin + 2 clearance = 7, under the padding's 8)
    // and one it cannot (12 + 3 + 2 = 17, nine past it).
    private static final ScrollbarThickness SWALLOWED_BAR = new ScrollbarThickness(2f);
    private static final ScrollbarThickness OVERSIZE_BAR = new ScrollbarThickness(12f);
    private static final float OVERSIZE_BAR_EXCESS = 9f;

    // Both faces measure alike here, so every expectation below stays a plain character count; the
    // faces being told apart is pinned where that is the point under test.
    private final StripTextMeasurers measurersFake = new StripTextMeasurers(
        new LineWidthMeasurerFake(WIDTH_PER_CHAR),
        new LineWidthMeasurerFake(WIDTH_PER_CHAR));

    // A header checkbox, a four-option scrolling list, and a footer checkbox - the shape the political
    // map's picker takes under the alliances view (a recede control pins below the list). Row heights
    // come out [20, 80, 20]: one control row for each checkbox, four for the list.
    private static List<ControlSpec> buildHeaderFlexFooterStrip() {
        return List.of(
            LabelledControlSpecs.buildCheckbox("H", false, ControlAction.NONE),
            buildScrollingSection(FLEX_OPTION_COUNT),
            LabelledControlSpecs.buildCheckbox("F", false, ControlAction.NONE));
    }

    // Two checkboxes and nothing that scrolls, so the strip is never capped and reserves no bar gutter.
    private static List<ControlSpec> buildPinnedOnlyStrip() {
        return List.of(
            LabelledControlSpecs.buildCheckbox("A", false, ControlAction.NONE),
            LabelledControlSpecs.buildCheckbox("B", false, ControlAction.NONE));
    }

    private static ControlSpec buildScrollingSection(int optionCount) {
        return new ScrollingSectionSpec(List.of(buildIconList(optionCount)));
    }

    // The list itself, for a case that puts it in a section beside something else.
    private static ControlSpec buildIconList(int optionCount) {

        var labels = new ArrayList<String>();
        var icons = new ArrayList<String>();

        for (var index = 0; index < optionCount; index++) {

            labels.add("Opt" + index);
            icons.add(null);
        }
        return VerticalTableSpecs.buildIconList(
            labels,
            icons,
            ControlSpec.NO_SELECTION,
            ControlAction.NONE);
    }

    @Nested
    class FindScrollingIndex {

        @Test
        void findScrollingIndexReturnsTheMarkedControlsIndex() {

            var index = CappedStripLayout.findScrollingIndex(buildHeaderFlexFooterStrip());

            assertThat(index)
                .isEqualTo(1);
        }

        @Test
        void findScrollingIndexTakesTheFirstOfTwoSections() {
            // Two sections would each need the leftover height the other is claiming, so the first is the
            // flex region and the second lays out as an ordinary pinned run. Stated by the doc and pinned
            // here, since nothing stops a host writing two.
            var strip = List.of(
                buildScrollingSection(FLEX_OPTION_COUNT),
                buildScrollingSection(FLEX_OPTION_COUNT));

            assertThat(CappedStripLayout.findScrollingIndex(strip))
                .isZero();
        }

        @Test
        void findScrollingIndexFindsASectionThatIsTheWholeStrip() {
            // A body that is nothing but its scrolling run: no header to hang the viewport below and no
            // footer to stop it above, which is the case the viewport edges fall back to the insets for.
            assertThat(CappedStripLayout.findScrollingIndex(
                    List.of(buildScrollingSection(FLEX_OPTION_COUNT))))
                .isZero();
        }

        @Test
        void findScrollingIndexReturnsNoFlexRegionWhenNoneScroll() {

            assertThat(CappedStripLayout.findScrollingIndex(buildPinnedOnlyStrip()))
                .isEqualTo(CappedStripLayout.NO_FLEX_REGION);
        }
    }

    @Nested
    class MeasureStrip {

        @Test
        void measureStripPairsTheMeasurementWithTheStripItWasTakenOf() {

            var specs = buildHeaderFlexFooterStrip();
            var strip = CappedStripLayout.measureStrip(specs, measurersFake);

            // One reading of one strip: the specs it holds are the ones measured, and there is a row
            // measured for each of them, so no phase downstream can be handed a mismatched pair.
            assertThat(strip.specs())
                .isEqualTo(specs);
            assertThat(strip.rowHeights())
                .hasSize(specs.size());
            assertThat(strip.rowWidths())
                .hasSize(specs.size());
        }

        @Test
        void measureStripReadsTheScrollingRegionOffTheSameSpecs() {

            var strip = CappedStripLayout.measureStrip(buildHeaderFlexFooterStrip(), measurersFake);

            assertThat(strip.flexIndex())
                .isEqualTo(1);
            assertThat(strip.hasScrollingRegion())
                .isTrue();
        }

        @Test
        void measureStripReportsNoScrollingRegionWhenNoneScroll() {

            var strip = CappedStripLayout.measureStrip(buildPinnedOnlyStrip(), measurersFake);

            assertThat(strip.hasScrollingRegion())
                .isFalse();
        }
    }

    @Nested
    class CapBodyHeight {

        @Test
        void capBodyHeightKeepsTheNaturalHeightWhenTheStripFits() {

            var strip = measure(buildHeaderFlexFooterStrip());
            var natural = readNaturalHeight(strip);

            // A cap well above the natural height leaves it untouched, so the strip draws at full size.
            assertThat(CappedStripLayout.capBodyHeight(strip, natural + 100f))
                .isCloseTo(natural, within(TOLERANCE));
        }

        @Test
        void capBodyHeightKeepsTheNaturalHeightWhenNoControlScrolls() {

            var strip = measure(buildPinnedOnlyStrip());

            // With no flex region there is nothing to shrink, so a tight cap cannot reduce the body -
            // the strip keeps its natural height (and would overflow the box, as it does today).
            assertThat(CappedStripLayout.capBodyHeight(strip, 10f))
                .isCloseTo(readNaturalHeight(strip), within(TOLERANCE));
        }

        @Test
        void capBodyHeightShrinksTheBodyToTheCapByTakingItFromTheList() {

            var strip = measure(buildHeaderFlexFooterStrip());

            // The overshoot comes entirely off the flex list (header and footer keep their height), so
            // the capped body is exactly the requested cap while the list still holds more than one row.
            var cap = readNaturalHeight(strip) - 40f;

            assertThat(CappedStripLayout.capBodyHeight(strip, cap))
                .isCloseTo(cap, within(TOLERANCE));
        }

        @Test
        void capBodyHeightFloorsTheShrinkAtOneListRow() {

            var strip = measure(buildHeaderFlexFooterStrip());

            // A cap tighter than "header + footer + one row" cannot shrink the list to nothing: the body
            // floors at that minimum (16 inset + 8 gaps + 20 header + 20 footer + 20 one row = 84) rather
            // than collapsing the list.
            assertThat(CappedStripLayout.capBodyHeight(strip, 10f))
                .isCloseTo(84f, within(TOLERANCE));
        }
    }

    @Nested
    class LayoutCappedControls {

        @Test
        void layoutCappedControlsMatchesThePlainStackWhenNothingScrolls() {

            var strip = measure(buildPinnedOnlyStrip());
            var body = buildFrameBody(readNaturalHeight(strip), readNaturalWidth(strip));
            var capped = layoutControlsIn(body, strip, 0f);

            var plain = ControlStripLayout.layoutControls(
                body,
                strip.specs(),
                strip.measurement(),
                measurersFake);

            // A strip with no flex region pins whole, so the capped placement is the plain stack with no
            // viewport and no overflow.
            assertThat(capped.controls())
                .hasSameSizeAs(plain);

            for (var index = 0; index < plain.size(); index++) {
                assertThat(capped.controls().get(index).bounds())
                    .isEqualTo(plain.get(index).bounds());
            }

            assertThat(capped.scrollOverflow())
                .isZero();
        }

        @Test
        void layoutCappedControlsLeavesNoOverflowWhenTheBodyIsNaturalHeight() {

            var strip = measure(buildHeaderFlexFooterStrip());
            var capped = layoutControlsIn(buildBodyFor(strip, 0f), strip, 0f);

            // At full height the list fills its viewport exactly - no overflow, so nothing to scroll and
            // no bar due, and the viewport is as tall as the list's natural rows.
            assertThat(capped.scrollOverflow())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(capped.flexViewport().height())
                .isCloseTo(strip.measureFlexRowHeight(), within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsPinsTheHeaderInPlaceWhenTheBodyIsCapped() {

            var strip = measure(buildHeaderFlexFooterStrip());
            var naturalHeader = readFirstControlBounds(strip, 0f);
            var cappedHeader = readFirstControlBounds(strip, 40f);

            // The header hangs from the fixed top edge, so shrinking the body (which rises from the
            // bottom) leaves the header exactly where it was - only the list gives up room.
            assertThat(cappedHeader.y())
                .isCloseTo(naturalHeader.y(), within(TOLERANCE));
            assertThat(cappedHeader.height())
                .isCloseTo(naturalHeader.height(), within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsPinsTheFooterToTheBodyBottom() {

            var strip = measure(buildHeaderFlexFooterStrip());
            var body = buildBodyFor(strip, 40f);
            var capped = layoutControlsIn(body, strip, 0f);

            // The footer (last control) sits one inset above the body's bottom edge, flush at the bottom
            // rather than trailing the scrolled list.
            var footer = capped.controls().get(capped.controls().size() - 1).bounds();

            assertThat(footer.y())
                .isCloseTo(
                    body.y() + ControlStripLayout.BODY_PADDING,
                    within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsReportsTheOverflowTakenFromTheList() {

            var strip = measure(buildHeaderFlexFooterStrip());
            var capped = layoutControlsIn(buildBodyFor(strip, 40f), strip, 0f);

            // The 40px the body lost all came off the list, so the list overruns its viewport by exactly
            // that much and a scrollbar is due.
            assertThat(capped.scrollOverflow())
                .isCloseTo(40f, within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsClampsAScrollPastTheBottomToTheOverflow() {

            var strip = measure(buildHeaderFlexFooterStrip());

            // A request far past the last row settles at the overflow, so the list stops with its bottom
            // row flush against the viewport bottom rather than scrolling into blank space.
            var capped = layoutControlsIn(buildBodyFor(strip, 40f), strip, 9999f);

            assertThat(capped.scrollOffset())
                .isCloseTo(40f, within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsClampsANegativeScrollToTheTop() {

            var strip = measure(buildHeaderFlexFooterStrip());
            var capped = layoutControlsIn(buildBodyFor(strip, 40f), strip, -50f);

            assertThat(capped.scrollOffset())
                .isZero();
        }

        @Test
        void layoutCappedControlsShiftsTheListUpAsItScrolls() {

            var strip = measure(buildHeaderFlexFooterStrip());
            var body = buildBodyFor(strip, 40f);

            var atTop = readFlexBounds(layoutControlsIn(body, strip, 0f));
            var scrolled = readFlexBounds(layoutControlsIn(body, strip, 40f));

            // Scrolling down slides the list's full-height content upward (UI y grows up) by the offset,
            // so the lower rows come into the viewport while the content keeps its natural height.
            assertThat(scrolled.y())
                .isCloseTo(atTop.y() + 40f, within(TOLERANCE));
            assertThat(scrolled.height())
                .isCloseTo(atTop.height(), within(TOLERANCE));
            assertThat(scrolled.height())
                .isCloseTo(strip.measureFlexRowHeight(), within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsAlignsTheBottomRowToTheViewportWhenFullyScrolled() {

            var strip = measure(buildHeaderFlexFooterStrip());
            var capped = layoutControlsIn(buildBodyFor(strip, 40f), strip, 40f);

            // Fully scrolled, the list's bottom edge meets the viewport's bottom edge, so the last row is
            // the one flush at the bottom of the scroll region.
            assertThat(readFlexBounds(capped).y())
                .isCloseTo(capped.flexViewport().y(), within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsFlattensAPinnedHeaderSideBySideIntoItsChildren() {
            // A side-by-side group heads the block above the scrolling list - the political map's picker
            // shape, where a selector sits beside a related block over the list. The pinned header expands
            // the group into its two children (the left control, then the right), each side by side above
            // the flex list, so the capped path flattens a group as the plain stack does.
            var left = LabelledControlSpecs.buildCheckbox("L", false, ControlAction.NONE);
            var right = LabelledControlSpecs.buildCheckbox("RR", false, ControlAction.NONE);
            var strip = measure(List.of(
                new SideBySideSpec(List.of(left), List.of(right)),
                buildScrollingSection(FLEX_OPTION_COUNT)));

            var capped = layoutControlsIn(buildBodyFor(strip, 0f), strip, 0f);

            // The group's two children pin ahead of the list, side by side (the right one to the right of
            // the left), and the scrolling list follows.
            assertThat(capped.controls())
                .hasSize(3);
            assertThat(capped.controls().get(0).spec())
                .isEqualTo(left);
            assertThat(capped.controls().get(1).spec())
                .isEqualTo(right);
            assertThat(capped.controls().get(1).bounds().x())
                .isGreaterThan(capped.controls().get(0).bounds().x());
            assertThat(capped.controls().get(2).spec())
                .isInstanceOf(VerticalTableSpec.class);
        }

        @Test
        void layoutCappedControlsPlacesAnEmptySectionWithoutPlacingAnything() {
            // A host may state a section whose run is empty for a frame - a picker whose items have not
            // arrived yet. It contributes no controls and leaves the pinned rows where they were, rather
            // than dividing by a zero row count on the way.
            var header = LabelledControlSpecs.buildCheckbox("H", false, ControlAction.NONE);
            var strip = measure(List.of(header, new ScrollingSectionSpec(List.of())));

            var capped = layoutControlsIn(buildBodyFor(strip, 0f), strip, 0f);

            assertThat(capped.controls())
                .hasSize(1);
            assertThat(capped.controls().get(0).spec())
                .isEqualTo(header);
            assertThat(capped.scrollOverflow())
                .isZero();
        }

        @Test
        void layoutCappedControlsScrollsEveryControlInTheSectionTogether() {
            // The run is what a section buys over a flag on one control: a heading and the list under it
            // travel as a block, so both are laid inside the viewport and both are marked scrolled - the
            // one value the clipping renderer and the viewport-limited hit-test read.
            var heading = LabelledControlSpecs.buildLabel("Blocs");
            var strip = measure(List.of(
                LabelledControlSpecs.buildCheckbox("H", false, ControlAction.NONE),
                new ScrollingSectionSpec(List.of(
                    heading,
                    buildIconList(FLEX_OPTION_COUNT)))));

            var capped = layoutControlsIn(buildBodyFor(strip, 0f), strip, 0f);

            assertThat(capped.controls())
                .hasSize(3);
            assertThat(capped.controls().get(0).isScrolled())
                .as("the pinned header is not scrolled")
                .isFalse();
            assertThat(capped.controls().get(1).spec())
                .isEqualTo(heading);
            assertThat(capped.controls())
                .filteredOn(Control::isScrolled)
                .hasSize(2);

            // Stacked inside the section in the order stated, the list hanging under the heading.
            assertThat(capped.controls().get(2).bounds().y())
                .isLessThan(capped.controls().get(1).bounds().y());
        }

        @Test
        void layoutCappedControlsShiftsTheWholeSectionByTheScrollOffset() {
            // Every control in the run moves by the same offset, so a heading scrolls away with the rows
            // it heads rather than staying put while they slide under it.
            var strip = measure(List.of(
                LabelledControlSpecs.buildCheckbox("H", false, ControlAction.NONE),
                new ScrollingSectionSpec(List.of(
                    LabelledControlSpecs.buildLabel("Blocs"),
                    buildIconList(FLEX_OPTION_COUNT)))));

            // Framed 40px under its natural height, so the section overruns its viewport and has room
            // to scroll; at natural height the offset would clamp to nothing and prove neither control
            // moved because none could.
            var body = buildBodyFor(strip, 40f);
            var unscrolled = layoutControlsIn(body, strip, 0f);
            var scrolled = layoutControlsIn(body, strip, 12f);
            var offset = scrolled.scrollOffset();

            assertThat(offset)
                .isGreaterThan(0f);
            assertThat(scrolled.controls().get(1).bounds().y() - unscrolled.controls().get(1).bounds().y())
                .isCloseTo(offset, within(TOLERANCE));
            assertThat(scrolled.controls().get(2).bounds().y() - unscrolled.controls().get(2).bounds().y())
                .isCloseTo(offset, within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsFillsTheFlexListToTheBodyContentWidth() {
            // A header wider than the list makes the body wider than the list's own rows. The flex list,
            // as the strip's main region, spreads to the body's content width rather than leaving a strip
            // of dead space between it and the scrollbar pinned at the body's right edge.
            var wideHeader = LabelledControlSpecs.buildCheckbox(
                "HeaderWiderThanTheList",
                false,
                ControlAction.NONE);

            var strip = measure(List.of(wideHeader, buildScrollingSection(FLEX_OPTION_COUNT)));
            var body = buildBodyFor(strip, 0f);
            var capped = layoutControlsIn(body, strip, 0f);

            var list = readFlexBounds(capped);
            var contentWidth = body.width() - 2f * ControlStripLayout.BODY_PADDING;

            // The list and its viewport both fill the content width, and the fill only ever grows the
            // list - it is wider than its own measured row width, never shrunk below it.
            assertThat(list.width())
                .isCloseTo(contentWidth, within(TOLERANCE));
            assertThat(list.width())
                .isGreaterThan(strip.measureFlexRowWidth());
            assertThat(capped.flexViewport().width())
                .isCloseTo(contentWidth, within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsSpansAPinnedHeaderDividerAcrossTheFullBody() {
            // A rule heads the block above the scrolling list - the political map's picker shape. The
            // pinned header divider spans the whole framed body (edge to edge inside the border inset),
            // not the padded content column, so the capped path spans dividers as the plain stack does.
            var strip = measure(List.of(
                new DividerSpec(),
                buildScrollingSection(FLEX_OPTION_COUNT)));

            var body = buildBodyFor(strip, 0f);
            var divider = layoutControlsIn(body, strip, 0f).controls().get(0).bounds();

            assertThat(divider.x())
                .isCloseTo(body.x(), within(TOLERANCE));
            assertThat(divider.width())
                .isCloseTo(body.width(), within(TOLERANCE));
        }

        @Test
        void layoutCappedControlsSpansAPinnedFooterDividerAcrossTheFullBody() {
            // The footer run is placed through the same path as the header, so a rule pinned beneath the
            // list spans the framed body exactly as one above it does.
            var strip = measure(List.of(
                buildScrollingSection(FLEX_OPTION_COUNT),
                new DividerSpec()));

            var body = buildBodyFor(strip, 0f);
            var controls = layoutControlsIn(body, strip, 0f).controls();
            var divider = controls.get(controls.size() - 1).bounds();

            assertThat(divider.x())
                .isCloseTo(body.x(), within(TOLERANCE));
            assertThat(divider.width())
                .isCloseTo(body.width(), within(TOLERANCE));
        }
    }

    @Nested
    class LayoutBodyStrip {

        @Test
        void layoutBodyStripKeepsTheMeasuredWidthAtTheDefaultThickness() {

            var specs = buildHeaderFlexFooterStrip();

            // The default bar fits inside the padding the body already insets its controls by, so it costs
            // the body nothing and the framed width is the strip's own measured width.
            assertThat(layoutBodyStripAt(specs, ScrollbarThickness.DEFAULT).bounds().width())
                .isCloseTo(readNaturalWidth(measure(specs)), within(TOLERANCE));
        }

        @Test
        void layoutBodyStripKeepsTheMeasuredWidthWhenThePaddingSwallowsTheBar() {

            var specs = buildHeaderFlexFooterStrip();

            // A bar thinner than the default has even more room inside the padding, so it likewise leaves
            // the body at its measured width rather than narrowing it.
            assertThat(layoutBodyStripAt(specs, SWALLOWED_BAR).bounds().width())
                .isCloseTo(readNaturalWidth(measure(specs)), within(TOLERANCE));
        }

        @Test
        void layoutBodyStripWidensTheBodyByWhatTheBarOverrunsThePadding() {

            var specs = buildHeaderFlexFooterStrip();
            var atDefault = layoutBodyStripAt(specs, ScrollbarThickness.DEFAULT).bounds();
            var atOversize = layoutBodyStripAt(specs, OVERSIZE_BAR).bounds();

            // The fat bar has nowhere to grow but over the rows, so the body grows rightward by exactly
            // what it overruns the padding by - and keeps its left edge, the direction a panel grows.
            assertThat(atOversize.width() - atDefault.width())
                .isCloseTo(OVERSIZE_BAR_EXCESS, within(TOLERANCE));
            assertThat(atOversize.x())
                .isCloseTo(atDefault.x(), within(TOLERANCE));
        }

        @Test
        void layoutBodyStripLeavesTheListWidthAloneAsTheBarThickens() {

            var specs = buildHeaderFlexFooterStrip();
            var atDefault = readFlexBounds(layoutBodyStripAt(specs, ScrollbarThickness.DEFAULT).placement());
            var atOversize = readFlexBounds(layoutBodyStripAt(specs, OVERSIZE_BAR).placement());

            // The box absorbed the whole of the growth, so the rows are exactly where they were - which is
            // the point of widening the body rather than letting the bar eat into the list.
            assertThat(atOversize.width())
                .isCloseTo(atDefault.width(), within(TOLERANCE));
            assertThat(atOversize.x())
                .isCloseTo(atDefault.x(), within(TOLERANCE));
        }

        @Test
        void layoutBodyStripLeavesThePinnedRowsAloneAsTheBarThickens() {

            var specs = buildHeaderFlexFooterStrip();
            var atDefault = layoutBodyStripAt(specs, ScrollbarThickness.DEFAULT).placement().controls();
            var atOversize = layoutBodyStripAt(specs, OVERSIZE_BAR).placement().controls();

            // The header and footer are measured, not flexed, so the widening reaches neither: every
            // pinned row keeps the x and the width it had at the default bar.
            for (var index = 0; index < atDefault.size(); index++) {

                assertThat(atOversize.get(index).bounds().x())
                    .isCloseTo(atDefault.get(index).bounds().x(), within(TOLERANCE));
                assertThat(atOversize.get(index).bounds().width())
                    .isCloseTo(atDefault.get(index).bounds().width(), within(TOLERANCE));
            }
        }

        @Test
        void layoutBodyStripReservesNothingWhenNoControlScrolls() {

            var specs = buildPinnedOnlyStrip();

            // A strip with nothing to scroll has no bar to reserve for, so even the fattest thickness
            // leaves its body at the measured width.
            assertThat(layoutBodyStripAt(specs, OVERSIZE_BAR).bounds().width())
                .isCloseTo(readNaturalWidth(measure(specs)), within(TOLERANCE));
        }
    }

    // A body strip laid out at the given bar thickness under a cap loose enough never to bite, so a case
    // reads only what the thickness did to the width.
    private CappedStripLayout.BodyStrip layoutBodyStripAt(
            List<ControlSpec> specs,
            ScrollbarThickness thickness) {

        return CappedStripLayout.layoutBodyStrip(
            new BodyRoom(BODY_LEFT_X, BODY_TOP_Y, UNCAPPED_BODY_HEIGHT),
            thickness,
            specs,
            measurersFake,
            0f);
    }

    // The capped placement for a strip inside a framed body, at the default bar thickness - the width
    // cases above name their own thickness through layoutBodyStripAt instead.
    private CappedStripPlacement layoutControlsIn(
            Rectangle body,
            MeasuredStrip strip,
            float rawScrollOffset) {

        return CappedStripLayout.layoutCappedControls(
            body,
            strip,
            ScrollbarThickness.DEFAULT,
            rawScrollOffset,
            measurersFake);
    }

    // The flex list's laid-out bounds - the second control, between the header and the footer.
    private static Rectangle readFlexBounds(CappedStripPlacement placement) {
        return placement.controls().get(1).bounds();
    }

    // The first control's bounds after laying the strip out in a body short of its natural height by
    // shortfall, for comparing the header's position across a natural and a capped body.
    private Rectangle readFirstControlBounds(MeasuredStrip strip, float shortfall) {
        return layoutControlsIn(buildBodyFor(strip, shortfall), strip, 0f)
            .controls()
            .get(0)
            .bounds();
    }

    private MeasuredStrip measure(List<ControlSpec> specs) {
        return CappedStripLayout.measureStrip(specs, measurersFake);
    }

    private static float readNaturalHeight(MeasuredStrip strip) {
        return strip.measurement().bodyHeight();
    }

    private static float readNaturalWidth(MeasuredStrip strip) {
        return strip.measurement().bodyWidth();
    }

    // The framed body for a strip, short of its natural height by shortfall - which is the height the flex
    // list then has to give up and scroll.
    private static Rectangle buildBodyFor(MeasuredStrip strip, float shortfall) {
        return buildFrameBody(readNaturalHeight(strip) - shortfall, readNaturalWidth(strip));
    }

    // Frames a body of the given height hanging from the fixed top edge, so a shorter body rises from the
    // bottom the way the tab panel frames it beneath the tab row.
    private static Rectangle buildFrameBody(float height, float width) {
        return new Rectangle(BODY_LEFT_X, BODY_TOP_Y - height, width, height);
    }
}
