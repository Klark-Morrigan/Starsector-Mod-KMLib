package kmlib.starsector.ui.input;

import kmlib.animation.TraverseDurations;
import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.LabelledControlSpecs;
import kmlib.starsector.ui.controls.ReselectBehaviour;
import kmlib.starsector.ui.controls.VerticalTableSpecs;
import kmlib.starsector.ui.sound.StarsectorUiSound;
import kmlib.starsector.ui.sound.UiSoundCue;
import kmlib.starsector.ui.sound.UiSoundScheme;
import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.testfixtures.starsector.ui.sound.UiSoundPlayerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the panel controller's click-to-control resolution: a press maps to the control under it and fires
 * that control's action, while a caption label - drawn but not clickable - is passed over so it never
 * swallows a click as if it acted, and a scrolling control only counts inside its viewport.
 *
 * <p>Each firing case asserts the hit reported alongside the cell the action was actually called with. They
 * are the same number by design - a caller marking what it just fired reads the reported one - so pinning
 * only one would let a press fire one cell and report another.
 *
 * <p>The resolver cases pin the other half of that split: the same geometry answered without the action
 * being reached, and answered without the reselect narrowing the firing cases above pin - a lit segment is
 * under the pointer whether or not pressing it would do anything, which is what lets a hover read the
 * resolver a press reads. The body walk is pinned there too rather than only through the press, since a
 * hover reads the walk and never the firing above it.
 *
 * <p>The wheel cases pin one of the two moments this end answers audibly, and the rule that decides it: the
 * sound follows the list having moved rather than the wheel having turned, so a notch against the end of a
 * list is as silent as a notch over a list that fits. The press cases pin the other, and the rule that
 * decides it: the sound follows the press having reached a control rather than having fired one, so an inert
 * cell sounds like the press it was and chrome stays silent.
 *
 * <p>The press lift is pinned off that same rule and against the slot it is keyed by, both halves of which
 * carry a fault nothing else would report: a lift keyed off the walk's start rather than off where the hit
 * landed, or off the control rather than the cell, would light a place the player did not press - which
 * shows as a flicker on a control nobody can press twice the same way.
 */
final class PanelControllerTest {

    // A look whose press and scroll roles are each the other's, so a moment answered from this end's own
    // code rather than from the look records the wrong sound. The vanilla scheme could not tell the two
    // apart, its roles being the ones a hardcoding would have reached for.
    private static final UiSoundScheme SWAPPED_SOUNDS = new UiSoundScheme(
        UiSoundCue.createAtFullVolume(StarsectorUiSound.LIST_SCROLLED),
        StarsectorUiSound.BUTTON_MOUSEOVER,
        UiSoundCue.createAtFullVolume(StarsectorUiSound.BUTTON_PRESSED));

    private static final Rectangle ROW =
        new Rectangle(100f, 200f, 120f, 20f);

    // A viewport covering the whole row, so a non-scrolling control's hit-test ignores it; the scrolling
    // cases below pass their own viewport to exercise the clip.
    private static final Rectangle FULL_VIEWPORT =
        new Rectangle(0f, 0f, 10000f, 10000f);

    // How far the body overruns its viewport in the wheel cases: less than one notch scrolls, so a single
    // wheel turn takes the list to its end and the one after it has nowhere to go. That pair is what tells a
    // list that moved from a list already against its stop.
    private static final float SHORT_SCROLL_OVERFLOW = 20f;

    // A body whose content fits, which is what leaves a panel with no scrollbar and a wheel with nothing to
    // move.
    private static final float NO_SCROLL_OVERFLOW = 0f;

    // How wide a gutter the guttered body leaves right of its list, so a press there lands in the column a
    // drag grabs the scrollbar by. Wider than the track, which is what the real grab column is.
    private static final float SCROLLBAR_GUTTER_WIDTH = 20f;

    // A point over the list itself - inside the box and inside the scroll region, the only place a wheel
    // reaches the list at all.
    private static final float ON_LIST_X = ROW.x() + ROW.width() / 2f;
    private static final float ON_LIST_Y = ROW.y() + ROW.height() / 2f;

    // A point in the guttered body's grab column: right of the list and still inside the box, and low in
    // the row so a drag mapped from it carries the list toward its end rather than leaving it where it was.
    private static final float IN_GRAB_COLUMN_X = ROW.x() + ROW.width() - SCROLLBAR_GUTTER_WIDTH / 2f;
    private static final float IN_GRAB_COLUMN_Y = ROW.y() + 1f;

    private static final float TOLERANCE = 0.0001f;

    // A whole traverse in one step, so a lift reaches an end without walking frames, and half of one for the
    // readings taken part-way through a cycle.
    private static final float FULL_STEP_SECONDS = 1f;
    private static final float HALF_STEP_SECONDS = 0.5f;
    private static final float PRESS_DURATION_SECONDS = 1f;

    // The same pace each way, so a step reads as a fraction of one duration whichever way the lift it charges
    // is heading. Which way a lift travels at which pace is pinned on the envelope itself; what this end owes
    // is only that a frame's time reaches the lifts it holds.
    private static final TraverseDurations PRESS_DURATIONS =
        TraverseDurations.createSymmetric(PRESS_DURATION_SECONDS);

    // Where a press on ROW lands for each of the bodies the lift cases lay: the strip's first control, taken
    // anywhere on its row; its second, when a caption stands above it; and the two segments of a row split in
    // half. Named rather than built at each use, a slot being what a lift is keyed by - two cases spelling
    // one place differently would pass while agreeing about nothing.
    //
    // The first row and the left segment are the same pair of numbers because a whole-row control's cell and
    // a first segment are both zero. That is exactly why both are named: a lift keyed by the wrong half of a
    // slot reads correctly at either of them, so a case has to press somewhere neither number covers.
    private static final BodyCellSlot FIRST_ROW_SLOT = new BodyCellSlot(0, ControlSpec.SINGLE_CELL);
    private static final BodyCellSlot SECOND_ROW_SLOT = new BodyCellSlot(1, ControlSpec.SINGLE_CELL);
    private static final BodyCellSlot LEFT_SEGMENT_SLOT = new BodyCellSlot(0, 0);
    private static final BodyCellSlot RIGHT_SEGMENT_SLOT = new BodyCellSlot(0, 1);

    @Nested
    class PressBodyControlAtPoint {

        private final UiSoundPlayerFake soundPlayerFake = new UiSoundPlayerFake();

        // The controller every case here presses through: it answers by the engine's own scheme, so the
        // role a press sounds at is the one a player hears on a vanilla control.
        private final PanelController controller =
            new PanelController(soundPlayerFake, UiSoundScheme.createVanillaSoundScheme());

        @Test
        void pressBodyControlAtPointPassesOverACaptionLabelWithoutActing() {
            // A caption row is drawn but never clickable - a Label is not Interactive - so a press over
            // it hits nothing and falls through rather than being swallowed as if it acted.
            var label = buildCaptionControl();
            var activatedCell = controller.pressBodyControlAtPoint(
                buildBodyPlacement(label),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell).as("a caption is not a hit target")
                .isNull();
            assertThat(soundPlayerFake.getPlayedSounds())
                .as("a press that reached no cell has nothing to answer for")
                .isEmpty();
        }

        @Test
        void pressBodyControlAtPointFiresACheckboxHitAnywhereOnItsRow() {

            var firedCell = new int[] {-1};
            var checkbox = buildCheckboxControl("Muted", cell -> firedCell[0] = cell);
            var activatedCell = controller.pressBodyControlAtPoint(
                buildBodyPlacement(checkbox),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .as("a single-cell control reports itself and cell 0")
                .isEqualTo(new ResolvedBodyCell(checkbox, new BodyCellSlot(0, 0)));
            assertThat(firedCell[0])
                .as("the cell reported is the cell the action fired for")
                .isZero();
        }

        @Test
        void pressBodyControlAtPointReportsNoHitForAPressOutsideACheckboxRow() {

            var checkbox = buildCheckboxControl("Muted", ControlAction.NONE);
            var activatedCell = controller.pressBodyControlAtPoint(
                buildBodyPlacement(checkbox),
                ROW.x() - 10f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .isNull();
        }

        @Test
        void pressBodyControlAtPointFiresAScrollingListOptionInsideItsViewport() {

            var firedCell = new int[] {-1};
            var list = buildScrollingListAtRow(cell -> firedCell[0] = cell);

            // The press lands on the list's one option and inside a viewport that covers the row, so the
            // option fires as a normal radio hit.
            var activatedCell = controller.pressBodyControlAtPoint(
                buildBodyPlacement(ROW, list),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .isEqualTo(new ResolvedBodyCell(list, new BodyCellSlot(0, 0)));
            assertThat(firedCell[0])
                .isZero();
        }

        @Test
        void pressBodyControlAtPointRejectsAScrollingListOptionScrolledOutOfItsViewport() {

            var fired = new boolean[1];
            var list = buildScrollingListAtRow(cell -> fired[0] = true);

            // The option's segment sits at ROW, but the viewport is a strip well above it - as if the row
            // scrolled up under the header - so the press over the clipped-out row must not fire it.
            var activatedCell = controller.pressBodyControlAtPoint(
                buildBodyPlacement(buildViewportAboveRow(), list),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .as("a row clipped from the viewport is not clickable")
                .isNull();
            assertThat(fired[0])
                .as("the clipped-out option's action must not fire")
                .isFalse();
        }

        @Test
        void pressBodyControlAtPointFiresATabHitReportedAsThatTabIndex() {

            var firedCell = new int[] {-1};

            // The lit tab is the left one, so a press on the right (non-lit) tab fires it by its index.
            var tabs = buildTwoTabRowAtRow(0, cell -> firedCell[0] = cell);
            var activatedCell = controller.pressBodyControlAtPoint(
                buildBodyPlacement(tabs),
                ROW.x() + 3f * ROW.width() / 4f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .as("a tab reports its own index")
                .isEqualTo(new ResolvedBodyCell(tabs, new BodyCellSlot(0, 1)));
            assertThat(firedCell[0])
                .as("the index reported is the index the action fired for")
                .isEqualTo(1);
        }

        @Test
        void pressBodyControlAtPointTreatsAPressOnTheLitTabAsInert() {

            var fired = new boolean[1];

            // A tabs row is always inert on its lit tab (INERT reselect), so a press on the left, lit tab
            // reaches no action - matching a vanilla tab strip, where clicking the active tab does nothing.
            var tabs = buildTwoTabRowAtRow(0, cell -> fired[0] = true);
            var activatedCell = controller.pressBodyControlAtPoint(
                buildBodyPlacement(tabs),
                ROW.x() + ROW.width() / 4f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .as("re-clicking the active tab is inert")
                .isNull();
            assertThat(fired[0])
                .as("the lit tab's action must not fire")
                .isFalse();
        }

        @Test
        void pressBodyControlAtPointReportsNoHitForAPressOutsideEveryTab() {

            var tabs = buildTwoTabRowAtRow(0, ControlAction.NONE);
            var activatedCell = controller.pressBodyControlAtPoint(
                buildBodyPlacement(tabs),
                ROW.x() - 10f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .isNull();
        }

        @Test
        void pressBodyControlAtPointFiresADeselectableHorizontalRadioOnARepickOfItsLitSegment() {

            var firedCell = new int[] {-1};

            // A DESELECT horizontal radio wants the re-pick to reach the action so the host turns the
            // control off, so a press on the left, lit segment fires it by its index (not swallowed).
            var radio = buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio.of(
                    List.of("Factions", "Alliances"),
                    0,
                    cell -> firedCell[0] = cell)
                .handlesReselect(ReselectBehaviour.DESELECT));

            var activatedCell = controller.pressBodyControlAtPoint(
                buildBodyPlacement(radio),
                ROW.x() + ROW.width() / 4f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .as("the lit segment reports its own index")
                .isEqualTo(new ResolvedBodyCell(radio, new BodyCellSlot(0, 0)));
            assertThat(firedCell[0])
                .as("the index reported is the index the action fired for")
                .isZero();
        }

        @Test
        void pressBodyControlAtPointTreatsAPressOnAnInertHorizontalRadiosLitSegmentAsInert() {

            var fired = new boolean[1];

            // A plain option pair is always one lit (INERT reselect), so a press on the lit segment
            // reaches no action - the standard radio behaviour a deselectable row opts out of.
            var radio = buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio.of(
                List.of("Short", "Full"),
                0,
                cell -> fired[0] = true));

            var activatedCell = controller.pressBodyControlAtPoint(
                buildBodyPlacement(radio),
                ROW.x() + ROW.width() / 4f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .as("re-clicking the lit option pair segment is inert")
                .isNull();
            assertThat(fired[0])
                .as("the lit segment's action must not fire")
                .isFalse();
        }

        @Test
        void pressBodyControlAtPointSoundsThePressThatReachedAControl() {
            // The moment the panel answers, and the smallest statement of it: one press on one control,
            // one sound. Everything below is about which presses do not get it.
            controller.pressBodyControlAtPoint(
                buildBodyPlacement(buildCheckboxControl("Muted", ControlAction.NONE)),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_PRESSED);
        }

        @Test
        void pressBodyControlAtPointSoundsAPressOnAnInertSegmentThatFiresNothing() {
            // The rule the sound hangs on the resolve for. A re-press on a lit segment changes nothing on
            // screen, so it is the one press the player has only the sound to go by for - and hanging the
            // sound on the firing would make it the panel's only silent press.
            var fired = new boolean[1];
            var radio = buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio.of(
                List.of("Short", "Full"),
                0,
                cell -> fired[0] = true));

            controller.pressBodyControlAtPoint(
                buildBodyPlacement(radio),
                ROW.x() + ROW.width() / 4f,
                ROW.y() + ROW.height() / 2f);

            assertThat(soundPlayerFake.getPlayedSounds())
                .as("a press that landed on a cell sounds like the press it was")
                .containsExactly(StarsectorUiSound.BUTTON_PRESSED);
            assertThat(fired[0])
                .as("the silence of the action is what makes the sound the only answer")
                .isFalse();
        }

        @Test
        void pressBodyControlAtPointStaysSilentForAPressOnADivider() {
            // Chrome answers no cell, so a press over it reached nothing to press. A divider is the case
            // worth pinning because it spans the whole body width, so it is what a press between two
            // controls actually lands on.
            controller.pressBodyControlAtPoint(
                buildBodyPlacement(new Control(new ControlSpec.Divider(), ROW, List.of())),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void pressBodyControlAtPointStaysSilentForAPressOnBlankBody() {
            // The same rule where there is no control at all: blank body swallows the click so the surface
            // behind does not act, and swallowing is not an act of its own.
            controller.pressBodyControlAtPoint(
                buildBodyPlacement(buildCheckboxControl("Muted", ControlAction.NONE)),
                ROW.x() - 10f,
                ROW.y() + ROW.height() / 2f);

            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void pressBodyControlAtPointTakesThePressRoleFromTheLookRatherThanNamingOne() {
            // The point of the seam: which sound a press makes is the panel's look talking. A scheme
            // agreeing with a hardcoded role would pass whether or not it was ever read, and the vanilla
            // press role is exactly what a hardcoding would have named.
            var swappedController = new PanelController(soundPlayerFake, SWAPPED_SOUNDS);

            swappedController.pressBodyControlAtPoint(
                buildBodyPlacement(buildCheckboxControl("Muted", ControlAction.NONE)),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.LIST_SCROLLED);
        }

        @Test
        void pressBodyControlAtPointStartsThePressLiftOfTheRowItLandedOn() {
            // A caption above the checkbox, so the press lands at the strip's second slot: a lift keyed off
            // the walk's start rather than off where the hit landed would read at the first and still pass.
            controller.pressBodyControlAtPoint(
                buildBodyPlacement(
                    buildCaptionControl(),
                    buildCheckboxControl("Muted", ControlAction.NONE)),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            controller.advanceBodyPressPulses(FULL_STEP_SECONDS, PRESS_DURATIONS);

            assertThat(controller.resolveBodyPressFractionAt(SECOND_ROW_SLOT))
                .as("the lift is held against the place the press landed on")
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(controller.resolveBodyPressFractionAt(FIRST_ROW_SLOT))
                .as("the caption the walk passed over was pressed by nobody")
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void pressBodyControlAtPointStartsThePressLiftOfTheSegmentItLandedOn() {
            // The other half of the slot. A press on the right segment lifts that segment alone, so a lift
            // keyed by the control rather than by the cell would light a whole row the player pressed one
            // end of - which is the same fault the other way about.
            var radio = buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio.of(
                List.of("Short", "Full"),
                0,
                ControlAction.NONE));

            controller.pressBodyControlAtPoint(
                buildBodyPlacement(radio),
                ROW.x() + 3f * ROW.width() / 4f,
                ROW.y() + ROW.height() / 2f);

            controller.advanceBodyPressPulses(FULL_STEP_SECONDS, PRESS_DURATIONS);

            assertThat(controller.resolveBodyPressFractionAt(RIGHT_SEGMENT_SLOT))
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(controller.resolveBodyPressFractionAt(LEFT_SEGMENT_SLOT))
                .as("the segment beside the one pressed carries nothing")
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void pressBodyControlAtPointAimsALiftAlreadyRunningBackAtItsPeak() {
            // One lift per cell, retriggered where it stands. A player clicking repeatedly is answered from
            // wherever the last press had got to, rather than by a second lift stacking beside the first or
            // by the first dropping to nothing and climbing again - the second reads as a dip in the
            // opposite direction to the one the click asked for.
            var placement = buildBodyPlacement(buildCheckboxControl("Muted", ControlAction.NONE));
            var pointX = ROW.x() + ROW.width() / 2f;
            var pointY = ROW.y() + ROW.height() / 2f;

            controller.pressBodyControlAtPoint(placement, pointX, pointY);
            controller.advanceBodyPressPulses(FULL_STEP_SECONDS, PRESS_DURATIONS);
            controller.advanceBodyPressPulses(HALF_STEP_SECONDS, PRESS_DURATIONS);

            controller.pressBodyControlAtPoint(placement, pointX, pointY);
            controller.advanceBodyPressPulses(HALF_STEP_SECONDS, PRESS_DURATIONS);

            // Half a traverse from half way is the whole of what is left, so this reads at the peak only for
            // a lift that climbed from where it stood; one restarted from rest would be half way up.
            assertThat(controller.resolveBodyPressFractionAt(FIRST_ROW_SLOT))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void pressBodyControlAtPointStartsNoPressLiftForAPressOnBlankBody() {
            // The rule the sound already answers to, on the seen channel: a press that reached no cell has
            // nothing to light, so blank body swallows the click without the strip showing anything for it.
            controller.pressBodyControlAtPoint(
                buildBodyPlacement(buildCheckboxControl("Muted", ControlAction.NONE)),
                ROW.x() - 10f,
                ROW.y() + ROW.height() / 2f);

            controller.advanceBodyPressPulses(FULL_STEP_SECONDS, PRESS_DURATIONS);

            assertThat(controller.resolveBodyPressFractionAt(FIRST_ROW_SLOT))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class AdvanceBodyPressPulses {

        private final PanelController controller = new PanelController();

        @Test
        void advanceBodyPressPulsesLeavesASpentLiftAtRest() {
            // A press is an act already over, so its lift times its own fall and is gone: nothing else lets
            // go of it, and one left standing would mark a click the player made minutes ago.
            controller.pressBodyControlAtPoint(
                buildBodyPlacement(buildCheckboxControl("Muted", ControlAction.NONE)),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            controller.advanceBodyPressPulses(FULL_STEP_SECONDS, PRESS_DURATIONS);
            controller.advanceBodyPressPulses(FULL_STEP_SECONDS, PRESS_DURATIONS);

            assertThat(controller.resolveBodyPressFractionAt(FIRST_ROW_SLOT))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class ResolveBodyPressFractionAt {

        @Test
        void resolveBodyPressFractionAtReadsAtRestBeforeAnyPressHasLanded() {
            // A freshly built panel has been pressed nowhere, so its first painted frame must show a strip
            // at rest rather than a cell already part-way lit.
            assertThat(new PanelController().resolveBodyPressFractionAt(FIRST_ROW_SLOT))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class ResetBodyPressPulses {

        private final PanelController controller = new PanelController();

        @Test
        void resetBodyPressPulsesDropsALiftLeftPartWayThroughItsCycle() {
            // A panel that stops showing drops what it was mid-way through, so the next session does not
            // open painting the tail of a press the player never saw made.
            controller.pressBodyControlAtPoint(
                buildBodyPlacement(buildCheckboxControl("Muted", ControlAction.NONE)),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            controller.advanceBodyPressPulses(HALF_STEP_SECONDS, PRESS_DURATIONS);
            controller.resetBodyPressPulses();

            assertThat(controller.resolveBodyPressFractionAt(FIRST_ROW_SLOT))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class ActivateCellIfActionable {

        @Test
        void activateCellIfActionableFiresTheCellItWasHandedWithoutHitTestingForOne() {
            // The seam a header press reaches directly, having resolved its own tab already: the cell handed
            // over is the cell that fires, with no point to test it against. Pinned apart from the press
            // above because that caller resolves and fires in one call, and this one cannot - a tab lifts on
            // the raw hit and acts only if the row says that hit is worth acting on.
            var firedCell = new int[] {-1};
            var tabs = buildTwoTabRowAtRow(0, cell -> firedCell[0] = cell);
            var activatedCell = PanelController.activateCellIfActionable(tabs, 1);

            assertThat(activatedCell)
                .isEqualTo(1);
            assertThat(firedCell[0])
                .as("the cell reported is the cell the action fired for")
                .isEqualTo(1);
        }

        @Test
        void activateCellIfActionableActsOnNothingForAControlThatIsNotInteractive() {
            // A caption carries no action to reach, so a cell named on one goes nowhere rather than throwing
            // on the cast that would reach it. Nothing resolves a cell on a label today, which is exactly
            // why this is stated here rather than left to whichever caller first hands one over.
            var label = buildCaptionControl();

            assertThat(PanelController.activateCellIfActionable(label, ControlSpec.SINGLE_CELL))
                .isNull();
        }
    }

    @Nested
    class IsSegmentedControl {

        @Test
        void isSegmentedControlIsTrueForARowOfOptionSegments() {
            // The rule the hit-test turns on, and the one a hover reads to say what kind of thing it
            // reached: a control whose cells are laid side by side is a row of things alike.
            var radio = buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio.of(
                List.of("Left", "Right"),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE));

            assertThat(PanelController.isSegmentedControl(radio))
                .isTrue();
        }

        @Test
        void isSegmentedControlIsFalseForAWholeRowCheckbox() {
            // Hit anywhere on its bounds rather than by segment, which is the other side of the same rule.
            assertThat(PanelController.isSegmentedControl(buildCheckboxControl("Muted", ControlAction.NONE)))
                .isFalse();
        }

        @Test
        void isSegmentedControlIsFalseForChrome() {
            // A caption has no cells at all, so nothing about it is one of many alike. Nothing resolves a
            // cell on one today, which is why the answer is stated here rather than left to whichever
            // reader first asks it of something that was never a hit target.
            assertThat(PanelController.isSegmentedControl(buildCaptionControl()))
                .isFalse();
        }
    }

    @Nested
    class ResolveHitBodyCell {

        @Test
        void resolveHitBodyCellReportsTheHitControlWithoutFiringItsAction() {

            var fired = new boolean[1];

            // The walk answers geometry alone: the same press that fires through pressBodyControlAtPoint
            // reports its control here with the action untouched, which is what lets a hover - a reader that
            // only wants to know what is under a point - share the walk with the press.
            var checkbox = buildCheckboxControl("Muted", cell -> fired[0] = true);
            var resolvedCell = PanelController.resolveHitBodyCell(
                buildBodyPlacement(checkbox),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .isEqualTo(new ResolvedBodyCell(checkbox, new BodyCellSlot(0, 0)));
            assertThat(fired[0])
                .as("resolving a cell must not fire it")
                .isFalse();
        }

        @Test
        void resolveHitBodyCellReportsTheLitSegmentOfAnInertRowLikeAnyOther() {

            // The contract line at the level a hover reads: an INERT row swallows a press on its lit
            // segment, and the segment is still what the point is over. Pinned on the walk as well as on the
            // single-control resolver, because a narrowing added here would leave the lit control dark while
            // every test of the resolver below it still passed.
            var radio = buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio.of(
                List.of("Short", "Full"),
                0,
                ControlAction.NONE));

            var resolvedCell = PanelController.resolveHitBodyCell(
                buildBodyPlacement(radio),
                ROW.x() + ROW.width() / 4f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .as("what a point is over does not depend on what pressing it would do")
                .isEqualTo(new ResolvedBodyCell(radio, new BodyCellSlot(0, 0)));
        }

        @Test
        void resolveHitBodyCellRejectsAScrollingListRowScrolledOutOfItsViewport() {

            // The step's own rule, stated where a hover will read it: the row's segment sits at ROW and the
            // viewport is a strip well above it, as if the row had scrolled up under a pinned control, so a
            // point over the clipped-out row is over nothing. The walk asks for the viewport itself, which
            // is why no caller can forget to.
            var list = buildScrollingListAtRow(ControlAction.NONE);
            var resolvedCell = PanelController.resolveHitBodyCell(
                buildBodyPlacement(buildViewportAboveRow(), list),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .as("a row clipped from the viewport is under nothing")
                .isNull();
        }

        @Test
        void resolveHitBodyCellReachesTheControlDrawnOverAScrolledAwayRow() {

            // Why the clip has to live in the walk rather than beside it. Both controls are laid at ROW -
            // the list's row having scrolled up to where the pinned checkbox is drawn - so the point is over
            // two laid-out controls and only one of them is on screen. Without the clip the walk stops on
            // the invisible row first and the checkbox the player can actually see never answers.
            var checkbox = buildCheckboxControl("Muted", ControlAction.NONE);
            var resolvedCell = PanelController.resolveHitBodyCell(
                buildBodyPlacement(
                    buildViewportAboveRow(),
                    buildScrollingListAtRow(ControlAction.NONE),
                    checkbox),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .as("the control on screen answers, not the one clipped away under it")
                .isEqualTo(new ResolvedBodyCell(checkbox, new BodyCellSlot(1, 0)));
        }

        @Test
        void resolveHitBodyCellReportsTheSlotTheHitControlOccupiesInTheStrip() {

            // Where a hit landed, not only what it landed on: a hover is held against the slot, so a walk
            // that reported the second control with the first one's position would light a control the
            // pointer is not on. Three controls with the hit on the last, so a slot answered off the walk's
            // start or off its first match reads as a wrong number rather than as the right one by luck.
            var checkbox = buildCheckboxControl("Muted", ControlAction.NONE);
            var resolvedCell = PanelController.resolveHitBodyCell(
                buildBodyPlacement(
                    buildCaptionControl(),
                    new Control(new ControlSpec.Divider(), ROW, List.of()),
                    checkbox),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .isEqualTo(new ResolvedBodyCell(checkbox, new BodyCellSlot(2, 0)));
        }

        @Test
        void resolveHitBodyCellReportsNoControlForAPointOutsideTheBoxTheBodyIsDrawnIn() {

            // The fold, reaching the body the way it actually reaches it. A collapsing panel narrows the box
            // and clips the body to it while every control keeps the position the layout gave it, so this
            // checkbox is laid where it always was and is on screen nowhere. Stated in the walk rather than
            // beside the press, which escapes it by accident: the press never gets this far, and a hover
            // that walked the strip for itself would light a control behind the docked rail.
            var checkbox = buildCheckboxControl("Muted", ControlAction.NONE);
            var resolvedCell = PanelController.resolveHitBodyCell(
                buildBodyPlacementBoxedTo(buildDockedRailBox(), checkbox),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .as("a control the fold has wiped off the screen is under nothing")
                .isNull();
        }

        @Test
        void resolveHitBodyCellReportsNoControlForAPointOnBlankBody() {

            var checkbox = buildCheckboxControl("Muted", ControlAction.NONE);
            var resolvedCell = PanelController.resolveHitBodyCell(
                buildBodyPlacement(checkbox),
                ROW.x() - 10f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .isNull();
        }
    }

    @Nested
    class ResolveHitCell {

        @Test
        void resolveHitCellReportsTheHitCellWithoutFiringItsAction() {

            var fired = new boolean[1];

            // Resolution answers geometry alone: the same press that fires through pressBodyControlAtPoint
            // reports its cell here while the control's action stays untouched, which is what lets a
            // reader that only wants to know what is under a point share this path with the press.
            var checkbox = buildCheckboxControl("Muted", cell -> fired[0] = true);
            var resolvedCell = PanelController.resolveHitCell(
                checkbox,
                FULL_VIEWPORT,
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .as("a single-cell control resolves to cell 0")
                .isZero();
            assertThat(fired[0])
                .as("resolving a cell must not fire it")
                .isFalse();
        }

        @Test
        void resolveHitCellRejectsAScrollingListOptionScrolledOutOfItsViewport() {

            var list = buildScrollingListAtRow(ControlAction.NONE);

            // The option's segment sits at ROW, but the viewport is a strip well above it - as if the row
            // scrolled up under the header - so a point over the clipped-out row resolves to no cell.
            var resolvedCell = PanelController.resolveHitCell(
                list,
                buildViewportAboveRow(),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .as("a row clipped from the viewport is under nothing")
                .isNull();
        }

        @Test
        void resolveHitCellIgnoresTheViewportForANonScrollingControl() {

            var checkbox = buildCheckboxControl("Muted", ControlAction.NONE);

            // The clip is the scrolling list's alone: a pinned control is drawn wherever it was laid, so a
            // viewport that excludes it says nothing about whether the player can see it. Without this the
            // clause holds only by luck, every other case passing a viewport that covers its control.
            var resolvedCell = PanelController.resolveHitCell(
                checkbox,
                buildViewportAboveRow(),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .as("a control that never scrolls is not clipped by the flex viewport")
                .isZero();
        }

        @Test
        void resolveHitCellReportsTheLitSegmentOfADeselectableRadio() {

            var radio = buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio.of(
                    List.of("Factions", "Alliances"),
                    0,
                    ControlAction.NONE)
                .handlesReselect(ReselectBehaviour.DESELECT));

            var resolvedCell = PanelController.resolveHitCell(
                radio,
                ROW.x() + ROW.width() / 4f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .isZero();
        }

        @Test
        void resolveHitCellReportsTheLitSegmentOfAnInertRadioToo() {

            // The line this resolver is drawn along: an INERT row swallows a press on its lit segment, and
            // the segment is still what the point is over. Pinned beside the deselectable case above, which
            // resolves the same way for a different reason - so a narrowing creeping back in shows here
            // rather than hiding behind a reselect that would have answered alike.
            var radio = buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio.of(
                List.of("Short", "Full"),
                0,
                ControlAction.NONE));

            var resolvedCell = PanelController.resolveHitCell(
                radio,
                ROW.x() + ROW.width() / 4f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .as("what a point is over does not depend on what pressing it would do")
                .isZero();
        }

        @Test
        void resolveHitCellReportsTheLitTabOfAnAlwaysInertTabsRow() {

            // The case the sidebar's look rests on: a tabs row fires nothing on the tab it is already
            // showing, and that tab still has to light under the pointer - the resting and the selected tab
            // converge on one hovered shade, with the underline left to mark the selection. A resolver that
            // answered "what would a press act on" would leave the lit tab dark for as long as it is lit.
            var tabs = buildTwoTabRowAtRow(0, ControlAction.NONE);
            var resolvedCell = PanelController.resolveHitCell(
                tabs,
                ROW.x() + ROW.width() / 4f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .as("the lit tab is under the pointer like any other")
                .isZero();
        }
    }

    @Nested
    class HandlePointer {

        private final UiSoundPlayerFake soundPlayerFake = new UiSoundPlayerFake();

        @Test
        void handlePointerSoundsTheWheelThatMovedTheList() {
            // One act, one sound. The wheel is the panel's answer to the list moving as a whole, which is
            // what lets the rows it carries past the cursor stay quiet.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacement());

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.LIST_SCROLLED);
        }

        @Test
        void handlePointerStaysSilentForAWheelAgainstTheEndOfTheList() {
            // Sounded on the list having moved rather than on the wheel having turned, for the reason a
            // release that let go of nothing must not click at the player: the panel answers what happened,
            // and at the end of a list nothing did.
            var controller = buildVanillaSoundingController();
            var placement = buildScrollingPlacement();

            controller.handlePointer(PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y), placement);

            soundPlayerFake.clearPlayedCues();

            controller.handlePointer(PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y), placement);

            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void handlePointerStaysSilentForAWheelOverAListThatFits() {
            // A body with nothing to scroll swallows the wheel so the surface behind does not act on it,
            // and swallowing is not an act of its own - a panel that ticked here would answer every wheel
            // turn the player made over it whether or not it had anything to show for it.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildUnscrollablePlacement());

            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void handlePointerTakesTheScrollRoleFromTheLookRatherThanNamingOne() {
            // The point of the seam, at the one moment this end answers audibly: which sound a wheel makes
            // is the panel's look talking. A scheme agreeing with a hardcoded role would pass whether or
            // not it was ever read.
            var controller = buildControllerSounding(SWAPPED_SOUNDS);

            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacement());

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_PRESSED);
        }

        @Test
        void handlePointerStaysSilentForAWheelOffTheScrollRegion() {
            // The wheel reaches the list only over the list. Off it - over a control pinned above or below
            // the scrolling strip, or over the scrollbar gutter - the panel still swallows the event so the
            // surface behind does not pan, and swallowing has nothing to answer for.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                buildGutteredPlacement());

            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void handlePointerStaysSilentForAScrollbarDragThatMovedTheList() {
            // A drag is one held act carrying the list continuously, with the pointer off on the scrollbar
            // rather than on the rows. Sounded per frame it would be exactly the chatter the wheel's single
            // sound exists to avoid, so the moment belongs to the wheel alone.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockLeftPressAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                buildGutteredPlacement());

            assertThat(controller.getScrollState().getOffset())
                .as("the drag has to have moved the list for the silence to mean anything")
                .isEqualTo(SHORT_SCROLL_OVERFLOW);
            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void handlePointerSettlesAWheeledOffsetWithinWhatTheListCanScroll() {
            // Settled at the wheel rather than left to the next layout's clamp, which is what makes the
            // silence above real: an unsettled request runs past the end of the list, so every further
            // notch would change a number and read as a list that moved.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacement());

            assertThat(controller.getScrollState().getOffset())
                .as("one notch is longer than this list's overflow")
                .isEqualTo(SHORT_SCROLL_OVERFLOW);
        }

        @Test
        void handlePointerSoundsThePressThatLandedOnABodyControl() {
            // The routing, which is the half of the press the method above cannot show: a left press over
            // the body reaches the control under it rather than being swallowed as an event the panel only
            // consumes.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockLeftPressAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacementOver(buildCheckboxControl("Muted", ControlAction.NONE)));

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_PRESSED);
        }

        @Test
        void handlePointerStaysSilentForAPressInTheScrollbarGrabColumn() {
            // The order the branches are tried in, stated as a sound. The grab column is laid over the body,
            // so a control sits under this press - and the drag takes it before any control is offered it,
            // which is what keeps the scrollbar from answering like a control.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockLeftPressAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                buildGutteredPlacementOver(buildCheckboxControl("Muted", ControlAction.NONE)));

            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void handlePointerStaysSilentForAPressOnAControlTheFoldHasWipedOffTheScreen() {
            // A press outside the box the body is drawn in never reaches the body at all. The checkbox is
            // laid where it always was and the box has narrowed to a docked panel's rail, so the control
            // under this press is on screen nowhere - and a control nobody can see must not answer.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockLeftPressAt(ON_LIST_X, ON_LIST_Y),
                buildBodyPlacementBoxedTo(
                    buildDockedRailBox(),
                    buildCheckboxControl("Muted", ControlAction.NONE)));

            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void handlePointerStaysSilentForAPointerMovedOverABodyControl() {
            // A press is what a control answers, and the pointer merely being over one is not that. The
            // panel claims every event it covers, so the branch that tells them apart is the only thing
            // between a control and a sound for each frame the cursor rests on it.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockPointerEventAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacementOver(buildCheckboxControl("Muted", ControlAction.NONE)));

            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void handlePointerSoundsOnlyTheWheelForAWheelOverABodyControl() {
            // The wheel is not a press, however squarely it lands on a control. Pinned because both moments
            // are answered from this end now, and a panel that sounded both would tick twice for one turn.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacementOver(buildCheckboxControl("Muted", ControlAction.NONE)));

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.LIST_SCROLLED);
        }

        // A controller recording into this case's fake and answering by the engine's own scheme - the look
        // every case not about the scheme itself is written against.
        private PanelController buildVanillaSoundingController() {
            return buildControllerSounding(UiSoundScheme.createVanillaSoundScheme());
        }

        // The same, by whichever scheme the case is about.
        private PanelController buildControllerSounding(UiSoundScheme soundScheme) {
            return new PanelController(soundPlayerFake, soundScheme);
        }
    }

    @Nested
    class TakeHasListScrolledSinceLastFrame {

        private final PanelController controller = new PanelController();

        @Test
        void takeHasListScrolledSinceLastFrameReportsAWheelThatMovedTheList() {
            // What a per-frame pass reads to tell rows carried under a still pointer from a pointer moving
            // over rows. Pinned here rather than only through a panel that acts on it, since the fault it
            // guards against - an arrival announced for a row nobody reached - shows nowhere else.
            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacement());

            assertThat(controller.takeHasListScrolledSinceLastFrame())
                .isTrue();
        }

        @Test
        void takeHasListScrolledSinceLastFrameReportsAScrollbarDragThatMovedTheList() {
            // The drag reports through the same latch as the wheel, silent though it is: what the latch
            // answers is that content moved and not what moved it, and rows carried past a cursor parked
            // off on the scrollbar were reached by nobody either way.
            controller.handlePointer(
                PointerEventMocks.mockLeftPressAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                buildGutteredPlacement());

            assertThat(controller.takeHasListScrolledSinceLastFrame())
                .isTrue();
        }

        @Test
        void takeHasListScrolledSinceLastFrameReportsNothingForAWheelAgainstTheEndOfTheList() {
            // A list already against its stop shows the same rows afterwards, so nothing was carried under
            // the pointer and the frame after it is an ordinary frame.
            var placement = buildScrollingPlacement();

            controller.handlePointer(PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y), placement);
            controller.takeHasListScrolledSinceLastFrame();
            controller.handlePointer(PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y), placement);

            assertThat(controller.takeHasListScrolledSinceLastFrame())
                .isFalse();
        }

        @Test
        void takeHasListScrolledSinceLastFrameIsClearedByTheReading() {
            // One movement is answered by the first frame after it and by that frame alone. Left standing,
            // every later frame would adopt whatever is under the cursor and the panel would go permanently
            // deaf to the pointer arriving on anything in its body.
            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacement());

            controller.takeHasListScrolledSinceLastFrame();

            assertThat(controller.takeHasListScrolledSinceLastFrame())
                .isFalse();
        }

        @Test
        void takeHasListScrolledSinceLastFrameReportsNothingOnceTheMovementWasReset() {
            // A movement no frame ever read is a movement the next session must not answer to: the panel
            // stopped showing between the scroll and the frame that would have adopted on it, and the
            // player has been somewhere else since.
            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacement());
                
            controller.resetListScrolled();

            assertThat(controller.takeHasListScrolledSinceLastFrame())
                .isFalse();
        }
    }

    // A panel whose body has somewhere to scroll and nothing laid in it: its viewport is the row and its
    // content overruns it by less than a wheel notch, so one notch takes the list to its end and the next
    // has nowhere to go.
    private static PanelPlacement buildScrollingPlacement() {
        return buildScrollingPlacementOver();
    }

    // The same panel with the given controls laid in it, for a case that has to tell what the wheel and the
    // scrollbar answer from what a control does - a body with nothing in it would be silent either way.
    private static PanelPlacement buildScrollingPlacementOver(Control... bodyControls) {
        return new PanelPlacement(ROW, ROW, List.of(bodyControls), ROW, 0f, SHORT_SCROLL_OVERFLOW);
    }

    // The same panel whose content fits, so there is no scrollbar and the wheel moves nothing.
    private static PanelPlacement buildUnscrollablePlacement() {
        return new PanelPlacement(ROW, ROW, List.of(), ROW, 0f, NO_SCROLL_OVERFLOW);
    }

    // The same scrolling panel with its list narrowed off the box's right edge, leaving the gutter a drag
    // grabs the scrollbar by. Every other case here lays the viewport across the whole box, which leaves no
    // gutter at all - so the drag and the off-the-list wheel need a body shaped like the real one.
    private static PanelPlacement buildGutteredPlacement() {
        return buildGutteredPlacementOver();
    }

    // The guttered panel with the given controls laid across the whole row, gutter included - which is where
    // the real ones sit, the grab column being drawn over the body rather than beside it. What a press in
    // that column answers is then a question the placement can actually pose.
    private static PanelPlacement buildGutteredPlacementOver(Control... bodyControls) {

        var list = new Rectangle(ROW.x(), ROW.y(), ROW.width() - SCROLLBAR_GUTTER_WIDTH, ROW.height());
        return new PanelPlacement(ROW, ROW, List.of(bodyControls), list, 0f, SHORT_SCROLL_OVERFLOW);
    }

    // A panel whose body holds the given controls and whose flex viewport covers everything, so the walk
    // over it is clipped only in the cases that build a viewport of their own.
    private static PanelPlacement buildBodyPlacement(Control... bodyControls) {
        return buildBodyPlacement(FULL_VIEWPORT, bodyControls);
    }

    // The same panel with the flex viewport a case wants to exercise the clip with. Its box covers
    // everything, so the walk's own box gate passes and each case here is about the clip it names.
    private static PanelPlacement buildBodyPlacement(
            Rectangle flexViewport,
            Control... bodyControls) {

        return buildBodyPlacement(FULL_VIEWPORT, flexViewport, bodyControls);
    }

    // The same panel narrowed to the box a case wants to exercise the fold with, its flex viewport covering
    // everything so the box is the only gate the walk can fail on.
    private static PanelPlacement buildBodyPlacementBoxedTo(Rectangle box, Control... bodyControls) {
        return buildBodyPlacement(box, FULL_VIEWPORT, bodyControls);
    }

    // The panel every case above is walked against: a box the body is drawn inside, a flex viewport its
    // scrolling control is clipped to, and the controls laid in it. The body region is the box, nothing
    // here reading it.
    private static PanelPlacement buildBodyPlacement(
            Rectangle box,
            Rectangle flexViewport,
            Control... bodyControls) {

        return new PanelPlacement(
            box,
            box,
            List.of(bodyControls),
            flexViewport,
            0f,
            0f);
    }

    // The rail a fully docked panel leaves of its box: a border's width at the body's left edge, well clear
    // of ROW, so a control still laid out at ROW is one the fold has wiped off the screen.
    private static Rectangle buildDockedRailBox() {
        return new Rectangle(ROW.x(), ROW.y(), 1f, ROW.height());
    }

    // A flex viewport sitting well above ROW, so a scrolling control laid out at ROW reads as a row that has
    // scrolled up out of sight under whatever is pinned above the list.
    private static Rectangle buildViewportAboveRow() {
        return new Rectangle(ROW.x(), ROW.y() + 100f, ROW.width(), 40f);
    }

    // A single-row checkbox occupying ROW, so each test states only the label and action that
    // distinguish its case rather than repeating the spec-and-bounds construction.
    private static Control buildCheckboxControl(String label, ControlAction action) {
        return new Control(LabelledControlSpecs.buildCheckbox(label, false, action), ROW, List.of());
    }

    // A caption occupying ROW - chrome, drawn but never clickable, and so what every case about a control
    // that is not a hit target reads. Its label says nothing, no case here turning on the words.
    private static Control buildCaptionControl() {
        return new Control(LabelledControlSpecs.buildLabel("Caption"), ROW, List.of());
    }

    // A one-option scrolling list laid out at ROW: a vertical icon table marked as the scroll region, its
    // single segment the whole row, so a press at ROW hits option 0 unless the viewport clips it.
    private static Control buildScrollingListAtRow(ControlAction action) {

        var spec = VerticalTableSpecs.buildIconList(
                List.of("Opt"),
                Arrays.asList((String) null),
                ControlSpec.NO_SELECTION,
                action)
            .asScrolling();

        return new Control(spec, ROW, List.of(ROW));
    }

    // A two-segment horizontal radio occupying ROW, split into two equal segment boxes (left, right), so a
    // press in a segment's box hits that segment - and a press on the lit segment fires or is swallowed by
    // the radio's own reselect. The caller supplies the spec so a test picks the deselectable or inert row.
    private static Control buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio spec) {

        var leftSegment = new Rectangle(ROW.x(), ROW.y(), ROW.width() / 2f, ROW.height());
        var rightSegment = new Rectangle(
            ROW.x() + ROW.width() / 2f,
            ROW.y(),
            ROW.width() / 2f,
            ROW.height());

        return new Control(spec, ROW, List.of(leftSegment, rightSegment));
    }

    // A two-tab row occupying ROW, split into two equal per-tab boxes (left tab, right tab), the lit tab
    // at selectedIndex, so a press in a tab's box hits that tab unless its own inert-on-lit reselect
    // swallows it. Shortcuts are irrelevant to the hit-test, so the tabs carry none.
    private static Control buildTwoTabRowAtRow(int selectedIndex, ControlAction action) {

        var leftTab = new Rectangle(ROW.x(), ROW.y(), ROW.width() / 2f, ROW.height());
        var rightTab = new Rectangle(
            ROW.x() + ROW.width() / 2f,
            ROW.y(),
            ROW.width() / 2f,
            ROW.height());

        var spec = new ControlSpec.Tabs(
            List.of("Political Map", "Alliances"),
            List.of(),
            selectedIndex,
            action);

        return new Control(spec, ROW, List.of(leftTab, rightTab));
    }
}
