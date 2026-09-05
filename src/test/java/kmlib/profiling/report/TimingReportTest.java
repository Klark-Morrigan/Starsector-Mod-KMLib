package kmlib.profiling.report;

import kmlib.profiling.PhasedSection;
import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.snapshot.CallCount;
import kmlib.profiling.snapshot.CountSpread;
import kmlib.profiling.snapshot.CountTotals;
import kmlib.profiling.snapshot.DurationBuckets;
import kmlib.profiling.snapshot.PhaseTotal;
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
 * Pins {@link TimingReport}: empty input yields a notice, a section renders a row with its name,
 * count, and nanos converted to milliseconds, a section that ran inside another is printed
 * indented under it with its own self time, every counter the tree touched heads a group of
 * columns that stays blank on the rows which never touched it, the duration bands are marked by
 * how many calls landed in each, a row whose slowest call has more to say than its duration carries
 * a second line saying it, a row whose calls ran a loop carries a line stating what one turn of it
 * cost, and neither of those lines disturbs the columns.
 */
final class TimingReportTest {

    private static final String PARENT_SECTION = "politicalMap.rebuild";
    private static final String CHILD_SECTION = "politicalMap.walk";

    private static final String SHORT_PARENT_SECTION = "rebuild";
    private static final String LONGER_CHILD_SECTION = "walkTheWholeSector";

    private static final String SYSTEMS_COUNTER = "systems";
    private static final String MARKETS_COUNTER = "markets";

    // 3.000ms holding 2.000ms, so the parent's self time is a third number again.
    private static final long PARENT_TOTAL_NANOS = 3_000_000;
    private static final long CHILD_TOTAL_NANOS = 2_000_000;

    // "walkTheWholeSector" plus the two spaces one level of nesting indents it by.
    private static final int WIDEST_NAME_WIDTH = 20;

    // The six numeric columns and the spread beside them: two spaces then 8, 10, 10, 10, 10, 11,
    // and 22 for the one character each duration band takes.
    private static final int NUMERIC_COLUMNS_WIDTH = 95;

    // The section name, the six timing columns, and the four of the one counter group that row
    // filled - the second group in the table stays blank on it.
    private static final int FILLED_CELLS_PER_ROW_WITH_ONE_COUNTER = 11;

    // The same row less its cost-each cell, which a row that counted nothing itself cannot state.
    private static final int FILLED_CELLS_PER_ROW_WITH_NO_PER_ITEM_COST = 10;

    // The header and the one section under it, which is all a row with nothing to add about its
    // slowest call may print.
    private static final int ROWS_OF_A_HEADER_AND_ONE_SECTION = 2;

    // A thousand calls in the band a microsecond opens, and three a thousand times slower in the
    // band around a millisecond - two rows a mean of 4us cannot tell apart.
    private static final int QUICK_CALL_BAND = 1;
    private static final int SLOW_CALL_BAND = 11;
    private static final long THOUSAND_CALLS = 1_000L;
    private static final long FEW_CALLS = 3L;

    // Those two bands marked by how many digits each tally has - 4 for the thousand, 1 for the
    // three - with a dot in each of the twenty bands nothing landed in.
    private static final String EXPECTED_BAND_MARKS = ".4.........1..........";

    // Eleven digits' worth of calls in the quick band, which the mark caps at the widest digit.
    private static final long UNCOUNTABLY_MANY_CALLS = 10_000_000_000L;
    private static final String EXPECTED_CAPPED_BAND_MARKS = ".9.........1..........";

    // A loop of two turns whose one step took 200us in all, which is 100us a turn - a figure
    // milliseconds would round to nothing and the row's own columns could never state.
    private static final String PLAN_PHASE_NAME = "plan";
    private static final long LOOP_TURNS = 2;
    private static final long PHASE_TOTAL_NANOS = 200_000;
    private static final String SLOWEST_TURN_TAG = "corvus";

    private static final String WORST_CALL_TAG = "eos";
    private static final long WORST_CALL_SYSTEMS = 48L;

    // Longer than the section column and every number beside it, so a table that measured the line
    // would be visibly pulled out of shape by it.
    private static final String LONG_WORST_CALL_TAG =
        "a tag longer than the whole row it was written under";

    @Nested
    class Format {

        @Test
        void formatReturnsANoticeWhenNothingWasRecorded() {

            assertThat(TimingReport.format(List.of()))
                .isEqualTo("No timings recorded.");
        }

        @Test
        void formatRendersSectionNameCountAndMillisecondColumns() {
            // 2 calls, total 3_000_000ns = 3.000ms, avg 1.500ms.
            var node = new ProfileNode(
                ProfileSection.registerSection("render"),
                new ProfileTiming(2, 3_000_000, 1_000_000, 2_000_000, DurationBuckets.NO_CALLS),
                WorstCall.NO_CALL,
                ProfileIterations.NO_ITERATIONS,
                List.of(),
                List.of());
            var report = TimingReport.format(List.of(node));

            assertThat(report)
                .contains("SECTION", "COUNT", "AVG ms", "MIN ms", "MAX ms", "SELF ms", "TOTAL ms");
            assertThat(report)
                .contains("render");

            // count and the millisecond conversions appear in the row.
            assertThat(report)
                .contains("2", "1.500", "1.000", "2.000", "3.000");
        }

        @Test
        void formatIndentsASectionUnderTheOneItRanInside() {
            // The whole point of the tree in text: a reader follows a row to what it is made of by
            // reading down and to the right, rather than by matching spelled-alike prefixes.
            var report = TimingReport.format(
                List.of(parentHoldingOneChild(PARENT_SECTION, CHILD_SECTION)));

            assertThat(report)
                .contains("\n" + PARENT_SECTION);
            assertThat(report)
                .contains("\n  " + CHILD_SECTION);
        }

        @Test
        void formatSeparatesSelfTimeFromTheInclusiveTotal() {
            // A parent of 3.000ms holding a 2.000ms walk spent 1.000ms itself, which is what says
            // whether to look at the row or below it.
            var report = TimingReport.format(
                List.of(parentHoldingOneChild(PARENT_SECTION, CHILD_SECTION)));

            assertThat(report)
                .contains("1.000", "3.000");
        }

        @Test
        void sizesTheNameColumnToTheWidestRowIndentIncluded() {
            // The name column is sized from the deepest row, not the shallowest: a child indented
            // past its parent's width would otherwise push its own numbers out of the columns the
            // header names, and a table whose rows disagree on where a column starts is unreadable.
            var report = TimingReport.format(
                List.of(parentHoldingOneChild(SHORT_PARENT_SECTION, LONGER_CHILD_SECTION)));

            assertThat(List.of(report.split("\n")))
                .extracting(String::length)
                .containsOnly(WIDEST_NAME_WIDTH + NUMERIC_COLUMNS_WIDTH);
        }

        @Test
        void formatHeadsAColumnGroupForEveryCounterTheTreeTouched() {
            // What a duration is read against: the row states how much it counted in all and how
            // far one call's worth spread, beside the milliseconds it took to do it.
            var report = TimingReport.format(List.of(nodeCounting(
                PARENT_SECTION, countOf(SYSTEMS_COUNTER, 300, 300, 100, 200))));

            assertThat(report)
                .contains("SYSTEMS", "SYSTEMS MIN", "SYSTEMS MAX", "SYSTEMS us/ea");
            assertThat(readFilledCells(report, PARENT_SECTION))
                .contains("300", "100", "200");
        }

        @Test
        void formatDividesSelfTimeByTheItemsTheRowCountedItself() {
            // 3.000ms of self time over 300 systems is 10 microseconds each - the number an
            // optimisation is judged against, and one milliseconds would round away.
            var report = TimingReport.format(List.of(nodeCounting(
                PARENT_SECTION, countOf(SYSTEMS_COUNTER, 300, 300, 300, 300))));

            assertThat(readFilledCells(report, PARENT_SECTION))
                .contains("10.0");
        }

        @Test
        void formatLeavesACounterBlankOnARowThatNeverTouchedIt() {
            // A zero would say the row counted none of that thing, which is a fact worth seeing.
            // A row that does not count it at all has nothing to say, and a wide tree where every
            // row answers every counter is a table of zeroes hiding the rows that count.
            var report = TimingReport.format(List.of(
                nodeCounting(PARENT_SECTION, countOf(SYSTEMS_COUNTER, 300, 300, 300, 300)),
                nodeCounting(CHILD_SECTION, countOf(MARKETS_COUNTER, 40, 40, 40, 40))));

            assertThat(readFilledCells(report, PARENT_SECTION))
                .hasSize(FILLED_CELLS_PER_ROW_WITH_ONE_COUNTER);
            assertThat(readFilledCells(report, CHILD_SECTION))
                .hasSize(FILLED_CELLS_PER_ROW_WITH_ONE_COUNTER);
        }

        @Test
        void formatPricesNoItemForARowWhoseChildrenDidAllTheCounting() {
            // The row still states the 7 counted beneath it, because that is what it is answerable
            // for. It states no cost each, because its self time bought none of those 7 - pricing
            // one against the other would charge this row for work a row below it did.
            var report = TimingReport.format(List.of(nodeCounting(
                PARENT_SECTION, countOf(SYSTEMS_COUNTER, 7, 0, 7, 7))));

            assertThat(readFilledCells(report, PARENT_SECTION))
                .hasSize(FILLED_CELLS_PER_ROW_WITH_NO_PER_ITEM_COST)
                .contains("7");
        }

        @Test
        void formatMarksEachDurationBandWithHowManyDigitsItsTallyHas() {
            // The shape a mean and a maximum cannot show: the band holding the thousand quick calls
            // reads taller than the one holding the single stall, so a row that stalled once is
            // told apart from a row that is always this slow.
            var report = TimingReport.format(List.of(nodeSpreadOver(
                bandsHolding(THOUSAND_CALLS, FEW_CALLS))));

            assertThat(readFilledCells(report, PARENT_SECTION))
                .contains(EXPECTED_BAND_MARKS);
        }

        @Test
        void formatShowsNoBandsForARowNoCallHasFinishedOn() {
            // A line of dots would read as calls that were all too fast to matter, which is the
            // opposite of what a row caught mid-call holds.
            var report = TimingReport.format(List.of(nodeCounting(
                PARENT_SECTION, countOf(SYSTEMS_COUNTER, 300, 300, 100, 200))));

            // Two dots in a row can only be empty bands: a millisecond figure carries one.
            assertThat(report)
                .doesNotContain("..");
        }

        @Test
        void formatWritesWhatTheSlowestCallWasDoingUnderTheRow() {
            // The one fact that says what to optimise: the maximum column carries the duration, and
            // the tag and the counters it was reached over have nowhere else to go.
            var report = TimingReport.format(List.of(nodeWhoseWorstCall(new WorstCall(
                PARENT_TOTAL_NANOS,
                WORST_CALL_TAG,
                List.of(new CallCount(
                    ProfileCounter.registerCounter(SYSTEMS_COUNTER), WORST_CALL_SYSTEMS))))));

            assertThat(report)
                .contains("\n  worst 3.000ms")
                .contains("\"" + WORST_CALL_TAG + "\"")
                .contains(SYSTEMS_COUNTER + "=" + WORST_CALL_SYSTEMS);
        }

        @Test
        void formatWritesTheSlowestCallsCountersWhereItWasNamedNothing() {
            // A call is worth a line for what it counted alone: most of the paths that count are
            // walkers, which have a tally to report and no name to give it.
            var report = TimingReport.format(List.of(nodeWhoseWorstCall(new WorstCall(
                PARENT_TOTAL_NANOS,
                WorstCall.NO_TAG,
                List.of(new CallCount(
                    ProfileCounter.registerCounter(SYSTEMS_COUNTER), WORST_CALL_SYSTEMS))))));

            assertThat(report)
                .contains("\n  worst 3.000ms")
                .contains(SYSTEMS_COUNTER + "=" + WORST_CALL_SYSTEMS)
                .doesNotContain("\"");
        }

        @Test
        void formatMarksABandHoldingMoreCallsThanADigitCanCountAtItsWidest() {
            // The mark is a digit, so a tally past nine digits has to stop widening it: a column
            // that grew with the capture could not be read against the capture before it.
            var report = TimingReport.format(List.of(nodeSpreadOver(
                bandsHolding(UNCOUNTABLY_MANY_CALLS, FEW_CALLS))));

            assertThat(readFilledCells(report, PARENT_SECTION))
                .contains(EXPECTED_CAPPED_BAND_MARKS);
        }

        @Test
        void formatWritesNoWorstCallLineWhereItRepeatsTheMaximumColumn() {
            // A call named nothing and counting nothing has only its duration to state, and the
            // table has already stated it - so the rows whose calls are all alike stay one line.
            var report = TimingReport.format(List.of(nodeWhoseWorstCall(
                new WorstCall(PARENT_TOTAL_NANOS, WorstCall.NO_TAG, List.of()))));

            assertThat(report.lines())
                .hasSize(ROWS_OF_A_HEADER_AND_ONE_SECTION);
        }

        @Test
        void formatWritesWhatOneTurnOfTheLoopCostInEachStep() {
            // What the row's own columns cannot say: they are per call, and a bake's cost scales
            // with the cells it turned over rather than with how often it ran.
            var report = TimingReport.format(List.of(nodeIterating(
                LOOP_TURNS, PHASE_TOTAL_NANOS, SLOWEST_TURN_TAG)));

            assertThat(report)
                .contains("\n  iterations=2")
                .contains("plan=100.0us/ea")
                .contains("slowest=3.000ms")
                .contains("\"" + SLOWEST_TURN_TAG + "\"");
        }

        @Test
        void formatWritesNoLoopLineForARowThatRanNone() {
            // Most rows, which would otherwise each carry a line saying they iterated over
            // nothing.
            var report = TimingReport.format(List.of(nodeWhoseWorstCall(WorstCall.NO_CALL)));

            assertThat(report.lines())
                .hasSize(ROWS_OF_A_HEADER_AND_ONE_SECTION);
        }

        @Test
        void formatKeepsTheColumnsAlignedAroundALoopLine() {
            // The loop line belongs to no column for the same reason the worst call's does: its
            // numbers are per turn, and its tag is a caller's own text of a caller's own length.
            var report = TimingReport.format(List.of(nodeIterating(
                LOOP_TURNS, PHASE_TOTAL_NANOS, LONG_WORST_CALL_TAG)));

            assertThat(readRow(report, PARENT_SECTION).length())
                .isEqualTo(PARENT_SECTION.length() + NUMERIC_COLUMNS_WIDTH);
        }

        @Test
        void formatKeepsTheColumnsAlignedAroundAWorstCallLine() {
            // The line belongs to no column and carries a caller's own text, so measuring it would
            // widen the section column by however long that text was and push every number away
            // from the header it sits under.
            var report = TimingReport.format(List.of(nodeWhoseWorstCall(new WorstCall(
                PARENT_TOTAL_NANOS, LONG_WORST_CALL_TAG, List.of()))));

            assertThat(readRow(report, PARENT_SECTION).length())
                .isEqualTo(PARENT_SECTION.length() + NUMERIC_COLUMNS_WIDTH);
        }
    }

    // One call of a parent holding one call of a child, which is the smallest tree that has a
    // parent, an indent and a self time to report.
    private static ProfileNode parentHoldingOneChild(String parentName, String childName) {
        return new ProfileNode(
            ProfileSection.registerSection(parentName),
            oneCallOf(PARENT_TOTAL_NANOS),
            WorstCall.NO_CALL,
            ProfileIterations.NO_ITERATIONS,
            List.of(),
            List.of(new ProfileNode(
                ProfileSection.registerSection(childName),
                oneCallOf(CHILD_TOTAL_NANOS),
                WorstCall.NO_CALL,
                ProfileIterations.NO_ITERATIONS,
                List.of(),
                List.of())));
    }

    // One 3.000ms call that counted something, which is the smallest tree with a counter group to
    // render and a per-item cost to divide out.
    private static ProfileNode nodeCounting(String sectionName, ProfileCount count) {
        return new ProfileNode(
            ProfileSection.registerSection(sectionName),
            oneCallOf(PARENT_TOTAL_NANOS),
            WorstCall.NO_CALL,
            ProfileIterations.NO_ITERATIONS,
            List.of(count),
            List.of());
    }

    private static ProfileTiming oneCallOf(long totalNanos) {
        return new ProfileTiming(1, totalNanos, totalNanos, totalNanos, DurationBuckets.NO_CALLS);
    }

    // Calls in two bands a thousandfold apart, which is the spread a mean and a maximum read as one
    // slow row.
    private static DurationBuckets bandsHolding(long quickCalls, long slowCalls) {

        var callsPerBucket = new long[DurationBuckets.countBuckets()];

        callsPerBucket[QUICK_CALL_BAND] = quickCalls;
        callsPerBucket[SLOW_CALL_BAND] = slowCalls;
        return new DurationBuckets(callsPerBucket);
    }

    private static ProfileNode nodeSpreadOver(DurationBuckets buckets) {
        return new ProfileNode(
            ProfileSection.registerSection(PARENT_SECTION),
            new ProfileTiming(
                1, PARENT_TOTAL_NANOS, PARENT_TOTAL_NANOS, PARENT_TOTAL_NANOS, buckets),
            WorstCall.NO_CALL,
            ProfileIterations.NO_ITERATIONS,
            List.of(),
            List.of());
    }

    // A row whose one call ran a loop of one step, which is the smallest tree with a per-turn cost
    // to divide out and a slowest turn to name.
    private static ProfileNode nodeIterating(long turns, long phaseNanos, String slowestTag) {

        var bakeSection = PhasedSection.registerPhasedSection(PARENT_SECTION, PLAN_PHASE_NAME);

        return new ProfileNode(
            ProfileSection.registerSection(PARENT_SECTION),
            oneCallOf(PARENT_TOTAL_NANOS),
            WorstCall.NO_CALL,
            new ProfileIterations(
                turns,
                List.of(new PhaseTotal(bakeSection.resolvePhase(PLAN_PHASE_NAME), phaseNanos)),
                PARENT_TOTAL_NANOS,
                slowestTag),
            List.of(),
            List.of());
    }

    private static ProfileNode nodeWhoseWorstCall(WorstCall worstCall) {
        return new ProfileNode(
            ProfileSection.registerSection(PARENT_SECTION),
            oneCallOf(PARENT_TOTAL_NANOS),
            worstCall,
            ProfileIterations.NO_ITERATIONS,
            List.of(),
            List.of());
    }

    private static ProfileCount countOf(
            String counterName,
            long total,
            long selfTotal,
            long minPerCall,
            long maxPerCall) {

        return new ProfileCount(
            ProfileCounter.registerCounter(counterName),
            new CountTotals(total, selfTotal),
            new CountSpread(minPerCall, maxPerCall));
    }

    // The cells a row actually fills, blank ones excluded - a blank cell is whitespace and so
    // splits away with the padding, which is what "prints nothing" has to mean in a fixed table.
    private static List<String> readFilledCells(String report, String sectionName) {
        return List.of(readRow(report, sectionName).trim().split("\\s+"));
    }

    // The row a section is reported on, padding and all, for the cases about how wide it is rather
    // than about what it says.
    private static String readRow(String report, String sectionName) {
        return report
            .lines()
            .filter(line -> line.startsWith(sectionName))
            .findFirst()
            .orElseThrow();
    }
}
