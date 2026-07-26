package kmlib.starsector.ui.input;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
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
 */
final class PanelControllerTest {
    private static final Rectangle ROW = new Rectangle(100f, 200f, 120f, 20f);
    // A viewport covering the whole row, so a non-scrolling control's hit-test ignores it; the scrolling
    // cases below pass their own viewport to exercise the clip.
    private static final Rectangle FULL_VIEWPORT = new Rectangle(0f, 0f, 10000f, 10000f);

    @Nested
    class ActivateControlIfHit {

        @Test
        void activateControlIfHitPassesOverACaptionLabelWithoutActing() {
            // A caption row is drawn but never clickable - a Label is not Interactive - so a press over
            // it hits nothing and falls through rather than being swallowed as if it acted.
            var label = new Control(new ControlSpec.Label("Caption"), ROW, List.of());
            var acted = PanelController.activateControlIfHit(label, FULL_VIEWPORT,
                    ROW.x() + ROW.width() / 2f, ROW.y() + ROW.height() / 2f);
            assertThat(acted).as("a caption is not a hit target").isFalse();
        }

        @Test
        void activateControlIfHitFiresACheckboxHitAnywhereOnItsRow() {
            var firedCell = new int[]{-1};
            var checkbox = buildCheckboxControl("Muted", cell -> firedCell[0] = cell);
            var acted = PanelController.activateControlIfHit(checkbox, FULL_VIEWPORT,
                    ROW.x() + ROW.width() / 2f, ROW.y() + ROW.height() / 2f);
            assertThat(acted).isTrue();
            assertThat(firedCell[0]).as("a single-cell control reports cell 0").isZero();
        }

        @Test
        void activateControlIfHitReportsNoHitForAPressOutsideACheckboxRow() {
            var checkbox = buildCheckboxControl("Muted", ControlAction.NONE);
            var acted = PanelController.activateControlIfHit(checkbox, FULL_VIEWPORT,
                    ROW.x() - 10f, ROW.y() + ROW.height() / 2f);
            assertThat(acted).isFalse();
        }

        @Test
        void activateControlIfHitFiresAScrollingListOptionInsideItsViewport() {
            var firedCell = new int[]{-1};
            var list = buildScrollingListAtRow(cell -> firedCell[0] = cell);
            // The press lands on the list's one option and inside a viewport that covers the row, so the
            // option fires as a normal radio hit.
            var acted = PanelController.activateControlIfHit(list, ROW,
                    ROW.x() + ROW.width() / 2f, ROW.y() + ROW.height() / 2f);
            assertThat(acted).isTrue();
            assertThat(firedCell[0]).isZero();
        }

        @Test
        void activateControlIfHitRejectsAScrollingListOptionScrolledOutOfItsViewport() {
            var fired = new boolean[1];
            var list = buildScrollingListAtRow(cell -> fired[0] = true);
            // The option's segment sits at ROW, but the viewport is a strip well above it - as if the row
            // scrolled up under the header - so the press over the clipped-out row must not fire it.
            var viewportAbove = new Rectangle(ROW.x(), ROW.y() + 100f, ROW.width(), 40f);
            var acted = PanelController.activateControlIfHit(list, viewportAbove,
                    ROW.x() + ROW.width() / 2f, ROW.y() + ROW.height() / 2f);
            assertThat(acted).as("a row clipped from the viewport is not clickable").isFalse();
            assertThat(fired[0]).as("the clipped-out option's action must not fire").isFalse();
        }

        @Test
        void activateControlIfHitFiresATabHitReportedAsThatTabIndex() {
            var firedCell = new int[]{-1};
            // The lit tab is the left one, so a press on the right (non-lit) tab fires it by its index.
            var tabs = buildTwoTabRowAtRow(0, cell -> firedCell[0] = cell);
            var acted = PanelController.activateControlIfHit(tabs, FULL_VIEWPORT,
                    ROW.x() + 3f * ROW.width() / 4f, ROW.y() + ROW.height() / 2f);
            assertThat(acted).isTrue();
            assertThat(firedCell[0]).as("a tab reports its own index").isEqualTo(1);
        }

        @Test
        void activateControlIfHitTreatsAPressOnTheLitTabAsInert() {
            var fired = new boolean[1];
            // A tabs row is always inert on its lit tab (INERT reselect), so a press on the left, lit tab
            // reaches no action - matching a vanilla tab strip, where clicking the active tab does nothing.
            var tabs = buildTwoTabRowAtRow(0, cell -> fired[0] = true);
            var acted = PanelController.activateControlIfHit(tabs, FULL_VIEWPORT,
                    ROW.x() + ROW.width() / 4f, ROW.y() + ROW.height() / 2f);
            assertThat(acted).as("re-clicking the active tab is inert").isFalse();
            assertThat(fired[0]).as("the lit tab's action must not fire").isFalse();
        }

        @Test
        void activateControlIfHitReportsNoHitForAPressOutsideEveryTab() {
            var tabs = buildTwoTabRowAtRow(0, ControlAction.NONE);
            var acted = PanelController.activateControlIfHit(tabs, FULL_VIEWPORT, ROW.x() - 10f,
                    ROW.y() + ROW.height() / 2f);
            assertThat(acted).isFalse();
        }

        @Test
        void activateControlIfHitFiresAHeaderTabThroughTheUnclippedCore() {
            var firedCell = new int[]{-1};
            // The viewport-less core is the path a tab panel's header takes - the header never scrolls, so
            // it is never clipped. A press on the right (non-lit) tab fires it by its index, no viewport.
            var tabs = buildTwoTabRowAtRow(0, cell -> firedCell[0] = cell);
            var acted = PanelController.activateControlIfHit(tabs, ROW.x() + 3f * ROW.width() / 4f,
                    ROW.y() + ROW.height() / 2f);
            assertThat(acted).isTrue();
            assertThat(firedCell[0]).as("a tab reports its own index").isEqualTo(1);
        }

        @Test
        void activateControlIfHitFiresADeselectableHorizontalRadioOnARepickOfItsLitSegment() {
            var firedCell = new int[]{-1};
            // A DESELECT horizontal radio wants the re-pick to reach the action so the host turns the
            // control off, so a press on the left, lit segment fires it by its index (not swallowed).
            var radio = buildTwoSegmentHorizontalRadioAtRow(
                    ControlSpec.HorizontalRadio.of(List.of("Factions", "Alliances"), 0,
                            cell -> firedCell[0] = cell).handlesReselect(ReselectBehaviour.DESELECT));
            var acted = PanelController.activateControlIfHit(radio, FULL_VIEWPORT,
                    ROW.x() + ROW.width() / 4f, ROW.y() + ROW.height() / 2f);
            assertThat(acted).isTrue();
            assertThat(firedCell[0]).as("the lit segment reports its own index").isZero();
        }

        @Test
        void activateControlIfHitTreatsAPressOnAnInertHorizontalRadiosLitSegmentAsInert() {
            var fired = new boolean[1];
            // A plain option pair is always one lit (INERT reselect), so a press on the lit segment
            // reaches no action - the standard radio behaviour a deselectable row opts out of.
            var radio = buildTwoSegmentHorizontalRadioAtRow(
                    ControlSpec.HorizontalRadio.of(List.of("Short", "Full"), 0,
                            cell -> fired[0] = true));
            var acted = PanelController.activateControlIfHit(radio, FULL_VIEWPORT,
                    ROW.x() + ROW.width() / 4f, ROW.y() + ROW.height() / 2f);
            assertThat(acted).as("re-clicking the lit option pair segment is inert").isFalse();
            assertThat(fired[0]).as("the lit segment's action must not fire").isFalse();
        }
    }

    // A single-row checkbox occupying ROW, so each test states only the label and action that
    // distinguish its case rather than repeating the spec-and-bounds construction.
    private static Control buildCheckboxControl(String label, ControlAction action) {
        return new Control(ControlSpec.Checkbox.lit(label, false, action), ROW, List.of());
    }

    // A one-option scrolling list laid out at ROW: a vertical icon table marked as the scroll region, its
    // single segment the whole row, so a press at ROW hits option 0 unless the viewport clips it.
    private static Control buildScrollingListAtRow(ControlAction action) {
        var spec = VerticalTableSpecs.buildIconList(List.of("Opt"), Arrays.asList((String) null),
                ControlSpec.NO_SELECTION, action).asScrolling();
        return new Control(spec, ROW, List.of(ROW));
    }

    // A two-segment horizontal radio occupying ROW, split into two equal segment boxes (left, right), so a
    // press in a segment's box hits that segment - and a press on the lit segment fires or is swallowed by
    // the radio's own reselect. The caller supplies the spec so a test picks the deselectable or inert row.
    private static Control buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio spec) {
        var leftSegment = new Rectangle(ROW.x(), ROW.y(), ROW.width() / 2f, ROW.height());
        var rightSegment = new Rectangle(ROW.x() + ROW.width() / 2f, ROW.y(), ROW.width() / 2f,
                ROW.height());
        return new Control(spec, ROW, List.of(leftSegment, rightSegment));
    }

    // A two-tab row occupying ROW, split into two equal per-tab boxes (left tab, right tab), the lit tab
    // at selectedIndex, so a press in a tab's box hits that tab unless its own inert-on-lit reselect
    // swallows it. Shortcuts are irrelevant to the hit-test, so the tabs carry none.
    private static Control buildTwoTabRowAtRow(int selectedIndex, ControlAction action) {
        var leftTab = new Rectangle(ROW.x(), ROW.y(), ROW.width() / 2f, ROW.height());
        var rightTab = new Rectangle(ROW.x() + ROW.width() / 2f, ROW.y(), ROW.width() / 2f,
                ROW.height());
        var spec = new ControlSpec.Tabs(List.of("Political Map", "Alliances"), List.of(),
                selectedIndex, action);
        return new Control(spec, ROW, List.of(leftTab, rightTab));
    }
}
