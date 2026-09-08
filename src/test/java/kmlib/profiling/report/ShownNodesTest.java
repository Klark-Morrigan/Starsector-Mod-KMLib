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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link ShownNodes}: a request that narrows nothing keeps every row, a namespace keeps what
 * it names along with what that row is made of and the rows it ran inside while dropping the
 * siblings it does not name, a row count keeps the rows the reading ranks highest along with the
 * rows they ran inside without those parents eating into the count, and a reading with nothing to
 * say about a row drops it.
 */
final class ShownNodesTest {

    private static final String NAMESPACE = "politicalMap.";
    private static final String OTHER_NAMESPACE = "otherMod.";

    private static final String REBUILD_SECTION = NAMESPACE + "rebuild";
    private static final String WALK_SECTION = NAMESPACE + "walk";
    private static final String FOREIGN_SECTION = OTHER_NAMESPACE + "draw";

    // A beat neither namespace names, standing for the framework row a consumer's own rows sit
    // inside.
    private static final String FRAME_SECTION = "mapLayer.prepare";

    private static final String WALKS_COUNTER = "walks";

    // Three costs a ranking can tell apart, and the two rows a top of one and a top of two keep.
    private static final long SLOW_NANOS = 3_000_000;
    private static final long MIDDLING_NANOS = 2_000_000;
    private static final long QUICK_NANOS = 1_000_000;

    private static final int TOP_ONE_ROW = 1;
    private static final int TOP_TWO_ROWS = 2;

    private static final long TWO_WALKS = 2;

    @Nested
    class SelectShownNodes {

        @Test
        void keepsEveryRowWhereTheRequestNarrowedNothing() {

            var walk = nodeOf(WALK_SECTION, QUICK_NANOS);
            var rebuild = nodeOf(REBUILD_SECTION, SLOW_NANOS, walk);

            var shown = ShownNodes.selectShownNodes(
                List.of(rebuild), ProfileReportRequest.showTree());

            assertThat(shown)
                .containsExactlyInAnyOrder(rebuild, walk);
        }

        @Test
        void dropsASiblingTheNamespaceDoesNotName() {
            // The point of the filter: a consumer reading their own rows sees nothing of another
            // mod's, which is what makes a shared capture safe to hand over.
            var mine = nodeOf(REBUILD_SECTION, SLOW_NANOS);
            var theirs = nodeOf(FOREIGN_SECTION, SLOW_NANOS);

            var shown = ShownNodes.selectShownNodes(
                List.of(mine, theirs),
                ProfileReportRequest.showTree().limitToNamespace(NAMESPACE));

            assertThat(shown)
                .containsExactly(mine);
        }

        @Test
        void keepsTheRowsANamedRowRanInside() {
            // A finding with no parent above it is a number with nothing to be judged against: what
            // a layer cost is only readable against the frame beat it cost it in.
            var rebuild = nodeOf(REBUILD_SECTION, SLOW_NANOS);
            var frame = nodeOf(FRAME_SECTION, SLOW_NANOS, rebuild);

            var shown = ShownNodes.selectShownNodes(
                List.of(frame),
                ProfileReportRequest.showTree().limitToNamespace(NAMESPACE));

            assertThat(shown)
                .containsExactlyInAnyOrder(frame, rebuild);
        }

        @Test
        void keepsWhatANamedRowIsMadeOfWhateverItIsCalled() {
            // A kept row's cost is its children's, so a subtree under a named row is part of
            // reading it however the rows below happen to be named.
            var foreign = nodeOf(FOREIGN_SECTION, QUICK_NANOS);
            var rebuild = nodeOf(REBUILD_SECTION, SLOW_NANOS, foreign);

            var shown = ShownNodes.selectShownNodes(
                List.of(rebuild),
                ProfileReportRequest.showTree().limitToNamespace(NAMESPACE));

            assertThat(shown)
                .containsExactlyInAnyOrder(rebuild, foreign);
        }

        @Test
        void keepsTheRowsTheReadingRanksHighest() {
            // What "the worst two" means is the reading's, and for both the tree and the listing it
            // means the rows that spent the time themselves.
            var slow = nodeOf(REBUILD_SECTION, SLOW_NANOS);
            var middling = nodeOf(WALK_SECTION, MIDDLING_NANOS);
            var quick = nodeOf(FOREIGN_SECTION, QUICK_NANOS);

            var shown = ShownNodes.selectShownNodes(
                List.of(slow, middling, quick),
                ProfileReportRequest.showTree().limitToTopRows(TOP_TWO_ROWS));

            assertThat(shown)
                .containsExactlyInAnyOrder(slow, middling);
        }

        @Test
        void keepsTheRowsAKeptRowRanInsideWithoutCountingThemAgainstTheLimit() {
            // A limit of one asks for one finding, not for one line: the parent comes with it, or
            // the row arrives with nothing to be read against.
            var walk = nodeOf(WALK_SECTION, SLOW_NANOS);
            var frame = nodeOf(FRAME_SECTION, SLOW_NANOS, walk);

            var shown = ShownNodes.selectShownNodes(
                List.of(frame),
                ProfileReportRequest.showTree().limitToTopRows(TOP_ONE_ROW));

            assertThat(shown)
                .containsExactlyInAnyOrder(frame, walk);
        }

        @Test
        void dropsARowTheReadingHasNothingToSayAbout() {
            // A listing of what walked answers which pass went looking for the sector; a row that
            // never walked is not an answer to it at zero.
            var walked = countingNodeOf(REBUILD_SECTION, countOfWalks());
            var never = nodeOf(FOREIGN_SECTION, SLOW_NANOS);

            var shown = ShownNodes.selectShownNodes(
                List.of(walked, never),
                ProfileReportRequest.showRowsCounting(
                    ProfileCounter.registerCounter(WALKS_COUNTER)));

            assertThat(shown)
                .containsExactly(walked);
        }
    }

    private static ProfileNode nodeOf(String name, long totalNanos, ProfileNode... children) {

        return new ProfileNode(
            ProfileSection.registerSection(name),
            new ProfileTiming(1, totalNanos, totalNanos, totalNanos, DurationBuckets.NO_CALLS),
            WorstCall.NO_CALL,
            BudgetBreach.NO_BREACH,
            ProfileIterations.NO_ITERATIONS,
            List.of(),
            List.of(children));
    }

    private static ProfileNode countingNodeOf(String name, ProfileCount count) {

        return new ProfileNode(
            ProfileSection.registerSection(name),
            new ProfileTiming(1, SLOW_NANOS, SLOW_NANOS, SLOW_NANOS, DurationBuckets.NO_CALLS),
            WorstCall.NO_CALL,
            BudgetBreach.NO_BREACH,
            ProfileIterations.NO_ITERATIONS,
            List.of(count),
            List.of());
    }

    private static ProfileCount countOfWalks() {

        return new ProfileCount(
            ProfileCounter.registerCounter(WALKS_COUNTER),
            new CountTotals(TWO_WALKS, TWO_WALKS),
            new CountSpread(TWO_WALKS, TWO_WALKS));
    }
}
