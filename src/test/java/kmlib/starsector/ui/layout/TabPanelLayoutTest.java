package kmlib.starsector.ui.layout;

import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlKind;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;
import kmlib.testfixtures.starsector.ui.font.LineWidthMeasurerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TabPanelLayout#computePlacement}: a tabs-control header laid flush under the top border and
 * a body strip framed beneath it, wrapped in one bordered box whose footprint the body placement carries.
 * The fake measurer reports four width units per character, so every expected rectangle is a hand-checkable
 * multiplication rather than a font-dependent value.
 */
final class TabPanelLayoutTest {
    // Four width units per character, so a label's measured width is its length times four.
    private static final float WIDTH_PER_CHAR = 4f;

    // A screen and placement chosen so every derived edge is an integer: box top at 950, content inset by
    // a 2px border, tabs one TAB_HEIGHT tall snapped past the text padding with the minimum-width floor.
    private static final float SCREEN_HEIGHT = 1000f;
    private static final int PADDING_TOP = 50;
    private static final int PADDING_LEFT = 20;
    private static final int PADDING_BOTTOM = 12;
    private static final int BORDER_WIDTH = 2;
    private static final float TOLERANCE = 0.01f;

    private static final float BOX_TOP_Y = SCREEN_HEIGHT - PADDING_TOP;
    private static final float CONTENT_X = PADDING_LEFT + BORDER_WIDTH;
    private static final float CONTENT_TOP_Y = BOX_TOP_Y - BORDER_WIDTH;
    private static final float HEADER_BOTTOM_Y = CONTENT_TOP_Y - ControlStripLayout.TAB_HEIGHT;

    // "No Layer" is 8 chars, "Political Map" 13; each snaps to its measured width plus the tab text
    // padding, floored at the minimum tab width.
    private static final float FIRST_TAB_WIDTH = Math.max(
            8 * WIDTH_PER_CHAR + ControlStripLayout.TAB_TEXT_PADDING, ControlStripLayout.MIN_TAB_WIDTH);
    private static final float SECOND_TAB_WIDTH = Math.max(
            13 * WIDTH_PER_CHAR + ControlStripLayout.TAB_TEXT_PADDING, ControlStripLayout.MIN_TAB_WIDTH);
    private static final float HEADER_WIDTH = FIRST_TAB_WIDTH + SECOND_TAB_WIDTH;

    private final LineWidthMeasurer measurerFake = new LineWidthMeasurerFake(WIDTH_PER_CHAR);

    private static final ControlSpec TABS = ControlSpec.createTabs(
            List.of("No Layer", "Political Map"), List.of(), 0, ControlAction.NONE);

    // A one-checkbox body, so the body has a definite non-zero height beneath the header.
    private static final List<ControlSpec> BODY = List.of(
            new ControlSpec(ControlKind.CHECKBOX, List.of("X"), "", ControlSpec.NO_SELECTION));

    @Nested
    class ComputePlacement {

        @Test
        void computePlacementSnapsTheHeaderTabsFlushAtTheContentTop() {
            var header = place(List.of()).tabsHeader();
            assertThat(header.spec().kind()).isEqualTo(ControlKind.TABS);
            assertThat(header.segments()).hasSize(2);

            var first = header.segments().get(0);
            assertThat(first.x()).isCloseTo(CONTENT_X, within(TOLERANCE));
            assertThat(first.width()).isCloseTo(FIRST_TAB_WIDTH, within(TOLERANCE));
            assertThat(first.y() + first.height())
                    .as("the header sits flush under the top border, not inset like a body row")
                    .isCloseTo(CONTENT_TOP_Y, within(TOLERANCE));
            assertThat(first.height()).isCloseTo(ControlStripLayout.TAB_HEIGHT, within(TOLERANCE));

            var second = header.segments().get(1);
            assertThat(second.x()).isCloseTo(CONTENT_X + FIRST_TAB_WIDTH, within(TOLERANCE));
            assertThat(second.width()).isCloseTo(SECOND_TAB_WIDTH, within(TOLERANCE));
        }

        @Test
        void computePlacementSizesTheBoxToTheHeaderWhenTheBodyIsEmpty() {
            var box = place(List.of()).body().box();
            // Header row (116) is wider than the absent body, so the box tracks it plus the border on each
            // edge (120 wide); it is one tab-height plus two borders tall (28) and pins to the top-left.
            assertThat(box.x()).isCloseTo(PADDING_LEFT, within(TOLERANCE));
            assertThat(box.width()).isCloseTo(HEADER_WIDTH + 2f * BORDER_WIDTH, within(TOLERANCE));
            assertThat(box.height())
                    .isCloseTo(ControlStripLayout.TAB_HEIGHT + 2f * BORDER_WIDTH, within(TOLERANCE));
            assertThat(box.y() + box.height()).isCloseTo(BOX_TOP_Y, within(TOLERANCE));
        }

        @Test
        void computePlacementLeavesAZeroBodyBeneathTheHeaderWhenBodyIsEmpty() {
            var body = place(List.of()).body().body();
            assertThat(body.width()).isCloseTo(0f, within(TOLERANCE));
            assertThat(body.height()).isCloseTo(0f, within(TOLERANCE));
            // The zero body still sits at the header's bottom edge, so nothing is reserved beneath.
            assertThat(body.y()).isCloseTo(HEADER_BOTTOM_Y, within(TOLERANCE));
        }

        @Test
        void computePlacementHangsTheBodyBeneathTheHeaderBand() {
            var body = place(BODY).body().body();
            assertThat(body.x()).isCloseTo(CONTENT_X, within(TOLERANCE));
            assertThat(body.y() + body.height())
                    .as("the body's top edge abuts the header band's bottom")
                    .isCloseTo(HEADER_BOTTOM_Y, within(TOLERANCE));
            assertThat(body.height()).isGreaterThan(0f);
        }

        @Test
        void computePlacementCarriesTheWholeFootprintInTheBodyBox() {
            var placement = place(BODY);
            var box = placement.body().box();
            var body = placement.body().body();
            // The body placement's box spans the whole panel - header band plus body plus the border on
            // every edge - so the single frame a renderer draws around it wraps the header too.
            assertThat(box.x()).isCloseTo(PADDING_LEFT, within(TOLERANCE));
            assertThat(box.y() + box.height()).isCloseTo(BOX_TOP_Y, within(TOLERANCE));
            assertThat(box.height())
                    .isCloseTo(ControlStripLayout.TAB_HEIGHT + body.height() + 2f * BORDER_WIDTH,
                            within(TOLERANCE));
            // And it encloses both the header segments and the body.
            var header = placement.tabsHeader();
            assertThat(box.y()).isLessThanOrEqualTo(body.y() + TOLERANCE);
            assertThat(header.bounds().y() + header.bounds().height())
                    .isLessThanOrEqualTo(box.y() + box.height() + TOLERANCE);
        }

        @Test
        void computePlacementCarriesTheTabsControlAsTheHeader() {
            var header = place(BODY).tabsHeader();
            assertThat(header.spec().kind()).isEqualTo(ControlKind.TABS);
            assertThat(header.spec().labels()).containsExactly("No Layer", "Political Map");
        }

        private TabPanelPlacement place(List<ControlSpec> bodyControls) {
            return TabPanelLayout.computePlacement(SCREEN_HEIGHT,
                    new Padding(PADDING_TOP, 0, PADDING_BOTTOM, PADDING_LEFT), BORDER_WIDTH, TABS,
                    bodyControls, measurerFake, 0f);
        }
    }
}
