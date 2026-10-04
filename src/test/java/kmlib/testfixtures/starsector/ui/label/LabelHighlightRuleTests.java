package kmlib.testfixtures.starsector.ui.label;

import kmlib.starsector.ui.highlight.Highlight;
import kmlib.starsector.ui.highlight.HighlightedParagraph;
import kmlib.testfixtures.starsector.ui.label.LabelHighlightRule.UnhighlightedRun;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the game's rule for which runs a highlight call leaves plain: a run needs whitespace or ASCII
 * punctuation on each side, or the start or end of the text.
 */
final class LabelHighlightRuleTests {

    // "Default: Gold" in Chinese (U+9ED8 U+8BA4 U+503C U+FF1A, then Gold): a run as a Chinese settings
    // description carries it.
    private static final String CHINESE_DEFAULT_RUN = "默认值：Gold";

    // A Chinese full stop (U+3002), the neighbour the game refuses most often.
    private static final String CHINESE_FULL_STOP = "。";

    // "Part" (U+90E8 U+5206), a CJK character standing directly before a run.
    private static final String CHINESE_WORD = "部分";

    @Nested
    class FindUnhighlightedRuns {

        @Test
        void runsBetweenSpacesAndAsciiPunctuationAreHighlighted() {

            assertThat(LabelHighlightRule.findUnhighlightedRuns(
                    "Draws above the fog. Default: Gold.",
                    List.of("above", "Default: Gold")))
                .isEmpty();
        }

        @Test
        void aRunBetweenChineseFullStopsIsLeftPlainNamingBothNeighbours() {

            var text = CHINESE_WORD + CHINESE_FULL_STOP + CHINESE_DEFAULT_RUN + CHINESE_FULL_STOP;

            assertThat(LabelHighlightRule.findUnhighlightedRuns(text, List.of(CHINESE_DEFAULT_RUN)))
                .containsExactly(new UnhighlightedRun(
                    CHINESE_DEFAULT_RUN,
                    "before it: " + CHINESE_FULL_STOP + ", after it: " + CHINESE_FULL_STOP));
        }

        @Test
        void aRunOnALineOfItsOwnAtTheEndIsHighlighted() {

            var text = CHINESE_WORD + CHINESE_FULL_STOP + "\n" + CHINESE_DEFAULT_RUN;

            assertThat(LabelHighlightRule.findUnhighlightedRuns(text, List.of(CHINESE_DEFAULT_RUN)))
                .isEmpty();
        }

        @Test
        void aRunPaddedWithSpacesBesideChineseTextIsHighlighted() {

            var text = CHINESE_WORD + " " + CHINESE_DEFAULT_RUN + " " + CHINESE_FULL_STOP;

            assertThat(LabelHighlightRule.findUnhighlightedRuns(text, List.of(CHINESE_DEFAULT_RUN)))
                .isEmpty();
        }

        @Test
        void anUnderscoreIsAcceptedAfterARunButNotBeforeIt() {

            assertThat(LabelHighlightRule.findUnhighlightedRuns("run_ and _run", List.of("run", "run")))
                .containsExactly(new UnhighlightedRun("run", "before it: _"));
        }

        @Test
        void aRunEndingBeforeADecimalPointIsLeftPlain() {

            assertThat(LabelHighlightRule.findUnhighlightedRuns("Costs 3.5 credits", List.of("3")))
                .containsExactly(new UnhighlightedRun("3", "after it: ."));
        }

        @Test
        void aLaterOccurrenceIsTakenWhereTheFirstIsRefused() {

            var text = CHINESE_WORD + "Gold and Gold";

            assertThat(LabelHighlightRule.findUnhighlightedRuns(text, List.of("Gold")))
                .isEmpty();
        }

        @Test
        void eachRunIsSearchedForFromWhereThePreviousOneMatched() {

            // The second run stands only before the first one's match, so it is never found.
            assertThat(LabelHighlightRule.findUnhighlightedRuns("alpha beta", List.of("beta", "alpha")))
                .containsExactly(new UnhighlightedRun(
                    "alpha",
                    "only before the previous run's match, where the search does not look"));
        }

        @Test
        void aRunTheTextLacksIsNamedAsMissing() {

            assertThat(LabelHighlightRule.findUnhighlightedRuns("alpha", List.of("gamma")))
                .containsExactly(new UnhighlightedRun("gamma", "not in the text"));
        }

        @Test
        void aParagraphsRunsAreHeldInTheOrderItHandsThemOver() {

            // Named in slot order, which the text reverses; the paragraph hands them over in the text's
            // order, so both highlight.
            var paragraph = new HighlightedParagraph(
                "beta, then alpha",
                Color.WHITE,
                new Highlight("alpha", Color.RED),
                new Highlight("beta", Color.WHITE));

            assertThat(LabelHighlightRule.findUnhighlightedRuns(paragraph))
                .isEmpty();
        }

        @Test
        void aParagraphsRunBesideAChineseFullStopIsFound() {

            var paragraph = new HighlightedParagraph(
                CHINESE_WORD + " " + CHINESE_DEFAULT_RUN + CHINESE_FULL_STOP,
                Color.WHITE,
                new Highlight(CHINESE_DEFAULT_RUN, Color.RED));

            assertThat(LabelHighlightRule.findUnhighlightedRuns(paragraph))
                .containsExactly(new UnhighlightedRun(CHINESE_DEFAULT_RUN, "after it: " + CHINESE_FULL_STOP));
        }

        @Test
        void anEmptyRunIsSkipped() {

            assertThat(LabelHighlightRule.findUnhighlightedRuns("Some text", List.of("", "text")))
                .isEmpty();
        }
    }
}
