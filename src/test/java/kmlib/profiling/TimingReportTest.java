package kmlib.profiling;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link TimingReport}: empty input yields a notice, a section renders a row with its name,
 * count, and nanos converted to milliseconds, and a section that ran inside another is printed
 * indented under it with its own self time.
 */
final class TimingReportTest {

    private static final String PARENT_SECTION = "politicalMap.rebuild";
    private static final String CHILD_SECTION = "politicalMap.walk";

    private static final String SHORT_PARENT_SECTION = "rebuild";
    private static final String LONGER_CHILD_SECTION = "walkTheWholeSector";

    // 3.000ms holding 2.000ms, so the parent's self time is a third number again.
    private static final long PARENT_TOTAL_NANOS = 3_000_000;
    private static final long CHILD_TOTAL_NANOS = 2_000_000;

    // "walkTheWholeSector" plus the two spaces one level of nesting indents it by.
    private static final int WIDEST_NAME_WIDTH = 20;

    // The six numeric columns: two spaces then 8, 10, 10, 10, 10 and 11 characters.
    private static final int NUMERIC_COLUMNS_WIDTH = 71;

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
            List.of(new ProfileNode(
                ProfileSection.registerSection(childName),
                1,
                CHILD_TOTAL_NANOS,
                CHILD_TOTAL_NANOS,
                CHILD_TOTAL_NANOS,
                List.of())));
    }
}
