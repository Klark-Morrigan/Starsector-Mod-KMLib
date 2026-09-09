package kmlib.profiling.recording;

import kmlib.profiling.CallLogThreshold;
import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.SectionTerms;
import kmlib.profiling.snapshot.BudgetBreach;
import kmlib.profiling.snapshot.WorstCall;
import kmlib.testfixtures.logging.LogAppenderFake;

import org.apache.log4j.Level;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the three sentences a capture writes as it happens and the shape of each: a breach and an
 * out-of-order close said once per section until the capture is cleared, and a closed call's line
 * carrying the section, the span, every count and the tag in a form a log reader can grep.
 */
final class CaptureLogTest {

    private static final String SECTION_NAME = "test.captureLog.section";
    private static final String OTHER_SECTION_NAME = "test.captureLog.otherSection";
    private static final String LOGGED_SECTION_NAME = "test.captureLog.logged";
    private static final String QUIET_SECTION_NAME = "test.captureLog.quiet";
    private static final String COUNTER_NAME = "test.captureLog.cells";
    private static final String OTHER_COUNTER_NAME = "test.captureLog.labels";

    private static final String TAG = "rebuilt added=2";

    private static final long ONE_MILLISECOND_IN_NANOS = 1_000_000L;
    private static final long ALLOWED_NANOS = 4_000_000L;
    private static final long FIVE_MILLISECONDS_IN_NANOS = 5_000_000L;

    private static final long TWELVE = 12L;
    private static final long THREE = 3L;

    private static final ProfileSection SECTION = ProfileSection.registerSection(SECTION_NAME);
    private static final ProfileSection OTHER_SECTION =
        ProfileSection.registerSection(OTHER_SECTION_NAME);
    private static final ProfileSection LOGGED_SECTION = ProfileSection.registerSection(
        LOGGED_SECTION_NAME,
        SectionTerms.DEFAULT.withCallLogThreshold(CallLogThreshold.LOGGING_EVERY_CALL));
    private static final ProfileSection QUIET_SECTION =
        ProfileSection.registerSection(QUIET_SECTION_NAME);

    private final CaptureLog captureLog = new CaptureLog();

    @Nested
    class ReportBreachOnce {

        @Test
        void warnsOnceForOneSectionHoweverOftenItBreaches() {

            var appenderFake = LogAppenderFake.captureLogOf(CaptureLog.class, () -> {
                captureLog.reportBreachOnce(SECTION, durationBreach(), WorstCall.NO_TAG);
                captureLog.reportBreachOnce(SECTION, durationBreach(), WorstCall.NO_TAG);
            });

            assertThat(appenderFake.getMessages())
                .hasSize(1);
            assertThat(appenderFake.getEvents().get(0).getLevel())
                .isEqualTo(Level.WARN);
        }

        @Test
        void warnsForEachSectionThatBreaches() {
            // A different section is a different bug, and still gets said.
            var messages = captureMessagesWhile(() -> {
                captureLog.reportBreachOnce(SECTION, durationBreach(), WorstCall.NO_TAG);
                captureLog.reportBreachOnce(OTHER_SECTION, durationBreach(), WorstCall.NO_TAG);
            });

            assertThat(messages)
                .hasSize(2);
        }

        @Test
        void saysNothingForACallThatKeptItsBound() {

            assertThat(captureMessagesWhile(() ->
                    captureLog.reportBreachOnce(SECTION, BudgetBreach.NO_BREACH, WorstCall.NO_TAG)))
                .isEmpty();
        }

        @Test
        void namesTheSectionWhatItBrokeAndTheCall() {

            var messages = captureMessagesWhile(() ->
                captureLog.reportBreachOnce(SECTION, durationBreach(), TAG));

            assertThat(messages.get(0))
                .isEqualTo("Profiling section '" + SECTION_NAME + "' went over budget: "
                    + durationBreach().describeBreach() + ", on call \"" + TAG + "\""
                    + ". Said once; the row carries the latest breach of it.");
        }
    }

    @Nested
    class ReportOutOfOrderCloseOnce {

        @Test
        void warnsOnceForOneSectionHoweverOftenItIsLeftOpen() {

            var messages = captureMessagesWhile(() -> {
                captureLog.reportOutOfOrderCloseOnce(SECTION);
                captureLog.reportOutOfOrderCloseOnce(SECTION);
            });

            assertThat(messages)
                .hasSize(1);
            assertThat(messages.get(0))
                .contains(SECTION_NAME)
                .contains("was still open");
        }
    }

    @Nested
    class ReportClosedCall {

        @Test
        void writesTheSectionAndTheSpan() {

            var messages = captureMessagesWhile(() ->
                captureLog.reportClosedCall(
                    LOGGED_SECTION, FIVE_MILLISECONDS_IN_NANOS, List.of(), WorstCall.NO_TAG));

            assertThat(messages)
                .containsExactly("Profiled '" + LOGGED_SECTION_NAME + "' took=5.00ms");
        }

        @Test
        void writesEveryCountInTheOrderItWasCountedThenTheTagQuoted() {

            var counts = List.of(
                countOf(COUNTER_NAME, TWELVE),
                countOf(OTHER_COUNTER_NAME, THREE));

            var messages = captureMessagesWhile(() ->
                captureLog.reportClosedCall(LOGGED_SECTION, ONE_MILLISECOND_IN_NANOS, counts, TAG));

            assertThat(messages)
                .containsExactly("Profiled '" + LOGGED_SECTION_NAME + "' took=1.00ms "
                    + COUNTER_NAME + "=" + TWELVE + " " + OTHER_COUNTER_NAME + "=" + THREE
                    + " \"" + TAG + "\"");
        }

        @Test
        void writesAtDebug() {
            // A trace a reader turns on to follow a rebuild, not a finding: it must not surface at
            // the level the breaches do.
            var appenderFake = LogAppenderFake.captureLogOf(CaptureLog.class, () ->
                captureLog.reportClosedCall(
                    LOGGED_SECTION, ONE_MILLISECOND_IN_NANOS, List.of(), WorstCall.NO_TAG));

            assertThat(appenderFake.getEvents().get(0).getLevel())
                .isEqualTo(Level.DEBUG);
        }

        @Test
        void writesNothingForASectionUnderItsThreshold() {

            assertThat(captureMessagesWhile(() ->
                    captureLog.reportClosedCall(
                        QUIET_SECTION, FIVE_MILLISECONDS_IN_NANOS, List.of(), WorstCall.NO_TAG)))
                .isEmpty();
        }
    }

    @Nested
    class ForgetWhatWasSaid {

        @Test
        void letsABreachBeSaidAgainForTheNextCapture() {
            // "Said once" is once per capture, not once per session: a reader who cleared the
            // timings to watch one pass would otherwise be told nothing about the pass they
            // cleared for.
            var messages = captureMessagesWhile(() -> {
                captureLog.reportBreachOnce(SECTION, durationBreach(), WorstCall.NO_TAG);
                captureLog.forgetWhatWasSaid();
                captureLog.reportBreachOnce(SECTION, durationBreach(), WorstCall.NO_TAG);
            });

            assertThat(messages)
                .hasSize(2);
        }
    }

    private static List<String> captureMessagesWhile(Runnable work) {
        return LogAppenderFake.captureLogOf(CaptureLog.class, work).getMessages();
    }

    private static BudgetBreach durationBreach() {
        return BudgetBreach.reportDurationBreach(ALLOWED_NANOS);
    }

    // One counter's tally as a scope holds it at close, counted by the scope itself.
    private static ScopeCount countOf(String counterName, long amount) {

        var count = new ScopeCount(ProfileCounter.registerCounter(counterName));

        count.addSelfAmount(amount);
        return count;
    }
}
