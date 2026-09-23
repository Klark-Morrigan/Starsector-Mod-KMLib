package kmlib.starsector.compatibility;

import kmlib.starsector.compatibility.CompatibilityNoticeLine.Emphasis;
import kmlib.starsector.compatibility.CompatibilityNoticeLine.EmphasisedRun;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Covers the rules a line's emphasis is assembled by, which are what a composition gets wrong
 * quietly: a run out of reading order, a run opening mid-word, or a value kept apart from wording
 * it is coloured the same as.
 *
 * <p>Written against templates of its own rather than any shipped wording. What is asserted is how
 * a slot and its value become runs, which holds whatever the notice happens to say.
 */
final class CompatibilityNoticeLinesTest {

    @Nested
    class BuildPhrase {

        @Test
        void splitsTheWordingEitherSideOfAValueMarkedDifferently() {

            // The case the whole split exists for: a name brought forward inside an instruction
            // that warns, rather than taking the instruction's colour.
            var phrase = CompatibilityNoticeLines.buildPhrase(
                "downgrade %s or wait",
                Emphasis.WARNING,
                CompatibilityNoticeLines.bringForward("Fast Rendering"));

            assertThat(phrase.lineText())
                .isEqualTo("downgrade Fast Rendering or wait");
            assertThat(phrase.emphasisedRuns())
                .extracting(EmphasisedRun::runText, EmphasisedRun::emphasis)
                .containsExactly(
                    tuple("downgrade", Emphasis.WARNING),
                    tuple("Fast Rendering", Emphasis.HIGHLIGHT),
                    tuple("or wait", Emphasis.WARNING));
        }

        @Test
        void foldsAValueMarkedLikeTheWordingIntoTheRunAroundIt() {

            // What reads as one colour arrives as one run. Kept apart, the join between two values
            // would be a run of its own - and a two-letter run is the one thing that might match
            // inside a longer word.
            var phrase = CompatibilityNoticeLines.buildPhrase(
                "update to %s or wait",
                Emphasis.WARNING,
                CompatibilityNoticeLines.warn("0.9.1"));

            assertThat(phrase.emphasisedRuns())
                .extracting(EmphasisedRun::runText, EmphasisedRun::emphasis)
                .containsExactly(tuple("update to 0.9.1 or wait", Emphasis.WARNING));
        }

        @Test
        void opensAndClosesEveryRunOnAWord() {

            // The engine checks the character before a match, so a run opening on a space would be
            // asked to start in the middle of the word before it and would tint nothing.
            var phrase = CompatibilityNoticeLines.buildPhrase(
                "downgrade %s to %s now",
                Emphasis.WARNING,
                CompatibilityNoticeLines.bringForward("Fast Rendering"),
                CompatibilityNoticeLines.bringForward("0.8.8"));

            assertThat(phrase.emphasisedRuns())
                .extracting(EmphasisedRun::runText)
                .allSatisfy(run -> assertThat(run).isEqualTo(run.trim()));
        }

        @Test
        void namesEveryRunInTheOrderItAppears() {

            // The engine matches each run from where the last one ended, so a list out of order
            // finds a later occurrence or nothing at all.
            var phrase = CompatibilityNoticeLines.buildPhrase(
                "downgrade %s to %s or wait for a %s update",
                Emphasis.WARNING,
                CompatibilityNoticeLines.bringForward("Fast Rendering"),
                CompatibilityNoticeLines.warn("0.8.8"),
                CompatibilityNoticeLines.bringForward("map-mod"));

            assertRunsAreFoundInOrder(phrase);
        }

        @Test
        void answersTheWordingAloneWhereTheTemplateCarriesNoSlot() {

            var phrase = CompatibilityNoticeLines.buildPhrase("nothing to fill", Emphasis.WARNING);

            assertThat(phrase.lineText())
                .isEqualTo("nothing to fill");
            assertThat(phrase.emphasisedRuns())
                .extracting(EmphasisedRun::runText)
                .containsExactly("nothing to fill");
        }
    }

    @Nested
    class BuildSplicedLine {

        @Test
        void putsThePhrasesRunsAfterTheLinesOwn() {

            // The phrase sits at the end of every template that takes one, so its runs follow. A
            // phrase spliced in ahead of them would be searched for before the words it follows
            // and would find nothing.
            var line = CompatibilityNoticeLines.buildSplicedLine(
                "Your Fast Rendering is behind and you can update to 0.9.1",
                List.of(CompatibilityNoticeLines.bringForward("Fast Rendering")),
                List.of(CompatibilityNoticeLines.warn("update to 0.9.1")));

            assertThat(line.emphasisedRuns())
                .extracting(EmphasisedRun::runText)
                .containsExactly("Fast Rendering", "update to 0.9.1");
            assertRunsAreFoundInOrder(line);
        }
    }

    @Nested
    class BuildLine {

        @Test
        void carriesTheWordingAndTheRunsGiven() {

            var line = CompatibilityNoticeLines.buildLine(
                "See starsector.log for more details.",
                CompatibilityNoticeLines.bringForward("starsector.log"));

            assertThat(line.lineText())
                .isEqualTo("See starsector.log for more details.");
            assertThat(line.emphasisedRuns())
                .extracting(EmphasisedRun::runText, EmphasisedRun::emphasis)
                .containsExactly(tuple("starsector.log", Emphasis.HIGHLIGHT));
        }

        @Test
        void carriesNoRunsWhereNoneStandOut() {

            var line = CompatibilityNoticeLines.buildLine("Nothing stands out here.");

            assertThat(line.emphasisedRuns())
                .isEmpty();
        }
    }

    // Walks a line's runs the way the engine does: each searched from where the last one ended.
    private void assertRunsAreFoundInOrder(CompatibilityNoticeLine line) {

        var searchedFrom = 0;

        for (var run : line.emphasisedRuns()) {
            var foundAt = line.lineText().indexOf(run.runText(), searchedFrom);

            assertThat(foundAt)
                .as("run '%s' in line '%s'", run.runText(), line.lineText())
                .isNotNegative();

            searchedFrom = foundAt + run.runText().length();
        }
    }
}
