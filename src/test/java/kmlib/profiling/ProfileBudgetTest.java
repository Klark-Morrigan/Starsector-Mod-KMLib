package kmlib.profiling;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what each bound judges one call by: a counted amount against a fixed allowance, a duration
 * against one the caller states as the call closes, a bound of nothing standing for no bound at
 * all, and two bounds over one section reporting whichever was broken first.
 */
final class ProfileBudgetTest {

    private static final String WALKS_COUNTER = "test.profileBudget.walks";
    private static final String MARKETS_COUNTER = "test.profileBudget.markets";

    private static final long ONE_WALK = 1L;
    private static final long THREE_WALKS = 3L;

    private static final long ONE_MILLISECOND_IN_NANOS = 1_000_000L;
    private static final long FOUR_MILLISECONDS_IN_NANOS = 4_000_000L;
    private static final long TEN_MILLISECONDS_IN_NANOS = 10_000_000L;

    // What a duration bound reads as, so a case can state the text a reader sees rather than the
    // nanoseconds behind it.
    private static final String FOUR_MILLISECONDS_ALLOWED = "4.00ms allowed per call";

    // A call that counted nothing at all, which is what every counter of a section with a duration
    // bound is asked for.
    private static final long NOTHING_COUNTED = 0L;

    @Nested
    class AllowingCountPerCall {

        @Test
        void reportsACallThatReachedMoreThanItWasAllowed() {
            // The finding a table of durations cannot produce: a pass that walked the sector three
            // times says so in the terms the reader acts on.
            var budget = ProfileBudget.allowingCountPerCall(
                ProfileCounter.registerCounter(WALKS_COUNTER), ONE_WALK);

            var breach = budget.findBreachInCall(ONE_MILLISECOND_IN_NANOS, counter -> THREE_WALKS);

            assertThat(breach.hasBreached())
                .isTrue();
            assertThat(breach.describeBreach())
                .isEqualTo("3 " + WALKS_COUNTER + ", 1 allowed per call");
        }

        @Test
        void reportsNothingForACallThatReachedExactlyItsAllowance() {
            // The bound is what a call may reach, not what it must stay under: one walk per rebuild
            // is the rule, and a rebuild that walked once kept it.
            var budget = ProfileBudget.allowingCountPerCall(
                ProfileCounter.registerCounter(WALKS_COUNTER), ONE_WALK);

            assertThat(budget.findBreachInCall(ONE_MILLISECOND_IN_NANOS, counter -> ONE_WALK))
                .isSameAs(BudgetBreach.NO_BREACH);
        }

        @Test
        void judgesOnlyTheCounterItWasStatedAgainst() {
            // A section may count several things and be bounded on one of them, so the amount asked
            // for is the bounded counter's rather than whatever the call happened to reach most of.
            var walks = ProfileCounter.registerCounter(WALKS_COUNTER);
            var budget = ProfileBudget.allowingCountPerCall(walks, ONE_WALK);

            var breach = budget.findBreachInCall(
                ONE_MILLISECOND_IN_NANOS,
                counter -> counter == walks ? ONE_WALK : THREE_WALKS);

            assertThat(breach)
                .isSameAs(BudgetBreach.NO_BREACH);
        }
    }

    @Nested
    class AllowingDurationPerCall {

        @Test
        void reportsACallThatRanLongerThanItWasAllowed() {

            var budget = ProfileBudget.allowingDurationPerCall(() -> FOUR_MILLISECONDS_IN_NANOS);

            var breach = budget.findBreachInCall(
                TEN_MILLISECONDS_IN_NANOS, counter -> NOTHING_COUNTED);

            assertThat(breach.hasBreached())
                .isTrue();
            // Only the allowance: how long the call took is the row's own maximum column, and a
            // third spelling of it beside them would read as another number.
            assertThat(breach.describeBreach())
                .isEqualTo(FOUR_MILLISECONDS_ALLOWED);
        }

        @Test
        void reportsNothingForACallInsideItsAllowance() {

            var budget = ProfileBudget.allowingDurationPerCall(() -> FOUR_MILLISECONDS_IN_NANOS);

            assertThat(budget.findBreachInCall(ONE_MILLISECOND_IN_NANOS, counter -> NOTHING_COUNTED))
                .isSameAs(BudgetBreach.NO_BREACH);
        }

        @Test
        void reportsNothingWhereNoBoundIsStated() {
            // Zero is the off position of the knob behind a bound, so a player who wants the
            // numbers without the findings turns one control down rather than hunting a second.
            var budget = ProfileBudget.allowingDurationPerCall(() -> 0L);

            assertThat(budget.findBreachInCall(
                TEN_MILLISECONDS_IN_NANOS, counter -> NOTHING_COUNTED))
                .isSameAs(BudgetBreach.NO_BREACH);
        }

        @Test
        void asksForTheAllowanceOnEveryCallItJudges() {
            // The bound is a knob a player moves mid-session, so a call is held to what the knob
            // says as it closes rather than to what it said when the section was registered.
            var statedNanos = new AtomicLong(TEN_MILLISECONDS_IN_NANOS);
            var budget = ProfileBudget.allowingDurationPerCall(statedNanos::get);

            var beforeTheKnobMoved = budget.findBreachInCall(
                FOUR_MILLISECONDS_IN_NANOS, counter -> NOTHING_COUNTED);

            statedNanos.set(ONE_MILLISECOND_IN_NANOS);

            var afterTheKnobMoved = budget.findBreachInCall(
                FOUR_MILLISECONDS_IN_NANOS, counter -> NOTHING_COUNTED);

            assertThat(beforeTheKnobMoved.hasBreached())
                .isFalse();
            assertThat(afterTheKnobMoved.hasBreached())
                .isTrue();
        }
    }

    @Nested
    class CombineWith {

        @Test
        void reportsTheFirstBoundTheCallBroke() {
            // One call, one fault: a rebuild that walked twice and took too long doing it is
            // reported as the walk, which is the rule its section was written to hold.
            var budget = ProfileBudget
                .allowingCountPerCall(ProfileCounter.registerCounter(WALKS_COUNTER), ONE_WALK)
                .combineWith(ProfileBudget.allowingDurationPerCall(
                    () -> FOUR_MILLISECONDS_IN_NANOS));

            var breach = budget.findBreachInCall(TEN_MILLISECONDS_IN_NANOS, counter -> THREE_WALKS);

            assertThat(breach.describeBreach())
                .isEqualTo("3 " + WALKS_COUNTER + ", 1 allowed per call");
        }

        @Test
        void reportsTheSecondBoundWhereTheFirstHeld() {

            var budget = ProfileBudget
                .allowingCountPerCall(ProfileCounter.registerCounter(MARKETS_COUNTER), THREE_WALKS)
                .combineWith(ProfileBudget.allowingDurationPerCall(
                    () -> FOUR_MILLISECONDS_IN_NANOS));

            var breach = budget.findBreachInCall(TEN_MILLISECONDS_IN_NANOS, counter -> ONE_WALK);

            assertThat(breach.describeBreach())
                .isEqualTo(FOUR_MILLISECONDS_ALLOWED);
        }

        @Test
        void reportsNothingWhereBothBoundsHeld() {

            var budget = ProfileBudget
                .allowingCountPerCall(ProfileCounter.registerCounter(WALKS_COUNTER), ONE_WALK)
                .combineWith(ProfileBudget.allowingDurationPerCall(
                    () -> FOUR_MILLISECONDS_IN_NANOS));

            assertThat(budget.findBreachInCall(ONE_MILLISECOND_IN_NANOS, counter -> ONE_WALK))
                .isSameAs(BudgetBreach.NO_BREACH);
        }
    }

    @Nested
    class NoBudget {

        @Test
        void reportsNothingWhateverACallDid() {
            // What every section that stated no bound carries, so the check is one comparison on a
            // path that runs every frame.
            assertThat(ProfileBudget.NO_BUDGET.findBreachInCall(
                TEN_MILLISECONDS_IN_NANOS, counter -> THREE_WALKS))
                .isSameAs(BudgetBreach.NO_BREACH);
        }
    }
}
