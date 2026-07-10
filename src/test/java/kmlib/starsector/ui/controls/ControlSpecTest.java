package kmlib.starsector.ui.controls;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two factory shapes hosts build controls through: a checkbox lit at cell 0 when on and off
 * otherwise carrying its click action, and a caption label that is drawn but never clicked. These fix
 * the "a checkbox is cell 0 lit or nothing" and "a label has no cell and no action" conventions in
 * one place so no host re-derives them.
 */
final class ControlSpecTest {

    @Nested
    class CreateCheckbox {

        @Test
        void createCheckboxLightsCellZeroWhenOn() {
            var checkbox = ControlSpec.createCheckbox("Muted", true, ControlAction.NONE);
            assertThat(checkbox.kind()).isEqualTo(ControlKind.CHECKBOX);
            assertThat(checkbox.labels()).containsExactly("Muted");
            assertThat(checkbox.selectedIndex()).isZero();
        }

        @Test
        void createCheckboxLeavesNoCellLitWhenOff() {
            var checkbox = ControlSpec.createCheckbox("Muted", false, ControlAction.NONE);
            assertThat(checkbox.selectedIndex()).isEqualTo(ControlSpec.NO_SELECTION);
        }

        @Test
        void createCheckboxCarriesTheClickActionOnCellZero() {
            // The row's single cell (0) is the hit target, so a click fires the action for cell 0 -
            // pinned by capturing which cell the action was invoked with.
            var firedCell = new int[]{-99};
            var checkbox = ControlSpec.createCheckbox("Muted", false, cell -> firedCell[0] = cell);
            checkbox.action().activateCell(0);
            assertThat(firedCell[0]).isZero();
        }
    }

    @Nested
    class CreateLabel {

        @Test
        void createLabelIsATextOnlyRowWithNoCellOrAction() {
            var label = ControlSpec.createLabel("Non-allied factions are");
            assertThat(label.kind()).isEqualTo(ControlKind.LABEL);
            assertThat(label.labels()).containsExactly("Non-allied factions are");
            assertThat(label.selectedIndex()).isEqualTo(ControlSpec.NO_SELECTION);
            assertThat(label.action()).isSameAs(ControlAction.NONE);
        }
    }
}
