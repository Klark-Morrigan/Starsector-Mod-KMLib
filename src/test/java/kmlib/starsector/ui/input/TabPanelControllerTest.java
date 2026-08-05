package kmlib.starsector.ui.input;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TabPanelController}'s construction seams - the default opens the panel expanded and the
 * docked-start factory opens it collapsed, so a host picks the initial fold through construction rather
 * than driving the animation to reach it - and the two channels the panel's tabs are painted from: the
 * hit-test that decides which tab a fade is held for, the fades it steps for the tabs and for the collapse
 * handle, the docked gate that silences the tabs while leaving the handle live, and the click pulse a press
 * on a tab starts. The pointer routing and the scroll delegation run against live input events and are
 * exercised in-engine, as is the cursor read the per-frame advance opens with - which is why the advance and
 * the press are pinned through the point that read would have returned rather than through a display there
 * is none of to point at.
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

    // The collapse handle, laid past the header's right edge as the real one is laid past the frame's, and
    // a point inside it. Clear of every tab on both axes, so a point on the handle cannot also read as a
    // point on a tab - which is what makes a crossed-over pairing show as a wrong fade rather than as two
    // fades that happen to agree.
    private static final Rectangle NOTCH = new Rectangle(300f, 440f, 12f, 40f);
    private static final float INSIDE_NOTCH_X = 306f;
    private static final float INSIDE_NOTCH_Y = 460f;

    // Off the header, off the body, and off the handle - the pointer resting on none of the panel's parts.
    private static final float OFF_PANEL_X = 900f;
    private static final float OFF_PANEL_Y = 900f;

    private static final float BORDER_WIDTH = 1f;

    private static final int FIRST_TAB_INDEX = 0;
    private static final int SECOND_TAB_INDEX = 1;

    // What the two hit-tests report when the pointer is on neither element, named so an advance reads as a
    // pointer position rather than as a null and a false.
    private static final Integer NO_TAB_HOVERED = null;
    private static final boolean NOTCH_HOVERED = true;
    private static final boolean NOTCH_NOT_HOVERED = false;

    // A whole duration in one step, so an end state is reached without walking frames, and half of one for
    // the part-way reads.
    private static final float FULL_STEP_SECONDS = 1f;
    private static final float HALF_STEP_SECONDS = 0.5f;
    private static final float DURATION_SECONDS = 1f;

    @Nested
    class Constructor {

        @Test
        void tabPanelControllerOpensFullyExpandedAtZeroCollapseFractionByDefault() {
            assertThat(new TabPanelController().getCollapseFraction())
                .isCloseTo(0f, within(TOLERANCE));
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
            assertThat(new TabPanelController().isFullyExpanded())
                .isTrue();
        }

        @Test
        void isFullyExpandedIsFalseForADockedStart() {
            // A panel opened docked is not expanded, so its hotkeys stay inert until it is animated open.
            assertThat(TabPanelController.createStartingDocked().isFullyExpanded())
                .isFalse();
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
        void getTabInteractionSourcesLiftsNoTabBeforeAnyClickHasLanded() {
            // Nothing has been pressed, so the lift channel must read at rest - a tab brightening on the
            // first frame would mark a click the player never made.
            assertThat(new TabPanelController()
                    .getTabInteractionSources()
                    .pulseSource()
                    .resolvePulseFractionAt(0))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class GetNotchHoverFraction {

        @Test
        void getNotchHoverFractionStartsFullyOffTheLitLook() {
            // A freshly built panel has been pointed at nothing, so its first painted frame must show a
            // handle at rest rather than one already part-way lit.
            assertThat(new TabPanelController().getNotchHoverFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class ActivateTabAtPoint {

        @Test
        void activateTabAtPointFiresThePressedTabsActionAndPulsesThatTab() {
            // The pairing this seam exists to pin: the tab that fires is the tab that lifts. Crossed over,
            // a press would switch to one tab and confirm on another.
            var firedTabs = new ArrayList<Integer>();
            var controller = new TabPanelController();
            var hasActed = controller.activateTabAtPoint(
                buildTwoTabPlacementShowing(FIRST_TAB_INDEX, firedTabs::add),
                INSIDE_SECOND_TAB_X,
                ON_TAB_ROW_Y);

            // One frame charged to the whole panel, then both tabs read off it - two frames would already
            // be winding the pressed tab's lift back down while the other is being asked about.
            advanceAWholeTraverse(controller);

            assertThat(hasActed)
                .isTrue();
            assertThat(firedTabs)
                .containsExactly(SECOND_TAB_INDEX);
            assertThat(pulseFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(pulseFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void activateTabAtPointPulsesNothingForAPressOnTheTabAlreadyShowing() {
            // A tabs row is inert on its lit tab, as a vanilla strip is, so that press fires nothing - and a
            // pulse marks a switch, so a tab that did not switch has nothing to confirm.
            var firedTabs = new ArrayList<Integer>();
            var controller = new TabPanelController();
            var hasActed = controller.activateTabAtPoint(
                buildTwoTabPlacementShowing(FIRST_TAB_INDEX, firedTabs::add),
                INSIDE_FIRST_TAB_X,
                ON_TAB_ROW_Y);

            advanceAWholeTraverse(controller);

            assertThat(hasActed)
                .isFalse();
            assertThat(firedTabs)
                .isEmpty();
            assertThat(pulseFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void activateTabAtPointActsOnNothingForAPressOffTheHeader() {
            // A press on the body falls through to the body controller, so this has to report that it did
            // not act rather than swallowing the press on a tab it never hit.
            var firedTabs = new ArrayList<Integer>();
            var controller = new TabPanelController();
            var hasActed = controller.activateTabAtPoint(
                buildTwoTabPlacementShowing(FIRST_TAB_INDEX, firedTabs::add),
                INSIDE_FIRST_TAB_X,
                BELOW_TABS_Y);

            advanceAWholeTraverse(controller);

            assertThat(hasActed)
                .isFalse();
            assertThat(firedTabs)
                .isEmpty();
            assertThat(pulseFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class AdvanceInputMotionsForHovered {

        @Test
        void advanceInputMotionsForHoveredRaisesTheNamedTabInTheSourceTheRendererReads() {
            // The seam the paint pass actually consumes: a fade stepped here has to surface through the
            // interaction sources, or the strip paints a row that never moves however long it is hovered.
            var controller = new TabPanelController();
            controller.advanceInputMotionsForHovered(
                FIRST_TAB_INDEX,
                NOTCH_NOT_HOVERED,
                FULL_STEP_SECONDS,
                DURATION_SECONDS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(hoverFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForHoveredWindsTheDepartedTabBackDown() {

            var controller = new TabPanelController();

            controller.advanceInputMotionsForHovered(
                FIRST_TAB_INDEX,
                NOTCH_NOT_HOVERED,
                FULL_STEP_SECONDS,
                DURATION_SECONDS);

            controller.advanceInputMotionsForHovered(
                SECOND_TAB_INDEX,
                NOTCH_NOT_HOVERED,
                FULL_STEP_SECONDS,
                DURATION_SECONDS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(hoverFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForHoveredHoversNoTabWhileThePanelIsNotFullyExpanded() {
            // A docked panel's header is behind the rail, so a tab still laid out under the pointer is not
            // one the player can see - the gate drops it however plainly the hit-test named it.
            var controller = TabPanelController.createStartingDocked();
            controller.advanceInputMotionsForHovered(
                FIRST_TAB_INDEX,
                NOTCH_NOT_HOVERED,
                FULL_STEP_SECONDS,
                DURATION_SECONDS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForHoveredLightsTheHandleWhileThePointerIsOnIt() {

            var controller = new TabPanelController();
            controller.advanceInputMotionsForHovered(
                NO_TAB_HOVERED,
                NOTCH_HOVERED,
                FULL_STEP_SECONDS,
                DURATION_SECONDS);

            assertThat(controller.getNotchHoverFraction())
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForHoveredDimsTheHandleOnceThePointerLeavesIt() {

            var controller = new TabPanelController();

            controller.advanceInputMotionsForHovered(
                NO_TAB_HOVERED,
                NOTCH_HOVERED,
                FULL_STEP_SECONDS,
                DURATION_SECONDS);

            controller.advanceInputMotionsForHovered(
                NO_TAB_HOVERED,
                NOTCH_NOT_HOVERED,
                FULL_STEP_SECONDS,
                DURATION_SECONDS);

            assertThat(controller.getNotchHoverFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForHoveredLightsTheHandleWhileThePanelIsDocked() {
            // The handle is the one part of a docked panel still on screen - it is what brings the body
            // back - so the gate that silences the tabs must not reach it.
            var controller = TabPanelController.createStartingDocked();
            controller.advanceInputMotionsForHovered(
                NO_TAB_HOVERED,
                NOTCH_HOVERED,
                FULL_STEP_SECONDS,
                DURATION_SECONDS);

            assertThat(controller.getNotchHoverFraction())
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForHoveredRunsAClickPulseBackOutWithNoPointerOnTheTab() {
            // A pulse takes no pointer: the press that started it has been and gone, so its cycle has to
            // run its course with the cursor anywhere at all, and finish without a second event.
            var controller = new TabPanelController();

            controller.activateTabAtPoint(
                buildTwoTabPlacementShowing(FIRST_TAB_INDEX, tabIndex -> { }),
                INSIDE_SECOND_TAB_X,
                ON_TAB_ROW_Y);

            controller.advanceInputMotionsForHovered(
                NO_TAB_HOVERED,
                NOTCH_NOT_HOVERED,
                FULL_STEP_SECONDS,
                DURATION_SECONDS);

            controller.advanceInputMotionsForHovered(
                NO_TAB_HOVERED,
                NOTCH_NOT_HOVERED,
                FULL_STEP_SECONDS,
                DURATION_SECONDS);

            assertThat(pulseFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class AdvanceInputMotionsAtPoint {

        @Test
        void advanceInputMotionsAtPointLightsTheTabUnderThePointerAndNotTheHandle() {
            // The pairing this seam exists to pin: the header hit-test feeds the tab fades. Crossed over,
            // a pointer on a tab would light the handle and every assertion below would still pass.
            var controller = new TabPanelController();
            controller.advanceInputMotionsAtPoint(
                buildTwoTabPlacementWithNotch(),
                INSIDE_FIRST_TAB_X,
                ON_TAB_ROW_Y,
                FULL_STEP_SECONDS,
                DURATION_SECONDS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(controller.getNotchHoverFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsAtPointLightsTheHandleUnderThePointerAndNoTab() {
            // The other half of the pairing: the notch hit-test feeds the lone fade. The handle's rect is
            // clear of every tab, so a tab lighting here could only come from the wrong hit-test.
            var controller = new TabPanelController();
            controller.advanceInputMotionsAtPoint(
                buildTwoTabPlacementWithNotch(),
                INSIDE_NOTCH_X,
                INSIDE_NOTCH_Y,
                FULL_STEP_SECONDS,
                DURATION_SECONDS);

            assertThat(controller.getNotchHoverFraction())
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(hoverFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsAtPointLightsNothingForAPointerOffThePanel() {

            var controller = new TabPanelController();
            controller.advanceInputMotionsAtPoint(
                buildTwoTabPlacementWithNotch(),
                OFF_PANEL_X,
                OFF_PANEL_Y,
                FULL_STEP_SECONDS,
                DURATION_SECONDS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(controller.getNotchHoverFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsAtPointLightsNoHandleOnABodylessPanelThatHasNone() {
            // A panel with nothing to fold carries no handle rect; the placement's own test absorbs that,
            // so a pointer anywhere over such a panel must leave the fade at rest rather than throwing.
            var controller = new TabPanelController();
            controller.advanceInputMotionsAtPoint(
                buildTwoTabPlacement(),
                INSIDE_NOTCH_X,
                INSIDE_NOTCH_Y,
                FULL_STEP_SECONDS,
                DURATION_SECONDS);

            assertThat(controller.getNotchHoverFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class ResetInputMotions {

        @Test
        void resetInputMotionsDropsATabFadeLeftPartWayUpWhenThePanelStopsShowing() {
            // Otherwise the next session opens painting the tail of a hover the player never saw begin.
            var controller = new TabPanelController();

            controller.advanceInputMotionsForHovered(
                FIRST_TAB_INDEX,
                NOTCH_NOT_HOVERED,
                HALF_STEP_SECONDS,
                DURATION_SECONDS);

            controller.resetInputMotions();

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void resetInputMotionsDropsTheHandlesFadeLeftPartWayUpWhenThePanelStopsShowing() {
            // The handle is dropped for the same reason and in the same call, so a panel re-opened under a
            // still pointer cannot paint one part lit and the other at rest.
            var controller = new TabPanelController();

            controller.advanceInputMotionsForHovered(
                NO_TAB_HOVERED,
                NOTCH_HOVERED,
                HALF_STEP_SECONDS,
                DURATION_SECONDS);

            controller.resetInputMotions();

            assertThat(controller.getNotchHoverFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void resetInputMotionsDropsAClickPulseLeftPartWayThroughItsCycle() {
            // A pulse is dropped in the same call for the same reason: the next session would otherwise
            // open decaying from a peak the player never saw rise.
            var controller = new TabPanelController();

            controller.activateTabAtPoint(
                buildTwoTabPlacementShowing(FIRST_TAB_INDEX, tabIndex -> { }),
                INSIDE_SECOND_TAB_X,
                ON_TAB_ROW_Y);

            controller.advanceInputMotionsForHovered(
                NO_TAB_HOVERED,
                NOTCH_NOT_HOVERED,
                HALF_STEP_SECONDS,
                DURATION_SECONDS);
                
            controller.resetInputMotions();

            assertThat(pulseFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class ResolveTabIndexAtPoint {

        @Test
        void resolveTabIndexAtPointReturnsTheTabThePointIsOn() {
            assertThat(TabPanelController.resolveTabIndexAtPoint(
                    buildTwoTabPlacement(),
                    INSIDE_FIRST_TAB_X,
                    ON_TAB_ROW_Y))
                .isEqualTo(0);
            assertThat(TabPanelController.resolveTabIndexAtPoint(
                    buildTwoTabPlacement(),
                    INSIDE_SECOND_TAB_X,
                    ON_TAB_ROW_Y))
                .isEqualTo(1);
        }

        @Test
        void resolveTabIndexAtPointReturnsNoTabForAPointOnTheBody() {
            // A pointer inside the panel but below the header is on no tab, so the whole row winds down
            // rather than the nearest tab staying lit.
            assertThat(TabPanelController.resolveTabIndexAtPoint(
                    buildTwoTabPlacement(),
                    INSIDE_FIRST_TAB_X,
                    BELOW_TABS_Y))
                .isNull();
        }

        @Test
        void resolveTabIndexAtPointReturnsNoTabForAPointOffThePanel() {
            assertThat(TabPanelController.resolveTabIndexAtPoint(
                    buildTwoTabPlacement(),
                    OFF_PANEL_X,
                    OFF_PANEL_Y))
                .isNull();
        }
    }

    // How far a tab has faded, read the way the render pass reads it - through the interaction sources
    // rather than off the fades directly, so these pin the value that actually reaches a strip.
    private static float hoverFractionAt(TabPanelController controller, int tabIndex) {
        return controller
            .getTabInteractionSources()
            .hoverSource()
            .resolveHoverFractionAt(tabIndex);
    }

    // How far a tab's click lift has run, read through the same sources for the same reason.
    private static float pulseFractionAt(TabPanelController controller, int tabIndex) {
        return controller
            .getTabInteractionSources()
            .pulseSource()
            .resolvePulseFractionAt(tabIndex);
    }

    // A triggered lift stands at nothing until a frame charges it, so the cases asking whether a press
    // started one step the panel a whole traverse first - which puts a started pulse at its peak and leaves
    // an unstarted one at rest, telling the two apart in one number. The pointer is on nothing, so a fade
    // cannot be mistaken for the lift being asked about.
    private static void advanceAWholeTraverse(TabPanelController controller) {
        controller.advanceInputMotionsForHovered(
            NO_TAB_HOVERED,
            NOTCH_NOT_HOVERED,
            FULL_STEP_SECONDS,
            DURATION_SECONDS);
    }

    // A placement with no collapse handle - the bodyless panel's shape, and all the tab hit-test needs.
    private static TabPanelPlacement buildTwoTabPlacement() {
        return buildPlacement(null, null);
    }

    // The same panel carrying a handle, for the hit-tests that have to tell the panel's two parts apart.
    private static TabPanelPlacement buildTwoTabPlacementWithNotch() {
        return buildPlacement(NOTCH, null);
    }

    // The same panel whose header carries a real tabs spec, for the press path - which fires the spec's own
    // action and is inert on the tab the spec says is already showing, neither of which a header with no
    // spec can express.
    private static TabPanelPlacement buildTwoTabPlacementShowing(
            int selectedIndex,
            ControlAction onTabFired) {

        return buildPlacement(
            NOTCH,
            new ControlSpec.Tabs(
                List.of("First", "Second"),
                List.of(),
                selectedIndex,
                onTabFired));
    }

    // A placement carrying what the hit-tests read: a two-tab header control with its per-tab segments, and
    // whichever handle and spec the caller wants it to have. Two tabs rather than one, so an index answered
    // off the row's start reads as a wrong number; one builder for every case, so the placements cannot
    // drift apart in any other respect.
    private static TabPanelPlacement buildPlacement(Rectangle notch, ControlSpec spec) {

        var headerBand = new Rectangle(100f, 500f, 160f, 20f);

        return new TabPanelPlacement(
            new Control(spec, headerBand, List.of(FIRST_TAB, SECOND_TAB)),
            new PanelPlacement(headerBand, headerBand, List.of(), headerBand, 0f, 0f),
            new BoxBorder(BORDER_WIDTH),
            notch);
    }
}
