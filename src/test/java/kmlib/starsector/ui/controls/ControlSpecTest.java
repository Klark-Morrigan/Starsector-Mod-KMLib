package kmlib.starsector.ui.controls;

import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.widgets.RowSlot;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
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
 * illegal shapes unrepresentable - a checkbox has no leading column to mis-set, and a table's rows hold
 * their own slots rather than lists that could fall out of step - so only the guards a vertical table's
 * own construction can still trip are pinned here.
 */
final class ControlSpecTest {

    @Nested
    class Interactive {

        @Test
        void checkboxAndRadiosAndTabsAreInteractive() {
            // The clickable, stateful controls implement Interactive, so the input listener acts on them.
            assertThat(LabelledControlSpecs.buildCheckbox("Muted", true, ControlAction.NONE))
                .isInstanceOf(ControlSpec.Interactive.class);
            assertThat(LabelledControlSpecs.buildToggle("Muted", true, ControlAction.NONE))
                .isInstanceOf(ControlSpec.Interactive.class);
            assertThat(ControlSpec.HorizontalRadio.of(List.of("A"), 0, ControlAction.NONE))
                .isInstanceOf(ControlSpec.Interactive.class);

            assertThat(VerticalTableSpecs.buildSegmentedList(
                    List.of("A"),
                    0,
                    ControlAction.NONE,
                    ReselectBehaviour.INERT))
                .isInstanceOf(ControlSpec.Interactive.class);

            assertThat(new ControlSpec.Tabs(List.of("A"), List.of(), 0, ControlAction.NONE))
                .isInstanceOf(ControlSpec.Interactive.class);
        }

        @Test
        void labelAndDividerAreNotInteractive() {
            // A caption and a rule are drawn but never clicked, so they are chrome, not Interactive - the
            // one property the input listener reads to skip them.
            assertThat(LabelledControlSpecs.buildLabel("Names"))
                .isNotInstanceOf(ControlSpec.Interactive.class);
            assertThat(new ControlSpec.Divider())
                .isNotInstanceOf(ControlSpec.Interactive.class);
        }
    }

    @Nested
    class VerticalTableConstructor {

        @Test
        void constructorRejectsAColumnCountBelowOne() {
            // A column count is how many columns the rows fold across; a count below one cannot lay
            // out any column, so it fails at construction rather than dividing by a zero column count.
            assertThatThrownBy(() -> new ControlSpec.VerticalTable(
                    VerticalTableSpecs.buildRows(List.of("A")),
                    RowGeometry.COLUMNS,
                    ControlSpec.NO_SELECTION,
                    ControlAction.NONE,
                    ReselectBehaviour.DESELECT,
                    0,
                    false))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void constructorRejectsAMissingRowGeometry() {
            // How the rows lay out is stated rather than inferred, so a table built without that answer
            // fails where its builder is still on the stack rather than at the first measurement.
            assertThatThrownBy(() -> new ControlSpec.VerticalTable(
                    VerticalTableSpecs.buildRows(List.of("A")),
                    null,
                    ControlSpec.NO_SELECTION,
                    ControlAction.NONE,
                    ReselectBehaviour.DESELECT,
                    ControlSpec.SINGLE_COLUMN,
                    false))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void constructorDoesNotAliasTheCallersRowList() {
            // The caller may hand in a mutable list it goes on to reuse; the table must copy it, so a
            // later mutation of the caller's list cannot rewrite the drawn rows.
            var callerRows = new ArrayList<>(VerticalTableSpecs.buildRows(
                List.of("Factions", "Alliances")));

            var table = new ControlSpec.VerticalTable(
                callerRows,
                RowGeometry.COLUMNS,
                0,
                ControlAction.NONE,
                ReselectBehaviour.DESELECT,
                ControlSpec.SINGLE_COLUMN,
                false);

            callerRows.set(0, VerticalTableSpecs.buildRow("Mutated"));

            assertThat(table.labels())
                .containsExactly("Factions", "Alliances");
        }
    }

    @Nested
    class CheckboxLit {

        @Test
        void litLightsCellZeroWhenOn() {

            var checkbox = LabelledControlSpecs.buildCheckbox("Muted", true, ControlAction.NONE);

            assertThat(checkbox.labels())
                .containsExactly("Muted");
            assertThat(checkbox.selectedIndex())
                .isZero();
            assertThat(checkbox.isLit())
                .isTrue();
        }

        @Test
        void litLeavesNoCellLitWhenOff() {

            var checkbox = LabelledControlSpecs.buildCheckbox("Muted", false, ControlAction.NONE);

            assertThat(checkbox.selectedIndex())
                .isEqualTo(ControlSpec.NO_SELECTION);
            assertThat(checkbox.isLit())
                .isFalse();
        }

        @Test
        void litCarriesTheClickActionOnCellZero() {
            // The row's single cell (0) is the hit target, so a click fires the action for cell 0 -
            // pinned by capturing which cell the action was invoked with.
            var firedCell = new int[] {-99};
            var checkbox = ControlSpec.Checkbox.lit(
                LabelledControlSpecs.buildLabelSpan("Muted"),
                false,
                cell -> firedCell[0] = cell);

            checkbox.action().activateCell(0);

            assertThat(firedCell[0])
                .isZero();
        }

        @Test
        void litCarriesTheLabelAsOneRunInItsOwnColour() {
            // A control that reads in one colour is the single run its factory builds - the bargain that
            // keeps a plain label a plain call while the model still holds runs.
            var checkbox = ControlSpec.Checkbox.lit(
                new TextSpan("Muted", Color.CYAN),
                true,
                ControlAction.NONE);

            assertThat(checkbox.labelRuns())
                .containsExactly(new TextSpan("Muted", Color.CYAN));
        }
    }

    @Nested
    class CheckboxConstructor {

        @Test
        void constructorRejectsALabelWithNoRuns() {
            // A control with nothing to say is not a control; the floor is held where the caller that
            // built it is still on the stack rather than at the first measurement of an empty label.
            assertThatThrownBy(() -> new ControlSpec.Checkbox(
                    List.of(),
                    ControlSpec.NO_SELECTION,
                    ControlAction.NONE))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class CheckboxContinuesWith {

        @Test
        void continuesWithAppendsTheRunAndKeepsTheRest() {

            var checkbox = LabelledControlSpecs
                .buildCheckbox("Muted", true, ControlAction.NONE)
                .continuesWith(new TextSpan("(recedes)", Color.YELLOW));

            assertThat(checkbox.labelRuns())
                .containsExactly(
                    new TextSpan("Muted", LabelledControlSpecs.LABEL_TEXT_COLOUR),
                    new TextSpan("(recedes)", Color.YELLOW));
            assertThat(checkbox.selectedIndex())
                .isZero();
        }

        @Test
        void labelsReadTheContinuedRunsAsOneLine() {
            // A strip snaps a control to what its label says, not to how many colours it says it in.
            var checkbox = LabelledControlSpecs
                .buildCheckbox("Muted", true, ControlAction.NONE)
                .continuesWith(new TextSpan("(recedes)", Color.YELLOW));

            assertThat(checkbox.labels())
                .containsExactly("Muted (recedes)");
        }
    }

    @Nested
    class ToggleLit {

        @Test
        void litLightsCellZeroWhenOn() {

            var toggle = LabelledControlSpecs.buildToggle("Factions", true, ControlAction.NONE);

            assertThat(toggle.labels())
                .containsExactly("Factions");
            assertThat(toggle.selectedIndex())
                .isZero();
            assertThat(toggle.isLit())
                .isTrue();
        }

        @Test
        void litLeavesNoCellLitWhenOff() {

            var toggle = LabelledControlSpecs.buildToggle("Factions", false, ControlAction.NONE);

            assertThat(toggle.selectedIndex())
                .isEqualTo(ControlSpec.NO_SELECTION);
        }
    }

    @Nested
    class ToggleConstructor {

        @Test
        void constructorRejectsALabelWithNoRuns() {
            assertThatThrownBy(() -> new ControlSpec.Toggle(
                    List.of(),
                    ControlSpec.NO_SELECTION,
                    ControlAction.NONE))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class ToggleContinuesWith {

        @Test
        void continuesWithAppendsTheRunAndKeepsTheRest() {

            var toggle = LabelledControlSpecs
                .buildToggle("Factions", true, ControlAction.NONE)
                .continuesWith(new TextSpan("3", Color.YELLOW));

            assertThat(toggle.labelRuns())
                .containsExactly(
                    new TextSpan("Factions", LabelledControlSpecs.LABEL_TEXT_COLOUR),
                    new TextSpan("3", Color.YELLOW));
            assertThat(toggle.selectedIndex())
                .isZero();
        }
    }

    @Nested
    class LabelCreateLabel {

        @Test
        void createLabelIsATextOnlyRowCarryingItsRunAsItsLabel() {

            var label = ControlSpec.Label.createLabel(
                new TextSpan("Non-allied factions are", Color.CYAN));

            assertThat(label.labelRuns())
                .containsExactly(new TextSpan("Non-allied factions are", Color.CYAN));
            assertThat(label.labels())
                .containsExactly("Non-allied factions are");
        }
    }

    @Nested
    class LabelConstructor {

        @Test
        void constructorRejectsALabelWithNoRuns() {
            assertThatThrownBy(() -> new ControlSpec.Label(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class LabelContinuesWith {

        @Test
        void continuesWithAppendsTheRun() {

            var label = LabelledControlSpecs
                .buildLabel("Non-allied factions are")
                .continuesWith(new TextSpan("hidden", Color.YELLOW));

            assertThat(label.labelRuns())
                .containsExactly(
                    new TextSpan("Non-allied factions are", LabelledControlSpecs.LABEL_TEXT_COLOUR),
                    new TextSpan("hidden", Color.YELLOW));
        }
    }

    @Nested
    class Divider {

        @Test
        void dividerIsARuleWithNoLabel() {
            // A divider is drawn but never clicked and carries no text, so it holds no label.
            assertThat(new ControlSpec.Divider().labels())
                .isEmpty();
        }
    }

    @Nested
    class CreateColumnTable {

        @Test
        void createColumnTableLaysItsRowsInColumns() {
            // The geometry is what the table states about itself, so a picker's rows lay out as a table
            // whatever their slots hold - a stack whose rows all lead with nothing is a table still.
            var picker = ControlSpec.VerticalTable.createColumnTable(
                VerticalTableSpecs.buildRows(List.of("Hegemony", "Tri-Tachyon")),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE);

            assertThat(picker.rowGeometry())
                .isEqualTo(RowGeometry.COLUMNS);
        }

        @Test
        void createColumnTableStandsInOneInertUnscrolledColumnUntilRefined() {
            // What a table holds before a host refines it: every row inert on a re-pick, one column, and
            // pinned - so a host states only the refinements its own list wants.
            var picker = ControlSpec.VerticalTable.createColumnTable(
                VerticalTableSpecs.buildRows(List.of("Hegemony", "Tri-Tachyon")),
                ControlSpec.NO_SELECTION,
                ControlAction.NONE);

            assertThat(picker.reselect())
                .isEqualTo(ReselectBehaviour.INERT);
            assertThat(picker.columnCount())
                .isEqualTo(1);
            assertThat(picker.scrolls())
                .isFalse();
        }

        @Test
        void createColumnTableCarriesItsRowsWhateverTheirSlotsHold() {
            // A row states what it leads and trails with itself, so a crested, valued row and a bare one
            // stack in one list rather than in a list per column that must stay index-aligned.
            var picker = ControlSpec.VerticalTable.createColumnTable(
                VerticalTableSpecs.buildRows(
                    List.of("Hegemony", "Free Traders"),
                    Arrays.asList("crest_heg", null),
                    List.of("7")),
                0,
                ControlAction.NONE);

            assertThat(picker.labelledRows())
                .hasSize(2);
            assertThat(picker.labelledRows().get(0).leadingRowSlot())
                .isEqualTo(new RowSlot.Image("crest_heg"));
            assertThat(picker.labelledRows().get(0).trailingRowSlot())
                .isEqualTo(new RowSlot.Text(new TextSpan("7", VerticalTableSpecs.ROW_TEXT_COLOUR)));
            assertThat(picker.labelledRows().get(1).leadingRowSlot())
                .isEqualTo(RowSlot.EMPTY);
            assertThat(picker.labelledRows().get(1).trailingRowSlot())
                .isEqualTo(RowSlot.EMPTY);
        }

        @Test
        void createColumnTableLightsTheSelectedRow() {

            var picker = VerticalTableSpecs.buildIconList(
                List.of("Hegemony", "Tri-Tachyon"),
                List.of("crest_heg", "crest_tt"),
                1,
                ControlAction.NONE);

            assertThat(picker.selectedIndex())
                .isEqualTo(1);
        }

        @Test
        void createColumnTableCarriesTheClickActionByRowIndex() {

            var firedCell = new int[] {-99};
            var picker = VerticalTableSpecs.buildIconList(
                List.of("Hegemony", "Tri-Tachyon"),
                List.of("crest_heg", "crest_tt"),
                ControlSpec.NO_SELECTION,
                cell -> firedCell[0] = cell);

            picker.action().activateCell(1);

            assertThat(firedCell[0])
                .isEqualTo(1);
        }
    }

    @Nested
    class CreateSegmentedList {

        @Test
        void createSegmentedListLaysItsRowsAsUniformCells() {
            // The other geometry a stack can read as: equal cells with each name centred, rather than a
            // table of columns - stated by which factory a host reaches for.
            var list = ControlSpec.VerticalTable.createSegmentedList(
                VerticalTableSpecs.buildRows(List.of("Factions", "Alliances")),
                0,
                ControlAction.NONE);

            assertThat(list.rowGeometry())
                .isEqualTo(RowGeometry.UNIFORM_SEGMENTS);
        }
    }

    @Nested
    class VerticalTableLabels {

        @Test
        void labelsAreEachRowsRunsReadAsOneLine() {
            // A row's label may be authored in several runs so part of it draws in its own colour; a
            // strip that snaps a control to its text charges the whole line, so the runs read as one -
            // parted by the run vocabulary's own space rather than by one written into a phrase.
            var table = ControlSpec.VerticalTable.createColumnTable(
                List.of(VerticalTableSpecs.buildRow("Hegemony")
                    .continuesWith(new TextSpan("(7)", VerticalTableSpecs.ROW_TEXT_COLOUR))),
                0,
                ControlAction.NONE);

            assertThat(table.labels())
                .containsExactly("Hegemony (7)");
        }
    }

    @Nested
    class HandlesReselect {

        @Test
        void handlesReselectCarriesTheChosenBehaviourAndLeavesTheRestAsItWas() {
            // A spotlight list clears on a re-pick of its lit row; the refinement changes that alone, so
            // everything the layout reads stays as the factory built it.
            var picker = ControlSpec.VerticalTable
                .createColumnTable(
                    VerticalTableSpecs.buildRows(List.of("Hegemony", "Tri-Tachyon")),
                    1,
                    ControlAction.NONE)
                .handlesReselect(ReselectBehaviour.DESELECT);

            assertThat(picker.reselect())
                .isEqualTo(ReselectBehaviour.DESELECT);
            assertThat(picker.labels())
                .containsExactly("Hegemony", "Tri-Tachyon");
            assertThat(picker.selectedIndex())
                .isEqualTo(1);
            assertThat(picker.rowGeometry())
                .isEqualTo(RowGeometry.COLUMNS);
        }
    }

    @Nested
    class SpreadsAcross {

        @Test
        void spreadsAcrossFoldsTheRowsAcrossThatManyColumns() {
            // The count rides on the spec so the layout and the renderer fold the rows the same way.
            var picker = ControlSpec.VerticalTable
                .createColumnTable(
                    VerticalTableSpecs.buildRows(List.of("Hegemony", "Tri-Tachyon")),
                    0,
                    ControlAction.NONE)
                .spreadsAcross(2);

            assertThat(picker.columnCount())
                .isEqualTo(2);
            assertThat(picker.labels())
                .containsExactly("Hegemony", "Tri-Tachyon");
        }
    }

    @Nested
    class AsScrolling {

        @Test
        void asScrollingMarksTheTableAsTheScrollingRegion() {
            // A picker opts its list into scrolling after building it through the ordinary factory, so
            // the copy carries the flag while everything else the layout reads stays as it was.
            var picker = VerticalTableSpecs.buildIconList(
                List.of("Hegemony", "Tri-Tachyon"),
                List.of("crest_heg", "crest_tt"),
                List.of("7", "3"),
                1,
                ControlAction.NONE,
                2);

            var scrolling = picker.asScrolling();

            assertThat(scrolling.scrolls())
                .isTrue();
            assertThat(scrolling.labels())
                .containsExactly("Hegemony", "Tri-Tachyon");
            assertThat(scrolling.rowGeometry())
                .isEqualTo(RowGeometry.COLUMNS);
            assertThat(scrolling.selectedIndex())
                .isEqualTo(1);
            assertThat(scrolling.columnCount())
                .isEqualTo(2);
        }

        @Test
        void asScrollingLeavesTheOriginalUnmarked() {
            // The copy is a fresh spec, so the source the host still holds is untouched - only the one it
            // opts in scrolls.
            var picker = VerticalTableSpecs.buildIconList(
                List.of("Hegemony"),
                List.of("crest_heg"),
                0,
                ControlAction.NONE);

            picker.asScrolling();

            assertThat(picker.scrolls())
                .isFalse();
        }
    }

    @Nested
    class Tabs {

        @Test
        void tabsCarriesLabelsAndShortcuts() {

            var tabs = new ControlSpec.Tabs(
                List.of("No Layer", "Political Map"),
                List.of("N", "P"),
                1,
                ControlAction.NONE);

            assertThat(tabs.labels())
                .containsExactly("No Layer", "Political Map");
            assertThat(tabs.shortcuts())
                .containsExactly("N", "P");
            assertThat(tabs.selectedIndex())
                .isEqualTo(1);
        }

        @Test
        void tabsKeepsNullShortcutEntriesForHintlessTabs() {
            // A tab with no bound shortcut rides as a null entry, so the list stays aligned to the labels
            // index for index; the copy must preserve the null rather than reject it.
            var tabs = new ControlSpec.Tabs(
                List.of("No Layer", "Political Map"),
                Arrays.asList("N", null),
                0,
                ControlAction.NONE);

            assertThat(tabs.shortcuts())
                .containsExactly("N", null);
        }

        @Test
        void tabsCarriesTheClickActionByTabIndex() {

            var firedTab = new int[] {-99};
            var tabs = new ControlSpec.Tabs(
                List.of("No Layer", "Political Map"),
                List.of("N", "P"),
                0,
                tab -> firedTab[0] = tab);

            tabs.action().activateCell(1);

            assertThat(firedTab[0])
                .isEqualTo(1);
        }

        @Test
        void tabsDoesNotAliasTheCallersShortcutList() {
            // The caller may hand in a mutable list it goes on to reuse; the spec must copy it, so a later
            // mutation of the caller's list cannot rewrite the drawn hints.
            var callerShortcuts = new ArrayList<String>(List.of("N", "P"));
            var tabs = new ControlSpec.Tabs(
                List.of("No Layer", "Political Map"),
                callerShortcuts,
                0,
                ControlAction.NONE);

            callerShortcuts.set(0, "X");

            assertThat(tabs.shortcuts())
                .containsExactly("N", "P");
        }
    }

    @Nested
    class ShortcutAt {

        @Test
        void shortcutAtIsTheTabsHintWhenItHasOne() {

            var tabs = new ControlSpec.Tabs(
                List.of("No Layer", "Political Map"),
                List.of("N", "P"),
                0,
                ControlAction.NONE);

            assertThat(tabs.shortcutAt(1))
                .isEqualTo("P");
        }

        @Test
        void shortcutAtIsEmptyForANullEntry() {
            // A null entry is a real "no hint", so it reads as an empty string rather than throwing.
            var tabs = new ControlSpec.Tabs(
                List.of("No Layer", "Political Map"),
                Arrays.asList("N", null),
                0,
                ControlAction.NONE);

            assertThat(tabs.shortcutAt(1))
                .isEmpty();
        }

        @Test
        void shortcutAtIsEmptyForAnIndexPastTheShortcutList() {
            // A shorter (or empty) shortcut list leaves the trailing tabs hint-less rather than throwing.
            var tabs = new ControlSpec.Tabs(
                List.of("No Layer", "Political Map"),
                List.of("N"),
                0,
                ControlAction.NONE);

            assertThat(tabs.shortcutAt(1))
                .isEmpty();
        }
    }

    @Nested
    class HorizontalRadioConstruction {

        @Test
        void constructorCopiesTheLabelListDefensively() {
            // The canonical constructor is public on a record, so a host can reach it directly; the copy
            // has to live there rather than in the factory for a caller's later edit not to reach the spec.
            var sourceLabels = new ArrayList<>(List.of("Short", "Full"));
            var radio = new ControlSpec.HorizontalRadio(
                sourceLabels,
                0,
                ControlAction.NONE,
                "",
                SegmentSizing.UNIFORM,
                ReselectBehaviour.INERT);

            sourceLabels.add("Mutated");

            assertThat(radio.labels())
                .containsExactly("Short", "Full");
        }
    }

    @Nested
    class HorizontalRadioOf {

        @Test
        void ofIsAUniformInertRadioCarryingNoCaption() {
            // The plain option row a host reaches for by default: cells all the widest label's width, no
            // trailing caption, and always one lit (a re-pick of the lit segment does nothing).
            var radio = ControlSpec.HorizontalRadio.of(List.of("Short", "Full"), 0, ControlAction.NONE);

            assertThat(radio.segmentSizing())
                .isEqualTo(SegmentSizing.UNIFORM);
            assertThat(radio.reselect())
                .isEqualTo(ReselectBehaviour.INERT);
            assertThat(radio.trailingLabel())
                .isEqualTo(ControlSpec.NO_TRAILING_CAPTION);
            assertThat(radio.hasTrailingCaption())
                .isFalse();
            assertThat(radio.labels())
                .containsExactly("Short", "Full");
            assertThat(radio.selectedIndex())
                .isZero();
        }

        @Test
        void ofCarriesTheClickActionByOptionIndex() {

            var firedCell = new int[] {-99};
            var radio = ControlSpec.HorizontalRadio.of(
                List.of("Short", "Full"),
                0,
                cell -> firedCell[0] = cell);

            radio.action().activateCell(1);

            assertThat(firedCell[0])
                .isEqualTo(1);
        }

        @Test
        void ofComposesWithEveryRefinementAtOnce() {
            // The three refinements are independent axes, so a host reaches combinations no single factory
            // names - here a captioned, snapped, clearable row all at once.
            var radio = ControlSpec.HorizontalRadio.of(List.of("Short", "Full"), 0, ControlAction.NONE)
                .showsCaption("Names")
                .sizesSegments(SegmentSizing.SNAPPED)
                .handlesReselect(ReselectBehaviour.DESELECT);

            assertThat(radio.trailingLabel())
                .isEqualTo("Names");
            assertThat(radio.segmentSizing())
                .isEqualTo(SegmentSizing.SNAPPED);
            assertThat(radio.reselect())
                .isEqualTo(ReselectBehaviour.DESELECT);
        }
    }

    @Nested
    class HorizontalRadioHasTrailingCaption {

        @Test
        void hasTrailingCaptionIsFalseOnAPlainRow() {

            var radio = ControlSpec.HorizontalRadio.of(List.of("Short", "Full"), 0, ControlAction.NONE);

            assertThat(radio.hasTrailingCaption())
                .isFalse();
        }

        @Test
        void hasTrailingCaptionIsTrueOnACaptionedRow() {

            var radio = ControlSpec.HorizontalRadio.of(List.of("Short", "Full"), 0, ControlAction.NONE)
                .showsCaption("Names");

            assertThat(radio.hasTrailingCaption())
                .isTrue();
        }

        @Test
        void hasTrailingCaptionIsFalseOnAWhitespaceOnlyCaption() {
            // A host that assembles a caption from parts and comes up with only spacing gets the
            // uncaptioned row, so the layout reserves no footprint the renderer then draws nothing in.
            var radio = ControlSpec.HorizontalRadio.of(List.of("Short", "Full"), 0, ControlAction.NONE)
                .showsCaption("   ");

            assertThat(radio.hasTrailingCaption())
                .isFalse();
        }
    }

    @Nested
    class HorizontalRadioShowsCaption {

        @Test
        void showsCaptionSetsOnlyTheTrailingLabel() {

            var radio = ControlSpec.HorizontalRadio.of(List.of("Short", "Full"), 0, ControlAction.NONE)
                .showsCaption("Names");

            assertThat(radio.trailingLabel())
                .isEqualTo("Names");
            assertThat(radio.segmentSizing())
                .isEqualTo(SegmentSizing.UNIFORM);
            assertThat(radio.reselect())
                .isEqualTo(ReselectBehaviour.INERT);
            assertThat(radio.labels())
                .containsExactly("Short", "Full");
            assertThat(radio.selectedIndex())
                .isZero();
        }
    }

    @Nested
    class HorizontalRadioSizesSegments {

        @Test
        void sizesSegmentsSetsOnlyTheSegmentSizing() {
            // A snapped row's cells each take their own label's width rather than sharing the widest
            // option's, so a ragged row does not waste space as even cells.
            var radio = ControlSpec.HorizontalRadio.of(List.of("Short", "Full"), 0, ControlAction.NONE)
                .sizesSegments(SegmentSizing.SNAPPED);

            assertThat(radio.segmentSizing())
                .isEqualTo(SegmentSizing.SNAPPED);
            assertThat(radio.trailingLabel())
                .isEmpty();
            assertThat(radio.reselect())
                .isEqualTo(ReselectBehaviour.INERT);
            assertThat(radio.labels())
                .containsExactly("Short", "Full");
            assertThat(radio.selectedIndex())
                .isZero();
        }
    }

    @Nested
    class HorizontalRadioHandlesReselect {

        @Test
        void handlesReselectSetsOnlyTheReselectBehaviour() {
            // A horizontal on/off selector: re-picking the lit segment fires the action to turn it off.
            var radio = ControlSpec.HorizontalRadio.of(
                    List.of("Factions", "Alliances"),
                    ControlSpec.NO_SELECTION,
                    ControlAction.NONE)
                .handlesReselect(ReselectBehaviour.DESELECT);

            assertThat(radio.reselect())
                .isEqualTo(ReselectBehaviour.DESELECT);
            assertThat(radio.segmentSizing())
                .isEqualTo(SegmentSizing.UNIFORM);
            assertThat(radio.trailingLabel())
                .isEmpty();
            assertThat(radio.labels())
                .containsExactly("Factions", "Alliances");
            assertThat(radio.selectedIndex())
                .isEqualTo(ControlSpec.NO_SELECTION);
        }
    }
}
