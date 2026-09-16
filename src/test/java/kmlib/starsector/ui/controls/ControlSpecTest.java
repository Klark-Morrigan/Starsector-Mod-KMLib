package kmlib.starsector.ui.controls;

import kmlib.starsector.ui.text.TextSpan;

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
        void hoverReportDefaultsToReportingNowhere() {
            // The channel is on Interactive and the component is on the one variant whose host asked for
            // one, so every other variant answers a report that drops what it is told. Read as a null, the
            // frame that first put the pointer on such a control would throw rather than report nothing.
            assertThat(LabelledControlSpecs.buildCheckbox("Muted", true, ControlAction.NONE).hoverReport())
                .isEqualTo(ControlHoverReport.NONE);
            assertThat(LabelledControlSpecs.buildToggle("Muted", true, ControlAction.NONE).hoverReport())
                .isEqualTo(ControlHoverReport.NONE);
            assertThat(ControlSpec.HorizontalRadio.of(List.of("A"), 0, ControlAction.NONE).hoverReport())
                .isEqualTo(ControlHoverReport.NONE);
            assertThat(new ControlSpec.Tabs(List.of("A"), List.of(), 0, ControlAction.NONE).hoverReport())
                .isEqualTo(ControlHoverReport.NONE);
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
    class ScrollingSectionConstruction {

        @Test
        void constructorCopiesTheRunDefensively() {
            // The canonical constructor is public on a record, so a host can reach it directly; the copy
            // has to live there for a caller's later edit not to reach the spec.
            var sourceControls = new ArrayList<ControlSpec>(List.of(
                VerticalTableSpecs.buildIconList(
                    List.of("Hegemony"),
                    List.of("crest_heg"),
                    0,
                    ControlAction.NONE)));

            var section = new ControlSpec.ScrollingSection(sourceControls);
            sourceControls.add(ControlSpec.HorizontalRadio.of(List.of("A"), 0, ControlAction.NONE));

            assertThat(section.controls())
                .hasSize(1);
        }

        @Test
        void sectionCarriesItsRunAndNoLabelsOfItsOwn() {
            // The group is chrome-free: its children carry the labels, so a strip measuring the section
            // against a label of its own would charge a width nothing draws.
            var section = new ControlSpec.ScrollingSection(List.of(
                LabelledControlSpecs.buildCheckbox("Muted", false, ControlAction.NONE),
                VerticalTableSpecs.buildIconList(
                    List.of("Hegemony"),
                    List.of("crest_heg"),
                    0,
                    ControlAction.NONE)));

            assertThat(section.controls())
                .hasSize(2);
            assertThat(section.labels())
                .isEmpty();
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
