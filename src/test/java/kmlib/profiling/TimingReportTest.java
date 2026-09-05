package kmlib.profiling;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link TimingReport}: empty input yields a notice, a section renders a row with its name,
 * count, and nanos converted to milliseconds, a section that ran inside another is printed
 * indented under it with its own self time, and every counter the tree touched heads a group of
 * columns that stays blank on the rows which never touched it.
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

    // The six numeric columns: two spaces then 8, 10, 10, 10, 10 and 11 characters.
    private static final int NUMERIC_COLUMNS_WIDTH = 71;

    // The section name, the six timing columns, and the four of the one counter group that row
    // filled - the second group in the table stays blank on it.
    private static final int FILLED_CELLS_PER_ROW_WITH_ONE_COUNTER = 11;

    // The same row less its cost-each cell, which a row that counted nothing itself cannot state.
    private static final int FILLED_CELLS_PER_ROW_WITH_NO_PER_ITEM_COST = 10;

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
                2,
                3_000_000,
                1_000_000,
                2_000_000,
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
    }

    // One call of a parent holding one call of a child, which is the smallest tree that has a
    // parent, an indent and a self time to report.
    private static ProfileNode parentHoldingOneChild(String parentName, String childName) {
        return new ProfileNode(
            ProfileSection.registerSection(parentName),
            1,
            PARENT_TOTAL_NANOS,
            PARENT_TOTAL_NANOS,
            PARENT_TOTAL_NANOS,
            List.of(),
            List.of(new ProfileNode(
                ProfileSection.registerSection(childName),
                1,
                CHILD_TOTAL_NANOS,
                CHILD_TOTAL_NANOS,
                CHILD_TOTAL_NANOS,
                List.of(),
                List.of())));
    }

    // One 3.000ms call that counted something, which is the smallest tree with a counter group to
    // render and a per-item cost to divide out.
    private static ProfileNode nodeCounting(String sectionName, ProfileCount count) {
        return new ProfileNode(
            ProfileSection.registerSection(sectionName),
            1,
            PARENT_TOTAL_NANOS,
            PARENT_TOTAL_NANOS,
            PARENT_TOTAL_NANOS,
            List.of(count),
            List.of());
    }

    private static ProfileCount countOf(
            String counterName,
            long total,
            long selfTotal,
            long minPerCall,
            long maxPerCall) {

        return new ProfileCount(
            ProfileCounter.registerCounter(counterName), total, selfTotal, minPerCall, maxPerCall);
    }

    // The cells a row actually fills, blank ones excluded - a blank cell is whitespace and so
    // splits away with the padding, which is what "prints nothing" has to mean in a fixed table.
    private static List<String> readFilledCells(String report, String sectionName) {
        return List.of(report
            .lines()
            .filter(line -> line.startsWith(sectionName))
            .findFirst()
            .orElseThrow()
            .trim()
            .split("\\s+"));
    }
}
