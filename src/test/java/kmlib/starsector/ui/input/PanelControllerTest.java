package kmlib.starsector.ui.input;

import kmlib.animation.TraverseDurations;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.specs.ControlAction;
import kmlib.starsector.ui.controls.specs.ControlSpec;
import kmlib.starsector.ui.controls.specs.DividerSpec;
import kmlib.starsector.ui.controls.specs.HorizontalRadioSpec;
import kmlib.starsector.ui.controls.specs.ReselectBehaviour;
import kmlib.starsector.ui.sound.PointerArrivalTarget;
import kmlib.starsector.ui.sound.StarsectorUiSound;
import kmlib.starsector.ui.sound.UiSoundCue;
import kmlib.starsector.ui.sound.UiSoundScheme;
import kmlib.starsector.ui.widgets.scroll.ScrollbarThickness;
import kmlib.testfixtures.starsector.ui.sound.UiSoundPlayerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static kmlib.starsector.ui.input.PanelBodyFixtures.IN_GRAB_COLUMN_X;
import static kmlib.starsector.ui.input.PanelBodyFixtures.IN_GRAB_COLUMN_Y;
import static kmlib.starsector.ui.input.PanelBodyFixtures.ON_LIST_X;
import static kmlib.starsector.ui.input.PanelBodyFixtures.ON_LIST_Y;
import static kmlib.starsector.ui.input.PanelBodyFixtures.ROW;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildBodyPlacement;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildBodyPlacementBoxedTo;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildCaptionControl;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildCheckboxControl;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildDockedRailBox;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildGutteredPlacement;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildGutteredPlacementAtThickness;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildGutteredPlacementOver;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildScrollingListAtRow;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildScrollingPlacement;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildScrollingPlacementOver;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildTwoSegmentHorizontalRadioAtRow;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildTwoTabRowAtRow;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildViewportAboveRow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.verify;

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
 * <p>The press cases pin the moment this end answers audibly, and the rule that decides it: the sound follows
 * the press having reached a control rather than having fired one, so an inert cell sounds like the press it
 * was and chrome stays silent. How the list moves and sounds is the scroll controller's and pinned there; the
 * pointer cases here pin only the routing to it - which branch an event reaches, in which order - since a
 * press in the grab column is a drag before it is a control's, and a held drag is followed before the panel
 * asks whether the pointer is over it at all.
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
    // What the frame's reading carries with the pointer on no body cell - off the strip, or on chrome that
    // resolves to nothing - named so an advance reads as a pointer position rather than as a bare null.
    private static final HoveredBodyCell NO_CELL_HOVERED = null;
    private static final BodyCellSlot NO_SLOT_HOVERED = null;

    // What a host answering a hover is told as the pointer leaves, named the way the panel's own cases name
    // it so one concept reads one way across the package.
    private static final Integer NO_CELL_REPORTED = null;

    // What the host behind a hovered control was told, in the order it was told. One recorder for the cases
    // about the body's frame pass, since each builds its own controller and only its own reading reaches it.
    private final List<Integer> reportedCells = new ArrayList<>();

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
            var radio = buildTwoSegmentHorizontalRadioAtRow(HorizontalRadioSpec.of(
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
            var radio = buildTwoSegmentHorizontalRadioAtRow(HorizontalRadioSpec.of(
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
            var radio = buildTwoSegmentHorizontalRadioAtRow(HorizontalRadioSpec.of(
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
                buildBodyPlacement(new Control(new DividerSpec(), ROW, List.of())),
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
            var radio = buildTwoSegmentHorizontalRadioAtRow(HorizontalRadioSpec.of(
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
    class HandlePointerClaims {

        private final PanelController controller = new PanelController();

        @Test
        void handlePointerParksAMoveOverThePanelRatherThanConsumingIt() {
            // What the screen underneath is owed. A consumed move is invisible to it, and a vanilla control
            // lets go of its hover only on hearing a move that is not on it - so consuming here would leave
            // whatever was lit when the pointer crossed onto the panel lit for as long as it rests here.
            // Moved instead, and left unconsumed, the screen hears the move and finds nothing under it.
            var moveFake = RelocatableEventFake.createMoveAt(Math.round(ON_LIST_X), Math.round(ON_LIST_Y));

            controller.handlePointer(moveFake, buildScrollingPlacement());

            assertThat(moveFake.isConsumed())
                .isFalse();
            assertThat(moveFake.getX())
                .isNotEqualTo(Math.round(ON_LIST_X));
        }

        @Test
        void handlePointerConsumesAPressOverThePanel() {
            // A press is an act the panel answers, which is exactly why the surface behind it must not.
            // Moving the pointer instead of consuming would hand the press to whatever now sits under it.
            var pressFake =
                RelocatableEventFake.createLeftPressAt(Math.round(ON_LIST_X), Math.round(ON_LIST_Y));

            controller.handlePointer(pressFake, buildScrollingPlacement());

            assertThat(pressFake.isConsumed())
                .isTrue();
        }

        @Test
        void handlePointerConsumesAWheelOverThePanel() {
            // The wheel scrolls this panel's list, so the list behind it must not scroll too - a claim that
            // only consuming makes.
            var wheelMock = PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y);

            controller.handlePointer(wheelMock, buildScrollingPlacement());

            verify(wheelMock)
                .consume();
        }

        @Test
        void handlePointerLeavesAMoveClearOfThePanelAlone() {
            // Off the panel the event is not the panel's to claim at all: neither consumed nor moved, since
            // a move the panel never touched is already the screen's to read where the player made it.
            var moveFake = RelocatableEventFake.createMoveAt(
                Math.round(ROW.x() + ROW.width() + 1f),
                Math.round(ROW.y()));

            controller.handlePointer(moveFake, buildScrollingPlacement());

            assertThat(moveFake.isConsumed())
                .isFalse();
            assertThat(moveFake.getX())
                .isEqualTo(Math.round(ROW.x() + ROW.width() + 1f));
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
    class AdvanceBodyInputMotionsForFrame {

        private final PanelController controller = new PanelController();

        @Test
        void advanceBodyInputMotionsForFrameRaisesTheHoveredSlotAndNoOther() {
            // The fade is keyed by the place under the pointer, so a frame lights that place alone - a
            // reading spent against the whole strip would light every cell of it at once.
            controller.advanceBodyInputMotionsForFrame(
                buildHoveredCell(FIRST_ROW_SLOT), FULL_STEP_SECONDS, PRESS_DURATIONS);

            assertThat(controller.resolveBodyHoverFractionAt(FIRST_ROW_SLOT))
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(controller.resolveBodyHoverFractionAt(SECOND_ROW_SLOT))
                .as("a slot the pointer is not on stays at rest")
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceBodyInputMotionsForFrameWindsTheDepartedSlotBackDown() {
            // A fade travels both ways, so the cell the pointer left comes back off its hovered look under
            // its own steam rather than being cut to nothing the frame the reading changed.
            controller.advanceBodyInputMotionsForFrame(
                buildHoveredCell(FIRST_ROW_SLOT), FULL_STEP_SECONDS, PRESS_DURATIONS);
            controller.advanceBodyInputMotionsForFrame(
                NO_CELL_HOVERED, FULL_STEP_SECONDS, PRESS_DURATIONS);

            assertThat(controller.resolveBodyHoverFractionAt(FIRST_ROW_SLOT))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceBodyInputMotionsForFrameReportsTheHoveredCellToItsHost() {
            // The reading spent outwards rather than on paint. Off a slot whose two halves are different
            // numbers, so a report carrying the control's place where it means its cell reads as a wrong
            // number rather than as the right one by coincidence.
            controller.advanceBodyInputMotionsForFrame(
                buildHoveredCell(RIGHT_SEGMENT_SLOT), FULL_STEP_SECONDS, PRESS_DURATIONS);

            assertThat(reportedCells)
                .containsExactly(1);
        }

        @Test
        void advanceBodyInputMotionsForFrameChargesAPressLiftRunningOnACell() {
            // One call charges every motion the body makes, so a panel pumping its frames through this one
            // cannot leave the lifts unstepped while the fades run - which would show as a press that never
            // fades out.
            controller.pressBodyControlAtPoint(
                buildBodyPlacement(buildCheckboxControl("Muted", ControlAction.NONE)),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            controller.advanceBodyInputMotionsForFrame(
                NO_CELL_HOVERED, FULL_STEP_SECONDS, PRESS_DURATIONS);

            assertThat(controller.resolveBodyPressFractionAt(FIRST_ROW_SLOT))
                .isCloseTo(1f, within(TOLERANCE));
        }
    }

    @Nested
    class DetectBodyCellArrivalAt {

        private final PanelController controller = new PanelController();

        @Test
        void detectBodyCellArrivalAtReportsThePointerReachingACell() {

            assertThat(controller.detectBodyCellArrivalAt(FIRST_ROW_SLOT))
                .isTrue();
        }

        @Test
        void detectBodyCellArrivalAtReportsNothingWhileThePointerRestsOnTheCell() {
            // A moment rather than a position, which is what the whole latch is for: the fade beside it
            // stands at the top for as long as the pointer stays, and an answer read off that would be a
            // tone rather than a tick.
            controller.detectBodyCellArrivalAt(FIRST_ROW_SLOT);

            assertThat(controller.detectBodyCellArrivalAt(FIRST_ROW_SLOT))
                .isFalse();
        }

        @Test
        void detectBodyCellArrivalAtReportsNothingForARowAWheelCarriedUnderTheCursor() {
            // The rule this end is the only one that can answer, being the end that moved the list: rows
            // sliding past a parked pointer were reached by nobody, so a wheel down a long list is one act
            // rather than one arrival per row it swept past.
            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacement());

            assertThat(controller.detectBodyCellArrivalAt(FIRST_ROW_SLOT))
                .isFalse();
        }

        @Test
        void detectBodyCellArrivalAtReportsTheRowAWheelLeftUnderTheCursorOnceThePointerReachesItItself() {
            // Adopted rather than gone deaf. The row the scroll carried under the cursor is taken without
            // being announced, so the pointer genuinely arriving on it afterwards is an arrival like any
            // other - a latch that had simply stopped tracking would swallow this one too.
            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacement());

            controller.detectBodyCellArrivalAt(FIRST_ROW_SLOT);
            controller.detectBodyCellArrivalAt(NO_SLOT_HOVERED);

            assertThat(controller.detectBodyCellArrivalAt(FIRST_ROW_SLOT))
                .isTrue();
        }
    }

    @Nested
    class ResetBodyInputMotions {

        private final PanelController controller = new PanelController();

        @Test
        void resetBodyInputMotionsDropsAFadeLeftPartWayUp() {
            // A panel that stops showing drops what it was mid-way through, so the next session does not
            // open painting the tail of a hover the player never made.
            controller.advanceBodyInputMotionsForFrame(
                buildHoveredCell(FIRST_ROW_SLOT), HALF_STEP_SECONDS, PRESS_DURATIONS);

            controller.resetBodyInputMotions();

            assertThat(controller.resolveBodyHoverFractionAt(FIRST_ROW_SLOT))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void resetBodyInputMotionsReportsTheLeaveToTheHostLastTold() {
            // What a panel going away is to whatever was answering the hover: no further frame resolves a
            // reading, so nothing else would tell that host to let go of the cell it holds.
            controller.advanceBodyInputMotionsForFrame(
                buildHoveredCell(RIGHT_SEGMENT_SLOT), FULL_STEP_SECONDS, PRESS_DURATIONS);

            controller.resetBodyInputMotions();

            assertThat(reportedCells)
                .containsExactly(1, NO_CELL_REPORTED);
        }

        @Test
        void resetBodyInputMotionsMakesAPointerParkedOnACellArriveAfresh() {
            // The strip was not there a moment ago, so the player reaching it is an arrival even though the
            // pointer never moved - the panel came to the cursor rather than the other way about.
            controller.detectBodyCellArrivalAt(FIRST_ROW_SLOT);

            controller.resetBodyInputMotions();

            assertThat(controller.detectBodyCellArrivalAt(FIRST_ROW_SLOT))
                .isTrue();
        }

        @Test
        void resetBodyInputMotionsDropsAScrollNoFrameHasReadYet() {
            // A movement no frame ever read is a movement the next session must not answer to: left
            // standing, the re-opened panel would adopt whatever is under the cursor and take that cell in
            // silence.
            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacement());

            controller.resetBodyInputMotions();

            assertThat(controller.detectBodyCellArrivalAt(FIRST_ROW_SLOT))
                .isTrue();
        }
    }

    @Nested
    class HandlePointer {

        private final UiSoundPlayerFake soundPlayerFake = new UiSoundPlayerFake();

        @Test
        void handlePointerRoutesAWheelOverTheListToIt() {
            // The wheel branch, shown by the one thing only the list answers with: the sound it makes on
            // moving. What decides that sound is the scroll controller's and pinned there.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacement());

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.LIST_SCROLLED);
        }

        @Test
        void handlePointerKeepsAHeldDragPastThePanelEdge() {
            // The order of the first two branches: a held drag is asked before the panel's own claim, so the
            // list keeps following a pointer that has wandered off the box rather than the drag dropping the
            // moment it leaves the narrow column. Grabbed low, which took the list to its end; a pointer
            // above the whole panel maps to the start, so a drag the box gate had dropped would leave it
            // where the press put it.
            var controller = buildVanillaSoundingController();
            var placement = buildGutteredPlacement();

            controller.handlePointer(
                PointerEventMocks.mockLeftPressAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                placement);
            controller.handlePointer(
                PointerEventMocks.mockMoveAt(ROW.x() + ROW.width() + 50f, ROW.y() + ROW.height() + 50f),
                placement);

            assertThat(controller.getScrollState().getOffset())
                .isZero();
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
        void handlePointerSoundsAPressInTheGrabColumnWhenNoBarIsDrawn() {
            // The same press one line up, on the same geometry, with the bar set away: with no track and no
            // thumb there is nothing over that column to grab, so it claims nothing and the press falls
            // through to the control it was always laid over. A bar of no width still taking presses would
            // leave a dead strip down the panel that nothing on screen accounts for.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockLeftPressAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                buildGutteredPlacementAtThickness(
                    ScrollbarThickness.NONE,
                    buildCheckboxControl("Muted", ControlAction.NONE)));

            assertThat(controller.getScrollState().getOffset())
                .as("no drag began, so the list is where it was")
                .isZero();
            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_PRESSED);
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

        // A controller recording into this case's fake and answering by the engine's own scheme.
        private PanelController buildVanillaSoundingController() {
            return new PanelController(soundPlayerFake, UiSoundScheme.createVanillaSoundScheme());
        }
    }

    // One frame's reading with the pointer on a cell reporting into this class's own recorder - what the
    // panel's own walk would have built. The kind of thing reached is left at the whole-row answer, no case
    // reading it through this path.
    private HoveredBodyCell buildHoveredCell(BodyCellSlot slot) {
        return new HoveredBodyCell(
            slot,
            PointerArrivalTarget.SINGLE_OPTION_CONTROL,
            reportedCells::add);
    }
}
