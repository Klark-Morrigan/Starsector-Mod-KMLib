package kmlib.starsector.ui.controls;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two questions an interactive control answers about itself: whether its cells are hit
 * separately, and what a re-pick of an already-lit cell does.
 *
 * <p>Both are asked by more than one reader - the hit-test that resolves a cell and the narrowing
 * that decides whether pressing one acts - so the answer lives on the control rather than being
 * worked out about it. Stated on either side, a variant would be hit as a row of segments and
 * pressed as a whole row, or the other way about.
 */
class ControlSpecInteractiveTest {

    @Nested
    class IsSegmented {

        @Test
        void isTrueForASetOfCellsLaidAcrossARow() {

            assertThat(buildRadio().isSegmented())
                .isTrue();
        }

        @Test
        void isTrueForATabsRow() {

            assertThat(buildTabs().isSegmented())
                .isTrue();
        }

        @Test
        void isFalseForASingleCellCheckbox() {
            // One whole-row target: there is no lit segment to re-pick, so every hit on it acts.
            assertThat(buildCheckbox().isSegmented())
                .isFalse();
        }

        @Test
        void isFalseForASingleCellToggle() {

            assertThat(buildToggle().isSegmented())
                .isFalse();
        }
    }

    @Nested
    class ReselectBehaviourRule {

        @Test
        void answersTheRuleASetCarries() {
            // Read off the radio rather than off its alignment, so a stacked set answers a re-pick
            // exactly as a laid-across one does.
            var radio = buildRadio().handlesReselect(ReselectBehaviour.DESELECT);

            assertThat(radio.reselectBehaviour())
                .isEqualTo(ReselectBehaviour.DESELECT);
        }

        @Test
        void isInertForATabsRow() {
            // A tabs row never acts on its own lit tab, which is why it carries no rule of its own.
            assertThat(buildTabs().reselectBehaviour())
                .isEqualTo(ReselectBehaviour.INERT);
        }

        @Test
        void isInertForASingleCellCheckbox() {

            assertThat(buildCheckbox().reselectBehaviour())
                .isEqualTo(ReselectBehaviour.INERT);
        }

        @Test
        void isInertForASingleCellToggle() {

            assertThat(buildToggle().reselectBehaviour())
                .isEqualTo(ReselectBehaviour.INERT);
        }
    }

    private static ControlSpec.Checkbox buildCheckbox() {

        return ControlSpec.Checkbox.lit(
            LabelledControlSpecs.buildLabelSpan("Muted"),
            true,
            ControlAction.NONE);
    }

    private static ControlSpec.HorizontalRadio buildRadio() {

        return ControlSpec.HorizontalRadio.of(
            List.of("Left", "Right"),
            ControlSpec.NO_SELECTION,
            ControlAction.NONE);
    }

    private static ControlSpec.Tabs buildTabs() {

        return new ControlSpec.Tabs(
            List.of("Systems", "Fleets"),
            List.of("", ""),
            0,
            ControlAction.NONE);
    }

    private static ControlSpec.Toggle buildToggle() {

        return ControlSpec.Toggle.lit(
            LabelledControlSpecs.buildLabelSpan("Borders"),
            true,
            ControlAction.NONE);
    }
}
