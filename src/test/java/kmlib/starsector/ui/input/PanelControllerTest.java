package kmlib.starsector.ui.input;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.LabelledControlSpecs;
import kmlib.starsector.ui.controls.ReselectBehaviour;
import kmlib.starsector.ui.controls.VerticalTableSpecs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the panel controller's click-to-control resolution: a press maps to the control under it and fires
 * that control's action, while a caption label - drawn but not clickable - is passed over so it never
 * swallows a click as if it acted, and a scrolling control only counts inside its viewport.
 *
 * <p>Each firing case asserts the cell the hit-test reports alongside the cell the action was actually
 * called with. They are the same number by design - a caller marking what it just fired reads the reported
 * one - so pinning only one would let a press fire one cell and report another.
 *
 * <p>The resolver cases pin the other half of that split: the same geometry answered without the action
 * being reached, so a reader that only wants the cell under a point cannot activate it by asking.
 */
final class PanelControllerTest {

    private static final Rectangle ROW =
        new Rectangle(100f, 200f, 120f, 20f);

    // A viewport covering the whole row, so a non-scrolling control's hit-test ignores it; the scrolling
    // cases below pass their own viewport to exercise the clip.
    private static final Rectangle FULL_VIEWPORT =
        new Rectangle(0f, 0f, 10000f, 10000f);

    @Nested
    class ActivateControlIfHit {

        @Test
        void activateControlIfHitPassesOverACaptionLabelWithoutActing() {
            // A caption row is drawn but never clickable - a Label is not Interactive - so a press over
            // it hits nothing and falls through rather than being swallowed as if it acted.
            var label = new Control(LabelledControlSpecs.buildLabel("Caption"), ROW, List.of());
            var activatedCell = PanelController.activateControlIfHit(
                label,
                FULL_VIEWPORT,
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell).as("a caption is not a hit target")
                .isNull();
        }

        @Test
        void activateControlIfHitFiresACheckboxHitAnywhereOnItsRow() {

            var firedCell = new int[] {-1};
            var checkbox = buildCheckboxControl("Muted", cell -> firedCell[0] = cell);
            var activatedCell = PanelController.activateControlIfHit(
                checkbox,
                FULL_VIEWPORT,
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .as("a single-cell control reports cell 0")
                .isZero();
            assertThat(firedCell[0])
                .as("the cell reported is the cell the action fired for")
                .isZero();
        }

        @Test
        void activateControlIfHitReportsNoHitForAPressOutsideACheckboxRow() {

            var checkbox = buildCheckboxControl("Muted", ControlAction.NONE);
            var activatedCell = PanelController.activateControlIfHit(
                checkbox,
                FULL_VIEWPORT,
                ROW.x() - 10f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .isNull();
        }

        @Test
        void activateControlIfHitFiresAScrollingListOptionInsideItsViewport() {

            var firedCell = new int[] {-1};
            var list = buildScrollingListAtRow(cell -> firedCell[0] = cell);

            // The press lands on the list's one option and inside a viewport that covers the row, so the
            // option fires as a normal radio hit.
            var activatedCell = PanelController.activateControlIfHit(
                list,
                ROW,
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .isZero();
            assertThat(firedCell[0])
                .isZero();
        }

        @Test
        void activateControlIfHitRejectsAScrollingListOptionScrolledOutOfItsViewport() {

            var fired = new boolean[1];
            var list = buildScrollingListAtRow(cell -> fired[0] = true);

            // The option's segment sits at ROW, but the viewport is a strip well above it - as if the row
            // scrolled up under the header - so the press over the clipped-out row must not fire it.
            var viewportAbove = new Rectangle(ROW.x(), ROW.y() + 100f, ROW.width(), 40f);
            var activatedCell = PanelController.activateControlIfHit(
                list,
                viewportAbove,
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
        void activateControlIfHitFiresATabHitReportedAsThatTabIndex() {

            var firedCell = new int[] {-1};

            // The lit tab is the left one, so a press on the right (non-lit) tab fires it by its index.
            var tabs = buildTwoTabRowAtRow(0, cell -> firedCell[0] = cell);
            var activatedCell = PanelController.activateControlIfHit(
                tabs,
                FULL_VIEWPORT,
                ROW.x() + 3f * ROW.width() / 4f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .as("a tab reports its own index")
                .isEqualTo(1);
            assertThat(firedCell[0])
                .as("the index reported is the index the action fired for")
                .isEqualTo(1);
        }

        @Test
        void activateControlIfHitTreatsAPressOnTheLitTabAsInert() {

            var fired = new boolean[1];

            // A tabs row is always inert on its lit tab (INERT reselect), so a press on the left, lit tab
            // reaches no action - matching a vanilla tab strip, where clicking the active tab does nothing.
            var tabs = buildTwoTabRowAtRow(0, cell -> fired[0] = true);
            var activatedCell = PanelController.activateControlIfHit(
                tabs,
                FULL_VIEWPORT,
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
        void activateControlIfHitReportsNoHitForAPressOutsideEveryTab() {

            var tabs = buildTwoTabRowAtRow(0, ControlAction.NONE);
            var activatedCell = PanelController.activateControlIfHit(
                tabs,
                FULL_VIEWPORT,
                ROW.x() - 10f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .isNull();
        }

        @Test
        void activateControlIfHitFiresADeselectableHorizontalRadioOnARepickOfItsLitSegment() {

            var firedCell = new int[] {-1};

            // A DESELECT horizontal radio wants the re-pick to reach the action so the host turns the
            // control off, so a press on the left, lit segment fires it by its index (not swallowed).
            var radio = buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio.of(
                    List.of("Factions", "Alliances"), 
                    0,
                    cell -> firedCell[0] = cell)
                .handlesReselect(ReselectBehaviour.DESELECT));

            var activatedCell = PanelController.activateControlIfHit(
                radio,
                FULL_VIEWPORT,
                ROW.x() + ROW.width() / 4f,
                ROW.y() + ROW.height() / 2f);

            assertThat(activatedCell)
                .as("the lit segment reports its own index")
                .isZero();
            assertThat(firedCell[0])
                .as("the index reported is the index the action fired for")
                .isZero();
        }

        @Test
        void activateControlIfHitTreatsAPressOnAnInertHorizontalRadiosLitSegmentAsInert() {

            var fired = new boolean[1];

            // A plain option pair is always one lit (INERT reselect), so a press on the lit segment
            // reaches no action - the standard radio behaviour a deselectable row opts out of.
            var radio = buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio.of(
                List.of("Short", "Full"),
                0,
                cell -> fired[0] = true));

            var activatedCell = PanelController.activateControlIfHit(
                radio,
                FULL_VIEWPORT,
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
            var viewportAbove = new Rectangle(ROW.x(), ROW.y() + 100f, ROW.width(), 40f);
            var resolvedCell = PanelController.resolveHitCell(
                list,
                viewportAbove,
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
            var viewportAbove = new Rectangle(ROW.x(), ROW.y() + 100f, ROW.width(), 40f);
            var resolvedCell = PanelController.resolveHitCell(
                checkbox,
                viewportAbove,
                ROW.x() + ROW.width() / 2f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .as("a control that never scrolls is not clipped by the flex viewport")
                .isZero();
        }

        @Test
        void resolveHitCellReportsTheLitSegmentOfADeselectableRadio() {

            // The other half of the press's narrowing: a DESELECT radio wants a re-pick to reach its action,
            // so its lit segment resolves to itself rather than to no cell - the answer an inert row gives.
            // Both directions are pinned because the narrowing is what separates this resolver from a plain
            // "what is under the pointer", and a change that dropped it would still pass the inert case.
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
                .as("a re-firing radio resolves its lit segment to itself")
                .isZero();
        }

        @Test
        void resolveHitCellTreatsTheLitTabAsInert() {

            // This is the press's resolver, so the reselect narrowing is part of its answer: the lit tab of
            // an always-inert tabs row resolves to no cell, matching what a press there would act on.
            var tabs = buildTwoTabRowAtRow(0, ControlAction.NONE);
            var resolvedCell = PanelController.resolveHitCell(
                tabs,
                ROW.x() + ROW.width() / 4f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .as("the lit tab of an inert tabs row resolves to no cell")
                .isNull();
        }
    }

    // A single-row checkbox occupying ROW, so each test states only the label and action that
    // distinguish its case rather than repeating the spec-and-bounds construction.
    private static Control buildCheckboxControl(String label, ControlAction action) {
        return new Control(LabelledControlSpecs.buildCheckbox(label, false, action), ROW, List.of());
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
