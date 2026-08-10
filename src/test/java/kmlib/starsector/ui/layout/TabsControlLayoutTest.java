package kmlib.starsector.ui.layout;

import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.font.TextFace;
import kmlib.starsector.ui.widgets.tabs.style.TabBox;
import kmlib.starsector.ui.widgets.tabs.style.TabStyles;
import kmlib.testfixtures.starsector.ui.font.LineWidthMeasurerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the tabs row: {@link TabsControlLayout#buildTabContents} reads a control's parallel label and
 * shortcut lists as the tab contents the strip geometry measures, and
 * {@link TabsControlLayout#layoutHeaderControl} hangs that row flush as a panel header at the band
 * height its style states, splitting every tab to that same band. A round per-character width makes
 * each snapped rectangle a hand-checkable multiple.
 *
 * <p>The body path - a tabs row stacked as an ordinary strip control - is pinned through the strip that
 * stacks it, since what is worth fixing there is that the strip charges a tabs row its own height and
 * its own snapped width rather than a body row's.
 */
final class TabsControlLayoutTest {

    private static final float WIDTH_PER_CHAR = 10f;
    private static final float TOLERANCE = 0.01f;

    // A header anchor clear of any body origin, so an assertion cannot pass by landing on a stale value.
    private static final float HEADER_X = 40f;
    private static final float HEADER_TOP_Y = 500f;
    private static final float BAND_HEIGHT = 17f;

    // The sector map's own tab box (com.fs.starfarer.coreui.A.G): a 130 x 18 tab, neighbours parted by a
    // pixel, standing in a band that reserves one more than the tab is tall. Literal rather than read off
    // the box under test, so a box quietly re-dimensioned fails here instead of agreeing with itself.
    private static final float VANILLA_TAB_WIDTH = 130f;
    private static final float VANILLA_TAB_HEIGHT = 18f;
    private static final float VANILLA_TAB_GAP = 1f;
    private static final float VANILLA_BAND_HEIGHT = 19f;
    private static final TabBox VANILLA_MAP_TAB = new TabBox(
        VANILLA_TAB_WIDTH,
        VANILLA_TAB_HEIGHT,
        VANILLA_TAB_GAP);

    private static final ControlSpec.Tabs TABS = new ControlSpec.Tabs(
        List.of("No Layer", "Political Map"),
        List.of("N", "P"),
        0,
        ControlAction.NONE);

    // A face at a size no baseline names, so a row measured at the style's size cannot be mistaken for
    // one measured at the layout's own constant. Which atlas it names never shows - nothing here loads a
    // font - so it is a real one at an invented size rather than an invented face.
    private static final double STYLED_FONT_SIZE = 8d;
    private static final TextFace STYLED_FACE = new TextFace(
        StarsectorFont.VANILLA_ORBITRON_20AA,
        STYLED_FONT_SIZE);

    private final LineWidthMeasurer measurerFake = new LineWidthMeasurerFake(WIDTH_PER_CHAR);

    // Widths that scale with the size they are asked for, unlike the shared fake above, which reports one
    // width per character whatever size it is handed. The size the layout chose is invisible to a
    // size-blind measurer, so pinning that choice needs a measurer that spends it.
    private final LineWidthMeasurer sizeScaledMeasurerFake =
        (line, fontSize) -> line.length() * fontSize;

    @Nested
    class BuildTabContents {

        @Test
        void buildTabContentsPairsEachLabelWithItsOwnShortcut() {

            var contents = TabsControlLayout.buildTabContents(TABS);

            assertThat(contents)
                .hasSize(2);
            assertThat(contents.get(0).label())
                .isEqualTo("No Layer");
            assertThat(contents.get(0).shortcut())
                .isEqualTo("N");
            assertThat(contents.get(1).shortcut())
                .isEqualTo("P");
        }

        @Test
        void buildTabContentsLeavesATabHintlessWhenTheShortcutListRunsShort() {
            // The shortcut list runs parallel to the labels rather than being required to match it, so a
            // strip written without hints - or with hints for its first tabs only - still builds.
            var contents = TabsControlLayout.buildTabContents(new ControlSpec.Tabs(
                List.of("No Layer", "Political Map"),
                List.of("N"),
                0,
                ControlAction.NONE));

            assertThat(contents.get(1).shortcut())
                .isEmpty();
        }
    }

    @Nested
    class LayoutHeaderControl {

        @Test
        void layoutHeaderControlHangsTheBandFromTheContentTopAtTheStyledHeight() {
            
            var header = TabsControlLayout.layoutHeaderControl(
                TABS,
                HEADER_X,
                HEADER_TOP_Y,
                TabStyles.buildAtBandHeight(BAND_HEIGHT),
                measurerFake);

            // Flush at the content top with no body inset - a header is framed directly under the border,
            // unlike a body row, which pulls in by the body padding.
            assertThat(header.bounds().y() + header.bounds().height())
                .isCloseTo(HEADER_TOP_Y, within(TOLERANCE));
            assertThat(header.bounds().x())
                .isCloseTo(HEADER_X, within(TOLERANCE));
            assertThat(header.bounds().height())
                .isCloseTo(BAND_HEIGHT, within(TOLERANCE));
        }

        @Test
        void layoutHeaderControlSnapsTheBandToItsTabsSideBySide() {
            // Both keys light a letter of their own label - the N of "No Layer", the P of "Political Map" -
            // so each tab shows its label and nothing more: 8 and 13 chars, each snapped to its width plus
            // the tab padding (both clear the minimum), and the band is the two side by side. A binding
            // that lights in place costs its tab no width, which is the half of the rule the layout owns.
            var header = TabsControlLayout.layoutHeaderControl(
                TABS,
                HEADER_X,
                HEADER_TOP_Y,
                TabStyles.buildAtBandHeight(BAND_HEIGHT),
                measurerFake);

            var first = 8 * WIDTH_PER_CHAR + TabsControlLayout.TAB_TEXT_PADDING;
            var second = 13 * WIDTH_PER_CHAR + TabsControlLayout.TAB_TEXT_PADDING;

            assertThat(header.bounds().width())
                .isCloseTo(first + second, within(TOLERANCE));
        }

        @Test
        void layoutHeaderControlLaysEveryTabToItsStyledBoxWhateverItsLabel() {
            // Vanilla's own box: two 130-wide tabs whatever their labels measure. The two labels differ by
            // 5 characters - 50 measured units apart under this fake - and the boxes do not differ at all,
            // which is the whole of what a fixed row claims over a snapped one.
            var header = TabsControlLayout.layoutHeaderControl(
                TABS,
                HEADER_X,
                HEADER_TOP_Y,
                TabStyles.buildAtBandHeightInBox(VANILLA_BAND_HEIGHT, VANILLA_MAP_TAB),
                measurerFake);

            assertThat(header.segments().get(0).width())
                .isCloseTo(130f, within(TOLERANCE));
            assertThat(header.segments().get(1).width())
                .isCloseTo(130f, within(TOLERANCE));
        }

        @Test
        void layoutHeaderControlPartsNeighbouringTabsByTheStyledChannel() {
            // The leading tab flush at the row's left edge and the second one channel past its right, so
            // the pair matches the engine's own: 40 and 40 + 130 + 1. The channel is stepped over rather
            // than taken from either box, so neither tab narrows to pay for it.
            var header = TabsControlLayout.layoutHeaderControl(
                TABS,
                HEADER_X,
                HEADER_TOP_Y,
                TabStyles.buildAtBandHeightInBox(VANILLA_BAND_HEIGHT, VANILLA_MAP_TAB),
                measurerFake);

            // Literal 171 rather than the constants re-added: an expectation computed from the very
            // numbers handed to the subject agrees with itself however the placement is arithmetically
            // wrong. 40 (the header anchor) + 130 + 1.
            assertThat(header.segments().get(0).x())
                .isCloseTo(HEADER_X, within(TOLERANCE));
            assertThat(header.segments().get(1).x())
                .isCloseTo(171f, within(TOLERANCE));
        }

        @Test
        void layoutHeaderControlSpansTheBoxesAndTheChannelsBetweenThem() {
            // 130 + 1 + 130. The channel counts toward the row because it is room the tabs do not cover,
            // so a panel framing chrome around this row reserves the width the row actually occupies.
            var header = TabsControlLayout.layoutHeaderControl(
                TABS,
                HEADER_X,
                HEADER_TOP_Y,
                TabStyles.buildAtBandHeightInBox(VANILLA_BAND_HEIGHT, VANILLA_MAP_TAB),
                measurerFake);

            assertThat(header.bounds().width())
                .isCloseTo(261f, within(TOLERANCE));
        }

        @Test
        void layoutHeaderControlStandsItsTabsShorterThanTheBandTheyHangIn() {
            // An 18 tab in a 19 band, hanging from the band's top: the spare pixel falls below the tabs,
            // which is where the vanilla row keeps the line its tabs stand on. The band itself is still
            // the full 19, since that is the room the panel gave the row.
            var header = TabsControlLayout.layoutHeaderControl(
                TABS,
                HEADER_X,
                HEADER_TOP_Y,
                TabStyles.buildAtBandHeightInBox(VANILLA_BAND_HEIGHT, VANILLA_MAP_TAB),
                measurerFake);

            assertThat(header.bounds().height())
                .isCloseTo(19f, within(TOLERANCE));
            assertThat(header.segments().get(0).height())
                .isCloseTo(18f, within(TOLERANCE));
            assertThat(header.segments().get(0).y() + header.segments().get(0).height())
                .isCloseTo(HEADER_TOP_Y, within(TOLERANCE));
        }

        @Test
        void layoutHeaderControlSnapsItsTabsAtTheSizeItsStyleLettersThemIn() {
            // A header wears its host's face, so a host on a smaller face would otherwise letter its
            // tabs into boxes cut for the baseline size - text adrift in a row measured for another
            // face. The same 8- and 13-character labels as above, measured at the style's size of 8
            // and each given the 16 of tab padding: 80 and 120, so 200 across. At the layout's own
            // baseline of 15 the row would come out 347 instead.
            var header = TabsControlLayout.layoutHeaderControl(
                TABS,
                HEADER_X,
                HEADER_TOP_Y,
                TabStyles.buildAtBandHeightInFace(BAND_HEIGHT, STYLED_FACE),
                sizeScaledMeasurerFake);

            assertThat(header.bounds().width())
                .isCloseTo(200f, within(TOLERANCE));

            // The segments are asserted beside the row because the two are snapped by separate calls:
            // a split left on the baseline while the row followed the style would measure the band
            // correctly and still hand the renderer boxes the tabs are not drawn in.
            assertThat(header.segments().get(0).width())
                .isCloseTo(80f, within(TOLERANCE));
            assertThat(header.segments().get(1).width())
                .isCloseTo(120f, within(TOLERANCE));
        }

        @Test
        void layoutHeaderControlSplitsEveryTabToTheStyledBandHeight() {
            // The segments are the hit rects the renderer paints; if they kept a fixed height while the
            // band moved, a styled header would be clickable somewhere other than where it is drawn.
            var header = TabsControlLayout.layoutHeaderControl(
                TABS,
                HEADER_X,
                HEADER_TOP_Y,
                TabStyles.buildAtBandHeight(BAND_HEIGHT),
                measurerFake);

            assertThat(header.segments())
                .hasSize(2);

            for (var segment : header.segments()) {

                assertThat(segment.height())
                    .isCloseTo(BAND_HEIGHT, within(TOLERANCE));
                assertThat(segment.y())
                    .isCloseTo(header.bounds().y(), within(TOLERANCE));
            }
        }
    }
}
