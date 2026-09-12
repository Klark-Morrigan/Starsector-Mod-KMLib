package kmlib.starsector.ui.layout;

import kmlib.math.geometry.BoxEdge;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.LabelledControlSpecs;
import kmlib.starsector.ui.controls.VerticalTableSpecs;
import kmlib.starsector.ui.font.StripTextMeasurers;
import kmlib.starsector.ui.text.ImageSpan;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.PanelChrome;
import kmlib.starsector.ui.widgets.scroll.ScrollbarThickness;
import kmlib.starsector.ui.widgets.tabs.BandButtonSpec;
import kmlib.starsector.ui.widgets.tabs.HeaderBandSpec;
import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;
import kmlib.starsector.ui.widgets.tabs.TabPanelViewState;
import kmlib.starsector.ui.widgets.tabs.style.TabBox;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;
import kmlib.starsector.ui.widgets.tabs.style.TabStyles;
import kmlib.testfixtures.starsector.ui.font.LineWidthMeasurerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TabPanelLayout#computePlacement}: a tabs-control header hung from the panel's anchor with a
 * body strip framed beneath it, the box wrapping that body alone. The fake measurer reports four width
 * units per character, so every expected rectangle is a hand-checkable multiplication rather than a
 * font-dependent value.
 */
final class TabPanelLayoutTest {

    // Four width units per character, so a label's measured width is its length times four.
    private static final float WIDTH_PER_CHAR = 4f;

    // A screen and placement chosen so every derived edge is an integer: the row hangs from 950, the box
    // from the row's bottom edge, content inset by a 2px border, tabs one TAB_HEIGHT tall snapped past the
    // text padding with the minimum-width floor.
    private static final float SCREEN_HEIGHT = 1000f;
    private static final int PADDING_TOP = 50;
    private static final int PADDING_LEFT = 20;
    private static final int PADDING_BOTTOM = 12;
    private static final int BORDER_WIDTH = 2;
    private static final float TOLERANCE = 0.01f;

    // A bar four times the default, far enough from it that a placement still carrying the default reads
    // as a plain failure rather than as rounding.
    private static final ScrollbarThickness THICK_BAR = new ScrollbarThickness(12f);

    // What that bar overruns the body padding by (12 + 3 margin + 2 clearance = 17, against a padding of
    // 8), which is what the body, the box framed around it, and the notch riding its edge each move by.
    private static final float THICK_BAR_GUTTER_EXCESS = 9f;

    // The tab row hangs from the panel's own top anchor, taking no border inset above it; across, it
    // starts at the body's content edge, the frame being drawn down the body alone.
    private static final float HEADER_TOP_Y = SCREEN_HEIGHT - PADDING_TOP;

    // The band the baseline style stands, so the expected header edges track whatever style is injected
    // rather than a constant the layout no longer reads.
    private static final float DEFAULT_BAND_HEIGHT = TabStyle.DEFAULT_HEADER_BAND_HEIGHT;
    private static final TabStyle DEFAULT_TAB_STYLE =
        TabStyles.buildAtBandHeight(TabStyle.DEFAULT_HEADER_BAND_HEIGHT);

    // The box starts where the row ends, and its content insets from there by the border.
    private static final float HEADER_BOTTOM_Y = HEADER_TOP_Y - DEFAULT_BAND_HEIGHT;
    private static final float BOX_TOP_Y = HEADER_BOTTOM_Y;
    private static final float CONTENT_X = PADDING_LEFT + BORDER_WIDTH;
    private static final float CONTENT_TOP_Y = BOX_TOP_Y - BORDER_WIDTH;

    // A band deliberately unlike the default, so an assertion that the injected height is honoured cannot
    // pass by coincidence against the baseline.
    private static final float CUSTOM_BAND_HEIGHT = 17f;

    // "No Layer" is 8 chars, "Political Map" 13; each snaps to its measured width plus the tab text
    // padding, floored at the minimum tab width.
    private static final float FIRST_TAB_WIDTH = Math.max(
        8 * WIDTH_PER_CHAR + TabsControlLayout.TAB_TEXT_PADDING,
        TabsControlLayout.MIN_TAB_WIDTH);

    private static final float SECOND_TAB_WIDTH = Math.max(
        13 * WIDTH_PER_CHAR + TabsControlLayout.TAB_TEXT_PADDING,
        TabsControlLayout.MIN_TAB_WIDTH);

    private static final float HEADER_WIDTH = FIRST_TAB_WIDTH + SECOND_TAB_WIDTH;

    // Both faces measure alike here, so every expectation below stays a plain character count; the
    // faces being told apart is pinned where that is the point under test.
    private final StripTextMeasurers measurersFake = new StripTextMeasurers(
        new LineWidthMeasurerFake(WIDTH_PER_CHAR),
        new LineWidthMeasurerFake(WIDTH_PER_CHAR));

    private static final ControlSpec.Tabs TABS = new ControlSpec.Tabs(
        List.of("No Layer", "Political Map"),
        List.of(),
        0,
        ControlAction.NONE);

    // What a host asking for no band button passes, named so the call sites read as a panel of tabs alone
    // rather than as a null among specs.
    private static final BandButtonSpec NO_BAND_BUTTON = null;

    // The band the button's own style names, deliberately not the panel's: the layout pins the button to
    // the panel's band, and a style agreeing with it by coincidence could not show that.
    private static final float BAND_BUTTON_STYLE_BAND_HEIGHT = 33f;

    // The button's own box: a width of its own, and a height deliberately short of the panel's band so the
    // icon box - which hangs from the band's top at the tab's height - cannot be mistaken for the bounds.
    private static final float BAND_BUTTON_WIDTH = 29f;
    private static final float BAND_BUTTON_HEIGHT = 15f;

    // The mark a band button carries in place of a word, which is why its label is empty: an image is
    // sized by the box the style states rather than measured like text.
    private static final ImageSpan BAND_BUTTON_ICON = new ImageSpan("graphics/icons/x.png", null);

    private static final BandButtonSpec BAND_BUTTON = new BandButtonSpec(
        new ControlSpec.Tabs(
            List.of(""),
            List.of(),
            ControlSpec.NO_SELECTION,
            ControlAction.NONE),
        TabStyles.buildAtBandHeightInBox(
            BAND_BUTTON_STYLE_BAND_HEIGHT,
            new TabBox(BAND_BUTTON_WIDTH, BAND_BUTTON_HEIGHT, 0f)),
        BAND_BUTTON_ICON);

    // A tab box wide enough that a button inheriting it would be visibly padded out, so the case about the
    // button keeping its own box cannot pass on two numbers that happen to be close.
    private static final float FIXED_TAB_WIDTH = 130f;

    // A one-checkbox body, so the body has a definite non-zero height beneath the header.
    private static final List<ControlSpec> BODY = List.of(
        LabelledControlSpecs.buildCheckbox("X", false, ControlAction.NONE));

    // A body that scrolls: a checkbox over a marked list. The gutter is reserved on a strip HAVING a
    // scrolling region rather than on that region overrunning, so this list is deliberately short enough
    // to fit - a bar's room is held whether or not the list is currently long enough to need one.
    private static final List<ControlSpec> SCROLLING_BODY = List.of(
        LabelledControlSpecs.buildCheckbox("X", false, ControlAction.NONE),
        VerticalTableSpecs.buildIconList(
                List.of("Alpha", "Beta", "Gamma"),
                Arrays.asList(null, null, null),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE)
            .asScrolling());

    @Nested
    class ComputePlacement {

        @Test
        void computePlacementSnapsTheHeaderTabsAtThePanelAnchor() {

            var header = place(List.of()).tabsHeader();

            assertThat(header.spec())
                .isInstanceOf(ControlSpec.Tabs.class);
            assertThat(header.segments())
                .hasSize(2);

            var first = header.segments().get(0);

            // Literal 22 - the 20 padding plus the 2 border - rather than the fixture's own sum, which
            // would restate the arithmetic the layout just did and agree with it however wrong it was.
            assertThat(first.x())
                .as("the row starts where the body's content does, not at the box's outer edge")
                .isCloseTo(22f, within(TOLERANCE));
            assertThat(first.width())
                .isCloseTo(FIRST_TAB_WIDTH, within(TOLERANCE));
            assertThat(first.y() + first.height())
                .as("the row hangs from the anchor, with no border reserved above it")
                .isCloseTo(HEADER_TOP_Y, within(TOLERANCE));
            assertThat(first.height())
                .isCloseTo(DEFAULT_BAND_HEIGHT, within(TOLERANCE));

            var second = header.segments().get(1);

            assertThat(second.x())
                .as("the second tab abuts the first, this style stating no channel between them")
                .isCloseTo(22f + FIRST_TAB_WIDTH, within(TOLERANCE));
            assertThat(second.width())
                .isCloseTo(SECOND_TAB_WIDTH, within(TOLERANCE));
        }

        @Test
        void computePlacementStandsTheBoxOnTheTabsBottomEdgeNotTheBandsWhenTheBoxIsShorter() {
            // A 18-tall tab in a 19 band: the box hangs from the TABS' bottom, so the pixel the band keeps
            // under them is the box's own top border - the row rules its baseline in that same pixel, and
            // the two coincide instead of stacking into a two-pixel rule with a dead pixel between.
            //
            // Literals rather than the fixture's arithmetic: 950 is the header top (1000 - 50), so the
            // tabs' bottom is 932 and the box top must be that, not the band's 931.
            var placement = placeStyled(
                TabStyles.buildAtBandHeightInBox(19f, new TabBox(130f, 18f, 1f)),
                BODY);

            assertThat(placement.body().box().y() + placement.body().box().height())
                .as("the box's top is the tabs' bottom edge, a pixel above the band's own")
                .isCloseTo(932f, within(TOLERANCE));
            assertThat(placement.tabsHeader().segments().get(0).y())
                .as("and that is where the tabs actually end")
                .isCloseTo(932f, within(TOLERANCE));
        }

        @Test
        void computePlacementLeavesTheBoxOnTheBandWhenTheTabsFillIt() {
            // The other end of the same rule: a snapped row's tabs are the band, so there is no spare pixel
            // and the box's top is the band's bottom as it always was. Pinned so the shorter-box case above
            // reads as the box following the tabs rather than as a constant offset applied everywhere.
            var placement = place(BODY);

            assertThat(placement.body().box().y() + placement.body().box().height())
                .isCloseTo(BOX_TOP_Y, within(TOLERANCE));
        }

        @Test
        void computePlacementSizesTheBoxWidthToTheBodyNotTheWiderTabRow() {

            var placement = place(BODY);
            var box = placement.body().box();
            var body = placement.body().body();

            // The tab row (116) is wider than this narrow body, but the box tracks the body's own width
            // plus the border on each edge - a wide tab row overhangs the frame rather than stretching it.
            assertThat(body.width())
                .isLessThan(HEADER_WIDTH);
            assertThat(box.width())
                .isCloseTo(body.width() + 2f * BORDER_WIDTH, within(TOLERANCE));
            assertThat(box.x())
                .isCloseTo(PADDING_LEFT, within(TOLERANCE));
            assertThat(box.y() + box.height())
                .isCloseTo(BOX_TOP_Y, within(TOLERANCE));
        }

        @Test
        void computePlacementPullsContentFlushAndShrinksTheBoxWhenTheLeftBorderIsDropped() {

            var framed = place(BODY);
            var droppedLeft = place(BODY, 0f, EnumSet.of(BoxEdge.TOP, BoxEdge.RIGHT, BoxEdge.BOTTOM));

            var body = droppedLeft.body().body();
            var box = droppedLeft.body().box();

            // With no left border to reserve, the content sits flush at the box's left edge (the anchor)
            // rather than inset by the border - and the tab row follows it there, the row being laid at
            // the same content edge. This is the half of that rule a flush-mounted panel depends on: the
            // fully framed case above stands its row at 22, and a row that took the inset unconditionally
            // would gap this panel off the edge it was placed flush with.
            assertThat(body.x())
                .isCloseTo(PADDING_LEFT, within(TOLERANCE));
            assertThat(droppedLeft.tabsHeader().segments().get(0).x())
                .isCloseTo(PADDING_LEFT, within(TOLERANCE));

            // The box stays anchored at the same left edge but reclaims the dropped border's width, so it is
            // exactly one border narrower than the fully framed box - no bare strip where the border was.
            assertThat(box.x())
                .isCloseTo(PADDING_LEFT, within(TOLERANCE));
            assertThat(box.width())
                .isCloseTo(body.width() + BORDER_WIDTH, within(TOLERANCE));
            assertThat(framed.body().box().width() - box.width())
                .isCloseTo(BORDER_WIDTH, within(TOLERANCE));
        }

        @Test
        void computePlacementKeepsTheBoxHeightWhenOnlyASideBorderIsDropped() {

            var framed = place(BODY);
            var droppedLeft = place(BODY, 0f, EnumSet.of(BoxEdge.TOP, BoxEdge.RIGHT, BoxEdge.BOTTOM));

            // Dropping the left border collapses width only; the top and bottom are still framed, so the box
            // keeps its full height and top anchor.
            assertThat(droppedLeft.body().box().height())
                .isCloseTo(framed.body().box().height(), within(TOLERANCE));
            assertThat(droppedLeft.body().box().y() + droppedLeft.body().box().height())
                .isCloseTo(BOX_TOP_Y, within(TOLERANCE));
        }

        @Test
        void computePlacementLetsAWiderTabRowOverhangTheBoxRatherThanClampIt() {

            var placement = place(BODY);
            var box = placement.body().box();
            var header = placement.tabsHeader();

            // The tab row lays out to its own width, independent of the box; wider than the body, its right
            // edge runs past the box's right border rather than being clamped to it - the overhang the tab
            // panel leaves for the build to resolve, and which the header's own segments stay hit-testable
            // over since input reads the tab geometry, not the box.
            assertThat(header.bounds().x() + header.bounds().width())
                .isGreaterThan(box.x() + box.width());
        }

        @Test
        void computePlacementLeavesNoBoxBeneathTheHeaderWhenBodyIsEmpty() {

            var placement = place(List.of());
            var box = placement.body().box();

            // A tab row with nothing under it is the whole panel: there is no frame to stroke and no
            // footprint to claim below the row, so the box is empty rather than a border-sized square
            // hanging off the row's left end.
            assertThat(placement.hasBody())
                .isFalse();
            assertThat(box.width())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(box.height())
                .isCloseTo(0f, within(TOLERANCE));

            // The empty box still sits at the row's bottom edge, so nothing is reserved beneath it.
            assertThat(box.y())
                .isCloseTo(HEADER_BOTTOM_Y, within(TOLERANCE));
        }

        @Test
        void computePlacementHangsTheBodyBeneathTheHeaderBand() {

            var body = place(BODY).body().body();

            assertThat(body.x()).
                isCloseTo(CONTENT_X, within(TOLERANCE));
            assertThat(body.y() + body.height())
                .as("the body insets from the box top, which is the header band's bottom")
                .isCloseTo(CONTENT_TOP_Y, within(TOLERANCE));
            assertThat(body.height())
                .isGreaterThan(0f);
        }

        @Test
        void computePlacementFramesTheBodyAloneWithTheRowStandingOnIt() {

            var placement = place(BODY);
            var box = placement.body().box();
            var body = placement.body().body();

            // The box wraps the body and its border and nothing else, so the frame it describes sits under
            // the tab row rather than around it - the row is chrome on the panel, not content inside it.
            assertThat(box.x())
                .isCloseTo(PADDING_LEFT, within(TOLERANCE));
            assertThat(box.y() + box.height())
                .isCloseTo(BOX_TOP_Y, within(TOLERANCE));
            assertThat(box.height())
                .isCloseTo(body.height() + 2f * BORDER_WIDTH, within(TOLERANCE));

            // The row stands entirely above it, meeting the box's top edge.
            var header = placement.tabsHeader();

            assertThat(header.bounds().y())
                .isCloseTo(box.y() + box.height(), within(TOLERANCE));
        }

        @Test
        void computePlacementCarriesTheTabsControlAsTheHeader() {

            var header = place(BODY).tabsHeader();

            assertThat(header.spec())
                .isInstanceOf(ControlSpec.Tabs.class);
            assertThat(header.spec().labels())
                .containsExactly("No Layer", "Political Map");
        }

        @Test
        void computePlacementLaysTheBodyAtItsInterpolatedWidthWhenPartlyCollapsed() {

            var fullWidth = place(BODY, 0f).body().body().width();
            var halfPlacement = place(BODY, 0.5f);
            var halfWidth = halfPlacement.body().body().width();

            // At half collapse the interior lays out at half its full width...
            assertThat(halfWidth)
                .isCloseTo(fullWidth * 0.5f, within(TOLERANCE));

            // ...and the box tracks the interpolated interior plus the border on each edge.
            assertThat(halfPlacement.body().box().width())
                .isCloseTo(halfWidth + 2f * BORDER_WIDTH, within(TOLERANCE));
        }

        @Test
        void computePlacementCollapsesTheBoxToADockedRailAtFullCollapse() {

            var placement = place(BODY, 1f);
            var box = placement.body().box();
            var body = placement.body().body();

            // Fully docked, the interior vanishes and the box reduces to the border-only rail at the left
            // anchor - the two borders meeting into the single vertical line the panel docks to.
            assertThat(body.width())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(box.width())
                .isCloseTo(2f * BORDER_WIDTH, within(TOLERANCE));
            assertThat(box.x())
                .isCloseTo(PADDING_LEFT, within(TOLERANCE));
            assertThat(box.y() + box.height())
                .isCloseTo(BOX_TOP_Y, within(TOLERANCE));

            // The collapse is horizontal only, so the rail keeps the fully expanded box's height.
            assertThat(box.height())
                .isCloseTo(place(BODY, 0f).body().box().height(), within(TOLERANCE));
        }

        @Test
        void computePlacementExposesANotchWhenTheBodyHasControlsToCollapse() {
            // A body with controls is collapsible, so the panel carries a handle to fold it with.
            assertThat(place(BODY).notch())
                .isNotNull();
        }

        @Test
        void computePlacementExposesNoNotchWhenTheBodyIsEmpty() {
            // An empty body has nothing to collapse, so the panel is not collapsible and carries no handle
            // - a notch protruding off a bodyless tab row would fold a body that is not there.
            assertThat(place(List.of()).notch())
                .isNull();
        }

        @Test
        void computePlacementSitsTheNotchOnTheRightBorderEdgeCentredOnTheFrame() {

            var placement = place(BODY, 0f);
            var box = placement.body().box();
            var notch = placement.notch();

            // The notch protrudes rightward from the box's right border edge, its own fixed size...
            assertThat(notch.x())
                .isCloseTo(box.x() + box.width(), within(TOLERANCE));
            assertThat(notch.width())
                .isCloseTo(TabPanelLayout.NOTCH_WIDTH, within(TOLERANCE));
            assertThat(notch.height())
                .isCloseTo(TabPanelLayout.NOTCH_HEIGHT, within(TOLERANCE));

            // ...vertically centred on the frame with the default zero offset.
            assertThat(notch.computeCenterY())
                .isCloseTo(box.computeCenterY(), within(TOLERANCE));
        }

        @Test
        void computePlacementTracksTheNotchToTheCollapsingRightEdge() {

            var expanded = place(BODY, 0f);
            var docked = place(BODY, 1f);

            // The notch rides the box's right edge at every fraction, so as the body collapses leftward the
            // handle moves in with it and stays reachable to expand the docked panel again.
            assertThat(expanded.notch().x())
                .isCloseTo(expanded.body().box().x() + expanded.body().box().width(), within(TOLERANCE));
            assertThat(docked.notch().x())
                .isCloseTo(docked.body().box().x() + docked.body().box().width(), within(TOLERANCE));
            assertThat(docked.notch().x())
                .isLessThan(expanded.notch().x());
        }

        @Test
        void computePlacementKeepsTheNotchOnTheEdgeAndCentredMidCollapse() {

            var placement = place(BODY, 0.5f);
            var box = placement.body().box();
            var notch = placement.notch();

            // Mid-collapse the handle stays reachable: it rides the box's right edge and stays centred on
            // the frame, so it never orphans from the shrinking box while the animation is in flight.
            assertThat(notch.x())
                .isCloseTo(box.x() + box.width(), within(TOLERANCE));
            assertThat(notch.computeCenterY())
                .isCloseTo(box.computeCenterY(), within(TOLERANCE));
        }

        @Test
        void computePlacementDocksToAZeroWidthRailWhenThereIsNoBorder() {

            var placement = TabPanelLayout.computePlacement(
                SCREEN_HEIGHT,
                new Padding(PADDING_TOP, 0, PADDING_BOTTOM, PADDING_LEFT),
                new PanelChrome(new BoxBorder(0f), ScrollbarThickness.DEFAULT),
                new HeaderBandSpec(DEFAULT_TAB_STYLE, TABS, NO_BAND_BUTTON),
                BODY,
                measurersFake,
                new TabPanelViewState(0f, 1f));

            var box = placement.body().box();

            // With no border, the docked rail has no interior and no border to keep, so the box collapses
            // to zero width - yet the placement stays well-formed and the notch still anchors to the edge.
            assertThat(box.width())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(box.x())
                .isCloseTo(PADDING_LEFT, within(TOLERANCE));
            assertThat(placement.notch().x())
                .isCloseTo(box.x() + box.width(), within(TOLERANCE));
        }

        @Test
        void computePlacementClampsCollapseFractionToTheUnitRange() {
            // A fraction past the ends behaves as the nearest end - past 1 stays fully docked, below 0 stays
            // fully expanded - so an overshooting animation value never inverts the geometry.
            assertThat(place(BODY, 2f).body().box().width())
                .isCloseTo(place(BODY, 1f).body().box().width(), within(TOLERANCE));
            assertThat(place(BODY, -1f).body().box().width())
                .isCloseTo(place(BODY, 0f).body().box().width(), within(TOLERANCE));
        }

        @Test
        void computePlacementStandsTheHeaderBandAtTheInjectedHeight() {

            var placement = placeStyled(DEFAULT_TAB_STYLE, BODY);
            var styled = placeStyled(TabStyles.buildAtBandHeight(CUSTOM_BAND_HEIGHT), BODY);

            // Every tab in the band takes the injected height, and the band hangs from the same content top,
            // so a shorter style shortens the row downward rather than floating it inside a fixed band.
            var styledTab = styled.tabsHeader().segments().get(0);

            assertThat(styledTab.height())
                .isCloseTo(CUSTOM_BAND_HEIGHT, within(TOLERANCE));
            assertThat(styledTab.y() + styledTab.height())
                .isCloseTo(HEADER_TOP_Y, within(TOLERANCE));

            // The box hangs from the row's bottom edge, so a shorter band lifts the whole frame by exactly
            // what the band gave up - the body keeps its own height rather than stretching to absorb it.
            assertThat(styled.body().body().height())
                .isCloseTo(placement.body().body().height(), within(TOLERANCE));

            var boxTopY = placement.body().box().y() + placement.body().box().height();
            var styledBoxTopY = styled.body().box().y() + styled.body().box().height();

            assertThat(styledBoxTopY - boxTopY)
                .isCloseTo(DEFAULT_BAND_HEIGHT - CUSTOM_BAND_HEIGHT, within(TOLERANCE));
        }

        @Test
        void computePlacementClampsANegativeBandToABandlessPanel() {
            // A negative height would hang the tab row above its own top edge; it floors at zero instead, so
            // the panel degrades to its body under the border rather than inverting the header.
            var styled = placeStyled(TabStyles.buildAtBandHeight(-8f), BODY);

            assertThat(styled.tabsHeader().bounds().height())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(styled.tabsHeader().bounds().y())
                .isCloseTo(HEADER_TOP_Y, within(TOLERANCE));
        }

        @Test
        void computePlacementDrawsTheWholeTabRowWhileThePanelRests() {

            var placement = place(BODY, 0f);

            // At rest the drawn band is the row as laid out, overhang and all: a row wider than its body
            // is not cut off at the frame, it stands past it.
            assertThat(placement.drawnHeaderBand())
                .isEqualTo(placement.tabsHeader().bounds());
        }

        @Test
        void computePlacementWipesTheTabRowWithTheFoldedBox() {

            var placement = place(BODY, 0.5f);
            var box = placement.body().box();
            var drawn = placement.drawnHeaderBand();

            // Mid-fold the row narrows to the box's own span, so the panel wipes toward its anchored edge
            // as one piece rather than leaving a full-width row above a half-folded frame. The wipe eats
            // the row from the right: its left edge stands where it was laid - at the body's content edge,
            // a border inside the box's own - and its right edge rides the shrinking frame down.
            assertThat(drawn.x())
                .as("the anchored edge does not move, so the row still starts at its content edge")
                .isCloseTo(CONTENT_X, within(TOLERANCE));
            assertThat(drawn.x() + drawn.width())
                .as("the wiped row ends where the folding box ends")
                .isCloseTo(box.x() + box.width(), within(TOLERANCE));
            assertThat(drawn.width())
                .isLessThan(placement.tabsHeader().bounds().width());

            // The wipe is horizontal only: the band keeps its full height throughout.
            assertThat(drawn.height())
                .isCloseTo(DEFAULT_BAND_HEIGHT, within(TOLERANCE));
        }

        @Test
        void computePlacementLeavesABodylessRowUnwipedByAStaleFold() {

            // The fold state outlives a tab switch, so a bodyless tab can be laid out at a fraction another
            // tab's body left standing. It has nothing to fold and no handle to unfold it, so its row must
            // stand whole - wiped, it would vanish with nothing on screen to bring it back.
            var placement = place(List.of(), 1f);

            assertThat(placement.drawnHeaderBand())
                .isEqualTo(placement.tabsHeader().bounds());
            assertThat(placement.drawnHeaderBand().width())
                .isCloseTo(HEADER_WIDTH, within(TOLERANCE));
        }

        @Test
        void computePlacementFliesNoBandButtonWhereNoneWasAskedFor() {
            // The band a panel of tabs alone lays, unchanged: the button is something a host asks for, so a
            // host that does not gets exactly the row it had before there was one to ask for.
            var placement = place(BODY);

            assertThat(placement.bandButton())
                .isNull();
            assertThat(placement.drawnHeaderBand())
                .isEqualTo(placement.tabsHeader().bounds());
        }

        @Test
        void computePlacementLaysTheBandButtonWhereTheTabsLeaveOff() {
            // Appended to the row rather than pinned somewhere of its own, so the button stands beside the
            // last tab at the same height and the band simply grows by one box.
            var button = placeWithBandButton(BODY).bandButton().control().bounds();

            assertThat(button.x())
                .isCloseTo(CONTENT_X + HEADER_WIDTH, within(TOLERANCE));
            assertThat(button.y())
                .isCloseTo(HEADER_TOP_Y - DEFAULT_BAND_HEIGHT, within(TOLERANCE));
            assertThat(button.height())
                .isCloseTo(DEFAULT_BAND_HEIGHT, within(TOLERANCE));
            assertThat(button.width())
                .isCloseTo(BAND_BUTTON_WIDTH, within(TOLERANCE));
        }

        @Test
        void computePlacementLeavesTheTabsRowExactlyAsItWasUnderABandButton() {
            // The one thing a button in the row may not do. Every index the pick, the lit tab and the
            // shortcut walk are resolved by is a position in this control, so a button that moved a segment
            // by a pixel would move what a click on it selects.
            var withButton = placeWithBandButton(BODY).tabsHeader();
            var withoutButton = place(BODY).tabsHeader();

            assertThat(withButton.bounds())
                .isEqualTo(withoutButton.bounds());
            assertThat(withButton.segments())
                .isEqualTo(withoutButton.segments());
        }

        @Test
        void computePlacementSizesTheBandButtonToItsOwnBoxBesideWiderTabs() {
            // The button wears its own box, not the row's. Under a fixed-width tab style - the sector map's,
            // whose boxes are wide enough for a layer name - a button that inherited the row would stand in
            // a box several times the width of the mark it carries.
            var fixedWidthTabs = TabStyles.buildAtBandHeightInBox(
                DEFAULT_BAND_HEIGHT,
                new TabBox(FIXED_TAB_WIDTH, 0f, 0f));

            var button = placeStyledWithBandButton(fixedWidthTabs, BODY).bandButton().control().bounds();

            assertThat(button.width())
                .isCloseTo(BAND_BUTTON_WIDTH, within(TOLERANCE));
            assertThat(button.width())
                .isLessThan(FIXED_TAB_WIDTH);
        }

        @Test
        void computePlacementCarriesTheBandButtonsIconOverToItsPlacement() {
            // The mark travels with the laid-out button rather than being looked up again at the draw, so
            // the pass that paints it cannot be handed one image while the box was sized for another.
            assertThat(placeWithBandButton(BODY).bandButton().icon())
                .isSameAs(BAND_BUTTON_ICON);
        }

        @Test
        void computePlacementBoxesTheIconAtTheButtonsTabRatherThanItsBand() {
            // The image fills the tab, and the tab is shorter than the band it hangs in - a chrome keeps the
            // difference for the line its tabs stand on, so an image drawn over the whole bounds would sit a
            // pixel low and cover that rule.
            var bandButton = placeWithBandButton(BODY).bandButton();
            var bounds = bandButton.control().bounds();
            var iconBox = bandButton.computeIconBox();

            assertThat(iconBox.height())
                .isCloseTo(BAND_BUTTON_HEIGHT, within(TOLERANCE));
            assertThat(iconBox.width())
                .isCloseTo(BAND_BUTTON_WIDTH, within(TOLERANCE));
            assertThat(iconBox.y() + iconBox.height())
                .as("the icon hangs from the band's top edge")
                .isCloseTo(bounds.y() + bounds.height(), within(TOLERANCE));
        }

        @Test
        void computePlacementStandsTheBandButtonInThePanelsBandRatherThanItsOwn() {
            // The band is the room the panel was given, so it is the one part of its look the button does
            // not choose. Left to its own, a button carrying a style built for another panel would stand
            // taller or shorter than the tabs it sits beside.
            var placement = placeWithBandButton(BODY);

            assertThat(placement.bandButton().style().headerBandHeight())
                .isCloseTo(DEFAULT_BAND_HEIGHT, within(TOLERANCE));
            assertThat(placement.bandButton().control().bounds().height())
                .isCloseTo(DEFAULT_BAND_HEIGHT, within(TOLERANCE));
        }

        @Test
        void computePlacementDrawsTheBandOverTheButtonAsWellAsTheTabs() {
            // One band for both, so the fold wipes them together, the footprint claims the button, and the
            // panel's outer bound reaches it. A band naming only its tabs would leave the button unclipped
            // and on screen the panel does not own.
            var drawn = placeWithBandButton(BODY).drawnHeaderBand();

            assertThat(drawn.x())
                .isCloseTo(CONTENT_X, within(TOLERANCE));
            assertThat(drawn.width())
                .isCloseTo(HEADER_WIDTH + BAND_BUTTON_WIDTH, within(TOLERANCE));
        }

        @Test
        void computePlacementCarriesTheBorderItFramedTheBoxAround() {
            // The width a later stroke must use: handed back on the placement so the pass that paints
            // the frame spends exactly the inset this layout reserved for it, rather than re-reading
            // the width from the source the caller read it from.
            var borderedEdges = Set.of(BoxEdge.TOP, BoxEdge.LEFT);
            var placement = place(BODY, 0f, borderedEdges);

            assertThat(placement.border().width())
                .isCloseTo(BORDER_WIDTH, within(TOLERANCE));
            assertThat(placement.border().edges())
                .isEqualTo(borderedEdges);
        }

        @Test
        void computePlacementCarriesTheScrollbarThicknessOntoTheBodyPlacement() {

            var placement = placeAtThickness(BODY, THICK_BAR);

            // The thickness travels with the body it sizes a bar over, since that is the placement both
            // the pass drawing the bar and the pass grabbing its thumb read.
            assertThat(placement.body().scrollbarThickness().pixels())
                .isCloseTo(12f, within(TOLERANCE));
        }

        @Test
        void computePlacementLeavesABodylessPanelWithNoBarAtAll() {

            var placement = placeAtThickness(List.of(), THICK_BAR);

            // A tab row with nothing under it has no body to scroll and nowhere to draw a bar, so the
            // thickness the placement must still name is the one that says there is no bar - not the
            // caller's, which describes a bar this panel never stands.
            assertThat(placement.body().scrollbarThickness())
                .isEqualTo(ScrollbarThickness.NONE);
        }

        @Test
        void computePlacementGrowsTheBoxWithTheScrollbarGutter() {

            var atDefault = placeAtThickness(SCROLLING_BODY, ScrollbarThickness.DEFAULT).body();
            var atThick = placeAtThickness(SCROLLING_BODY, THICK_BAR).body();

            // The body reserves the gutter and the box frames the body, so a bar too fat for the padding
            // widens the panel rather than drawing over its rows. Both grow by the same amount, and the
            // box keeps its left edge - the panel grows rightward.
            assertThat(atThick.body().width() - atDefault.body().width())
                .isCloseTo(THICK_BAR_GUTTER_EXCESS, within(TOLERANCE));
            assertThat(atThick.box().width() - atDefault.box().width())
                .isCloseTo(THICK_BAR_GUTTER_EXCESS, within(TOLERANCE));
            assertThat(atThick.box().x())
                .isCloseTo(PADDING_LEFT, within(TOLERANCE));
        }

        @Test
        void computePlacementDocksAWidenedBodyToTheSameRail() {

            var box = placeAtThickness(SCROLLING_BODY, THICK_BAR, 1f).body().box();

            // The collapse interpolates whatever width the body was laid at down to nothing, so a panel
            // widened for a fat bar still docks to the border-only rail at the left anchor rather than to
            // a rail carrying the gutter it no longer shows.
            assertThat(box.width())
                .isCloseTo(2f * BORDER_WIDTH, within(TOLERANCE));
            assertThat(box.x())
                .isCloseTo(PADDING_LEFT, within(TOLERANCE));
        }

        @Test
        void computePlacementRidesTheNotchOnTheWidenedBoxEdge() {

            var placement = placeAtThickness(SCROLLING_BODY, THICK_BAR);
            var box = placement.body().box();

            // The handle rides the box's right border edge, so widening the box for the bar carries the
            // notch out with it - it stays flush against the frame rather than floating over the gutter.
            assertThat(placement.notch().x())
                .isCloseTo(box.x() + box.width(), within(TOLERANCE));
            assertThat(placement.notch().x())
                .isCloseTo(
                    placeAtThickness(SCROLLING_BODY, ScrollbarThickness.DEFAULT).notch().x()
                        + THICK_BAR_GUTTER_EXCESS,
                    within(TOLERANCE));
        }

        private TabPanelPlacement place(List<ControlSpec> bodyControls) {
            return place(bodyControls, 0f);
        }

        // The same panel with a band button asked for, so the button's cases differ from the baseline in
        // that one argument and nothing else - which is what lets them assert the tabs are unchanged.
        private TabPanelPlacement placeWithBandButton(List<ControlSpec> bodyControls) {
            return placeStyledWithBandButton(DEFAULT_TAB_STYLE, bodyControls);
        }

        // The same again under a stated panel style, for the cases about the button not inheriting it.
        private TabPanelPlacement placeStyledWithBandButton(
                TabStyle tabStyle,
                List<ControlSpec> bodyControls) {

            return TabPanelLayout.computePlacement(
                SCREEN_HEIGHT,
                new Padding(PADDING_TOP, 0, PADDING_BOTTOM, PADDING_LEFT),
                new PanelChrome(new BoxBorder(BORDER_WIDTH), ScrollbarThickness.DEFAULT),
                new HeaderBandSpec(tabStyle, TABS, BAND_BUTTON),
                bodyControls,
                measurersFake,
                TabPanelViewState.RESTING);
        }

        private TabPanelPlacement placeAtThickness(
                List<ControlSpec> bodyControls,
                ScrollbarThickness scrollbarThickness) {

            return placeAtThickness(bodyControls, scrollbarThickness, 0f);
        }

        private TabPanelPlacement placeAtThickness(
                List<ControlSpec> bodyControls,
                ScrollbarThickness scrollbarThickness,
                float collapseFraction) {

            return TabPanelLayout.computePlacement(
                SCREEN_HEIGHT,
                new Padding(PADDING_TOP, 0, PADDING_BOTTOM, PADDING_LEFT),
                new PanelChrome(new BoxBorder(BORDER_WIDTH), scrollbarThickness),
                new HeaderBandSpec(DEFAULT_TAB_STYLE, TABS, NO_BAND_BUTTON),
                bodyControls,
                measurersFake,
                new TabPanelViewState(0f, collapseFraction));
        }

        private TabPanelPlacement placeStyled(TabStyle tabStyle, List<ControlSpec> bodyControls) {
            return TabPanelLayout.computePlacement(
                SCREEN_HEIGHT,
                new Padding(PADDING_TOP, 0, PADDING_BOTTOM, PADDING_LEFT),
                new PanelChrome(new BoxBorder(BORDER_WIDTH), ScrollbarThickness.DEFAULT),
                new HeaderBandSpec(tabStyle, TABS, NO_BAND_BUTTON),
                bodyControls,
                measurersFake,
                TabPanelViewState.RESTING);
        }

        private TabPanelPlacement place(List<ControlSpec> bodyControls, float collapseFraction) {
            return place(bodyControls, collapseFraction, BoxEdge.ALL);
        }

        private TabPanelPlacement place(
                List<ControlSpec> bodyControls,
                float collapseFraction,
                Set<BoxEdge> borderedEdges) {
            return TabPanelLayout.computePlacement(
                SCREEN_HEIGHT,
                new Padding(PADDING_TOP, 0, PADDING_BOTTOM, PADDING_LEFT),
                new PanelChrome(new BoxBorder(BORDER_WIDTH, borderedEdges), ScrollbarThickness.DEFAULT),
                new HeaderBandSpec(DEFAULT_TAB_STYLE, TABS, NO_BAND_BUTTON),
                bodyControls,
                measurersFake,
                new TabPanelViewState(0f, collapseFraction));
        }
    }
}
