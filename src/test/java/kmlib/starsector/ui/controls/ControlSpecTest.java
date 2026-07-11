package kmlib.starsector.ui.controls;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        @Test
        void createLabelCarriesNoIconPaths() {
            // Every non-icon kind reports an empty icon-path list, so the layout and renderer read it
            // uniformly without a kind check.
            assertThat(ControlSpec.createLabel("Names").iconPaths()).isEmpty();
        }
    }

    @Nested
    class CreateIconRadioList {

        @Test
        void createIconRadioListIsAVerticalDeselectableRadioCarryingIcons() {
            // The icon list is a vertical, deselectable radio; the non-empty icon paths are what mark
            // it as the icon-drawing variant rather than a plain vertical radio.
            var picker = ControlSpec.createIconRadioList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), ControlSpec.NO_SELECTION, ControlAction.NONE);
            assertThat(picker.kind()).isEqualTo(ControlKind.RADIO);
            assertThat(picker.alignment()).isEqualTo(RadioAlignment.VERTICAL);
            assertThat(picker.canDeselect()).isTrue();
            assertThat(picker.labels()).containsExactly("Hegemony", "Tri-Tachyon");
            assertThat(picker.iconPaths()).containsExactly("crest_heg", "crest_tt");
        }

        @Test
        void createIconRadioListKeepsNullIconEntriesForIconlessOptions() {
            // A crestless option (every alliance) rides as a null entry, so the list stays aligned to
            // the labels index for index; the copy must preserve the null rather than reject it.
            var picker = ControlSpec.createIconRadioList(List.of("Hegemony", "Free Traders"),
                    Arrays.asList("crest_heg", null), 0, ControlAction.NONE);
            assertThat(picker.iconPaths()).containsExactly("crest_heg", null);
        }

        @Test
        void createIconRadioListLightsTheSelectedOption() {
            var picker = ControlSpec.createIconRadioList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), 1, ControlAction.NONE);
            assertThat(picker.selectedIndex()).isEqualTo(1);
        }

        @Test
        void createIconRadioListDoesNotAliasTheCallersIconPathList() {
            // The caller may hand in a mutable list it goes on to reuse; the spec must copy it, so a
            // later mutation of the caller's list cannot rewrite the drawn icons.
            var callerIconPaths = new ArrayList<String>(List.of("crest_heg", "crest_tt"));
            var picker = ControlSpec.createIconRadioList(List.of("Hegemony", "Tri-Tachyon"),
                    callerIconPaths, 0, ControlAction.NONE);
            callerIconPaths.set(0, "crest_mutated");
            assertThat(picker.iconPaths()).containsExactly("crest_heg", "crest_tt");
        }

        @Test
        void createIconRadioListCarriesTheClickActionByOptionIndex() {
            var firedCell = new int[]{-99};
            var picker = ControlSpec.createIconRadioList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), ControlSpec.NO_SELECTION,
                    cell -> firedCell[0] = cell);
            picker.action().activateCell(1);
            assertThat(firedCell[0]).isEqualTo(1);
        }
    }

    @Nested
    class HasIconAt {

        @Test
        void hasIconAtIsTrueForAnOptionWithANonNullIconPath() {
            var picker = ControlSpec.createIconRadioList(List.of("Hegemony", "Free Traders"),
                    Arrays.asList("crest_heg", null), ControlSpec.NO_SELECTION, ControlAction.NONE);
            assertThat(picker.hasIconAt(0)).isTrue();
        }

        @Test
        void hasIconAtIsFalseForAnOptionWithANullIconPath() {
            // A crestless option (an alliance) rides as a null entry, so it draws no icon.
            var picker = ControlSpec.createIconRadioList(List.of("Hegemony", "Free Traders"),
                    Arrays.asList("crest_heg", null), ControlSpec.NO_SELECTION, ControlAction.NONE);
            assertThat(picker.hasIconAt(1)).isFalse();
        }

        @Test
        void hasIconAtIsFalseForAnIndexPastTheIconPathList() {
            // A shorter icon-path list leaves the trailing options icon-less rather than throwing.
            var picker = ControlSpec.createIconRadioList(List.of("Hegemony", "Free Traders"),
                    List.of("crest_heg"), ControlSpec.NO_SELECTION, ControlAction.NONE);
            assertThat(picker.hasIconAt(1)).isFalse();
        }

        @Test
        void hasIconAtIsFalseForAControlWithNoIconPaths() {
            // Every non-icon control carries an empty icon-path list, so no option reports an icon.
            assertThat(ControlSpec.createCheckbox("Muted", true, ControlAction.NONE).hasIconAt(0))
                    .isFalse();
        }
    }
}
