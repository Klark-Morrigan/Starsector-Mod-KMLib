package kmlib.starsector.ui.input;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TabPanelController}'s construction seams - the default opens the panel expanded and the
 * docked-start factory opens it collapsed, so a host picks the initial fold through construction rather
 * than driving the animation to reach it - and the hover hit-test that decides which tab a fade is held
 * for. The pointer routing, the scroll delegation, and the per-frame hover advance all run against live
 * input or the live cursor and are exercised in-engine, so what is unit-pinned here is the state a host
 * seeds and the geometry rule the advance is built on.
 */
final class TabPanelControllerTest {
    private static final float TOLERANCE = 0.0001f;

    // A two-tab header laid away from the origin, so a point outside a tab is outside on both axes rather
    // than by a coordinate that happens to be zero.
    private static final Rectangle FIRST_TAB = new Rectangle(100f, 500f, 80f, 20f);
    private static final Rectangle SECOND_TAB = new Rectangle(180f, 500f, 80f, 20f);

    private static final float INSIDE_FIRST_TAB_X = 140f;
    private static final float INSIDE_SECOND_TAB_X = 220f;
    private static final float ON_TAB_ROW_Y = 510f;

    // Below the header band, where the body sits - a point on the panel but on no tab.
    private static final float BELOW_TABS_Y = 400f;

    private static final float BORDER_WIDTH = 1f;

    @Nested
    class Constructor {

        @Test
        void tabPanelControllerOpensFullyExpandedAtZeroCollapseFractionByDefault() {
            assertThat(new TabPanelController().getCollapseFraction()).isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class CreateStartingDocked {

        @Test
        void createStartingDockedOpensFullyCollapsedAtTheDockedRail() {
            assertThat(TabPanelController.createStartingDocked().getCollapseFraction())
                .isCloseTo(1f, within(TOLERANCE));
        }
    }

    @Nested
    class IsFullyExpanded {

        @Test
        void isFullyExpandedIsTrueForTheExpandedDefault() {
            // The expanded default is idle at the open end, so a host's expanded-only hotkeys are live.
            assertThat(new TabPanelController().isFullyExpanded()).isTrue();
        }

        @Test
        void isFullyExpandedIsFalseForADockedStart() {
            // A panel opened docked is not expanded, so its hotkeys stay inert until it is animated open.
            assertThat(TabPanelController.createStartingDocked().isFullyExpanded()).isFalse();
        }
    }

    @Nested
    class GetTabInteractionSources {

        @Test
        void getTabInteractionSourcesHoversNoTabBeforeAnyFrameHasAdvanced() {
            // A freshly built panel has been pointed at nothing, so its first painted frame must show a
            // row at rest rather than a tab already part-way lit.
            assertThat(new TabPanelController()
                    .getTabInteractionSources()
                    .hoverSource()
                    .resolveHoverFractionAt(0))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void getTabInteractionSourcesLiftsNoTabWhileNoPulseAnimatorDrivesIt() {
            assertThat(new TabPanelController()
                    .getTabInteractionSources()
                    .washSource()
                    .resolveWashAt(0)
                    .strength())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class ResolveHoveredTabIndex {

        @Test
        void resolveHoveredTabIndexReturnsTheTabThePointIsOn() {
            assertThat(TabPanelController.resolveHoveredTabIndex(
                    buildTwoTabPlacement(),
                    INSIDE_FIRST_TAB_X,
                    ON_TAB_ROW_Y))
                .isEqualTo(0);
            assertThat(TabPanelController.resolveHoveredTabIndex(
                    buildTwoTabPlacement(),
                    INSIDE_SECOND_TAB_X,
                    ON_TAB_ROW_Y))
                .isEqualTo(1);
        }

        @Test
        void resolveHoveredTabIndexReturnsNoTabForAPointOnTheBody() {
            // A pointer inside the panel but below the header is on no tab, so the whole row winds down
            // rather than the nearest tab staying lit.
            assertThat(TabPanelController.resolveHoveredTabIndex(
                    buildTwoTabPlacement(),
                    INSIDE_FIRST_TAB_X,
                    BELOW_TABS_Y))
                .isNull();
        }

        @Test
        void resolveHoveredTabIndexReturnsNoTabForAPointOffThePanel() {
            assertThat(TabPanelController.resolveHoveredTabIndex(
                    buildTwoTabPlacement(),
                    900f,
                    900f))
                .isNull();
        }
    }

    // A placement carrying only what the hover hit-test reads: a two-tab header control and its per-tab
    // segments. Two tabs rather than one, so an index answered off the row's start reads as a wrong number.
    private static TabPanelPlacement buildTwoTabPlacement() {

        var headerBand = new Rectangle(100f, 500f, 160f, 20f);

        return new TabPanelPlacement(
            new Control(null, headerBand, List.of(FIRST_TAB, SECOND_TAB)),
            new PanelPlacement(headerBand, headerBand, List.of(), headerBand, 0f, 0f),
            new BoxBorder(BORDER_WIDTH),
            null);
    }
}
