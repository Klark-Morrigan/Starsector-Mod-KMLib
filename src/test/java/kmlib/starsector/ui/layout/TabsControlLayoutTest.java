package kmlib.starsector.ui.layout;

import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.font.LineWidthMeasurer;
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

    private static final ControlSpec.Tabs TABS = new ControlSpec.Tabs(
        List.of("No Layer", "Political Map"),
        List.of("N", "P"),
        0,
        ControlAction.NONE);

    private final LineWidthMeasurer measurerFake = new LineWidthMeasurerFake(WIDTH_PER_CHAR);

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
