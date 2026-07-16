package kmlib.starsector.ui.controls;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the sealed variants hosts build controls through: a checkbox and a toggle lit at cell 0 when on
 * and off otherwise carrying their click action, a caption label and a divider rule that are drawn but
 * never clicked (and so are not {@link ControlSpec.Interactive}), the horizontal radios, the vertical
 * radio table, and the tabs row. These fix the "a single cell is lit or nothing", "a label / divider has
 * no cell and no action" conventions in one place so no host re-derives them. The variants make the
 * illegal shapes unrepresentable - a checkbox has no icon column to mis-set - so only the two numeric
 * guards a vertical table's own construction can still trip are pinned here.
 */
final class ControlSpecTest {

    @Nested
    class Interactive {

        @Test
        void checkboxAndRadiosAndTabsAreInteractive() {
            // The clickable, stateful controls implement Interactive, so the input listener acts on them.
            assertThat(ControlSpec.Checkbox.lit("Muted", true, ControlAction.NONE))
                    .isInstanceOf(ControlSpec.Interactive.class);
            assertThat(ControlSpec.Toggle.lit("Muted", true, ControlAction.NONE))
                    .isInstanceOf(ControlSpec.Interactive.class);
            assertThat(ControlSpec.HorizontalRadio.uniform(List.of("A"), "", 0, ControlAction.NONE))
                    .isInstanceOf(ControlSpec.Interactive.class);
            assertThat(VerticalTableSpecs.buildPlainTable(List.of("A"), 0, ControlAction.NONE,
                    ReselectBehaviour.INERT)).isInstanceOf(ControlSpec.Interactive.class);
            assertThat(new ControlSpec.Tabs(List.of("A"), List.of(), 0, ControlAction.NONE))
                    .isInstanceOf(ControlSpec.Interactive.class);
        }

        @Test
        void labelAndDividerAreNotInteractive() {
            // A caption and a rule are drawn but never clicked, so they are chrome, not Interactive - the
            // one property the input listener reads to skip them.
            assertThat(new ControlSpec.Label("Names")).isNotInstanceOf(ControlSpec.Interactive.class);
            assertThat(new ControlSpec.Divider()).isNotInstanceOf(ControlSpec.Interactive.class);
        }
    }

    @Nested
    class VerticalTableConstructor {

        @Test
        void constructorRejectsAColumnCountBelowOne() {
            // A column count is how many columns the options wrap across; a count below one cannot lay
            // out any column, so it fails at construction rather than dividing by a zero column count.
            assertThatThrownBy(() -> new ControlSpec.VerticalTable(List.of("A"), List.of(), List.of(),
                    List.of(), ControlSpec.NO_SELECTION, ControlAction.NONE, ReselectBehaviour.DESELECT,
                    0, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void constructorDoesNotAliasTheCallersLabelList() {
            // The caller may hand in a mutable list it goes on to reuse; the table must copy it, so a
            // later mutation of the caller's list cannot rewrite the drawn labels.
            var callerLabels = new ArrayList<String>(List.of("Factions", "Alliances"));
            var table = new ControlSpec.VerticalTable(callerLabels, List.of(), List.of(), List.of(),
                    0, ControlAction.NONE, ReselectBehaviour.DESELECT, ControlSpec.SINGLE_COLUMN, false);
            callerLabels.set(0, "Mutated");
            assertThat(table.labels()).containsExactly("Factions", "Alliances");
        }
    }

    @Nested
    class CheckboxLit {

        @Test
        void litLightsCellZeroWhenOn() {
            var checkbox = ControlSpec.Checkbox.lit("Muted", true, ControlAction.NONE);
            assertThat(checkbox.labels()).containsExactly("Muted");
            assertThat(checkbox.selectedIndex()).isZero();
            assertThat(checkbox.isLit()).isTrue();
        }

        @Test
        void litLeavesNoCellLitWhenOff() {
            var checkbox = ControlSpec.Checkbox.lit("Muted", false, ControlAction.NONE);
            assertThat(checkbox.selectedIndex()).isEqualTo(ControlSpec.NO_SELECTION);
            assertThat(checkbox.isLit()).isFalse();
        }

        @Test
        void litCarriesTheClickActionOnCellZero() {
            // The row's single cell (0) is the hit target, so a click fires the action for cell 0 -
            // pinned by capturing which cell the action was invoked with.
            var firedCell = new int[]{-99};
            var checkbox = ControlSpec.Checkbox.lit("Muted", false, cell -> firedCell[0] = cell);
            checkbox.action().activateCell(0);
            assertThat(firedCell[0]).isZero();
        }
    }

    @Nested
    class ToggleLit {

        @Test
        void litLightsCellZeroWhenOn() {
            var toggle = ControlSpec.Toggle.lit("Factions", true, ControlAction.NONE);
            assertThat(toggle.labels()).containsExactly("Factions");
            assertThat(toggle.selectedIndex()).isZero();
            assertThat(toggle.isLit()).isTrue();
        }

        @Test
        void litLeavesNoCellLitWhenOff() {
            var toggle = ControlSpec.Toggle.lit("Factions", false, ControlAction.NONE);
            assertThat(toggle.selectedIndex()).isEqualTo(ControlSpec.NO_SELECTION);
        }
    }

    @Nested
    class Label {

        @Test
        void labelIsATextOnlyRowCarryingItsTextAsItsLabel() {
            var label = new ControlSpec.Label("Non-allied factions are");
            assertThat(label.text()).isEqualTo("Non-allied factions are");
            assertThat(label.labels()).containsExactly("Non-allied factions are");
        }
    }

    @Nested
    class Divider {

        @Test
        void dividerIsARuleWithNoLabel() {
            // A divider is drawn but never clicked and carries no text, so it holds no label.
            assertThat(new ControlSpec.Divider().labels()).isEmpty();
        }
    }

    @Nested
    class VerticalTableIconList {

        @Test
        void iconListIsAVerticalDeselectableTableCarryingIcons() {
            // The icon list is a vertical, deselectable table; the non-empty icon paths are what mark it
            // as the icon-drawing variant rather than a label-only table.
            var picker = VerticalTableSpecs.buildIconList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), ControlSpec.NO_SELECTION, ControlAction.NONE);
            assertThat(picker.reselect()).isEqualTo(ReselectBehaviour.DESELECT);
            assertThat(picker.labels()).containsExactly("Hegemony", "Tri-Tachyon");
            assertThat(picker.iconPaths()).containsExactly("crest_heg", "crest_tt");
        }

        @Test
        void iconListKeepsNullIconEntriesForIconlessOptions() {
            // A crestless option (every alliance) rides as a null entry, so the list stays aligned to
            // the labels index for index; the copy must preserve the null rather than reject it.
            var picker = VerticalTableSpecs.buildIconList(List.of("Hegemony", "Free Traders"),
                    Arrays.asList("crest_heg", null), 0, ControlAction.NONE);
            assertThat(picker.iconPaths()).containsExactly("crest_heg", null);
        }

        @Test
        void iconListLightsTheSelectedOption() {
            var picker = VerticalTableSpecs.buildIconList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), 1, ControlAction.NONE);
            assertThat(picker.selectedIndex()).isEqualTo(1);
        }

        @Test
        void iconListDoesNotAliasTheCallersIconPathList() {
            // The caller may hand in a mutable list it goes on to reuse; the table must copy it, so a
            // later mutation of the caller's list cannot rewrite the drawn icons.
            var callerIconPaths = new ArrayList<String>(List.of("crest_heg", "crest_tt"));
            var picker = VerticalTableSpecs.buildIconList(List.of("Hegemony", "Tri-Tachyon"),
                    callerIconPaths, 0, ControlAction.NONE);
            callerIconPaths.set(0, "crest_mutated");
            assertThat(picker.iconPaths()).containsExactly("crest_heg", "crest_tt");
        }

        @Test
        void iconListCarriesTheClickActionByOptionIndex() {
            var firedCell = new int[]{-99};
            var picker = VerticalTableSpecs.buildIconList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), ControlSpec.NO_SELECTION,
                    cell -> firedCell[0] = cell);
            picker.action().activateCell(1);
            assertThat(firedCell[0]).isEqualTo(1);
        }

        @Test
        void iconListCarriesNoTrailingValuesWhenTheValueColumnIsEmpty() {
            // An icon list built with no value column carries no per-option values, so the rows draw
            // name-only; a non-empty value column is what adds the trailing value.
            var picker = VerticalTableSpecs.buildIconList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), ControlSpec.NO_SELECTION, ControlAction.NONE);
            assertThat(picker.trailingLabels()).isEmpty();
        }

        @Test
        void iconListCarriesPerOptionTrailingValues() {
            // A value column turns the list into a table: each option's value rides parallel to its
            // label, so the row draws its crest, name, and ranked value.
            var picker = ControlSpec.VerticalTable.iconList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), List.of("7", "3"), 0, ControlAction.NONE,
                    ControlSpec.SINGLE_COLUMN);
            assertThat(picker.trailingLabels()).containsExactly("7", "3");
        }

        @Test
        void iconListDoesNotAliasTheCallersTrailingValueList() {
            // Like the icon paths, the values are copied, so a later mutation of the caller's list cannot
            // rewrite the drawn values.
            var callerValues = new ArrayList<String>(List.of("7", "3"));
            var picker = ControlSpec.VerticalTable.iconList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), callerValues, 0, ControlAction.NONE,
                    ControlSpec.SINGLE_COLUMN);
            callerValues.set(0, "99");
            assertThat(picker.trailingLabels()).containsExactly("7", "3");
        }

        @Test
        void iconListCarriesTheChosenColumnCount() {
            // The icon list spreads the options across that many columns; the count rides on the
            // spec so the layout and renderer both wrap the rows the same way.
            var picker = ControlSpec.VerticalTable.iconList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), List.of("7", "3"), 0, ControlAction.NONE, 2);
            assertThat(picker.columnCount()).isEqualTo(2);
        }
    }

    @Nested
    class VerticalTableDirectionTable {

        @Test
        void directionTableCarriesADirectionTrianglePerRowAndReFires() {
            // The sort selector's shape: a direction triangle per row (up for ascending, down for
            // descending) and a re-fire on the lit row so a re-pick can flip the direction.
            var selector = ControlSpec.VerticalTable.directionTable(List.of("Domination", "Presence"),
                    List.of(TriangleDirection.UP, TriangleDirection.DOWN), 0, ControlAction.NONE,
                    ReselectBehaviour.REFIRE);
            assertThat(selector.reselect()).isEqualTo(ReselectBehaviour.REFIRE);
            assertThat(selector.directionAt(0)).isEqualTo(TriangleDirection.UP);
            assertThat(selector.directionAt(1)).isEqualTo(TriangleDirection.DOWN);
        }

        @Test
        void directionTableDrawsNoIconAndNoTrailingText() {
            // The direction table reuses the three-column geometry with an all-null icon column and an
            // empty text column, so the triangle is the only trailing content and no row shows a crest.
            var selector = ControlSpec.VerticalTable.directionTable(List.of("Domination", "Presence"),
                    List.of(TriangleDirection.UP, TriangleDirection.DOWN), 0, ControlAction.NONE,
                    ReselectBehaviour.REFIRE);
            assertThat(selector.hasIconAt(0)).isFalse();
            assertThat(selector.hasIconAt(1)).isFalse();
            assertThat(selector.trailingLabels()).isEmpty();
        }

        @Test
        void directionTableDoesNotAliasTheCallersDirectionList() {
            // The caller may hand in a mutable list it goes on to reuse; the table must copy it, so a
            // later mutation of the caller's list cannot rewrite the drawn triangles - the same guard
            // the icon-path and value lists get.
            var callerDirections = new ArrayList<TriangleDirection>(
                    List.of(TriangleDirection.UP, TriangleDirection.DOWN));
            var selector = ControlSpec.VerticalTable.directionTable(List.of("Domination", "Presence"),
                    callerDirections, 0, ControlAction.NONE, ReselectBehaviour.REFIRE);
            callerDirections.set(0, TriangleDirection.DOWN);
            assertThat(selector.directionAt(0)).isEqualTo(TriangleDirection.UP);
        }
    }

    @Nested
    class AsScrolling {

        @Test
        void asScrollingMarksTheTableAsTheScrollingRegion() {
            // A picker opts its list into scrolling after building it through the ordinary factory, so
            // the copy carries the flag while everything else the layout reads stays as it was.
            var picker = ControlSpec.VerticalTable.iconList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), List.of("7", "3"), 1, ControlAction.NONE, 2);
            var scrolling = picker.asScrolling();
            assertThat(scrolling.scrolls()).isTrue();
            assertThat(scrolling.labels()).isEqualTo(picker.labels());
            assertThat(scrolling.iconPaths()).isEqualTo(picker.iconPaths());
            assertThat(scrolling.trailingLabels()).isEqualTo(picker.trailingLabels());
            assertThat(scrolling.trailingDirections()).isEqualTo(picker.trailingDirections());
            assertThat(scrolling.selectedIndex()).isEqualTo(picker.selectedIndex());
            assertThat(scrolling.columnCount()).isEqualTo(picker.columnCount());
        }

        @Test
        void asScrollingLeavesTheOriginalUnmarked() {
            // The copy is a fresh spec, so the source the host still holds is untouched - only the one it
            // opts in scrolls.
            var picker = VerticalTableSpecs.buildIconList(List.of("Hegemony"), List.of("crest_heg"),
                    0, ControlAction.NONE);
            picker.asScrolling();
            assertThat(picker.scrolls()).isFalse();
        }
    }

    @Nested
    class TrailingLabelAt {

        @Test
        void trailingLabelAtIsTheOptionsValueWhenItHasOne() {
            var picker = ControlSpec.VerticalTable.iconList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), List.of("7", "3"), 0, ControlAction.NONE,
                    ControlSpec.SINGLE_COLUMN);
            assertThat(picker.trailingLabelAt(1)).isEqualTo("3");
        }

        @Test
        void trailingLabelAtIsEmptyForANullValueEntry() {
            // A null entry is a real "no value", so it reads as an empty string rather than throwing.
            var picker = ControlSpec.VerticalTable.iconList(List.of("Hegemony", "Free Traders"),
                    Arrays.asList("crest_heg", null), Arrays.asList("7", null), 0, ControlAction.NONE,
                    ControlSpec.SINGLE_COLUMN);
            assertThat(picker.trailingLabelAt(1)).isEmpty();
        }

        @Test
        void trailingLabelAtIsEmptyForAnIndexPastTheValueList() {
            // A shorter (or empty) value list leaves the trailing options value-less rather than
            // throwing, matching how a short icon-path list leaves options icon-less.
            var picker = VerticalTableSpecs.buildIconList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), ControlSpec.NO_SELECTION, ControlAction.NONE);
            assertThat(picker.trailingLabelAt(0)).isEmpty();
        }
    }

    @Nested
    class DirectionAt {

        @Test
        void directionAtIsTheOptionsTriangleWhenItHasOne() {
            var selector = ControlSpec.VerticalTable.directionTable(List.of("Domination", "Presence"),
                    List.of(TriangleDirection.UP, TriangleDirection.DOWN), 0, ControlAction.NONE,
                    ReselectBehaviour.REFIRE);
            assertThat(selector.directionAt(1)).isEqualTo(TriangleDirection.DOWN);
        }

        @Test
        void directionAtIsNullForANullDirectionEntry() {
            // A null entry is a real "no triangle", so it reads as null rather than throwing - the same
            // shape a null icon-path or value entry takes.
            var selector = ControlSpec.VerticalTable.directionTable(List.of("Domination", "Presence"),
                    Arrays.asList(TriangleDirection.UP, null), 0, ControlAction.NONE,
                    ReselectBehaviour.REFIRE);
            assertThat(selector.directionAt(1)).isNull();
        }

        @Test
        void directionAtIsNullForAnIndexPastTheDirectionList() {
            // A shorter (or empty) direction list leaves the trailing options triangle-less rather than
            // throwing, matching how a short value list leaves options value-less.
            var picker = VerticalTableSpecs.buildIconList(List.of("Hegemony", "Tri-Tachyon"),
                    List.of("crest_heg", "crest_tt"), ControlSpec.NO_SELECTION, ControlAction.NONE);
            assertThat(picker.directionAt(0)).isNull();
        }
    }

    @Nested
    class HasIconAt {

        @Test
        void hasIconAtIsTrueForAnOptionWithANonNullIconPath() {
            var picker = VerticalTableSpecs.buildIconList(List.of("Hegemony", "Free Traders"),
                    Arrays.asList("crest_heg", null), ControlSpec.NO_SELECTION, ControlAction.NONE);
            assertThat(picker.hasIconAt(0)).isTrue();
        }

        @Test
        void hasIconAtIsFalseForAnOptionWithANullIconPath() {
            // A crestless option (an alliance) rides as a null entry, so it draws no icon.
            var picker = VerticalTableSpecs.buildIconList(List.of("Hegemony", "Free Traders"),
                    Arrays.asList("crest_heg", null), ControlSpec.NO_SELECTION, ControlAction.NONE);
            assertThat(picker.hasIconAt(1)).isFalse();
        }

        @Test
        void hasIconAtIsFalseForAnIndexPastTheIconPathList() {
            // A shorter icon-path list leaves the trailing options icon-less rather than throwing.
            var picker = VerticalTableSpecs.buildIconList(List.of("Hegemony", "Free Traders"),
                    List.of("crest_heg"), ControlSpec.NO_SELECTION, ControlAction.NONE);
            assertThat(picker.hasIconAt(1)).isFalse();
        }
    }

    @Nested
    class Tabs {

        @Test
        void tabsCarriesLabelsAndShortcuts() {
            var tabs = new ControlSpec.Tabs(List.of("No Layer", "Political Map"),
                    List.of("N", "P"), 1, ControlAction.NONE);
            assertThat(tabs.labels()).containsExactly("No Layer", "Political Map");
            assertThat(tabs.shortcuts()).containsExactly("N", "P");
            assertThat(tabs.selectedIndex()).isEqualTo(1);
        }

        @Test
        void tabsKeepsNullShortcutEntriesForHintlessTabs() {
            // A tab with no bound shortcut rides as a null entry, so the list stays aligned to the labels
            // index for index; the copy must preserve the null rather than reject it.
            var tabs = new ControlSpec.Tabs(List.of("No Layer", "Political Map"),
                    Arrays.asList("N", null), 0, ControlAction.NONE);
            assertThat(tabs.shortcuts()).containsExactly("N", null);
        }

        @Test
        void tabsCarriesTheClickActionByTabIndex() {
            var firedTab = new int[]{-99};
            var tabs = new ControlSpec.Tabs(List.of("No Layer", "Political Map"),
                    List.of("N", "P"), 0, tab -> firedTab[0] = tab);
            tabs.action().activateCell(1);
            assertThat(firedTab[0]).isEqualTo(1);
        }

        @Test
        void tabsDoesNotAliasTheCallersShortcutList() {
            // The caller may hand in a mutable list it goes on to reuse; the spec must copy it, so a later
            // mutation of the caller's list cannot rewrite the drawn hints.
            var callerShortcuts = new ArrayList<String>(List.of("N", "P"));
            var tabs = new ControlSpec.Tabs(List.of("No Layer", "Political Map"), callerShortcuts, 0,
                    ControlAction.NONE);
            callerShortcuts.set(0, "X");
            assertThat(tabs.shortcuts()).containsExactly("N", "P");
        }
    }

    @Nested
    class ShortcutAt {

        @Test
        void shortcutAtIsTheTabsHintWhenItHasOne() {
            var tabs = new ControlSpec.Tabs(List.of("No Layer", "Political Map"),
                    List.of("N", "P"), 0, ControlAction.NONE);
            assertThat(tabs.shortcutAt(1)).isEqualTo("P");
        }

        @Test
        void shortcutAtIsEmptyForANullEntry() {
            // A null entry is a real "no hint", so it reads as an empty string rather than throwing.
            var tabs = new ControlSpec.Tabs(List.of("No Layer", "Political Map"),
                    Arrays.asList("N", null), 0, ControlAction.NONE);
            assertThat(tabs.shortcutAt(1)).isEmpty();
        }

        @Test
        void shortcutAtIsEmptyForAnIndexPastTheShortcutList() {
            // A shorter (or empty) shortcut list leaves the trailing tabs hint-less rather than throwing.
            var tabs = new ControlSpec.Tabs(List.of("No Layer", "Political Map"), List.of("N"), 0,
                    ControlAction.NONE);
            assertThat(tabs.shortcutAt(1)).isEmpty();
        }
    }

    @Nested
    class HorizontalRadioUniform {

        @Test
        void uniformIsAHorizontalRadioSizedUniform() {
            // The standard side-by-side option pair, its cells all the widest label's width (even cells).
            var radio = ControlSpec.HorizontalRadio.uniform(List.of("Short", "Full"), "Names", 0,
                    ControlAction.NONE);
            assertThat(radio.segmentSizing()).isEqualTo(SegmentSizing.UNIFORM);
            assertThat(radio.trailingLabel()).isEqualTo("Names");
            assertThat(radio.labels()).containsExactly("Short", "Full");
            assertThat(radio.selectedIndex()).isZero();
        }

        @Test
        void uniformCarriesTheClickActionByOptionIndex() {
            var firedCell = new int[]{-99};
            var radio = ControlSpec.HorizontalRadio.uniform(List.of("Short", "Full"), "", 0,
                    cell -> firedCell[0] = cell);
            radio.action().activateCell(1);
            assertThat(firedCell[0]).isEqualTo(1);
        }
    }

    @Nested
    class HorizontalRadioSnapped {

        @Test
        void snappedIsAHorizontalRadioSizedSnapped() {
            // The snapped horizontal radio: a standard side-by-side option row whose cells each snap to
            // their own label width rather than sharing the widest option's, so a ragged row does not
            // waste space as even cells.
            var radio = ControlSpec.HorizontalRadio.snapped(List.of("Short", "Full"), "Names", 0,
                    ControlAction.NONE);
            assertThat(radio.segmentSizing()).isEqualTo(SegmentSizing.SNAPPED);
            assertThat(radio.labels()).containsExactly("Short", "Full");
            assertThat(radio.selectedIndex()).isZero();
        }

        @Test
        void snappedCarriesTheClickActionByOptionIndex() {
            var firedCell = new int[]{-99};
            var radio = ControlSpec.HorizontalRadio.snapped(List.of("Short", "Full"), "", 0,
                    cell -> firedCell[0] = cell);
            radio.action().activateCell(1);
            assertThat(firedCell[0]).isEqualTo(1);
        }

        @Test
        void snappedIsInertOnARepick() {
            // A plain option row is always one lit, so re-picking the lit segment does nothing.
            var radio = ControlSpec.HorizontalRadio.snapped(List.of("Short", "Full"), "", 0,
                    ControlAction.NONE);
            assertThat(radio.reselect()).isEqualTo(ReselectBehaviour.INERT);
        }
    }

    @Nested
    class HorizontalRadioDeselectable {

        @Test
        void deselectableIsAUniformRadioThatClearsOnARepick() {
            // A horizontal on/off selector: even cells, no trailing caption, and DESELECT so re-picking
            // the lit segment fires the action to turn the control off.
            var radio = ControlSpec.HorizontalRadio.deselectable(List.of("Factions", "Alliances"),
                    ControlSpec.NO_SELECTION, ControlAction.NONE);
            assertThat(radio.segmentSizing()).isEqualTo(SegmentSizing.UNIFORM);
            assertThat(radio.reselect()).isEqualTo(ReselectBehaviour.DESELECT);
            assertThat(radio.trailingLabel()).isEmpty();
            assertThat(radio.labels()).containsExactly("Factions", "Alliances");
            assertThat(radio.selectedIndex()).isEqualTo(ControlSpec.NO_SELECTION);
        }

        @Test
        void deselectableCarriesTheClickActionByOptionIndex() {
            var firedCell = new int[]{-99};
            var radio = ControlSpec.HorizontalRadio.deselectable(List.of("Factions", "Alliances"), 0,
                    cell -> firedCell[0] = cell);
            radio.action().activateCell(1);
            assertThat(firedCell[0]).isEqualTo(1);
        }
    }
}
