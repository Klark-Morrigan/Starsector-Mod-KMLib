package kmlib.starsector.ui.input;

import com.fs.starfarer.api.input.InputEventAPI;

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
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
 * <p>The wheel cases pin the one moment this end answers audibly, and the rule that decides it: the sound
 * follows the list having moved rather than the wheel having turned, so a notch against the end of a list
 * is as silent as a notch over a list that fits.
 */
final class PanelControllerTest {

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

    // A wheel notch turned toward the bottom of the list. Only the sign is read, so the magnitude is
    // immaterial; the engine reports a wheel down as negative and the panel scrolls the list the other way.
    private static final int WHEEL_DOWN = -1;

    @Nested
    class ActivateBodyControlIfHit {

        @Test
        void activateBodyControlIfHitPassesOverACaptionLabelWithoutActing() {
            // A caption row is drawn but never clickable - a Label is not Interactive - so a press over
            // it hits nothing and falls through rather than being swallowed as if it acted.
            var label = buildCaptionControl();
            var activatedCell = PanelController.activateBodyControlIfHit(
                buildBodyPlacement(label),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell).as("a caption is not a hit target")
                .isNull();
        }

        @Test
        void activateBodyControlIfHitFiresACheckboxHitAnywhereOnItsRow() {

            var firedCell = new int[] {-1};
            var checkbox = buildCheckboxControl("Muted", cell -> firedCell[0] = cell);
            var activatedCell = PanelController.activateBodyControlIfHit(
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
        void activateBodyControlIfHitReportsNoHitForAPressOutsideACheckboxRow() {

            var checkbox = buildCheckboxControl("Muted", ControlAction.NONE);
            var activatedCell = PanelController.activateBodyControlIfHit(
                buildBodyPlacement(checkbox),
                ROW.x() - 10f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .isNull();
        }

        @Test
        void activateBodyControlIfHitFiresAScrollingListOptionInsideItsViewport() {

            var firedCell = new int[] {-1};
            var list = buildScrollingListAtRow(cell -> firedCell[0] = cell);

            // The press lands on the list's one option and inside a viewport that covers the row, so the
            // option fires as a normal radio hit.
            var activatedCell = PanelController.activateBodyControlIfHit(
                buildBodyPlacement(ROW, list),
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .isEqualTo(new ResolvedBodyCell(list, new BodyCellSlot(0, 0)));
            assertThat(firedCell[0])
                .isZero();
        }

        @Test
        void activateBodyControlIfHitRejectsAScrollingListOptionScrolledOutOfItsViewport() {

            var fired = new boolean[1];
            var list = buildScrollingListAtRow(cell -> fired[0] = true);

            // The option's segment sits at ROW, but the viewport is a strip well above it - as if the row
            // scrolled up under the header - so the press over the clipped-out row must not fire it.
            var activatedCell = PanelController.activateBodyControlIfHit(
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
        void activateBodyControlIfHitFiresATabHitReportedAsThatTabIndex() {

            var firedCell = new int[] {-1};

            // The lit tab is the left one, so a press on the right (non-lit) tab fires it by its index.
            var tabs = buildTwoTabRowAtRow(0, cell -> firedCell[0] = cell);
            var activatedCell = PanelController.activateBodyControlIfHit(
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
        void activateBodyControlIfHitTreatsAPressOnTheLitTabAsInert() {

            var fired = new boolean[1];

            // A tabs row is always inert on its lit tab (INERT reselect), so a press on the left, lit tab
            // reaches no action - matching a vanilla tab strip, where clicking the active tab does nothing.
            var tabs = buildTwoTabRowAtRow(0, cell -> fired[0] = true);
            var activatedCell = PanelController.activateBodyControlIfHit(
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
        void activateBodyControlIfHitReportsNoHitForAPressOutsideEveryTab() {

            var tabs = buildTwoTabRowAtRow(0, ControlAction.NONE);
            var activatedCell = PanelController.activateBodyControlIfHit(
                buildBodyPlacement(tabs),
                ROW.x() - 10f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .isNull();
        }

        @Test
        void activateBodyControlIfHitFiresADeselectableHorizontalRadioOnARepickOfItsLitSegment() {

            var firedCell = new int[] {-1};

            // A DESELECT horizontal radio wants the re-pick to reach the action so the host turns the
            // control off, so a press on the left, lit segment fires it by its index (not swallowed).
            var radio = buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio.of(
                    List.of("Factions", "Alliances"),
                    0,
                    cell -> firedCell[0] = cell)
                .handlesReselect(ReselectBehaviour.DESELECT));

            var activatedCell = PanelController.activateBodyControlIfHit(
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
        void activateBodyControlIfHitTreatsAPressOnAnInertHorizontalRadiosLitSegmentAsInert() {

            var fired = new boolean[1];

            // A plain option pair is always one lit (INERT reselect), so a press on the lit segment
            // reaches no action - the standard radio behaviour a deselectable row opts out of.
            var radio = buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio.of(
                List.of("Short", "Full"),
                0,
                cell -> fired[0] = true));

            var activatedCell = PanelController.activateBodyControlIfHit(
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

            // The walk answers geometry alone: the same press that fires through activateBodyControlIfHit
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

            // Resolution answers geometry alone: the same press that fires through activateControlIfHit
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

        // A look whose scroll role is not the one the panel would have named for itself, so a wheel answered
        // from this end's own code rather than from the look records the wrong sound. The vanilla scheme
        // could not tell the two apart.
        private static final UiSoundScheme SWAPPED_SOUNDS = new UiSoundScheme(
            UiSoundCue.createAtFullVolume(StarsectorUiSound.LIST_SCROLLED),
            StarsectorUiSound.BUTTON_MOUSEOVER,
            UiSoundCue.createAtFullVolume(StarsectorUiSound.BUTTON_PRESSED));

        private final UiSoundPlayerFake soundPlayerFake = new UiSoundPlayerFake();

        @Test
        void handlePointerSoundsTheWheelThatMovedTheList() {
            // One act, one sound. The wheel is the panel's answer to the list moving as a whole, which is
            // what lets the rows it carries past the cursor stay quiet.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(buildWheelEventAtRowCentre(WHEEL_DOWN), buildScrollingPlacement());

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

            controller.handlePointer(buildWheelEventAtRowCentre(WHEEL_DOWN), placement);
            
            soundPlayerFake.clearPlayedCues();

            controller.handlePointer(buildWheelEventAtRowCentre(WHEEL_DOWN), placement);

            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void handlePointerStaysSilentForAWheelOverAListThatFits() {
            // A body with nothing to scroll swallows the wheel so the surface behind does not act on it,
            // and swallowing is not an act of its own - a panel that ticked here would answer every wheel
            // turn the player made over it whether or not it had anything to show for it.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(buildWheelEventAtRowCentre(WHEEL_DOWN), buildUnscrollablePlacement());

            assertThat(soundPlayerFake.getPlayedSounds())
                .isEmpty();
        }

        @Test
        void handlePointerTakesTheScrollRoleFromTheLookRatherThanNamingOne() {
            // The point of the seam, at the one moment this end answers audibly: which sound a wheel makes
            // is the panel's look talking. A scheme agreeing with a hardcoded role would pass whether or
            // not it was ever read.
            var controller = buildControllerSounding(SWAPPED_SOUNDS);

            controller.handlePointer(buildWheelEventAtRowCentre(WHEEL_DOWN), buildScrollingPlacement());

            assertThat(soundPlayerFake.getPlayedSounds())
                .containsExactly(StarsectorUiSound.BUTTON_PRESSED);
        }

        @Test
        void handlePointerSettlesAWheeledOffsetWithinWhatTheListCanScroll() {
            // Settled at the wheel rather than left to the next layout's clamp, which is what makes the
            // silence above real: an unsettled request runs past the end of the list, so every further
            // notch would change a number and read as a list that moved.
            var controller = buildVanillaSoundingController();

            controller.handlePointer(buildWheelEventAtRowCentre(WHEEL_DOWN), buildScrollingPlacement());

            assertThat(controller.getScrollState().getOffset())
                .as("one notch is longer than this list's overflow")
                .isEqualTo(SHORT_SCROLL_OVERFLOW);
        }

        // A panel whose body has somewhere to scroll: its viewport is the row and its content overruns it by
        // less than a wheel notch, so one notch takes the list to its end and the next has nowhere to go.
        private static PanelPlacement buildScrollingPlacement() {
            return new PanelPlacement(ROW, ROW, List.of(), ROW, 0f, SHORT_SCROLL_OVERFLOW);
        }

        // The same panel whose content fits, so there is no scrollbar and the wheel moves nothing.
        private static PanelPlacement buildUnscrollablePlacement() {
            return new PanelPlacement(ROW, ROW, List.of(), ROW, 0f, NO_SCROLL_OVERFLOW);
        }

        // A wheel event over the middle of the body, which is inside both the panel's box and its scroll
        // region - the only place a wheel reaches the list at all.
        private static InputEventAPI buildWheelEventAtRowCentre(int wheelValue) {

            var eventMock = Mockito.mock(InputEventAPI.class);

            Mockito
                .when(eventMock.getX())
                .thenReturn(Math.round(ROW.x() + ROW.width() / 2f));
            Mockito
                .when(eventMock.getY())
                .thenReturn(Math.round(ROW.y() + ROW.height() / 2f));
            Mockito
                .when(eventMock.isMouseScrollEvent())
                .thenReturn(true);
            Mockito
                .when(eventMock.getEventValue())
                .thenReturn(wheelValue);

            return eventMock;
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
