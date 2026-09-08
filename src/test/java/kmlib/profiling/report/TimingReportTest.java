package kmlib.profiling.report;

import kmlib.profiling.PhasedSection;
import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileOrigin;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.snapshot.BudgetBreach;
import kmlib.profiling.snapshot.CallCount;
import kmlib.profiling.snapshot.CountSpread;
import kmlib.profiling.snapshot.CountTotals;
import kmlib.profiling.snapshot.DurationBuckets;
import kmlib.profiling.snapshot.PhaseTotal;
import kmlib.profiling.snapshot.ProfileCount;
import kmlib.profiling.snapshot.ProfileIterations;
import kmlib.profiling.snapshot.ProfileNode;
import kmlib.profiling.snapshot.ProfileOriginTree;
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
 * how many calls landed in each, a row whose worst call has more to say than its duration carries
 * a second line saying it, a row that went over what its section allows always carries that line
 * with the bound it broke, a row whose calls ran a loop carries a line stating what one turn of it
 * cost, each group of roots is headed by the origin it was measured in, and none of those lines
 * disturbs the columns.
 *
 * <p>And what a request makes of the same capture: a listing names each row by its whole path and
 * puts the row that spent the time first, a listing over a counter drops the rows that never
 * counted it, a frame beat divides the totals and says in the heading how many frames they were
 * divided by, a beat that never ran leaves them as captured, and a request that keeps nothing says
 * so rather than reading as an empty capture.
 */
final class TimingReportTest {

    private static final String PARENT_SECTION = "politicalMap.rebuild";
    private static final String CHILD_SECTION = "politicalMap.walk";

    private static final String SHORT_PARENT_SECTION = "rebuild";
    private static final String LONGER_CHILD_SECTION = "walkTheWholeSector";

    private static final String SYSTEMS_COUNTER = "systems";
    private static final String MARKETS_COUNTER = "markets";
    private static final String WALKS_COUNTER = "walks";

    // A namespace nothing in these cases is named under, and a beat none of them opened: what a
    // request that keeps nothing and a capture with no frames in it are asked with.
    private static final String UNUSED_NAMESPACE = "someOtherMod.";
    private static final String UNOPENED_FRAME_BEAT = "mapLayer.prepare";

    // Two calls of the beat, so a total divided by them is a third figure again rather than the
    // total itself.
    private static final long FRAMES_MEASURED = 2;

    // The game every case below is reported as having been measured in, and a second for the case
    // about two of them. Spelled without a full stop, since one case reads a pair of them as the
    // mark of an empty duration band.
    private static final String ORIGIN_LABEL = "MN-6220 - Marat";
    private static final String OTHER_ORIGIN_LABEL = "PQ-1183 - Ceres";

    // What a group of roots is headed by, which is also how a case tells that heading from the
    // rows it heads.
    private static final String ORIGIN_HEADING_PREFIX = "origin ";

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

    // The column header, the origin heading under it and the one section under that, which is all
    // a row with nothing to add about its slowest call may print.
    private static final int ROWS_OF_A_HEADER_AN_ORIGIN_AND_ONE_SECTION = 3;

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

    // What the case about a flagged row allows that call, so its 48 systems are 47 too many.
    private static final long ONE_SYSTEM = 1L;

    // Longer than the section column and every number beside it, so a table that measured the line
    // would be visibly pulled out of shape by it.
    private static final String LONG_WORST_CALL_TAG =
        "a tag longer than the whole row it was written under";

    // The same, for the heading a group of roots is written under.
    private static final String LONG_ORIGIN_LABEL =
        "a seed and a name longer than the whole row beneath them";

    @Nested
    class Format {

        @Test
        void formatReturnsANoticeWhenNothingWasRecorded() {

            assertThat(formatTree(List.of()))
                .isEqualTo("No timings recorded.");
        }

        @Test
        void formatReturnsADifferentNoticeWhereTheRequestKeptNothing() {
            // Not the same answer as an empty capture: one says nothing was measured, the other
            // says the question found nothing, and a reader narrowing a filter needs to know which.
            var report = TimingReport.format(
                List.of(new ProfileOriginTree(
                    ProfileOrigin.registerOrigin(ORIGIN_LABEL),
                    List.of(nodeWhoseWorstCall(WorstCall.NO_CALL)))),
                ProfileReportRequest.showTree().limitToNamespace(UNUSED_NAMESPACE));

            assertThat(report)
                .isEqualTo("No rows matched.");
        }

        @Test
        void formatRendersSectionNameCountAndMillisecondColumns() {
            // 2 calls, total 3_000_000ns = 3.000ms, avg 1.500ms.
            var node = new ProfileNode(
                ProfileSection.registerSection("render"),
                new ProfileTiming(2, 3_000_000, 1_000_000, 2_000_000, DurationBuckets.NO_CALLS),
                WorstCall.NO_CALL,
                BudgetBreach.NO_BREACH,
                ProfileIterations.NO_ITERATIONS,
                List.of(),
                List.of());
                
            var report = formatOneOrigin(List.of(node));

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
            var report = formatOneOrigin(
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
            var report = formatOneOrigin(
                List.of(parentHoldingOneChild(PARENT_SECTION, CHILD_SECTION)));

            assertThat(report)
                .contains("1.000", "3.000");
        }

        @Test
        void sizesTheNameColumnToTheWidestRowIndentIncluded() {
            // The name column is sized from the deepest row, not the shallowest: a child indented
            // past its parent's width would otherwise push its own numbers out of the columns the
            // header names, and a table whose rows disagree on where a column starts is unreadable.
            var report = formatOneOrigin(
                List.of(parentHoldingOneChild(SHORT_PARENT_SECTION, LONGER_CHILD_SECTION)));

            assertThat(List.of(report.split("\n")))
                .filteredOn(line -> !line.startsWith(ORIGIN_HEADING_PREFIX))
                .extracting(String::length)
                .containsOnly(WIDEST_NAME_WIDTH + NUMERIC_COLUMNS_WIDTH);
        }

        @Test
        void formatHeadsAColumnGroupForEveryCounterTheTreeTouched() {
            // What a duration is read against: the row states how much it counted in all and how
            // far one call's worth spread, beside the milliseconds it took to do it.
            var report = formatOneOrigin(List.of(nodeCounting(
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
            var report = formatOneOrigin(List.of(nodeCounting(
                PARENT_SECTION, countOf(SYSTEMS_COUNTER, 300, 300, 300, 300))));

            assertThat(readFilledCells(report, PARENT_SECTION))
                .contains("10.0");
        }

        @Test
        void formatLeavesACounterBlankOnARowThatNeverTouchedIt() {
            // A zero would say the row counted none of that thing, which is a fact worth seeing.
            // A row that does not count it at all has nothing to say, and a wide tree where every
            // row answers every counter is a table of zeroes hiding the rows that count.
            var report = formatOneOrigin(List.of(
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
            var report = formatOneOrigin(List.of(nodeCounting(
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
            var report = formatOneOrigin(List.of(nodeSpreadOver(
                bandsHolding(THOUSAND_CALLS, FEW_CALLS))));

            assertThat(readFilledCells(report, PARENT_SECTION))
                .contains(EXPECTED_BAND_MARKS);
        }

        @Test
        void formatShowsNoBandsForARowNoCallHasFinishedOn() {
            // A line of dots would read as calls that were all too fast to matter, which is the
            // opposite of what a row caught mid-call holds.
            var report = formatOneOrigin(List.of(nodeCounting(
                PARENT_SECTION, countOf(SYSTEMS_COUNTER, 300, 300, 100, 200))));

            // Two dots in a row can only be empty bands: a millisecond figure carries one.
            assertThat(report)
                .doesNotContain("..");
        }

        @Test
        void formatWritesWhatTheSlowestCallWasDoingUnderTheRow() {
            // The one fact that says what to optimise: the maximum column carries the duration, and
            // the tag and the counters it was reached over have nowhere else to go.
            var report = formatOneOrigin(List.of(nodeWhoseWorstCall(new WorstCall(
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
            var report = formatOneOrigin(List.of(nodeWhoseWorstCall(new WorstCall(
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
            var report = formatOneOrigin(List.of(nodeSpreadOver(
                bandsHolding(UNCOUNTABLY_MANY_CALLS, FEW_CALLS))));

            assertThat(readFilledCells(report, PARENT_SECTION))
                .contains(EXPECTED_CAPPED_BAND_MARKS);
        }

        @Test
        void formatWritesNoWorstCallLineWhereItRepeatsTheMaximumColumn() {
            // A call named nothing and counting nothing has only its duration to state, and the
            // table has already stated it - so the rows whose calls are all alike stay one line.
            var report = formatOneOrigin(List.of(nodeWhoseWorstCall(
                new WorstCall(PARENT_TOTAL_NANOS, WorstCall.NO_TAG, List.of()))));

            assertThat(report.lines())
                .hasSize(ROWS_OF_A_HEADER_AN_ORIGIN_AND_ONE_SECTION);
        }

        @Test
        void formatWritesWhatARowWentOverBudgetByBesideTheCallThatBrokeIt() {
            // A finding and its evidence on one line: the bound that was broken says what is wrong,
            // and the call beside it is what a reader would otherwise go looking for.
            var report = formatOneOrigin(List.of(nodeWhoseWorstCall(
                new WorstCall(
                    PARENT_TOTAL_NANOS,
                    WORST_CALL_TAG,
                    List.of(new CallCount(
                        ProfileCounter.registerCounter(SYSTEMS_COUNTER), WORST_CALL_SYSTEMS))),
                breachOfOneAllowedSystem())));

            assertThat(report)
                .contains("\n  worst 3.000ms  over budget: 48 " + SYSTEMS_COUNTER
                    + ", 1 allowed per call")
                .contains("\"" + WORST_CALL_TAG + "\"");
        }

        @Test
        void formatWritesTheBreachEvenWhereTheCallHasNothingElseToAdd() {
            // A row is one line while its calls are alike, but a broken bound is never a repeat of
            // the maximum column: it is the reason to look at the row at all.
            var report = formatOneOrigin(List.of(nodeWhoseWorstCall(
                new WorstCall(PARENT_TOTAL_NANOS, WorstCall.NO_TAG, List.of()),
                breachOfOneAllowedSystem())));

            assertThat(report.lines())
                .hasSize(ROWS_OF_A_HEADER_AN_ORIGIN_AND_ONE_SECTION + 1);
        }

        @Test
        void formatWritesWhatOneTurnOfTheLoopCostInEachStep() {
            // What the row's own columns cannot say: they are per call, and a bake's cost scales
            // with the cells it turned over rather than with how often it ran.
            var report = formatOneOrigin(List.of(nodeIterating(
                LOOP_TURNS, PHASE_TOTAL_NANOS, SLOWEST_TURN_TAG, WorstCall.NO_TAG)));

            assertThat(report)
                .contains("\n  iterations=2")
                .contains("plan=100.0us/ea")
                .contains("slowest=3.000ms")
                .contains("\"" + SLOWEST_TURN_TAG + "\"");
        }

        @Test
        void formatWritesTheLoopLineUnderTheWorstCallLine() {
            // Both say something the columns do not, about different denominators - the call and
            // the turn - so both are written, the call's first since the columns are per call.
            var report = formatOneOrigin(List.of(nodeIterating(
                LOOP_TURNS, PHASE_TOTAL_NANOS, SLOWEST_TURN_TAG, WORST_CALL_TAG)));

            assertThat(report.lines())
                .containsSequence(
                    "  worst 3.000ms  \"" + WORST_CALL_TAG + "\"",
                    "  iterations=2  plan=100.0us/ea  slowest=3.000ms  \"" + SLOWEST_TURN_TAG + "\"");
        }

        @Test
        void formatWritesNoLoopLineForARowThatRanNone() {
            // Most rows, which would otherwise each carry a line saying they iterated over
            // nothing.
            var report = formatOneOrigin(List.of(nodeWhoseWorstCall(WorstCall.NO_CALL)));

            assertThat(report.lines())
                .hasSize(ROWS_OF_A_HEADER_AN_ORIGIN_AND_ONE_SECTION);
        }

        @Test
        void formatKeepsTheColumnsAlignedAroundALoopLine() {
            // The loop line belongs to no column for the same reason the worst call's does: its
            // numbers are per turn, and its tag is a caller's own text of a caller's own length.
            var report = formatOneOrigin(List.of(nodeIterating(
                LOOP_TURNS, PHASE_TOTAL_NANOS, LONG_WORST_CALL_TAG, WorstCall.NO_TAG)));

            assertThat(readRow(report, PARENT_SECTION).length())
                .isEqualTo(PARENT_SECTION.length() + NUMERIC_COLUMNS_WIDTH);
        }

        @Test
        void formatKeepsTheColumnsAlignedAroundAWorstCallLine() {
            // The line belongs to no column and carries a caller's own text, so measuring it would
            // widen the section column by however long that text was and push every number away
            // from the header it sits under.
            var report = formatOneOrigin(List.of(nodeWhoseWorstCall(new WorstCall(
                PARENT_TOTAL_NANOS, LONG_WORST_CALL_TAG, List.of()))));

            assertThat(readRow(report, PARENT_SECTION).length())
                .isEqualTo(PARENT_SECTION.length() + NUMERIC_COLUMNS_WIDTH);
        }

        @Test
        void formatHeadsEachGroupOfRootsWithTheOriginItWasMeasuredIn() {
            // Two games in one capture: without the headings a reader has two rows spelled alike
            // and no way to say which save either came from, which is the one thing that would let
            // them go back and reproduce it.
            var report = formatTree(List.of(
                new ProfileOriginTree(
                    ProfileOrigin.registerOrigin(ORIGIN_LABEL),
                    List.of(nodeWhoseWorstCall(WorstCall.NO_CALL))),
                new ProfileOriginTree(
                    ProfileOrigin.registerOrigin(OTHER_ORIGIN_LABEL),
                    List.of(nodeWhoseWorstCall(WorstCall.NO_CALL)))));

            assertThat(report.lines())
                .containsSequence(
                    ORIGIN_HEADING_PREFIX + ORIGIN_LABEL,
                    readRow(report, PARENT_SECTION),
                    ORIGIN_HEADING_PREFIX + OTHER_ORIGIN_LABEL);
        }

        @Test
        void formatNamesEachRowByItsWholePathInAListing() {
            // A listing has taken the rows out of the tree, so the indent that said what a row ran
            // inside is gone: without the path, two rows of one section under two parents would be
            // one name printed twice.
            var report = TimingReport.format(
                oneOriginOf(List.of(parentHoldingOneChild(PARENT_SECTION, CHILD_SECTION))),
                ProfileReportRequest.showRowsBySelfTime());

            assertThat(readRowNames(report))
                .contains(PARENT_SECTION + "/" + CHILD_SECTION);
        }

        @Test
        void formatSortsAListingByWhatEachRowSpentItself() {
            // The 2.000ms walk before the 3.000ms rebuild that was waiting on it: what a reader
            // hunting for time to save opens next is the row that spent it, not the row above it.
            var report = TimingReport.format(
                oneOriginOf(List.of(parentHoldingOneChild(PARENT_SECTION, CHILD_SECTION))),
                ProfileReportRequest.showRowsBySelfTime());

            assertThat(readRowNames(report))
                .containsSequence(PARENT_SECTION + "/" + CHILD_SECTION, PARENT_SECTION);
        }

        @Test
        void formatShowsOnlyTheRowsThatCountedInACounterListing() {
            // A listing of what walked is unreadable beside the rows that never walked: the
            // question is which pass went looking for the sector, and a row that never did is not
            // an answer at zero.
            var report = TimingReport.format(
                oneOriginOf(List.of(
                    nodeCounting(PARENT_SECTION, countOf(WALKS_COUNTER, 2, 2, 2, 2)),
                    nodeCounting(CHILD_SECTION, countOf(MARKETS_COUNTER, 40, 40, 40, 40)))),
                ProfileReportRequest.showRowsCounting(
                    ProfileCounter.registerCounter(WALKS_COUNTER)));

            assertThat(readRowNames(report))
                .contains(PARENT_SECTION)
                .doesNotContain(CHILD_SECTION);
        }

        @Test
        void formatDividesTotalsByTheFramesTheBeatRanFor() {
            // What a reader is after: 3.000ms over two frames is 1.500ms a frame, which is a figure
            // that means the same whether the capture ran for four seconds or four minutes.
            var beat = ProfileSection.registerSection(PARENT_SECTION);
            var report = TimingReport.format(
                oneOriginOf(List.of(nodeCalledTwice())),
                ProfileReportRequest.showTree().divideByFramesOf(beat));

            assertThat(report)
                .contains("COUNT/f", "SELF ms/f", "TOTAL ms/f")
                .contains("per frame, over 2 of " + PARENT_SECTION);
            assertThat(readFilledCells(report, PARENT_SECTION))
                .contains("1.00", "1.500");
        }

        @Test
        void formatReportsAsCapturedWhereTheFrameBeatNeverRanInThatGame() {
            // A capture holding no frame of the beat cannot say what one cost, and dividing by
            // nothing would report a figure no frame ever had - so it says so and leaves the
            // totals alone.
            var report = TimingReport.format(
                oneOriginOf(List.of(nodeCalledTwice())),
                ProfileReportRequest.showTree().divideByFramesOf(
                    ProfileSection.registerSection(UNOPENED_FRAME_BEAT)));

            assertThat(report)
                .contains(UNOPENED_FRAME_BEAT + " never ran here; totals as captured");
            assertThat(readFilledCells(report, PARENT_SECTION))
                .contains("3.000");
        }

        @Test
        void formatKeepsTheColumnsAlignedAroundAnOriginHeading() {
            // The heading belongs to no column and carries a label a caller composed, so measuring
            // it would widen the section column by however long that label was.
            var report = formatTree(List.of(new ProfileOriginTree(
                ProfileOrigin.registerOrigin(LONG_ORIGIN_LABEL),
                List.of(nodeWhoseWorstCall(WorstCall.NO_CALL)))));

            assertThat(readRow(report, PARENT_SECTION).length())
                .isEqualTo(PARENT_SECTION.length() + NUMERIC_COLUMNS_WIDTH);
        }
    }

    // The report of one game's roots, which is what every case not about the grouping itself is
    // reading: what a row says is no different for having a second game's rows below it.
    private static String formatOneOrigin(List<ProfileNode> roots) {
        return formatTree(oneOriginOf(roots));
    }

    // The capture as it was measured, over every row of it, which is what a reader opens first and
    // what every case not about another reading is reading.
    private static String formatTree(List<ProfileOriginTree> originTrees) {
        return TimingReport.format(originTrees, ProfileReportRequest.showTree());
    }

    // One game's roots as a capture, for the cases that ask a request of them rather than the tree
    // reading formatOneOrigin takes.
    private static List<ProfileOriginTree> oneOriginOf(List<ProfileNode> roots) {
        return List.of(new ProfileOriginTree(ProfileOrigin.registerOrigin(ORIGIN_LABEL), roots));
    }

    // Two 1.500ms calls, which is a total a frame count divides into a figure the row itself never
    // reported.
    private static ProfileNode nodeCalledTwice() {
        return new ProfileNode(
            ProfileSection.registerSection(PARENT_SECTION),
            new ProfileTiming(
                FRAMES_MEASURED,
                PARENT_TOTAL_NANOS,
                CHILD_TOTAL_NANOS,
                CHILD_TOTAL_NANOS,
                DurationBuckets.NO_CALLS),
            WorstCall.NO_CALL,
            BudgetBreach.NO_BREACH,
            ProfileIterations.NO_ITERATIONS,
            List.of(),
            List.of());
    }

    // What each line is about: the first word of it, which is the section column for a row and the
    // heading's own first word for the lines that are not rows.
    private static List<String> readRowNames(String report) {
        return report
            .lines()
            .map(line -> line.trim().split("\\s+")[0])
            .toList();
    }

    // One call of a parent holding one call of a child, which is the smallest tree that has a
    // parent, an indent and a self time to report.
    private static ProfileNode parentHoldingOneChild(String parentName, String childName) {
        return new ProfileNode(
            ProfileSection.registerSection(parentName),
            oneCallOf(PARENT_TOTAL_NANOS),
            WorstCall.NO_CALL,
            BudgetBreach.NO_BREACH,
            ProfileIterations.NO_ITERATIONS,
            List.of(),
            List.of(new ProfileNode(
                ProfileSection.registerSection(childName),
                oneCallOf(CHILD_TOTAL_NANOS),
                WorstCall.NO_CALL,
                BudgetBreach.NO_BREACH,
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
            BudgetBreach.NO_BREACH,
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
            BudgetBreach.NO_BREACH,
            ProfileIterations.NO_ITERATIONS,
            List.of(),
            List.of());
    }

    // A row whose one call ran a loop of one step, which is the smallest tree with a per-turn cost
    // to divide out and a slowest turn to name. The call is named too where a case is about the
    // two lines standing together.
    private static ProfileNode nodeIterating(
            long turns,
            long phaseNanos,
            String slowestTag,
            String worstCallTag) {

        var bakeSection = PhasedSection.registerPhasedSection(PARENT_SECTION, PLAN_PHASE_NAME);

        return new ProfileNode(
            ProfileSection.registerSection(PARENT_SECTION),
            oneCallOf(PARENT_TOTAL_NANOS),
            new WorstCall(PARENT_TOTAL_NANOS, worstCallTag, List.of()),
            BudgetBreach.NO_BREACH,
            new ProfileIterations(
                turns,
                List.of(new PhaseTotal(bakeSection.resolvePhase(PLAN_PHASE_NAME), phaseNanos)),
                PARENT_TOTAL_NANOS,
                slowestTag),
            List.of(),
            List.of());
    }

    private static ProfileNode nodeWhoseWorstCall(WorstCall worstCall) {
        return nodeWhoseWorstCall(worstCall, BudgetBreach.NO_BREACH);
    }

    // A bound of one system broken by the 48 the worst call reached, stated as the measurements a
    // bound would have produced rather than as prose - a view renders what a capture holds, and a
    // sentence typed here could pass while the one a real breach carries drifted.
    private static BudgetBreach breachOfOneAllowedSystem() {
        return BudgetBreach.reportCountBreach(
            ProfileCounter.registerCounter(SYSTEMS_COUNTER), WORST_CALL_SYSTEMS, ONE_SYSTEM);
    }

    // The same row with a bound broken on it, which is what makes the line under it a finding
    // rather than a remark about the slowest call.
    private static ProfileNode nodeWhoseWorstCall(WorstCall worstCall, BudgetBreach breach) {
        return new ProfileNode(
            ProfileSection.registerSection(PARENT_SECTION),
            oneCallOf(PARENT_TOTAL_NANOS),
            worstCall,
            breach,
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
