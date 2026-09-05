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
            var report = TimingReport.format(List.of(rebuildWithAWalkInside()));

            assertThat(report)
                .contains("\n" + PARENT_SECTION);
            assertThat(report)
                .contains("\n  " + CHILD_SECTION);
        }

        @Test
        void formatSeparatesSelfTimeFromTheInclusiveTotal() {
            // A parent of 3.000ms holding a 2.000ms walk spent 1.000ms itself, which is what says
            // whether to look at the row or below it.
            var report = TimingReport.format(List.of(rebuildWithAWalkInside()));

            assertThat(report)
                .contains("1.000", "3.000");
        }
    }

    private static ProfileNode rebuildWithAWalkInside() {
        var walk = new ProfileNode(
            ProfileSection.registerSection(CHILD_SECTION),
            1,
            2_000_000,
            2_000_000,
            2_000_000,
            List.of());
        return new ProfileNode(
            ProfileSection.registerSection(PARENT_SECTION),
            1,
            3_000_000,
            3_000_000,
            3_000_000,
            List.of(walk));
    }
}
