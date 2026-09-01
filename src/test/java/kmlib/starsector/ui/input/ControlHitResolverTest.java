package kmlib.starsector.ui.input;

import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.LabelledControlSpecs;
import kmlib.starsector.ui.controls.ReselectBehaviour;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static kmlib.starsector.ui.input.PanelBodyFixtures.FULL_VIEWPORT;
import static kmlib.starsector.ui.input.PanelBodyFixtures.ROW;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildBodyPlacement;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildBodyPlacementBoxedTo;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildCaptionControl;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildCheckboxControl;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildDividerControl;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildDockedRailBox;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildScrollingListAtRow;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildTwoSegmentHorizontalRadioAtRow;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildTwoTabRowAtRow;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildViewportAboveRow;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one hit-test a panel answers its body with, and the one its header is answered by: which control
 * a point is on, which cell of it, and what is on screen to be hit at all - a folded body is behind its
 * rail, a scrolled row is behind its viewport, and chrome is no hit target however squarely a point lands
 * on it.
 *
 * <p>The line every case here is drawn along is that it answers geometry and visibility and nothing else.
 * The lit segment of an inert radio, and the tab a row is already showing, resolve to themselves like any
 * other cell even though pressing either fires nothing - which is what lets a hover read exactly what a
 * press reads. Whether a cell would <em>act</em> is {@link ControlActivation}'s question and is pinned
 * beside it, so a narrowing creeping in here would fail on the cases below rather than showing up as a
 * control that goes dark whenever it is the one selected.
 */
final class ControlHitResolverTest {

    @Nested
    class ResolveHitBodyCell {

        @Test
        void resolveHitBodyCellReportsTheHitControlWithoutFiringItsAction() {

            var fired = new boolean[1];

            // The walk answers geometry alone: the same press that fires through a panel's press path
            // reports its control here with the action untouched, which is what lets a hover - a reader that
            // only wants to know what is under a point - share the walk with the press.
            var checkbox = buildCheckboxControl("Muted", cell -> fired[0] = true);
            var resolvedCell = ControlHitResolver.resolveHitBodyCell(
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
            // every case for the resolver below it still passed.
            var radio = buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio.of(
                List.of("Short", "Full"),
                0,
                ControlAction.NONE));

            var resolvedCell = ControlHitResolver.resolveHitBodyCell(
                buildBodyPlacement(radio),
                ROW.x() + ROW.width() / 4f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .as("what a point is over does not depend on what pressing it would do")
                .isEqualTo(new ResolvedBodyCell(radio, new BodyCellSlot(0, 0)));
        }

        @Test
        void resolveHitBodyCellRejectsAScrollingListRowScrolledOutOfItsViewport() {

            // The clip, stated where a hover will read it: the row's segment sits at ROW and the viewport is
            // a strip well above it, as if the row had scrolled up under a pinned control, so a point over
            // the clipped-out row is over nothing. The walk asks for the viewport itself, which is why no
            // caller can forget to.
            var list = buildScrollingListAtRow(ControlAction.NONE);
            var resolvedCell = ControlHitResolver.resolveHitBodyCell(
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
            var resolvedCell = ControlHitResolver.resolveHitBodyCell(
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
            var resolvedCell = ControlHitResolver.resolveHitBodyCell(
                buildBodyPlacement(
                    buildCaptionControl(),
                    buildDividerControl(),
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
            var resolvedCell = ControlHitResolver.resolveHitBodyCell(
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
            var resolvedCell = ControlHitResolver.resolveHitBodyCell(
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

            // Resolution answers geometry alone: the cell is reported while the control's action stays
            // untouched, which is what lets a reader that only wants to know what is under a point share
            // this path with the press.
            var checkbox = buildCheckboxControl("Muted", cell -> fired[0] = true);
            var resolvedCell = ControlHitResolver.resolveHitCell(
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
            var resolvedCell = ControlHitResolver.resolveHitCell(
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
            var resolvedCell = ControlHitResolver.resolveHitCell(
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

            var resolvedCell = ControlHitResolver.resolveHitCell(
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

            var resolvedCell = ControlHitResolver.resolveHitCell(
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
            var resolvedCell = ControlHitResolver.resolveHitCell(
                tabs,
                ROW.x() + ROW.width() / 4f,
                ROW.y() + ROW.height() / 2f);

            assertThat(resolvedCell)
                .as("the lit tab is under the pointer like any other")
                .isZero();
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

            assertThat(ControlHitResolver.isSegmentedControl(radio))
                .isTrue();
        }

        @Test
        void isSegmentedControlIsFalseForAWholeRowCheckbox() {
            // Hit anywhere on its bounds rather than by segment, which is the other side of the same rule.
            assertThat(ControlHitResolver.isSegmentedControl(
                    buildCheckboxControl("Muted", ControlAction.NONE)))
                .isFalse();
        }

        @Test
        void isSegmentedControlIsFalseForChrome() {
            // A caption has no cells at all, so nothing about it is one of many alike. Nothing resolves a
            // cell on one today, which is why the answer is stated here rather than left to whichever
            // reader first asks it of something that was never a hit target.
            assertThat(ControlHitResolver.isSegmentedControl(buildCaptionControl()))
                .isFalse();
        }
    }

    @Nested
    class IsSegmentedSpec {

        @Test
        void isSegmentedSpecAnswersTheSameRuleTheLaidOutControlIsAskedFor() {
            // One rule with two readers - the hit-test that resolves a cell, and the narrowing that decides
            // whether pressing it acts. Restated on either side, a variant added to the set would be hit as
            // a row of segments and pressed as a whole row, or the other way about.
            var radio = ControlSpec.HorizontalRadio.of(
                List.of("Left", "Right"),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE);

            assertThat(ControlHitResolver.isSegmentedSpec(radio))
                .isTrue();
            assertThat(ControlHitResolver.isSegmentedSpec(
                    ControlSpec.Checkbox.lit(
                        LabelledControlSpecs.buildLabelSpan("Muted"), true, ControlAction.NONE)))
                .isFalse();
        }
    }
}
