package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how a tab says which key it answers to: the engine's rule that a key already present in the label
 * lights that letter where it stands, and only a key with nowhere to land is spelt out in brackets after
 * it. Both the run breakdown the paint pass walks and the display text the layout measures come from
 * here, so the two are pinned together - a tab sized for a spelt-out key and drawn with a lit letter
 * would be a tab with a gap at its end.
 */
final class TabShortcutTextTest {

    private static final String LABEL = "Political Map";

    @Nested
    class ResolveRuns {

        @Test
        void resolveRunsReadsAsTheBareLabelWhenTheTabHasNoKey() {
            assertThat(TabShortcutText.resolveRuns(new VanillaTabContent(LABEL, null)))
                .containsExactly(new TabTextRun(LABEL, TabTextRun.Role.LABEL));
        }

        @Test
        void resolveRunsReadsAsTheBareLabelWhenTheKeyIsBlank() {
            // An unbound key arrives as blank rather than absent from some callers; both mean the tab has
            // nothing to say about a binding.
            assertThat(TabShortcutText.resolveRuns(new VanillaTabContent(LABEL, "  ")))
                .containsExactly(new TabTextRun(LABEL, TabTextRun.Role.LABEL));
        }

        @Test
        void resolveRunsLightsTheKeysLetterWhereItStandsInTheLabel() {
            // "Political Map" bound to M lights the M of "Map" - the tab gains no width and reads as its
            // own name rather than as a name with a key stapled to it.
            assertThat(TabShortcutText.resolveRuns(new VanillaTabContent(LABEL, "M")))
                .containsExactly(
                    new TabTextRun("Political ", TabTextRun.Role.LABEL),
                    new TabTextRun("M", TabTextRun.Role.KEY),
                    new TabTextRun("ap", TabTextRun.Role.LABEL));
        }

        @Test
        void resolveRunsLightsTheLabelsOwnLetterWhateverCaseTheKeyIsReportedIn() {
            // The lit run holds the label's capital, not the key name's lower case: what lights is a
            // letter of the name, not a copy of the key laid over it.
            assertThat(TabShortcutText.resolveRuns(new VanillaTabContent("Sector", "s")))
                .containsExactly(
                    new TabTextRun("S", TabTextRun.Role.KEY),
                    new TabTextRun("ector", TabTextRun.Role.LABEL));
        }

        @Test
        void resolveRunsLightsTheFirstOccurrenceWhenTheLetterRepeats() {
            assertThat(TabShortcutText.resolveRuns(new VanillaTabContent("Alliances", "A")))
                .containsExactly(
                    new TabTextRun("A", TabTextRun.Role.KEY),
                    new TabTextRun("lliances", TabTextRun.Role.LABEL));
        }

        @Test
        void resolveRunsSpellsTheKeyOutWhenTheLabelDoesNotContainIt() {
            assertThat(TabShortcutText.resolveRuns(new VanillaTabContent(LABEL, "Z")))
                .containsExactly(
                    new TabTextRun(LABEL, TabTextRun.Role.LABEL),
                    new TabTextRun("  ", TabTextRun.Role.LABEL),
                    new TabTextRun("[", TabTextRun.Role.LABEL),
                    new TabTextRun("Z", TabTextRun.Role.KEY),
                    new TabTextRun("]", TabTextRun.Role.LABEL));
        }

        @Test
        void resolveRunsSpellsAMultiGlyphKeyOutEvenWhereItsLettersOccur() {
            // "Fleet" bound to F1 must not light its F: the key is not that letter, and no single glyph in
            // the name stands for it.
            assertThat(TabShortcutText.resolveRuns(new VanillaTabContent("Fleet", "F1")))
                .contains(new TabTextRun("F1", TabTextRun.Role.KEY))
                .startsWith(new TabTextRun("Fleet", TabTextRun.Role.LABEL));
        }
    }

    @Nested
    class ComposeDisplayText {

        @Test
        void composeDisplayTextReadsAsTheLabelAloneWhenTheKeyLightsInPlace() {
            // The whole point of the width: a lit letter costs a tab nothing to show.
            assertThat(TabShortcutText.composeDisplayText(new VanillaTabContent(LABEL, "M")))
                .isEqualTo(LABEL);
        }

        @Test
        void composeDisplayTextBracketsASpeltOutKeyAfterTheLabel() {
            // The delimiter is the vanilla-parity contract - a spelt-out hotkey reads "[Z]", not "(Z)",
            // matching the engine's own tabs - so pin the string lest it silently regress.
            assertThat(TabShortcutText.composeDisplayText(new VanillaTabContent(LABEL, "Z")))
                .isEqualTo("Political Map  [Z]");
        }

        @Test
        void composeDisplayTextReadsAsTheLabelAloneWhenTheTabHasNoKey() {
            assertThat(TabShortcutText.composeDisplayText(new VanillaTabContent(LABEL, null)))
                .isEqualTo(LABEL);
        }
    }
}
