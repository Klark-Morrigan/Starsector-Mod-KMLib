package kmlib.starsector.ui.input;

import kmlib.animation.TraverseDurations;
import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.LabelledControlSpecs;
import kmlib.starsector.ui.controls.VerticalTableSpecs;
import kmlib.starsector.ui.sound.PointerArrivalTarget;
import kmlib.starsector.ui.sound.PointerArrivalVolumes;
import kmlib.starsector.ui.sound.StarsectorUiSound;
import kmlib.starsector.ui.sound.UiSoundCue;
import kmlib.starsector.ui.sound.UiSoundScheme;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;
import kmlib.testfixtures.starsector.ui.sound.UiSoundPlayerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TabPanelController}'s construction seams - the default opens the panel expanded and the
 * docked-start factory opens it collapsed, so a host picks the initial fold through construction rather
 * than driving the animation to reach it - and the two channels the panel's tabs are painted from: the one
 * resolver that decides which tab a fade is held for and which tab a press landed on, the fades it steps for
 * the tabs, for the body's own cells - keyed by the slot each occupies in the strip, so a fade follows the
 * place under the pointer rather than the widget a rebuild puts there - and for the collapse handle, the
 * docked gate that resolver carries - silencing the tabs while
 * leaving the handle live - the click pulse a press on a tab starts, and the blink a bound key's press runs
 * on the look channel beside the hover it shares that channel with. It also pins what the panel answers those moments with: which sounds, at which moments,
 * and - the point of that seam - taken from the look the panel wears rather than named in its own code.
 * The pointer routing and the scroll delegation run against live input events and are
 * exercised in-engine, as is the cursor read the per-frame advance opens with - which is why the advance and
 * the press are pinned through the point that read would have returned rather than through a display there
 * is none of to point at.
 */
final class TabPanelControllerTest {

    private static final float TOLERANCE = 0.0001f;

    // A two-tab header laid away from the origin, so a point outside a tab is outside on both axes rather
    // than by a coordinate that happens to be zero. The band spans both tabs, as a drawn row does.
    private static final Rectangle FIRST_TAB = new Rectangle(100f, 500f, 80f, 20f);
    private static final Rectangle SECOND_TAB = new Rectangle(180f, 500f, 80f, 20f);
    private static final Rectangle HEADER_BAND = new Rectangle(100f, 500f, 160f, 20f);

    // The framed body, standing beneath the row and meeting its bottom edge, as the layout lays it. Apart
    // from the row on the y axis, so a point on the tabs is a point the body does not also claim - which is
    // what tells the row's own answer apart from the body's.
    private static final Rectangle BODY_BOX = new Rectangle(100f, 380f, 160f, 120f);

    // What the fold leaves of that box once the panel is fully docked: a border's width at the left anchor,
    // the two frame edges met into one vertical line. Clear of every point the body cases test, so a control
    // still laid across BODY_BOX is one the fold has wiped off the screen.
    private static final Rectangle DOCKED_RAIL_BOX =
        new Rectangle(BODY_BOX.x(), BODY_BOX.y(), 1f, BODY_BOX.height());

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

    // A row of the scrolling list the wheel cases lay in the body, one wheel notch tall. That the two match
    // is the point: a case hands in the placement the drawn frame would carry after the wheel, so one notch
    // has to carry exactly one row past the pointer for the before and after to be the layout's own.
    private static final float SCROLLING_ROW_HEIGHT = 40f;

    // Four such rows against a body showing three, so the list has exactly one row's worth to give.
    private static final int SCROLLING_ROW_COUNT = 4;
    private static final float SCROLLING_LIST_OVERFLOW = SCROLLING_ROW_HEIGHT;

    // The list at rest and after one notch, which is also its end - so a second notch has nowhere to go.
    private static final float UNSCROLLED_OFFSET = 0f;
    private static final float SCROLLED_BY_ONE_ROW_OFFSET = SCROLLING_ROW_HEIGHT;

    // The middle of the list's top row while it is at rest, and of its second row once one row has gone
    // past. One point standing for a still cursor, which is what a case about content moving under one
    // needs: the reading changes without the pointer having moved at all.
    private static final float ON_TOP_SCROLLING_ROW_Y =
        BODY_BOX.y() + BODY_BOX.height() - SCROLLING_ROW_HEIGHT / 2f;

    private static final float BORDER_WIDTH = 1f;

    private static final int FIRST_TAB_INDEX = 0;
    private static final int SECOND_TAB_INDEX = 1;

    // The header spec every placement carries unless its case needs an action recorded or another tab lit.
    // A header is an ordinary laid-out tabs control, so the resolver reads the row's selection off a spec
    // like this one rather than off the placement - which is why none of these placements can go without.
    private static final ControlSpec.Tabs TABS_SHOWING_FIRST_TAB =
        buildTabsSpecShowing(FIRST_TAB_INDEX, ControlAction.NONE);

    // What the hit-tests report when the pointer is on none of the panel's parts, named so an advance reads
    // as a pointer position rather than as two nulls and a false.
    private static final Integer NO_TAB_HOVERED = null;
    private static final HoveredBodyCell NO_BODY_CELL_HOVERED = null;
    private static final boolean NOTCH_HOVERED = true;
    private static final boolean NOTCH_NOT_HOVERED = false;

    // The strip's only body control, laid at BODY_BOX by the placements below, and the cell of it a pointer
    // over the body lands on. Named rather than built at each use, since a slot is what the body's fades are
    // keyed by and two cases naming it differently would pass while agreeing about nothing.
    private static final BodyCellSlot FIRST_BODY_SLOT = new BodyCellSlot(0, ControlSpec.SINGLE_CELL);

    // A slot no placement here lays a control at, for the cases reading a cell the pointer is not on.
    private static final BodyCellSlot SECOND_BODY_SLOT = new BodyCellSlot(1, ControlSpec.SINGLE_CELL);

    // A segment part-way down a strip, its two halves deliberately different numbers: the slots above name
    // a single-cell control, whose cell is zero, so a reading that crossed the place with the cell would
    // answer the same fade and every case using them would still pass.
    private static final BodyCellSlot MID_STRIP_SEGMENT_SLOT = new BodyCellSlot(2, 1);

    // Those slots as the pointer reads them - the place a fade is held against, and the kind of thing the
    // player reached. The kinds match the slots they are paired with, a whole-row control's single cell
    // being what the first two name and a segment what the third does, so a case reading either half sees
    // the pairing the panel's own walk would have produced.
    private static final HoveredBodyCell FIRST_BODY_CELL =
        new HoveredBodyCell(FIRST_BODY_SLOT, PointerArrivalTarget.SINGLE_OPTION_CONTROL);
    private static final HoveredBodyCell SECOND_BODY_CELL =
        new HoveredBodyCell(SECOND_BODY_SLOT, PointerArrivalTarget.SINGLE_OPTION_CONTROL);
    private static final HoveredBodyCell MID_STRIP_SEGMENT_CELL =
        new HoveredBodyCell(MID_STRIP_SEGMENT_SLOT, PointerArrivalTarget.LISTED_ITEM);

    // A whole duration in one step, so an end state is reached without walking frames, and half of one for
    // the part-way reads.
    private static final float FULL_STEP_SECONDS = 1f;
    private static final float HALF_STEP_SECONDS = 0.5f;
    private static final float DURATION_SECONDS = 1f;

    // The same pace each way, so a step reads as a fraction of one duration whichever direction the motion
    // it charges is heading. Which way a motion travels at which pace is pinned where the pair is read -
    // on the fade and the envelope - and the panel's own job is only to hand every travel the same pair.
    private static final TraverseDurations DURATIONS =
        TraverseDurations.createSymmetric(DURATION_SECONDS);

    // Half way down the blink's own fall, which is where a decaying strike is read against a fade rising
    // under it. Off the panel's own pace, since the blink does not answer to the pair above.
    private static final float HALF_OF_THE_BLINKS_FALL_SECONDS =
        TabPanelController.HOTKEY_BLINK_DURATIONS.fallSeconds() / 2f;

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
    class GetInteractionSources {

        @Test
        void getInteractionSourcesCarriesTheLiveChannelsOfBothHalves() {
            // The pair is what a consumer draws from, so what it hands over has to be the panel's live
            // state and not a resting stand-in: a half wired to a fresh source would paint a row and a
            // strip that disagree about where the pointer is, which is the whole reason they travel
            // together.
            var controller = new TabPanelController();
            controller.advanceInputMotionsForFrame(
                new TabPanelHover(FIRST_TAB_INDEX, MID_STRIP_SEGMENT_CELL, NOTCH_NOT_HOVERED),
                FULL_STEP_SECONDS,
                DURATIONS);

            var interactions = controller.getInteractionSources();

            assertThat(interactions.headerTabs().hoverSource().resolveHoverFractionAt(FIRST_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(interactions.bodyControls()
                    .resolveControlHoverSourceAt(MID_STRIP_SEGMENT_SLOT.controlIndex())
                    .resolveHoverFractionAt(MID_STRIP_SEGMENT_SLOT.cell()))
                .isCloseTo(1f, within(TOLERANCE));
        }
    }

    @Nested
    class GetBodyHoverSource {

        @Test
        void getBodyHoverSourceHoversNoCellBeforeAnyFrameHasAdvanced() {
            // A freshly built panel has been pointed at nothing, so its first painted frame must show a
            // strip at rest rather than a control already part-way lit.
            assertThat(new TabPanelController()
                    .getBodyHoverSource()
                    .resolveControlHoverSourceAt(MID_STRIP_SEGMENT_SLOT.controlIndex())
                    .resolveHoverFractionAt(MID_STRIP_SEGMENT_SLOT.cell()))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void getBodyHoverSourceAnswersTheFadeHeldForThatSlot() {
            // The two halves of a slot arrive one at a time - the strip walk binds the control's place and
            // the widget passes the cell it is painting - so the pair the source puts back together has to
            // be the pair the fade is keyed by.
            var controller = new TabPanelController();
            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, MID_STRIP_SEGMENT_CELL, NOTCH_NOT_HOVERED),
                FULL_STEP_SECONDS,
                DURATIONS);

            assertThat(controller
                    .getBodyHoverSource()
                    .resolveControlHoverSourceAt(MID_STRIP_SEGMENT_SLOT.controlIndex())
                    .resolveHoverFractionAt(MID_STRIP_SEGMENT_SLOT.cell()))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void getBodyHoverSourceLeavesTheTransposedSlotUnlit() {
            // The guard the two-step seam exists for: a control's place and a cell of it are both ints, so
            // a binding that crossed them would light a cell of the wrong control - and would pass the case
            // above, which names a slot whose halves differ.
            var controller = new TabPanelController();
            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, MID_STRIP_SEGMENT_CELL, NOTCH_NOT_HOVERED),
                FULL_STEP_SECONDS,
                DURATIONS);

            assertThat(controller
                    .getBodyHoverSource()
                    .resolveControlHoverSourceAt(MID_STRIP_SEGMENT_SLOT.cell())
                    .resolveHoverFractionAt(MID_STRIP_SEGMENT_SLOT.controlIndex()))
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
    class IsPresentingTabsOf {

        @Test
        void isPresentingTabsOfAnswersYesForAnExpandedPanel() {
            assertThat(new TabPanelController().isPresentingTabsOf(buildTwoTabPlacement()))
                .isTrue();
        }

        @Test
        void isPresentingTabsOfAnswersNoForADockedPanelWithABody() {
            // Its header is behind the rail, so its tabs are not there to be pressed, lit, or keyed to.
            assertThat(TabPanelController.createStartingDocked()
                    .isPresentingTabsOf(buildTwoTabPlacement()))
                .isFalse();
        }

        @Test
        void isPresentingTabsOfAnswersYesForABodylessPanelWhateverTheFoldSays() {
            // The fold outlives a tab switch, so a bodyless tab can be laid out under a fraction another
            // tab's body left standing. It has nothing to fold and no handle to unfold it, so acting on
            // that fraction would leave its row drawn in full but dead to every press, pointer and key -
            // with nothing on screen to explain it and no way back.
            assertThat(TabPanelController.createStartingDocked()
                    .isPresentingTabsOf(buildBodylessTwoTabPlacement()))
                .isTrue();
        }
    }

    @Nested
    class HandlePointer {

        @Test
        void handlePointerSwallowsAnEventOverTheDrawnTabRow() {
            // The row is drawn outside the body's box, so without this the surface behind the panel would
            // go on reading a pointer the player has parked on the tabs - and a tab row with no body under
            // it, which is the whole of such a panel, would block nothing at all.
            var eventMock = PointerEventMocks.mockPointerEventAt(INSIDE_FIRST_TAB_X, ON_TAB_ROW_Y);

            new TabPanelController().handlePointer(eventMock, buildTwoTabPlacement());

            Mockito
                .verify(eventMock)
                .consume();
        }

        @Test
        void handlePointerLeavesAnEventOffThePanelAlone() {
            // Off every part of it the panel claims nothing, so the map underneath keeps answering the
            // pointer as it did before the panel was there.
            var eventMock = PointerEventMocks.mockPointerEventAt(OFF_PANEL_X, OFF_PANEL_Y);

            new TabPanelController().handlePointer(eventMock, buildTwoTabPlacement());

            Mockito
                .verify(eventMock, Mockito.never())
                .consume();
        }

        @Test
        void handlePointerLeavesAnEventOverAWipedTabRowAlone() {
            // Mid-fold the drawn band is narrower than the row was laid out; the panel claims only what it
            // still paints, so the screen its tabs have wiped off goes back to whatever is behind.
            var eventMock = PointerEventMocks.mockPointerEventAt(INSIDE_SECOND_TAB_X, ON_TAB_ROW_Y);

            new TabPanelController().handlePointer(eventMock, buildPlacementWithDrawnBand(FIRST_TAB));

            Mockito
                .verify(eventMock, Mockito.never())
                .consume();
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
        void activateTabAtPointLiftsTheTabAlreadyShowingWithoutFiringIt() {
            // Where the lift and the action part. A tabs row is inert on its lit tab, as a vanilla strip is,
            // so the press fires nothing; but the press was still an act the player made, and a tab that
            // answered it with nothing at all would read as a panel that missed the click.
            var firedTabs = new ArrayList<Integer>();
            var controller = new TabPanelController();
            var hasActed = controller.activateTabAtPoint(
                buildTwoTabPlacementShowing(FIRST_TAB_INDEX, firedTabs::add),
                INSIDE_FIRST_TAB_X,
                ON_TAB_ROW_Y);

            advanceAWholeTraverse(controller);

            assertThat(hasActed)
                .isTrue();
            assertThat(firedTabs)
                .isEmpty();
            assertThat(pulseFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void activateTabAtPointActsOnNothingWhileTheTabsAreNotPresented() {
            // The gate that keeps a docked panel from acting on bare screen. Folding only clips the header
            // at paint time - its tabs keep the hit boxes they were laid at - so without this a press where
            // a tab used to be would fire that tab and swallow the click with nothing drawn to explain it.
            var firedTabs = new ArrayList<Integer>();
            var controller = TabPanelController.createStartingDocked();
            var hasActed = controller.activateTabAtPoint(
                buildTwoTabPlacementShowing(FIRST_TAB_INDEX, firedTabs::add),
                INSIDE_SECOND_TAB_X,
                ON_TAB_ROW_Y);

            advanceAWholeTraverse(controller);

            // Reporting that it did not act is what leaves the press to the body, and through it to the
            // screen behind - so the click reaches the map rather than being consumed by an unseen tab.
            assertThat(hasActed)
                .isFalse();
            assertThat(firedTabs)
                .isEmpty();
            assertThat(pulseFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void activateTabAtPointFiresABodylessPanelsTabWhateverTheFoldSays() {
            // The same docked controller, now laying out a tab with nothing under it: the row is drawn in
            // full, so a press on it has to act. Gated on the fold alone this tab would be unpressable for
            // the rest of the session, with no handle to expand a body it does not have.
            var firedTabs = new ArrayList<Integer>();
            var controller = TabPanelController.createStartingDocked();
            var hasActed = controller.activateTabAtPoint(
                buildBodylessTwoTabPlacementShowing(FIRST_TAB_INDEX, firedTabs::add),
                INSIDE_SECOND_TAB_X,
                ON_TAB_ROW_Y);

            assertThat(hasActed)
                .isTrue();
            assertThat(firedTabs)
                .containsExactly(SECOND_TAB_INDEX);
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
    class StartHotkeyBlinkAt {

        @Test
        void startHotkeyBlinkAtCarriesTheNamedTabOntoTheHoveredShade() {
            // The blink is read on the look channel, so a key's press shows on the tab it is bound to with
            // the pointer nowhere near it - which is the whole point of marking a keyboard switch.
            var controller = new TabPanelController();
            controller.startHotkeyBlinkAt(SECOND_TAB_INDEX);

            advanceAWholeTraverse(controller);

            assertThat(hoverFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void startHotkeyBlinkAtLiftsNothingOnTheWashChannel() {
            // A blink travels onto the hovered shade rather than past it, so it must leave the lift channel
            // alone - read there as well, it would brighten the tab twice and outshine a click.
            var controller = new TabPanelController();
            controller.startHotkeyBlinkAt(SECOND_TAB_INDEX);

            advanceAWholeTraverse(controller);

            assertThat(pulseFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void startHotkeyBlinkAtStrikesAtItsOwnPaceRatherThanThePanelsTravelPace() {
            // The whole of this pace: a strike confirms a key pressed away from the panel, so it lands and
            // is gone however leisurely the host is stepping the panel's travels. Stepped by the blink's own
            // rise while the travel pair says a traverse takes a whole second - on the travel pair this same
            // step would leave the tab barely off its resting shade.
            var controller = new TabPanelController();
            controller.startHotkeyBlinkAt(FIRST_TAB_INDEX);

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
                TabPanelController.HOTKEY_BLINK_DURATIONS.riseSeconds(),
                DURATIONS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void startHotkeyBlinkAtAddsNothingToATabTheHoverAlreadyHoldsFullyLit() {
            // The composition rule: the greater of the two, not their sum. Both stand fully on the shade
            // here, so a summed channel would read twice over and only the greater reads the one shade
            // either motion aims at - which is what makes a key pressed for the hovered tab a no-op.
            var controller = new TabPanelController();

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(FIRST_TAB_INDEX, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
                FULL_STEP_SECONDS,
                DURATIONS);

            controller.startHotkeyBlinkAt(FIRST_TAB_INDEX);
            controller.advanceInputMotionsForFrame(
                new TabPanelHover(FIRST_TAB_INDEX, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
                TabPanelController.HOTKEY_BLINK_DURATIONS.riseSeconds(),
                DURATIONS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void startHotkeyBlinkAtKeepsTheTabOnTheBlinkUntilAnArrivingHoverOvertakesIt() {
            // The other side of the same rule, and the one that says which motion the greater picks: neither
            // is aware of the other, so a pointer arriving at the blink's peak reads the blink's decay - not
            // its own fade starting from rest - until the two cross. Read half way down the blink's fall,
            // where the fade it is being read over stands at 0.028 of its own rise.
            var controller = new TabPanelController();
            controller.startHotkeyBlinkAt(FIRST_TAB_INDEX);

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
                TabPanelController.HOTKEY_BLINK_DURATIONS.riseSeconds(),
                DURATIONS);

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(FIRST_TAB_INDEX, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
                HALF_OF_THE_BLINKS_FALL_SECONDS,
                DURATIONS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void startHotkeyBlinkAtRunsItsBlinkBackOutWithNoFurtherPress() {
            // A blink is one in-and-out cycle from a single trigger, so the tab has to fall back to its own
            // look on its own - left held, a key press would light a tab until something else moved it. Its
            // own rise and fall are the whole of what that takes.
            var controller = new TabPanelController();
            controller.startHotkeyBlinkAt(SECOND_TAB_INDEX);

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
                TabPanelController.HOTKEY_BLINK_DURATIONS.riseSeconds(),
                DURATIONS);

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
                TabPanelController.HOTKEY_BLINK_DURATIONS.fallSeconds(),
                DURATIONS);

            assertThat(hoverFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void startHotkeyBlinkAtRunsItsCycleOutWhileThePanelIsDocked() {
            // Unlike a hover, a blink takes no docked gate: it is an event already seen, so a fold arriving
            // mid-cycle must let it finish rather than cutting it off part-way lit.
            var controller = TabPanelController.createStartingDocked();
            controller.startHotkeyBlinkAt(SECOND_TAB_INDEX);

            advanceAWholeTraverse(controller);

            assertThat(hoverFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
        }
    }

    @Nested
    class AdvanceInputMotionsForFrame {

        @Test
        void advanceInputMotionsForFrameRaisesTheNamedTabInTheSourceTheRendererReads() {
            // The seam the paint pass actually consumes: a fade stepped here has to surface through the
            // interaction sources, or the strip paints a row that never moves however long it is hovered.
            var controller = new TabPanelController();
            controller.advanceInputMotionsForFrame(
                new TabPanelHover(FIRST_TAB_INDEX, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
                FULL_STEP_SECONDS,
                DURATIONS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(hoverFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForFrameWindsTheDepartedTabBackDown() {

            var controller = new TabPanelController();

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(FIRST_TAB_INDEX, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
                FULL_STEP_SECONDS,
                DURATIONS);

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(SECOND_TAB_INDEX, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
                FULL_STEP_SECONDS,
                DURATIONS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(hoverFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForFrameLightsWhateverTabItIsHandedEvenWhileDocked() {
            // The frame advance takes the pointer's position as settled: whether the panel is presenting
            // its tabs is decided where the placement is known, so a tab named here is a tab to light. The
            // fold is asked once, not twice.
            var controller = TabPanelController.createStartingDocked();
            controller.advanceInputMotionsForFrame(
                new TabPanelHover(FIRST_TAB_INDEX, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
                FULL_STEP_SECONDS,
                DURATIONS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForFrameRaisesTheNamedBodySlotAndNoOther() {
            // The body's cells travel on the panel's own pair of paces, like the row above them: one frame
            // of a whole traverse puts the hovered slot fully on its hovered look and leaves every other
            // slot at rest.
            var controller = new TabPanelController();
            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, FIRST_BODY_CELL, NOTCH_NOT_HOVERED),
                FULL_STEP_SECONDS,
                DURATIONS);

            assertThat(controller.resolveBodyHoverFractionAt(FIRST_BODY_SLOT))
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(controller.resolveBodyHoverFractionAt(SECOND_BODY_SLOT))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForFrameWindsTheDepartedBodySlotBackDown() {

            var controller = new TabPanelController();

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, FIRST_BODY_CELL, NOTCH_NOT_HOVERED),
                FULL_STEP_SECONDS,
                DURATIONS);

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, SECOND_BODY_CELL, NOTCH_NOT_HOVERED),
                FULL_STEP_SECONDS,
                DURATIONS);

            assertThat(controller.resolveBodyHoverFractionAt(FIRST_BODY_SLOT))
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(controller.resolveBodyHoverFractionAt(SECOND_BODY_SLOT))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForFrameKeepsTheBodySlotsFadesApartFromTheTabsOwn() {
            // The two rows are keyed in different terms and held apart, so a slot and a tab index that
            // happen to name the same number cannot read as one another. Held in one set, the body cell of
            // control 0 and tab 0 would light together and every case above would still pass.
            var controller = new TabPanelController();
            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, FIRST_BODY_CELL, NOTCH_NOT_HOVERED),
                FULL_STEP_SECONDS,
                DURATIONS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .as("a hovered body cell must leave the row above it at rest")
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForFrameLightsTheHandleWhileThePointerIsOnIt() {

            var controller = new TabPanelController();
            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, NO_BODY_CELL_HOVERED, NOTCH_HOVERED),
                FULL_STEP_SECONDS,
                DURATIONS);

            assertThat(controller.getNotchHoverFraction())
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForFrameDimsTheHandleOnceThePointerLeavesIt() {

            var controller = new TabPanelController();

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, NO_BODY_CELL_HOVERED, NOTCH_HOVERED),
                FULL_STEP_SECONDS,
                DURATIONS);

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
                FULL_STEP_SECONDS,
                DURATIONS);

            assertThat(controller.getNotchHoverFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForFrameLightsTheHandleWhileThePanelIsDocked() {
            // The handle is the one part of a docked panel still on screen - it is what brings the body
            // back - so the gate that silences the tabs must not reach it.
            var controller = TabPanelController.createStartingDocked();
            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, NO_BODY_CELL_HOVERED, NOTCH_HOVERED),
                FULL_STEP_SECONDS,
                DURATIONS);

            assertThat(controller.getNotchHoverFraction())
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForFrameHoldsAPressLiftAtItsPeakWhileTheButtonIsDown() {
            // The whole of what a held lift is: the press has not ended, so the lift may not either, however
            // many frames pass with the cursor anywhere at all. A lift that timed its own fall would drop
            // out from under a button the player is still holding.
            var controller = new TabPanelController();

            controller.activateTabAtPoint(
                buildTwoTabPlacementShowing(FIRST_TAB_INDEX, tabIndex -> { }),
                INSIDE_SECOND_TAB_X,
                ON_TAB_ROW_Y);

            advanceAWholeTraverse(controller);
            advanceAWholeTraverse(controller);

            assertThat(pulseFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForFrameRunsAPressLiftBackOutOnceTheButtonIsReleased() {
            // The release is what ends it, and it takes no pointer of its own: the lift falls with the
            // cursor anywhere at all, and finishes without a third event.
            var controller = new TabPanelController();
            var placement = buildTwoTabPlacementShowing(FIRST_TAB_INDEX, tabIndex -> { });

            controller.activateTabAtPoint(placement, INSIDE_SECOND_TAB_X, ON_TAB_ROW_Y);
            advanceAWholeTraverse(controller);

            controller.handlePointer(PointerEventMocks.mockLeftReleaseAt(OFF_PANEL_X, OFF_PANEL_Y), placement);

            // Two frames, because the release only lets go: the frame after it turns the lift at the peak
            // and the frame after that runs it down. The turn stays in the advance rather than moving into
            // the release, so a lift changes direction in one place whatever ended it.
            advanceAWholeTraverse(controller);
            advanceAWholeTraverse(controller);

            assertThat(pulseFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForFrameEndsAPressLiftReleasedAwayFromItsOwnTab() {
            // A press begun on a tab and let go somewhere else entirely - over a neighbour, off the panel -
            // still ends that tab's lift, the act it reported being the press rather than where the pointer
            // finished up. Released by where the cursor landed, this lift would stand at its peak until the
            // panel itself was dropped.
            var controller = new TabPanelController();
            var placement = buildTwoTabPlacementShowing(FIRST_TAB_INDEX, tabIndex -> { });

            controller.activateTabAtPoint(placement, INSIDE_SECOND_TAB_X, ON_TAB_ROW_Y);
            
            advanceAWholeTraverse(controller);

            controller.handlePointer(PointerEventMocks.mockLeftReleaseAt(INSIDE_FIRST_TAB_X, ON_TAB_ROW_Y), placement);

            advanceAWholeTraverse(controller);
            advanceAWholeTraverse(controller);

            assertThat(pulseFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsForFrameChargesAPressLiftRunningOnABodyCell() {
            // The body's lifts are held where a body press lands - on its own controller - and charged from
            // here with the row's, so a pass that stepped only the motions this end holds would leave a
            // press that sounded standing still on screen, and standing still for the rest of the session.
            var controller = new TabPanelController();

            controller.handlePointer(
                PointerEventMocks.mockLeftPressAt(INSIDE_FIRST_TAB_X, BELOW_TABS_Y),
                buildTwoTabPlacement());

            advanceAWholeTraverse(controller);

            assertThat(controller.resolveBodyPressFractionAt(FIRST_BODY_SLOT))
                .isCloseTo(1f, within(TOLERANCE));
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
                DURATIONS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(controller.resolveBodyHoverFractionAt(FIRST_BODY_SLOT))
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(controller.getNotchHoverFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsAtPointLightsTheTabTheHeaderIsAlreadyShowing() {
            // The hover half of what the shared resolver is for. A press on this tab fires nothing, and the
            // pointer still has to light it: the sidebar's resting and selected tabs converge on one hovered
            // shade, with the underline left to mark the selection. Hover read as "what a press would
            // activate" would leave the selected tab the one tab that never answers the pointer.
            var controller = new TabPanelController();
            controller.advanceInputMotionsAtPoint(
                buildTwoTabPlacementShowing(FIRST_TAB_INDEX, ControlAction.NONE),
                INSIDE_FIRST_TAB_X,
                ON_TAB_ROW_Y,
                FULL_STEP_SECONDS,
                DURATIONS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsAtPointLightsNoTabOfADockedPanel() {
            // Where the gate now lives: the placement says the panel has a body, the fold says that body is
            // behind the rail, so the tab under the pointer is not one the player can see to point at.
            var controller = TabPanelController.createStartingDocked();
            controller.advanceInputMotionsAtPoint(
                buildTwoTabPlacement(),
                INSIDE_FIRST_TAB_X,
                ON_TAB_ROW_Y,
                FULL_STEP_SECONDS,
                DURATIONS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsAtPointLightsABodylessPanelsTabWhateverTheFoldSays() {
            // The row of a bodyless tab is drawn in full, so it lights under the pointer like any other -
            // a fold another tab left standing says nothing about a panel that has none.
            var controller = TabPanelController.createStartingDocked();
            controller.advanceInputMotionsAtPoint(
                buildBodylessTwoTabPlacement(),
                INSIDE_FIRST_TAB_X,
                ON_TAB_ROW_Y,
                FULL_STEP_SECONDS,
                DURATIONS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsAtPointLightsTheBodyControlUnderThePointerAndNoTab() {
            // The third pairing this seam holds: the body walk feeds the body fades. The point is inside the
            // box and below the row, so a tab lighting here could only come from the wrong hit-test - and a
            // body cell staying dark from the walk never being reached.
            var controller = new TabPanelController();
            controller.advanceInputMotionsAtPoint(
                buildTwoTabPlacementWithNotch(),
                INSIDE_FIRST_TAB_X,
                BELOW_TABS_Y,
                FULL_STEP_SECONDS,
                DURATIONS);

            assertThat(controller.resolveBodyHoverFractionAt(FIRST_BODY_SLOT))
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceInputMotionsAtPointLightsNoBodyControlBehindADockedPanelsRail() {
            // The fold reaching the body, end to end: the placement's box is the rail a fully docked panel
            // leaves, and the control is laid where it always was. The panel's own collapse state is not
            // consulted for the body - the box the layout narrowed is - so a hover reads what is drawn
            // rather than what the controller remembers.
            var controller = new TabPanelController();
            controller.advanceInputMotionsAtPoint(
                buildDockedRailPlacement(),
                INSIDE_FIRST_TAB_X,
                BELOW_TABS_Y,
                FULL_STEP_SECONDS,
                DURATIONS);

            assertThat(controller.resolveBodyHoverFractionAt(FIRST_BODY_SLOT))
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
                DURATIONS);

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
                DURATIONS);

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(controller.resolveBodyHoverFractionAt(FIRST_BODY_SLOT))
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
                DURATIONS);

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

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(FIRST_TAB_INDEX, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
                HALF_STEP_SECONDS,
                DURATIONS);

            controller.resetInputMotions();

            assertThat(hoverFractionAt(controller, FIRST_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void resetInputMotionsDropsABodyCellsFadeLeftPartWayUpWhenThePanelStopsShowing() {
            // The body's cells are dropped with the row's and for the same reason, and in the same call:
            // a strip rebuilt for the next session would otherwise open with whatever now occupies that
            // slot part-way lit, which the player never saw rise and would see fall for no reason.
            var controller = new TabPanelController();

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, FIRST_BODY_CELL, NOTCH_NOT_HOVERED),
                HALF_STEP_SECONDS,
                DURATIONS);

            controller.resetInputMotions();

            assertThat(controller.resolveBodyHoverFractionAt(FIRST_BODY_SLOT))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void resetInputMotionsDropsABodyCellsPressLiftLeftPartWayThroughItsCycle() {
            // The body's lifts are dropped with the panel's own and in the same call, though they are held
            // one level down: a lift left standing there would be inherited by whatever the next session's
            // rebuilt strip puts in that slot, showing a press made on a control that is no longer there.
            var controller = new TabPanelController();

            controller.handlePointer(
                PointerEventMocks.mockLeftPressAt(INSIDE_FIRST_TAB_X, BELOW_TABS_Y),
                buildTwoTabPlacement());

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
                HALF_STEP_SECONDS,
                DURATIONS);

            controller.resetInputMotions();

            assertThat(controller.resolveBodyPressFractionAt(FIRST_BODY_SLOT))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void resetInputMotionsDropsTheHandlesFadeLeftPartWayUpWhenThePanelStopsShowing() {
            // The handle is dropped for the same reason and in the same call, so a panel re-opened under a
            // still pointer cannot paint one part lit and the other at rest.
            var controller = new TabPanelController();

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, NO_BODY_CELL_HOVERED, NOTCH_HOVERED),
                HALF_STEP_SECONDS,
                DURATIONS);

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

            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
                HALF_STEP_SECONDS,
                DURATIONS);
                
            controller.resetInputMotions();

            assertThat(pulseFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void resetInputMotionsDropsALiftStillHeldAtItsPeakByAPressedButton() {
            // The one hold with no release owed to it. A press outlives the panel when the overlay closes
            // under a button still down - the release then lands on a screen that is no longer routing to
            // this controller - so without the reset that lift would stand at its peak for the rest of the
            // session and greet the next opening fully lit.
            var controller = new TabPanelController();

            controller.activateTabAtPoint(
                buildTwoTabPlacementShowing(FIRST_TAB_INDEX, tabIndex -> { }),
                INSIDE_SECOND_TAB_X,
                ON_TAB_ROW_Y);

            advanceAWholeTraverse(controller);

            assertThat(pulseFractionAt(controller, SECOND_TAB_INDEX))
                .as("the lift is held at its peak, which is what makes the drop below worth pinning")
                .isCloseTo(1f, within(TOLERANCE));

            controller.resetInputMotions();
            
            advanceAWholeTraverse(controller);

            assertThat(pulseFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void resetInputMotionsDropsAHotkeyBlinkLeftPartWayThroughItsCycle() {
            // The blink is dropped with the rest: it runs on the look channel, so one left part-way would
            // open the next session with a tab lit as though the pointer were on it.
            var controller = new TabPanelController();

            controller.startHotkeyBlinkAt(SECOND_TAB_INDEX);
            controller.advanceInputMotionsForFrame(
                new TabPanelHover(NO_TAB_HOVERED, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
                HALF_STEP_SECONDS,
                DURATIONS);

            controller.resetInputMotions();

            assertThat(hoverFractionAt(controller, SECOND_TAB_INDEX))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class ResolveTabIndexAtPoint {

        @Test
        void resolveTabIndexAtPointReturnsTheTabThePointIsOn() {
            assertThat(new TabPanelController().resolveTabIndexAtPoint(
                    buildTwoTabPlacement(),
                    INSIDE_FIRST_TAB_X,
                    ON_TAB_ROW_Y))
                .isEqualTo(0);
            assertThat(new TabPanelController().resolveTabIndexAtPoint(
                    buildTwoTabPlacement(),
                    INSIDE_SECOND_TAB_X,
                    ON_TAB_ROW_Y))
                .isEqualTo(1);
        }

        @Test
        void resolveTabIndexAtPointReturnsNoTabForAPointOnTheBody() {
            // A pointer inside the panel but below the header is on no tab, so the whole row winds down
            // rather than the nearest tab staying lit.
            assertThat(new TabPanelController().resolveTabIndexAtPoint(
                    buildTwoTabPlacement(),
                    INSIDE_FIRST_TAB_X,
                    BELOW_TABS_Y))
                .isNull();
        }

        @Test
        void resolveTabIndexAtPointReturnsNoTabForAPointOffThePanel() {
            assertThat(new TabPanelController().resolveTabIndexAtPoint(
                    buildTwoTabPlacement(),
                    OFF_PANEL_X,
                    OFF_PANEL_Y))
                .isNull();
        }

        @Test
        void resolveTabIndexAtPointReturnsNoTabOnADockedPanelsLaidOutTab() {
            // The fold gate, now part of the answer rather than a test each reader runs for itself. The tab
            // is laid out exactly where it was - folding only clips the header at paint time - so this point
            // is on a tab by geometry alone and on bare screen by what the player can see.
            assertThat(TabPanelController.createStartingDocked().resolveTabIndexAtPoint(
                    buildTwoTabPlacement(),
                    INSIDE_FIRST_TAB_X,
                    ON_TAB_ROW_Y))
                .isNull();
        }

        @Test
        void resolveTabIndexAtPointReturnsABodylessPanelsTabWhateverTheFoldSays() {
            // The same docked controller over a row with nothing under it: it is drawn in full, so it
            // resolves in full. A fold another tab left standing says nothing about a panel that has none.
            assertThat(TabPanelController.createStartingDocked().resolveTabIndexAtPoint(
                    buildBodylessTwoTabPlacement(),
                    INSIDE_FIRST_TAB_X,
                    ON_TAB_ROW_Y))
                .isEqualTo(0);
        }

        @Test
        void resolveTabIndexAtPointReturnsTheTabTheHeaderIsAlreadyShowing() {
            // Geometry and visibility, never actionability: the lit tab fires nothing and is still the tab
            // the pointer is on, so it lights like any other - which is the shade the sidebar's resting and
            // selected tabs converge on. Resolved to no cell, the tab under the pointer would go dark for
            // as long as it was the one selected.
            assertThat(new TabPanelController().resolveTabIndexAtPoint(
                    buildTwoTabPlacementShowing(FIRST_TAB_INDEX, ControlAction.NONE),
                    INSIDE_FIRST_TAB_X,
                    ON_TAB_ROW_Y))
                .isEqualTo(FIRST_TAB_INDEX);
        }
    }

    @Nested
    class InterfaceSounds {

        // The three levels, each a number of its own, so which kind the controller named for a moment can
        // be read straight off what sounded. Sharing any two of them would let a cell answered as the wrong
        // kind pass unnoticed, which is the fault these cases exist to catch.
        private static final float CHROME_ARRIVAL_VOLUME = 0.75f;
        private static final float LISTED_ITEM_ARRIVAL_VOLUME = 0.1f;
        private static final float SINGLE_OPTION_ARRIVAL_VOLUME = 0.2f;

        // A look whose arrival levels differ by kind. The vanilla scheme cannot tell them apart, its three
        // being a pair and a half rather than three distinct numbers.
        private static final UiSoundScheme KIND_DISTINGUISHING_SOUNDS = new UiSoundScheme(
            UiSoundCue.createAtFullVolume(StarsectorUiSound.BUTTON_PRESSED),
            StarsectorUiSound.BUTTON_MOUSEOVER,
            new PointerArrivalVolumes(
                CHROME_ARRIVAL_VOLUME,
                SINGLE_OPTION_ARRIVAL_VOLUME,
                LISTED_ITEM_ARRIVAL_VOLUME),
            UiSoundCue.createAtFullVolume(StarsectorUiSound.LIST_SCROLLED));

        // The two roles crossed over, so a moment answered from the controller's own code rather than from
        // the look it was handed records the sound the other moment would have made. The vanilla pair could
        // not tell the two apart - it names exactly what the controller used to name for itself. Its wheel
        // is left at the ordinary role, no case using this scheme turning one.
        private static final UiSoundScheme SWAPPED_SOUNDS = new UiSoundScheme(
            UiSoundCue.createAtFullVolume(StarsectorUiSound.BUTTON_MOUSEOVER),
            StarsectorUiSound.BUTTON_PRESSED,
            UiSoundCue.createAtFullVolume(StarsectorUiSound.LIST_SCROLLED));

        // A look whose wheel answers with a role neither of its other moments uses and which is not the one
        // a list takes by default. Three ways a scroll could be answered wrongly - by the library's own
        // scheme, by this look's press, by this look's arrival - and all three record the same other role,
        // so only a wheel reading this look's own scroll cue passes.
        private static final UiSoundScheme SWAPPED_SCROLL_SOUND = new UiSoundScheme(
            UiSoundCue.createAtFullVolume(StarsectorUiSound.LIST_SCROLLED),
            StarsectorUiSound.LIST_SCROLLED,
            UiSoundCue.createAtFullVolume(StarsectorUiSound.BUTTON_MOUSEOVER));

        private final UiSoundPlayerFake soundPlayerFake = new UiSoundPlayerFake();

        @Test
        void interfaceSoundsPlayThePressOnTheReleaseRatherThanOnTheWayDown() {
            // The engine's own tabs sound as the button comes up, so the sound and the wash fading out are
            // one answer. Pinned as two assertions either side of the release, since a press that sounded on
            // the way down would still leave the right sound recorded by the end.
            var controller = buildVanillaSoundingController();
            var placement = buildTwoTabPlacementShowing(FIRST_TAB_INDEX, tabIndex -> { });

            controller.activateTabAtPoint(placement, INSIDE_SECOND_TAB_X, ON_TAB_ROW_Y);

            assertThat(soundPlayerFake.getPlayedSounds())
                .as("the button is still down, so nothing has been answered yet")
                .isEmpty();

            controller.handlePointer(PointerEventMocks.mockLeftReleaseAt(INSIDE_SECOND_TAB_X, ON_TAB_ROW_Y), placement);

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_PRESSED);
        }

        @Test
        void interfaceSoundsPlayThePressForTheTabAlreadyShowing() {
            // The lit tab fires no action and still answers the press, so it must sound like every other
            // tab - a press that lifted but stayed silent would read as a half-registered click.
            var controller = buildVanillaSoundingController();
            var placement = buildTwoTabPlacementShowing(FIRST_TAB_INDEX, tabIndex -> { });

            controller.activateTabAtPoint(placement, INSIDE_FIRST_TAB_X, ON_TAB_ROW_Y);
            controller.handlePointer(PointerEventMocks.mockLeftReleaseAt(INSIDE_FIRST_TAB_X, ON_TAB_ROW_Y), placement);

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_PRESSED);
        }

        @Test
        void interfaceSoundsStaySilentForAReleaseThatEndedNoPress() {
            // Every release on the screen reaches the panel, so one that let go of nothing must not click at
            // the player - otherwise clicking the map behind the sidebar would sound like pressing it.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockLeftReleaseAt(OFF_PANEL_X, OFF_PANEL_Y),
                buildTwoTabPlacementShowing(FIRST_TAB_INDEX, tabIndex -> { }));

            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void interfaceSoundsPlayThePressForABoundKey() {
            // A keypress puts nothing under the pointer to explain itself, so it takes both answers the
            // engine gives a press rather than the flash alone.
            var controller = buildVanillaSoundingController();

            controller.startHotkeyBlinkAt(SECOND_TAB_INDEX);

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_PRESSED);
        }

        @Test
        void interfaceSoundsPlayTheMouseoverOnceAsThePointerArrives() {
            // A moment, not a position: the pointer resting on a tab holds its fade at the top for as long
            // as it stays, and a sound read off that would be a tone rather than a tick.
            var controller = buildVanillaSoundingController();

            advanceWithPointerOn(controller, buildHoverOnTab(FIRST_TAB_INDEX));
            advanceWithPointerOn(controller, buildHoverOnTab(FIRST_TAB_INDEX));
            advanceWithPointerOn(controller, buildHoverOnTab(FIRST_TAB_INDEX));

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_MOUSEOVER);
        }

        @Test
        void interfaceSoundsAnnounceTheHandleAgainOnceThePointerHasBeenBackOnTheTabs() {
            // Both parts' arrivals are stepped every frame, not only whichever one answers. Read through a
            // short-circuit, the part left unstepped keeps a stale latch saying it never left - and then
            // stays silent on the frame the pointer actually does come back to it.
            var controller = buildVanillaSoundingController();

            advanceWithPointerOn(controller, buildHoverOnTab(FIRST_TAB_INDEX));
            advanceWithPointerOn(controller, buildHoverOnNotch());
            advanceWithPointerOn(controller, buildHoverOnTab(FIRST_TAB_INDEX));

            soundPlayerFake.clearPlayedCues();

            advanceWithPointerOn(controller, buildHoverOnNotch());

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_MOUSEOVER);
        }

        @Test
        void interfaceSoundsTakeThePressRoleFromTheLookRatherThanNamingOne() {
            // The point of the whole seam: which sound a press makes is the panel's look talking, so a look
            // naming something else must be what sounds. A scheme agreeing with the old hardcoded role
            // would pass whether or not it was ever read.
            var controller = buildControllerSounding(SWAPPED_SOUNDS);

            controller.startHotkeyBlinkAt(FIRST_TAB_INDEX);

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_MOUSEOVER);
        }

        @Test
        void interfaceSoundsTakeThePointerArrivalRoleFromTheLookRatherThanNamingOne() {
            // The arrival half of the same rule, crossed the other way.
            var controller = buildControllerSounding(SWAPPED_SOUNDS);

            advanceWithPointerOn(controller, buildHoverOnTab(FIRST_TAB_INDEX));

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_PRESSED);
        }

        @Test
        void interfaceSoundsAnswerAnArrivalOnTheHeaderAtThePanelChromeLevel() {
            // Which kind of thing was reached is the one part of an arrival this end names, the look owning
            // the rest - so a controller that named the wrong kind would answer a tab at a body control's
            // level, silently. A tab and its handle are the panel's own furniture, not its contents.
            var controller = buildControllerSounding(KIND_DISTINGUISHING_SOUNDS);

            advanceWithPointerOn(controller, buildHoverOnTab(FIRST_TAB_INDEX));

            assertThat(soundPlayerFake.getPlayedCues())
                .containsExactly(new UiSoundCue(
                    StarsectorUiSound.BUTTON_MOUSEOVER,
                    CHROME_ARRIVAL_VOLUME));
        }

        @Test
        void interfaceSoundsAnswerAnArrivalOnAWholeRowControlAtTheSingleOptionLevel() {
            // Driven through the point rather than through a handed-in reading, because the kind is the one
            // part of a body arrival the panel works out for itself: the walk has the control in hand and
            // reads what it is off that. Handed in, a controller naming one kind for every body cell would
            // pass this and every case below it.
            var controller = buildControllerSounding(KIND_DISTINGUISHING_SOUNDS);

            advanceWithPointerAt(controller, buildTwoTabPlacement(), INSIDE_FIRST_TAB_X, BELOW_TABS_Y);

            assertThat(soundPlayerFake.getPlayedCues())
                .containsExactly(new UiSoundCue(
                    StarsectorUiSound.BUTTON_MOUSEOVER,
                    SINGLE_OPTION_ARRIVAL_VOLUME));
        }

        @Test
        void interfaceSoundsAnswerAnArrivalOnOneSegmentOfARowAtTheListedItemLevel() {
            // The other half of the same rule, over a body laying a row of segments where the case above
            // lays one whole-row control. A sweep down a strip crosses several of these on its way
            // somewhere, which is what the quieter level is for.
            var controller = buildControllerSounding(KIND_DISTINGUISHING_SOUNDS);

            advanceWithPointerAt(
                controller,
                buildPlacementWithTwoSegmentBody(),
                INSIDE_FIRST_TAB_X,
                BELOW_TABS_Y);

            assertThat(soundPlayerFake.getPlayedCues())
                .containsExactly(new UiSoundCue(
                    StarsectorUiSound.BUTTON_MOUSEOVER,
                    LISTED_ITEM_ARRIVAL_VOLUME));
        }

        @Test
        void interfaceSoundsAnnounceEachSegmentOfOneRowTheCursorCrossesInto() {
            // Crossing straight from one segment to its neighbour is an arrival like any other: the pointer
            // never left the control, and on abutting segments that is the ordinary way to reach one. Keyed
            // by the control rather than by the cell, the second segment would be reached in silence.
            var controller = buildControllerSounding(KIND_DISTINGUISHING_SOUNDS);
            var placement = buildPlacementWithTwoSegmentBody();

            advanceWithPointerAt(controller, placement, INSIDE_FIRST_TAB_X, BELOW_TABS_Y);
            advanceWithPointerAt(controller, placement, INSIDE_SECOND_TAB_X, BELOW_TABS_Y);

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(
                    StarsectorUiSound.BUTTON_MOUSEOVER,
                    StarsectorUiSound.BUTTON_MOUSEOVER);
        }

        @Test
        void interfaceSoundsPlayTheMouseoverOnceWhileThePointerRestsOnABodyCell() {
            // A moment, not a position, and the reason a body cell needs its own latch: its fade stands at
            // the top for as long as the pointer stays, so a sound read off the fade would be a tone.
            var controller = buildVanillaSoundingController();

            advanceWithPointerOn(controller, buildHoverOnBodyCell(FIRST_BODY_CELL));
            advanceWithPointerOn(controller, buildHoverOnBodyCell(FIRST_BODY_CELL));
            advanceWithPointerOn(controller, buildHoverOnBodyCell(FIRST_BODY_CELL));

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_MOUSEOVER);
        }

        @Test
        void interfaceSoundsAnnounceABodyCellAgainOnceThePointerHasBeenBackOnTheTabs() {
            // The third latch joins the rule the other two already answer to: every one is stepped each
            // frame, not only whichever one sounds. Left unstepped while a tab answered, the body's would
            // hold a stale reading saying the pointer never left this cell - and then say nothing on the
            // frame it came back to it.
            var controller = buildVanillaSoundingController();

            advanceWithPointerOn(controller, buildHoverOnBodyCell(FIRST_BODY_CELL));
            advanceWithPointerOn(controller, buildHoverOnTab(FIRST_TAB_INDEX));

            soundPlayerFake.clearPlayedCues();

            advanceWithPointerOn(controller, buildHoverOnBodyCell(FIRST_BODY_CELL));

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_MOUSEOVER);
        }

        @Test
        void interfaceSoundsStaySilentForABodyCellBehindADockedPanelsRail() {
            // The gate is the walk's, not a test of this end's own: the box a fully docked panel leaves is
            // the rail, and the control is laid where it always was. A cell the fold has wiped off the
            // screen lights for nobody, so it announces itself to nobody either.
            var controller = buildVanillaSoundingController();

            advanceWithPointerAt(
                controller, buildDockedRailPlacement(), INSIDE_FIRST_TAB_X, BELOW_TABS_Y);

            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void interfaceSoundsAnnounceABodyCellAgainOnceTheMotionsWereReset() {
            // The panel dropping its motions drops what it announced with them, so a panel re-opened with
            // the cursor already over a cell answers it. It is an arrival to the player - the strip was not
            // there a moment ago - and the latch left standing would call it a cell they never left.
            var controller = buildVanillaSoundingController();

            advanceWithPointerOn(controller, buildHoverOnBodyCell(FIRST_BODY_CELL));
            controller.resetInputMotions();
            soundPlayerFake.clearPlayedCues();

            advanceWithPointerOn(controller, buildHoverOnBodyCell(FIRST_BODY_CELL));

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_MOUSEOVER);
        }

        @Test
        void interfaceSoundsStaySilentThroughoutForALookThatNamesNone() {
            // Silence is something a look states, so a panel is quietened by the value it is built from
            // rather than by visiting every moment that ever asked for a sound. Both moments in one case,
            // since a scheme that silenced only one of them would be the fault worth catching.
            var controller = buildControllerSounding(UiSoundScheme.createSilentSoundScheme());

            advanceWithPointerOn(controller, buildHoverOnTab(FIRST_TAB_INDEX));
            
            controller.startHotkeyBlinkAt(FIRST_TAB_INDEX);

            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void interfaceSoundsPlayThePressAsTheCollapseHandleIsPressed() {
            // The handle is a control the player aims at and presses, so it answers like one. On the way
            // down rather than on the release the tabs wait for: the fold is already moving, so the moment
            // it acts is the moment there is something to confirm.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockLeftPressAt(INSIDE_NOTCH_X, INSIDE_NOTCH_Y),
                buildTwoTabPlacementWithNotch());

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_PRESSED);
        }

        @Test
        void interfaceSoundsPlayTheMouseoverOnceAsThePointerArrivesOnTheCollapseHandle() {
            // The handle's half of the arrival rule, and a moment rather than a position for the same
            // reason: the pointer parked on the handle holds its fade at the top for as long as it stays.
            var controller = buildVanillaSoundingController();

            advanceWithPointerOn(controller, buildHoverOnNotch());
            advanceWithPointerOn(controller, buildHoverOnNotch());

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_MOUSEOVER);
        }

        @Test
        void interfaceSoundsScrollTheBodyByTheLookThePanelWasBuiltWith() {
            // The body is the panel's own, so it sounds by the panel's own look. Built with a scheme of its
            // own, the body would answer the wheel from the library's defaults while the header answered
            // from the host's - one panel presenting itself two ways.
            var controller = buildControllerSounding(SWAPPED_SCROLL_SOUND);

            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(INSIDE_FIRST_TAB_X, ON_TOP_SCROLLING_ROW_Y),
                buildScrollingListPlacement(UNSCROLLED_OFFSET));

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_MOUSEOVER);
        }

        @Test
        void interfaceSoundsAnswerAWheelWithTheScrollAndNotTheRowsItCarriedUnderTheCursor() {
            // The rule the whole listed-item level is liveable because of. Rows sliding under a parked
            // pointer are arrivals by the slot key and by nothing the player did, so a wheel down a long
            // list would tick once per row; the scroll answers for the whole movement in one sound, which
            // is also the honest reading - the player turned the wheel once.
            var controller = buildVanillaSoundingController();
            var restingPlacement = buildScrollingListPlacement(UNSCROLLED_OFFSET);

            advanceWithPointerAt(controller, restingPlacement, INSIDE_FIRST_TAB_X, ON_TOP_SCROLLING_ROW_Y);

            soundPlayerFake.clearPlayedCues();

            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(INSIDE_FIRST_TAB_X, ON_TOP_SCROLLING_ROW_Y),
                restingPlacement);

            advanceWithPointerAt(
                controller,
                buildScrollingListPlacement(SCROLLED_BY_ONE_ROW_OFFSET),
                INSIDE_FIRST_TAB_X,
                ON_TOP_SCROLLING_ROW_Y);

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.LIST_SCROLLED);
        }

        @Test
        void interfaceSoundsAnnounceTheRowAScrollLeftUnderTheCursorOnceThePointerReachesItItself() {
            // Adopted rather than gone deaf. The latch takes the row the scroll carried under the cursor
            // without announcing it, so the pointer genuinely arriving on that row afterwards is an arrival
            // like any other - a latch that had simply stopped tracking would swallow this one too.
            var controller = buildVanillaSoundingController();
            var scrolledPlacement = buildScrollingListPlacement(SCROLLED_BY_ONE_ROW_OFFSET);

            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(INSIDE_FIRST_TAB_X, ON_TOP_SCROLLING_ROW_Y),
                buildScrollingListPlacement(UNSCROLLED_OFFSET));

            advanceWithPointerAt(
                controller, scrolledPlacement, INSIDE_FIRST_TAB_X, ON_TOP_SCROLLING_ROW_Y);

            advanceWithPointerAt(controller, scrolledPlacement, OFF_PANEL_X, OFF_PANEL_Y);

            soundPlayerFake.clearPlayedCues();

            advanceWithPointerAt(
                controller, scrolledPlacement, INSIDE_FIRST_TAB_X, ON_TOP_SCROLLING_ROW_Y);

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_MOUSEOVER);
        }

        @Test
        void interfaceSoundsAnnounceARowAfreshWhenThePanelHidBetweenTheScrollAndTheNextFrame() {
            // A movement no frame ever read is a movement the next session must not answer to. Left
            // standing, it would make the re-opened panel take the cell under the cursor in silence - the
            // one thing dropping the panel's motions exists to prevent.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(INSIDE_FIRST_TAB_X, ON_TOP_SCROLLING_ROW_Y),
                buildScrollingListPlacement(UNSCROLLED_OFFSET));

            controller.resetInputMotions();
            
            soundPlayerFake.clearPlayedCues();

            advanceWithPointerAt(
                controller,
                buildScrollingListPlacement(SCROLLED_BY_ONE_ROW_OFFSET),
                INSIDE_FIRST_TAB_X,
                ON_TOP_SCROLLING_ROW_Y);

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_MOUSEOVER);
        }

        // One frame with the pointer where the given reading puts it - what the panel's own hit-tests
        // would have produced, handed in so these cases need no display to point at.
        private void advanceWithPointerOn(TabPanelController controller, TabPanelHover hover) {
            controller.advanceInputMotionsForFrame(hover, FULL_STEP_SECONDS, DURATIONS);
        }

        // One frame with the pointer at a point, letting the panel run its own hit-tests - what the cases
        // about the kind of thing reached take, that kind being the part the panel works out for itself.
        private void advanceWithPointerAt(
                TabPanelController controller,
                TabPanelPlacement placement,
                float pointX,
                float pointY) {

            controller.advanceInputMotionsAtPoint(
                placement,
                pointX,
                pointY,
                FULL_STEP_SECONDS,
                DURATIONS);
        }

        // The pointer on one tab and off the handle.
        private static TabPanelHover buildHoverOnTab(Integer tabIndex) {
            return new TabPanelHover(tabIndex, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED);
        }

        // The pointer on one body cell and off both the row and the handle, so what sounds can only have
        // come from the body.
        private static TabPanelHover buildHoverOnBodyCell(HoveredBodyCell bodyCell) {
            return new TabPanelHover(NO_TAB_HOVERED, bodyCell, NOTCH_NOT_HOVERED);
        }

        // The pointer on the handle and off every tab, so what sounds can only have come from the handle.
        private static TabPanelHover buildHoverOnNotch() {
            return new TabPanelHover(NO_TAB_HOVERED, NO_BODY_CELL_HOVERED, NOTCH_HOVERED);
        }

        // A controller recording into this case's fake and answering by the engine's own scheme - the look
        // every case not about the scheme itself is written against.
        private TabPanelController buildVanillaSoundingController() {
            return buildControllerSounding(UiSoundScheme.createVanillaSoundScheme());
        }

        // The same, by whichever scheme the case is about.
        private TabPanelController buildControllerSounding(UiSoundScheme soundScheme) {
            return new TabPanelController(soundPlayerFake, soundScheme);
        }
    }

    // How far onto the hovered shade a tab stands, read the way the render pass reads it - through the
    // interaction sources rather than off the fades directly, so these pin the composed value that actually
    // reaches a strip rather than either motion feeding it.
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
        controller.advanceInputMotionsForFrame(
            new TabPanelHover(NO_TAB_HOVERED, NO_BODY_CELL_HOVERED, NOTCH_NOT_HOVERED),
            FULL_STEP_SECONDS,
            DURATIONS);
    }

    // A panel with a body but no handle laid for it - all the tab hit-test needs, and the shape every case
    // that is not about the handle reads.
    private static TabPanelPlacement buildTwoTabPlacement() {
        return buildPlacement(null, TABS_SHOWING_FIRST_TAB);
    }

    // The same panel carrying a handle, for the hit-tests that have to tell the panel's two parts apart.
    private static TabPanelPlacement buildTwoTabPlacementWithNotch() {
        return buildPlacement(NOTCH, TABS_SHOWING_FIRST_TAB);
    }

    // The same panel whose header fires into the caller's recorder, for the press path - which is what the
    // spec's own action is reached through.
    private static TabPanelPlacement buildTwoTabPlacementShowing(
            int selectedIndex,
            ControlAction onTabFired) {

        return buildPlacement(NOTCH, buildTabsSpecShowing(selectedIndex, onTabFired));
    }

    // A placement carrying what the hit-tests read: a two-tab header control with its per-tab segments, and
    // whichever handle and spec the caller wants it to have. Two tabs rather than one, so an index answered
    // off the row's start reads as a wrong number; one builder for every case, so the placements cannot
    // drift apart in any other respect.
    private static TabPanelPlacement buildPlacement(Rectangle notch, ControlSpec spec) {
        return buildPlacement(notch, spec, HEADER_BAND);
    }

    // The same panel with only part of its row still drawn, for the cases about what a folding panel
    // claims: the row is laid out whole either way, and the drawn band is what the fold has left of it.
    private static TabPanelPlacement buildPlacementWithDrawnBand(Rectangle drawnHeaderBand) {
        return buildPlacement(null, TABS_SHOWING_FIRST_TAB, drawnHeaderBand);
    }

    // The same panel with nothing beneath its row - what a tab whose body is empty lays out: no controls,
    // and with nothing to fold, no handle either. Every other builder here carries a body, since a panel
    // that has one is what the fold gate is about.
    private static TabPanelPlacement buildBodylessTwoTabPlacement() {
        return buildBodylessTwoTabPlacementShowing(FIRST_TAB_INDEX, ControlAction.NONE);
    }

    // The bodyless panel firing into the caller's recorder, for the press path.
    private static TabPanelPlacement buildBodylessTwoTabPlacementShowing(
            int selectedIndex,
            ControlAction onTabFired) {

        return buildPlacement(
            null,
            buildTabsSpecShowing(selectedIndex, onTabFired),
            HEADER_BAND,
            List.of());
    }

    // The header's own spec: a two-tab row showing one of them and firing the given action. Every placement
    // here carries one, a header being an ordinary laid-out tabs control - so the hit-test reads the row's
    // selection and its action off the same spec the layout would have put there.
    private static ControlSpec.Tabs buildTabsSpecShowing(int selectedIndex, ControlAction onTabFired) {
        return new ControlSpec.Tabs(List.of("First", "Second"), List.of(), selectedIndex, onTabFired);
    }

    // The same panel with a row of segments beneath its tabs in place of the whole-row control every other
    // case lays there, for the cases about a cell that is one of many alike. Its two segments split the
    // body box left and right, so the x of either tab above reaches the segment under that tab.
    private static TabPanelPlacement buildPlacementWithTwoSegmentBody() {
        return buildPlacement(
            null,
            TABS_SHOWING_FIRST_TAB,
            HEADER_BAND,
            List.of(buildTwoSegmentRadioControl()));
    }

    // The same panel with a scrolling list beneath its tabs, laid at the given scroll offset - the rows
    // where the layout would have put them for that offset, and the body carrying the offset and the
    // overflow the scrollbar and the wheel read. Taking the offset rather than holding one, so a case
    // drives the wheel and then hands in the frame the layout would next have drawn.
    private static TabPanelPlacement buildScrollingListPlacement(float scrollOffset) {

        return buildPlacement(
            null,
            TABS_SHOWING_FIRST_TAB,
            HEADER_BAND,
            new PanelPlacement(
                BODY_BOX,
                BODY_BOX,
                List.of(buildScrollingListControl(scrollOffset)),
                BODY_BOX,
                scrollOffset,
                SCROLLING_LIST_OVERFLOW));
    }

    // The list itself: a scrolling table whose rows are stacked down from the body's top edge and shifted
    // by the offset, so a row scrolled past the top is laid above the viewport and drawn away exactly as
    // the real layout leaves it. What each case reads off it is which row the one test point falls in.
    private static Control buildScrollingListControl(float scrollOffset) {

        var rows = new ArrayList<Rectangle>();
        var labels = new ArrayList<String>();

        for (var rowIndex = 0; rowIndex < SCROLLING_ROW_COUNT; rowIndex++) {

            rows.add(new Rectangle(
                BODY_BOX.x(),
                BODY_BOX.y() + BODY_BOX.height() - (rowIndex + 1) * SCROLLING_ROW_HEIGHT + scrollOffset,
                BODY_BOX.width(),
                SCROLLING_ROW_HEIGHT));
            labels.add("Row " + rowIndex);
        }
        var spec = VerticalTableSpecs.buildIconList(
                labels,
                Arrays.asList(new String[SCROLLING_ROW_COUNT]),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE)
            .asScrolling();

        return new Control(spec, BODY_BOX, rows);
    }

    // The same panel folded away to the rail a fully docked one leaves: the box narrowed to a border's
    // width while the control beneath the row keeps the place the layout gave it, which is how a fold
    // actually reaches the body - the box is clipped to, and the controls are not moved.
    private static TabPanelPlacement buildDockedRailPlacement() {
        return buildPlacement(
            null,
            TABS_SHOWING_FIRST_TAB,
            HEADER_BAND,
            buildBody(DOCKED_RAIL_BOX, List.of(buildBodyControl())));
    }

    private static TabPanelPlacement buildPlacement(
            Rectangle notch,
            ControlSpec spec,
            Rectangle drawnHeaderBand) {

        return buildPlacement(notch, spec, drawnHeaderBand, List.of(buildBodyControl()));
    }

    private static TabPanelPlacement buildPlacement(
            Rectangle notch,
            ControlSpec spec,
            Rectangle drawnHeaderBand,
            List<Control> bodyControls) {

        return buildPlacement(notch, spec, drawnHeaderBand, buildBody(BODY_BOX, bodyControls));
    }

    private static TabPanelPlacement buildPlacement(
            Rectangle notch,
            ControlSpec spec,
            Rectangle drawnHeaderBand,
            PanelPlacement body) {

        return new TabPanelPlacement(
            new Control(spec, HEADER_BAND, List.of(FIRST_TAB, SECOND_TAB)),
            drawnHeaderBand,
            body,
            new BoxBorder(BORDER_WIDTH),
            notch);
    }

    // The body beneath the row: the controls, and the box they are drawn inside - which is what a fold
    // narrows, and so the one part of a body a case here ever varies. Assembled apart from the panel so
    // the builder above takes a body rather than the pieces of one, two rectangles side by side in a
    // parameter list being two rectangles that can be handed over the wrong way round.
    private static PanelPlacement buildBody(Rectangle box, List<Control> bodyControls) {
        return new PanelPlacement(box, box, bodyControls, box, 0f, 0f);
    }

    // A body control, so the placement reads as having a body at all; what it is never matters here, only
    // that the panel has something under its row that a fold could take away.
    private static Control buildBodyControl() {
        return new Control(
            LabelledControlSpecs.buildCheckbox("X", false, ControlAction.NONE),
            BODY_BOX,
            List.of());
    }

    // A two-option row filling the body, its segments splitting the box left and right. Segments are what
    // make a control one of many alike, so this is the body a case about a listed item lays - the checkbox
    // above is hit anywhere on its row and is the single-option kind for exactly that reason.
    private static Control buildTwoSegmentRadioControl() {

        var segmentWidth = BODY_BOX.width() / 2f;
        var leftSegment = new Rectangle(BODY_BOX.x(), BODY_BOX.y(), segmentWidth, BODY_BOX.height());
        var rightSegment = new Rectangle(
            BODY_BOX.x() + segmentWidth,
            BODY_BOX.y(),
            segmentWidth,
            BODY_BOX.height());

        return new Control(
            ControlSpec.HorizontalRadio.of(
                List.of("Left", "Right"),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE),
            BODY_BOX,
            List.of(leftSegment, rightSegment));
    }

}
