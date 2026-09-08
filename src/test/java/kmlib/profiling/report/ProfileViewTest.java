package kmlib.profiling.report;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.snapshot.BudgetBreach;
import kmlib.profiling.snapshot.CountSpread;
import kmlib.profiling.snapshot.CountTotals;
import kmlib.profiling.snapshot.DurationBuckets;
import kmlib.profiling.snapshot.ProfileCount;
import kmlib.profiling.snapshot.ProfileIterations;
import kmlib.profiling.snapshot.ProfileNode;
import kmlib.profiling.snapshot.ProfileTiming;
import kmlib.profiling.snapshot.WorstCall;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how a reading ranks two rows: by what each spent itself, and by how much of a counter one
 * call of it reached - the row that reached most in a single call first, ties settled by which
 * reached most in all, and a row that never counted it ranking below every row that did.
 */
final class ProfileViewTest {

    private static final String SLOW_SECTION = "politicalMap.rebuild";
    private static final String QUICK_SECTION = "politicalMap.walk";
    private static final String SILENT_SECTION = "politicalMap.draw";

    private static final String WALKS_COUNTER = "walks";

    private static final long SLOW_NANOS = 3_000_000;
    private static final long QUICK_NANOS = 1_000_000;

    // One row reaches three walks in a call, the other one - and the row that ties on that reaches
    // more of them in all, which is what the tie is settled by.
    private static final long THREE_WALKS_PER_CALL = 3;
    private static final long ONE_WALK_PER_CALL = 1;
    private static final long MANY_WALKS_IN_ALL = 90;
    private static final long FEW_WALKS_IN_ALL = 9;

    @Nested
    class OrderBySelfTimeDescending {

        @Test
        void putsTheRowThatSpentTheMostItselfFirst() {
            // What a reader hunting for time to save opens next is the row that spent it, not the
            // row that was waiting on it.
            var quick = nodeCosting(QUICK_SECTION, QUICK_NANOS);
            var slow = nodeCosting(SLOW_SECTION, SLOW_NANOS);

            var ranked = sortedBy(ProfileView.orderBySelfTimeDescending(), quick, slow);

            assertThat(ranked)
                .containsExactly(slow, quick);
        }
    }

    @Nested
    class OrderByCountPerCallDescending {

        @Test
        void putsTheRowThatReachedMostInOneCallFirst() {
            // A bound is broken by one call, so the row that walked the sector three times in a
            // single call is the one to look at - not the row that walked it once on each of a
            // thousand frames.
            var occasional = nodeWalking(QUICK_SECTION, ONE_WALK_PER_CALL, MANY_WALKS_IN_ALL);
            var repeated = nodeWalking(SLOW_SECTION, THREE_WALKS_PER_CALL, FEW_WALKS_IN_ALL);

            var ranked = sortedBy(walksPerCall(), occasional, repeated);

            assertThat(ranked)
                .containsExactly(repeated, occasional);
        }

        @Test
        void settlesATieOnOneCallByWhichReachedMostInAll() {
            // Two rows that each walked once per call are told apart by how often they ran, which
            // is the next thing worth knowing about them.
            var seldom = nodeWalking(QUICK_SECTION, ONE_WALK_PER_CALL, FEW_WALKS_IN_ALL);
            var often = nodeWalking(SLOW_SECTION, ONE_WALK_PER_CALL, MANY_WALKS_IN_ALL);

            var ranked = sortedBy(walksPerCall(), seldom, often);

            assertThat(ranked)
                .containsExactly(often, seldom);
        }

        @Test
        void ranksTwoRowsThatNeverCountedItAlike() {
            // Neither has anything to say about the counter, so neither outranks the other and the
            // order they arrived in stands - a ranking that fell over on a row with no count would
            // decide which readings may use it.
            var first = nodeCosting(QUICK_SECTION, QUICK_NANOS);
            var second = nodeCosting(SILENT_SECTION, SLOW_NANOS);

            var ranked = sortedBy(walksPerCall(), first, second);

            assertThat(ranked)
                .containsExactly(first, second);
        }

        @Test
        void ranksARowThatNeverCountedItBelowEveryRowThatDid() {
            // A row with no count of it ranks as nothing rather than as an error: a reading that
            // cares whether it counted has already dropped it, and one that does not still has to
            // put it somewhere.
            var silent = nodeCosting(SILENT_SECTION, SLOW_NANOS);
            var walked = nodeWalking(SLOW_SECTION, ONE_WALK_PER_CALL, FEW_WALKS_IN_ALL);

            var ranked = sortedBy(walksPerCall(), silent, walked);

            assertThat(ranked)
                .containsExactly(walked, silent);
        }
    }

    private static Comparator<ProfileNode> walksPerCall() {

        return ProfileView.orderByCountPerCallDescending(
            ProfileCounter.registerCounter(WALKS_COUNTER));
    }

    private static List<ProfileNode> sortedBy(
            Comparator<ProfileNode> ranking,
            ProfileNode... nodes) {

        var ranked = new ArrayList<>(List.of(nodes));

        ranked.sort(ranking);
        return ranked;
    }

    private static ProfileNode nodeCosting(String name, long selfNanos) {

        return nodeOf(name, List.of(), selfNanos);
    }

    private static ProfileNode nodeWalking(String name, long walksPerCall, long walksInAll) {

        return nodeOf(
            name,
            List.of(new ProfileCount(
                ProfileCounter.registerCounter(WALKS_COUNTER),
                new CountTotals(walksInAll, walksInAll),
                new CountSpread(walksPerCall, walksPerCall))),
            SLOW_NANOS);
    }

    private static ProfileNode nodeOf(String name, List<ProfileCount> counts, long totalNanos) {

        return new ProfileNode(
            ProfileSection.registerSection(name),
            new ProfileTiming(1, totalNanos, totalNanos, totalNanos, DurationBuckets.NO_CALLS),
            WorstCall.NO_CALL,
            BudgetBreach.NO_BREACH,
            ProfileIterations.NO_ITERATIONS,
            counts,
            List.of());
    }
}
