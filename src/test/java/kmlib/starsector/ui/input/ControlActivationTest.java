package kmlib.starsector.ui.input;

import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.ReselectBehaviour;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static kmlib.starsector.ui.input.PanelBodyFixtures.buildCaptionControl;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildCheckboxControl;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildTwoCellVerticalRadioAtRow;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildTwoSegmentHorizontalRadioAtRow;
import static kmlib.starsector.ui.input.PanelBodyFixtures.buildTwoTabRowAtRow;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the whole of a press's narrowing: which already-resolved cells reach their control's action, and
 * that firing one fires the cell it was handed.
 *
 * <p>Every case here takes a cell rather than a point, which is the shape of the seam - the geometry is
 * {@link ControlHitResolver}'s and is pinned there. That split is what these cases are ultimately about: a
 * lit segment resolves like any other cell and acts like none, so the two answers have to be reachable
 * apart or a hover reading the press's answer would leave the lit control dark.
 */
final class ControlActivationTest {

    @Nested
    class ActivateCellIfActionableOnAStackedRadio {

        @Test
        void activateCellIfActionableReadsTheRePickRuleThroughTheRadioTypeNotTheAlignment() {
            // The re-pick rule belongs to the control rather than to how its cells are arranged: a stacked
            // DESELECT radio swallows nothing and re-fires its lit cell exactly as a laid-across one does.
            // A narrowing back to one alignment would leave a stacked radio inert on its lit cell.
            var firedCell = new int[] {-1};
            var radio = buildTwoCellVerticalRadioAtRow(ControlSpec.VerticalRadio
                .of(List.of("Factions", "Alliances"), 0, cell -> firedCell[0] = cell)
                .handlesReselect(ReselectBehaviour.DESELECT));

            var activatedCell = ControlActivation.activateCellIfActionable(radio, 0);

            assertThat(activatedCell)
                .isZero();
            assertThat(firedCell[0])
                .isZero();
        }

        @Test
        void activateCellIfActionableSwallowsARePickOfAnInertStackedRadiosLitCell() {
            // The other half of the same rule: an INERT stack holds its lit cell, so a re-pick of it acts
            // on nothing rather than re-firing.
            var firedCell = new int[] {-1};
            var radio = buildTwoCellVerticalRadioAtRow(ControlSpec.VerticalRadio.of(
                List.of("Factions", "Alliances"),
                1,
                cell -> firedCell[0] = cell));

            var activatedCell = ControlActivation.activateCellIfActionable(radio, 1);

            assertThat(activatedCell)
                .isNull();
            assertThat(firedCell[0])
                .isEqualTo(-1);
        }
    }

    @Nested
    class ActivateCellIfActionable {

        @Test
        void activateCellIfActionableFiresTheCellItWasHandedWithoutHitTestingForOne() {
            // The seam a header press reaches directly, having resolved its own tab already: the cell handed
            // over is the cell that fires, with no point to test it against.
            var firedCell = new int[] {-1};
            var tabs = buildTwoTabRowAtRow(0, cell -> firedCell[0] = cell);
            var activatedCell = ControlActivation.activateCellIfActionable(tabs, 1);

            assertThat(activatedCell)
                .isEqualTo(1);
            assertThat(firedCell[0])
                .as("the cell reported is the cell the action fired for")
                .isEqualTo(1);
        }

        @Test
        void activateCellIfActionableFiresEveryCellOfAWholeRowControl() {
            // A checkbox has no lit segment to re-pick, so every hit on it acts - the case the narrowing
            // below must not reach.
            var firedCell = new int[] {-1};
            var checkbox = buildCheckboxControl("Muted", cell -> firedCell[0] = cell);

            assertThat(ControlActivation.activateCellIfActionable(checkbox, ControlSpec.SINGLE_CELL))
                .isEqualTo(ControlSpec.SINGLE_CELL);
            assertThat(firedCell[0])
                .isZero();
        }

        @Test
        void activateCellIfActionableSwallowsARepickOfAnInertRowsLitSegment() {
            // The standard radio rule, and what makes re-clicking a vanilla tab strip's active tab do
            // nothing: a plain option pair is always one lit, so its lit segment reaches no action.
            var fired = new boolean[1];
            var radio = buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio.of(
                List.of("Short", "Full"),
                0,
                cell -> fired[0] = true));

            assertThat(ControlActivation.activateCellIfActionable(radio, 0))
                .as("re-picking the lit segment of an option pair is inert")
                .isNull();
            assertThat(fired[0])
                .isFalse();
        }

        @Test
        void activateCellIfActionableFiresARepickOfADeselectableRowsLitSegment() {
            // The opposite reselect, and why the narrowing reads the control rather than assuming: a
            // deselectable row wants the re-pick so its host can turn the control off.
            var firedCell = new int[] {-1};
            var radio = buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio.of(
                    List.of("Factions", "Alliances"),
                    0,
                    cell -> firedCell[0] = cell)
                .handlesReselect(ReselectBehaviour.DESELECT));

            assertThat(ControlActivation.activateCellIfActionable(radio, 0))
                .isZero();
            assertThat(firedCell[0])
                .isZero();
        }

        @Test
        void activateCellIfActionableFiresASegmentThatIsNotTheLitOne() {
            // The ordinary pick. Pinned beside the inert case above so the narrowing cannot be read as "a
            // segmented control never acts" - only its own lit segment is the one in question.
            var firedCell = new int[] {-1};
            var radio = buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio.of(
                List.of("Short", "Full"),
                0,
                cell -> firedCell[0] = cell));

            assertThat(ControlActivation.activateCellIfActionable(radio, 1))
                .isEqualTo(1);
            assertThat(firedCell[0])
                .isEqualTo(1);
        }

        @Test
        void activateCellIfActionableActsOnNothingForACellThatResolvedToNone() {
            // What a walk hands over when the point was on no cell at all - passed straight back, so a
            // caller can offer this whatever its resolver answered without checking first.
            assertThat(ControlActivation.activateCellIfActionable(
                    buildCheckboxControl("Muted", ControlAction.NONE),
                    ControlHitResolver.NO_CELL_RESOLVED))
                .isNull();
        }

        @Test
        void activateCellIfActionableActsOnNothingForAControlThatIsNotInteractive() {
            // A caption carries no action to reach, so a cell named on one goes nowhere rather than throwing
            // on the cast that would reach it. Nothing resolves a cell on a label today, which is exactly
            // why this is stated here rather than left to whichever caller first hands one over.
            assertThat(ControlActivation.activateCellIfActionable(
                    buildCaptionControl(),
                    ControlSpec.SINGLE_CELL))
                .isNull();
        }
    }
}
