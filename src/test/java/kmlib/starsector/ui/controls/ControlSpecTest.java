package kmlib.starsector.ui.controls;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the factory shapes hosts build controls through: a checkbox lit at cell 0 when on and off
 * otherwise carrying its click action, a caption label and a divider rule that are drawn but never
 * clicked, and the icon radio list. These fix the "a checkbox is cell 0 lit or nothing", "a label /
 * divider has no cell and no action" conventions in one place so no host re-derives them. Also pins
 * the canonical constructor's guards, which reject the shapes the layout and renderer cannot draw so a
 * hand-built spec fails at construction rather than dropping state at paint time.
 */
final class ControlSpecTest {

    @Nested
    class Constructor {

        @Test
        void constructorRejectsIconPathsOnANonRadioKind() {
            // The icon column is drawn only for a vertical radio, so a checkbox carrying icon paths
            // would drop them unseen - it must fail at construction instead.
            assertThatThrownBy(() -> new ControlSpec(ControlKind.CHECKBOX, List.of("Muted"),
                    List.of("crest"), List.of(), "", ControlSpec.NO_SELECTION, ControlAction.NONE,
                    RadioAlignment.HORIZONTAL, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void constructorRejectsTrailingValuesOnAHorizontalRadio() {
            // Per-option values are a vertical-table concept; a horizontal radio never draws them, so
            // the layout would never reserve room and the values would be lost.
            assertThatThrownBy(() -> new ControlSpec(ControlKind.RADIO, List.of("Short", "Full"),
                    List.of(), List.of("7", "3"), "", ControlSpec.NO_SELECTION, ControlAction.NONE,
                    RadioAlignment.HORIZONTAL, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void constructorRejectsDeselectOnANonRadioKind() {
            // Deselection clears a radio's lit segment; a checkbox has no segment to clear, so a
            // deselectable checkbox is a shape the input path could not act on.
            assertThatThrownBy(() -> new ControlSpec(ControlKind.CHECKBOX, List.of("Muted"),
                    List.of(), List.of(), "", ControlSpec.NO_SELECTION, ControlAction.NONE,
                    RadioAlignment.HORIZONTAL, true))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void constructorAcceptsAVerticalIconRadioListCarryingBothColumns() {
            // The valid shape the icon list takes: a vertical, deselectable radio carrying both the
            // icon and value columns, which the factory builds and the guard must let through.
            assertThatCode(() -> ControlSpec.createIconRadioList(List.of("Hegemony"), List.of("crest"),
                    List.of("7"), 0, ControlAction.NONE)).doesNotThrowAnyException();
        }
    }

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
    class CreateDivider {

        @Test
        void createDividerIsARuleWithNoLabelCellOrAction() {
            // A divider is drawn but never clicked and carries no text, so it holds no label, no lit
            // cell, and the inert action.
            var divider = ControlSpec.createDivider();
            assertThat(divider.kind()).isEqualTo(ControlKind.DIVIDER);
            assertThat(divider.labels()).isEmpty();
            assertThat(divider.selectedIndex()).isEqualTo(ControlSpec.NO_SELECTION);
            assertThat(divider.action()).isSameAs(ControlAction.NONE);
        }

        @Test
        void createDividerCarriesNoIconPaths() {
            // Like every non-icon kind, the divider reports an empty icon-path list so the layout and
            // renderer read it uniformly without a kind check.
            assertThat(ControlSpec.createDivider().iconPaths()).isEmpty();
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

        @Test
        void createIconRadioListCarriesNoTrailingValuesInTheIconOnlyOverload() {
            // The four-arg overload is the icon-only list, so it carries no per-option values and the
            // rows draw name-only (the trailing-value overload is what adds the value column).
            var picker = ControlSpec.createIconRadioList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), ControlSpec.NO_SELECTION, ControlAction.NONE);
            assertThat(picker.trailingLabels()).isEmpty();
        }

        @Test
        void createIconRadioListCarriesPerOptionTrailingValues() {
            // The value overload turns the list into a table: each option's value rides parallel to
            // its label, so the row draws its crest, name, and ranked value.
            var picker = ControlSpec.createIconRadioList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), List.of("7", "3"), 0, ControlAction.NONE);
            assertThat(picker.trailingLabels()).containsExactly("7", "3");
        }

        @Test
        void createIconRadioListDoesNotAliasTheCallersTrailingValueList() {
            // Like the icon paths, the values are copied, so a later mutation of the caller's list
            // cannot rewrite the drawn values.
            var callerValues = new ArrayList<String>(List.of("7", "3"));
            var picker = ControlSpec.createIconRadioList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), callerValues, 0, ControlAction.NONE);
            callerValues.set(0, "99");
            assertThat(picker.trailingLabels()).containsExactly("7", "3");
        }
    }

    @Nested
    class TrailingLabelAt {

        @Test
        void trailingLabelAtIsTheOptionsValueWhenItHasOne() {
            var picker = ControlSpec.createIconRadioList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), List.of("7", "3"), 0, ControlAction.NONE);
            assertThat(picker.trailingLabelAt(1)).isEqualTo("3");
        }

        @Test
        void trailingLabelAtIsEmptyForANullValueEntry() {
            // A null entry is a real "no value", so it reads as an empty string rather than throwing.
            var picker = ControlSpec.createIconRadioList(List.of("Hegemony", "Free Traders"),
                    Arrays.asList("crest_heg", null), Arrays.asList("7", null), 0, ControlAction.NONE);
            assertThat(picker.trailingLabelAt(1)).isEmpty();
        }

        @Test
        void trailingLabelAtIsEmptyForAnIndexPastTheValueList() {
            // A shorter (or empty) value list leaves the trailing options value-less rather than
            // throwing, matching how a short icon-path list leaves options icon-less.
            var picker = ControlSpec.createIconRadioList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), ControlSpec.NO_SELECTION, ControlAction.NONE);
            assertThat(picker.trailingLabelAt(0)).isEmpty();
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
