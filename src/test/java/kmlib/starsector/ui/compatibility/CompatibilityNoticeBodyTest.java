package kmlib.starsector.ui.compatibility;

import kmlib.starsector.compatibility.CompatibilityFailure;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;
import kmlib.testfixtures.starsector.compatibility.CompatibilitySlotTemplates;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;
import kmlib.testfixtures.starsector.ui.widgets.TooltipMakerFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Covers what the on-screen notice is made of: which paragraphs it adds, how they are spaced, and
 * what colour each kind of emphasis is tinted.
 *
 * <p>The colour mapping is the part with no second reader. Which runs there are is the failure's
 * and is asserted where that is decided; what a kind is tinted is decided only here, so two kinds
 * collapsing to one colour would reach a player as a notice with the good news and the bad in one
 * shade and nothing else would catch it.
 *
 * <p>Colours are answered per key rather than left at a single default shade: with every key the
 * same colour, three kinds mapping to one would pass.
 */
final class CompatibilityNoticeBodyTest {

    private static final Object CONFIRM_BUTTON_ID = new Object();

    private static final String CONFIRM_TEXT = "Ok";

    @AfterEach
    void clearSettings() {

        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class FillNoticeBody {

        @Test
        void opensOnTheHeadingAndClosesOnThePointerAtTheLog() {

            var box = fillBodyFor(CompatibilityFailureFixture.createFailure());

            assertThat(box.readParagraphs())
                .extracting(TooltipMakerFake.AddedParagraph::text)
                .startsWith("title{failed|" + CompatibilityFailureFixture.MAP_OVERLAY_MOD_ID
                    + "|Fast Rendering}")
                .endsWith("seelog{starsector.log}");
        }

        @Test
        void addsOneParagraphForTheHeadingEveryRowAndTheClosingLine() {

            // No diagnosis in this shape, the build having stamped no target, so what is left is
            // the heading, the rows and the closing line.
            var failure = CompatibilityFailureFixture.createFailure();
            var box = fillBodyFor(failure);

            assertThat(box.readParagraphs())
                .hasSize(failure.describeRowsForPlayer().size() + 2);
        }

        @Test
        void addsTheDiagnosisBetweenTheHeadingAndTheRows() {

            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", null);
            var box = fillBodyFor(failure);

            // The unreadable-install shape, whose diagnosis is the lead and the two cases under it
            // - three paragraphs between the heading and the first row.
            assertThat(box.readParagraphs())
                .hasSize(failure.describeRowsForPlayer().size()
                    + failure.describeDiagnosisForPlayer().size()
                    + 2);
        }

        @Test
        void opensEachBlockOnAParagraphGapAndSpacesTheLinesUnderItAsAList() {

            var box = fillBodyFor(CompatibilityFailureFixture.createFailure());

            var gaps = box.readParagraphs()
                .stream()
                .map(TooltipMakerFake.AddedParagraph::gapAbove)
                .toList();

            // The heading sits flush at the top of the box; the first row opens a block, and every
            // row under it is spaced as a list within it.
            assertThat(gaps.get(0))
                .isZero();
            assertThat(gaps.get(1))
                .isGreaterThan(gaps.get(2));
            assertThat(gaps.get(2))
                .isEqualTo(gaps.get(3));
        }

        @Test
        void addsOneDismissButtonUnderTheNotice() {

            var box = fillBodyFor(CompatibilityFailureFixture.createFailure());

            assertThat(box.readButtons())
                .extracting(
                    TooltipMakerFake.AddedButton::label,
                    TooltipMakerFake.AddedButton::buttonId)
                .containsExactly(tuple(CONFIRM_TEXT, CONFIRM_BUTTON_ID));
        }

        @Test
        void handsOverAColourForEveryRunItNamesAsStandingOut() {

            var box = fillBodyFor(CompatibilityFailureFixture.createFailure());

            // The runs and their colours arrive as two arrays the engine reads in step, so a
            // paragraph carrying more of one than the other tints the wrong words.
            assertThat(box.readParagraphs())
                .allSatisfy(paragraph -> assertThat(paragraph.highlightColours())
                    .hasSameSizeAs(paragraph.highlightRuns()));
        }

        @Test
        void tintsWhatIsLostApartFromWhatStillWorksAndFromAPlainAnswer() {

            // The three kinds of emphasis, read off the three rows that carry one each: what broke
            // warns, what still works is set at ease, and a plain answer is brought forward. Two of
            // them sharing a colour is the failure this case exists for.
            var box = fillBodyFor(CompatibilityFailureFixture.createFailure());

            var lostColour = readRunColour(box, CompatibilityFailureFixture.LOST_FEATURE);
            var unaffectedColour = readRunColour(box, CompatibilityFailureFixture.UNAFFECTED_FEATURE);
            var plainColour = readRunColour(box, CompatibilityFailureFixture.FAILURE_SITE);

            assertThat(List.of(lostColour, unaffectedColour, plainColour))
                .doesNotHaveDuplicates();
        }

        // The colour the paragraph standing out this run tinted it.
        private Color readRunColour(TooltipMakerFake box, String runText) {

            return box.readParagraphs()
                .stream()
                .filter(paragraph -> paragraph.highlightRuns().contains(runText))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No paragraph stands out the run " + runText))
                .highlightColours()
                .get(0);
        }
    }

    // The body drawn into a recording element, over templates that expose their slots and a colour
    // of its own per key, so a case reads back what was added without a running game.
    private TooltipMakerFake fillBodyFor(CompatibilityFailure failure) {

        var colourPerKey = new HashMap<String, Color>();
        Map<String, String> templates = CompatibilitySlotTemplates.readSlotTemplates();

        StarsectorSettingsFake.buildSettings()
            .answerStrings((category, key) -> templates.get(key))
            .answerColours(key -> colourPerKey.computeIfAbsent(
                key,
                unused -> new Color(colourPerKey.size() + 1, 0, 0)))
            .installSettings();

        var box = TooltipMakerFake.createRecording();

        CompatibilityNoticeBody.fillNoticeBody(box.asElement(), failure, CONFIRM_BUTTON_ID, CONFIRM_TEXT);

        return box;
    }
}
