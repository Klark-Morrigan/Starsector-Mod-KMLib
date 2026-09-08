package kmlib.profiling.snapshot;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileSection;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what a snapshot row derives rather than receives: self time is the total less what ran
 * inside it, a row whose children outlast it - a scope still open when the snapshot was taken -
 * reports no self time rather than a negative duration, and a row answers what it counted of a
 * given counter rather than handing its list over to be searched.
 */
final class ProfileNodeTest {

    private static final long PARENT_TOTAL_NANOS = 100L;
    private static final long CHILD_TOTAL_NANOS = 30L;

    private static final String SYSTEMS_COUNTER = "test.profileNode.systems";
    private static final String MARKETS_COUNTER = "test.profileNode.markets";
    private static final long SYSTEMS_TOTAL = 48L;

    @Nested
    class GetSelfNanos {

        @Test
        void reportsTheTotalLessEveryChildTotal() {

            var parent = new ProfileNode(
                ProfileSection.registerSection("test.profileNode.parent"),
                new ProfileTiming(
                    1,
                    PARENT_TOTAL_NANOS,
                    PARENT_TOTAL_NANOS,
                    PARENT_TOTAL_NANOS,
                    DurationBuckets.NO_CALLS),
                WorstCall.NO_CALL,
                BudgetBreach.NO_BREACH,
                ProfileIterations.NO_ITERATIONS,
                List.of(),
                List.of(
                    childNode("test.profileNode.firstChild", CHILD_TOTAL_NANOS),
                    childNode("test.profileNode.secondChild", CHILD_TOTAL_NANOS)));

            assertThat(parent.getSelfNanos())
                .isEqualTo(40L);
        }

        @Test
        void reportsNoSelfTimeWhereTheChildrenOutweighTheTotal() {
            // The shape a scope left open leaves behind: its children closed and it did not, so it
            // holds no span of its own to take theirs out of.
            var unclosed = new ProfileNode(
                ProfileSection.registerSection("test.profileNode.unclosed"),
                new ProfileTiming(0, 0, 0, 0, DurationBuckets.NO_CALLS),
                WorstCall.NO_CALL,
                BudgetBreach.NO_BREACH,
                ProfileIterations.NO_ITERATIONS,
                List.of(),
                List.of(childNode("test.profileNode.closedChild", CHILD_TOTAL_NANOS)));

            assertThat(unclosed.getSelfNanos())
                .isEqualTo(0L);
        }
    }

    @Nested
    class FindCount {

        @Test
        void answersWhatTheRowCountedOfTheCounterAsked() {
            // The row answers about its own contents, so a reader states a counter rather than
            // scanning the list it was handed for one.
            var counted = nodeCounting(countOf(SYSTEMS_COUNTER, SYSTEMS_TOTAL));

            assertThat(counted.findCount(ProfileCounter.registerCounter(SYSTEMS_COUNTER)))
                .isNotNull()
                .extracting(count -> count.getTotals().getTotal())
                .isEqualTo(SYSTEMS_TOTAL);
        }

        @Test
        void answersNothingForACounterTheRowNeverTouched() {
            // Absent rather than a zero: "counted none of it" and "does not count this" are
            // different facts, and a reader prints the second as a blank.
            var counted = nodeCounting(countOf(SYSTEMS_COUNTER, SYSTEMS_TOTAL));

            assertThat(counted.findCount(ProfileCounter.registerCounter(MARKETS_COUNTER)))
                .isNull();
        }
    }

    private static ProfileCount countOf(String counterName, long total) {
        return new ProfileCount(
            ProfileCounter.registerCounter(counterName),
            new CountTotals(total, total),
            new CountSpread(total, total));
    }

    private static ProfileNode nodeCounting(ProfileCount count) {
        return new ProfileNode(
            ProfileSection.registerSection("test.profileNode.counting"),
            new ProfileTiming(
                1,
                PARENT_TOTAL_NANOS,
                PARENT_TOTAL_NANOS,
                PARENT_TOTAL_NANOS,
                DurationBuckets.NO_CALLS),
            WorstCall.NO_CALL,
            BudgetBreach.NO_BREACH,
            ProfileIterations.NO_ITERATIONS,
            List.of(count),
            List.of());
    }

    private static ProfileNode childNode(String name, long totalNanos) {
        return new ProfileNode(
            ProfileSection.registerSection(name),
            new ProfileTiming(1, totalNanos, totalNanos, totalNanos, DurationBuckets.NO_CALLS),
            WorstCall.NO_CALL,
            BudgetBreach.NO_BREACH,
            ProfileIterations.NO_ITERATIONS,
            List.of(),
            List.of());
    }
}
