package kmlib.starsector.ui.input;

import kmlib.animation.TraverseDurations;
import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.ReselectBehaviour;
import kmlib.starsector.ui.sound.PointerArrivalTarget;
import kmlib.starsector.ui.sound.StarsectorUiSound;
import kmlib.starsector.ui.sound.UiSoundCue;
import kmlib.starsector.ui.sound.UiSoundScheme;
import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.starsector.ui.widgets.scroll.ScrollbarThickness;
import kmlib.testfixtures.starsector.ui.sound.UiSoundPlayerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static kmlib.starsector.ui.input.PanelBodyFixtures.ROW;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildBodyPlacement;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildBodyPlacementBoxedTo;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildCaptionControl;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildCheckboxControl;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildDockedRailBox;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildScrollingListAtRow;
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
 * <p>The wheel cases pin one of the two moments this end answers audibly, and the rule that decides it: the
 * sound follows the list having moved rather than the wheel having turned, so a notch against the end of a
 * list is as silent as a notch over a list that fits. The press cases pin the other, and the rule that
 * decides it: the sound follows the press having reached a control rather than having fired one, so an inert
 * cell sounds like the press it was and chrome stays silent.
 *
 * <p>The barless cases pin where those two part. A host may set the bar to no width at all, which takes the
 * track and the thumb away and with them the column a drag grabs - so the press that column would have
 * swallowed reaches the control beneath it, while the wheel, being what is left to move the list with, still
 * carries it and still sounds.
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

    // The bar a host has set away. The gutter is still there and the list still overruns it - which is what
    // makes the cases about it worth having, the geometry being identical either way.
    private static final ScrollbarThickness NO_SCROLLBAR = new ScrollbarThickness(0f);

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
        void handlePointerSoundsAPressInTheGrabColumnWhenNoBarIsDrawn() {
            // The same press one line up, on the same geometry, with the bar set away: with no track and no
            // thumb there is nothing over that column to grab, so it claims nothing and the press falls
            // through to the control it was always laid over. A bar of no width still taking presses would
            // leave a dead strip down the panel that nothing on screen accounts for.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockLeftPressAt(IN_GRAB_COLUMN_X, IN_GRAB_COLUMN_Y),
                buildGutteredPlacementAtThickness(
                    NO_SCROLLBAR,
                    buildCheckboxControl("Muted", ControlAction.NONE)));

            assertThat(controller.getScrollState().getOffset())
                .as("no drag began, so the list is where it was")
                .isZero();
            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_PRESSED);
        }

        @Test
        void handlePointerSoundsTheWheelThatMovedTheListWhenNoBarIsDrawn() {
            // The wheel is what is left to a player who has set the bar away, so it answers to the list
            // overrunning and not to the bar being drawn - taking it with the bar would strand the rows past
            // the viewport with no way to reach them.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(
                PointerEventMocks.mockWheelDownAt(ON_LIST_X, ON_LIST_Y),
                buildScrollingPlacementAtThickness(NO_SCROLLBAR));

            assertThat(controller.getScrollState().getOffset())
                .as("the list has to have moved for the sound to mean anything")
                .isEqualTo(SHORT_SCROLL_OVERFLOW);
            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.LIST_SCROLLED);
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
        return buildScrollingPlacementAtThickness(ScrollbarThickness.DEFAULT, bodyControls);
    }

    // The same panel with the bar's width named, for the cases about a host that has set it away. Taken as
    // an argument rather than written into a second placement, so a barless case and the cases above differ
    // in that one number and in nothing else.
    private static PanelPlacement buildScrollingPlacementAtThickness(
            ScrollbarThickness thickness,
            Control... bodyControls) {

        return new PanelPlacement(
            ROW,
            ROW,
            List.of(bodyControls),
            ROW,
            0f,
            SHORT_SCROLL_OVERFLOW,
            thickness);
    }

    // The same panel whose content fits, so there is no scrollbar and the wheel moves nothing.
    private static PanelPlacement buildUnscrollablePlacement() {
        return new PanelPlacement(
            ROW,
            ROW,
            List.of(),
            ROW,
            0f,
            NO_SCROLL_OVERFLOW,
            ScrollbarThickness.DEFAULT);
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
        return buildGutteredPlacementAtThickness(ScrollbarThickness.DEFAULT, bodyControls);
    }

    // The guttered panel with the bar's width named, for the same reason the scrolling one takes it: the
    // barless cases have to be the drawn ones with one number changed, the gutter and the list being where
    // they always were.
    private static PanelPlacement buildGutteredPlacementAtThickness(
            ScrollbarThickness thickness,
            Control... bodyControls) {

        var list = new Rectangle(ROW.x(), ROW.y(), ROW.width() - SCROLLBAR_GUTTER_WIDTH, ROW.height());
        return new PanelPlacement(
            ROW,
            ROW,
            List.of(bodyControls),
            list,
            0f,
            SHORT_SCROLL_OVERFLOW,
            thickness);
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
