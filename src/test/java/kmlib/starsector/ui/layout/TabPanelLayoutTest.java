package kmlib.starsector.ui.layout;

import kmlib.math.geometry.BoxEdge;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.LabelledControlSpecs;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;
import kmlib.starsector.ui.widgets.tabs.TabPanelViewState;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;
import kmlib.starsector.ui.widgets.tabs.style.TabStyles;
import kmlib.testfixtures.starsector.ui.font.LineWidthMeasurerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

    // The tab row hangs from the panel's own top anchor, taking no border inset above it; across, it
    // starts at the body's content edge (CONTENT_X below), the frame being drawn down the body alone.
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

    private final LineWidthMeasurer measurerFake = new LineWidthMeasurerFake(WIDTH_PER_CHAR);

    private static final ControlSpec.Tabs TABS = new ControlSpec.Tabs(
        List.of("No Layer", "Political Map"),
        List.of(),
        0,
        ControlAction.NONE);

    // A one-checkbox body, so the body has a definite non-zero height beneath the header.
    private static final List<ControlSpec> BODY = List.of(
        LabelledControlSpecs.buildCheckbox("X", false, ControlAction.NONE));

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

            assertThat(first.x())
                .as("the row starts where the body's content does, not at the box's outer edge")
                .isCloseTo(CONTENT_X, within(TOLERANCE));
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
                .isCloseTo(CONTENT_X + FIRST_TAB_WIDTH, within(TOLERANCE));
            assertThat(second.width())
                .isCloseTo(SECOND_TAB_WIDTH, within(TOLERANCE));
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
            // rather than inset by the border - where the tab row above it already stands.
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
                new BoxBorder(0f),
                DEFAULT_TAB_STYLE,
                TABS,
                BODY,
                measurerFake,
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

        private TabPanelPlacement place(List<ControlSpec> bodyControls) {
            return place(bodyControls, 0f);
        }

        private TabPanelPlacement placeStyled(TabStyle tabStyle, List<ControlSpec> bodyControls) {
            return TabPanelLayout.computePlacement(
                SCREEN_HEIGHT,
                new Padding(PADDING_TOP, 0, PADDING_BOTTOM, PADDING_LEFT),
                new BoxBorder(BORDER_WIDTH),
                tabStyle,
                TABS,
                bodyControls,
                measurerFake,
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
                new BoxBorder(BORDER_WIDTH, borderedEdges),
                DEFAULT_TAB_STYLE,
                TABS,
                bodyControls,
                measurerFake,
                new TabPanelViewState(0f, collapseFraction));
        }
    }
}
