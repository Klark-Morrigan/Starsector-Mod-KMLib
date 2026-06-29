package kmlib.profiling;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link TimingReport}: empty input yields a notice, and a section renders
 * a row with its name, count, and nanos converted to milliseconds.
 */
final class TimingReportTest {

    @Nested
    class Format {
        @Test
        void formatReturnsANoticeWhenNothingWasRecorded() {
            assertThat(TimingReport.format(List.of())).isEqualTo("No timings recorded.");
        }

        @Test
        void formatRendersSectionNameCountAndMillisecondColumns() {
            // 2 calls, total 3_000_000ns = 3.000ms, avg 1.500ms.
            var timing = new SectionTiming("render", 2, 3_000_000, 1_000_000, 2_000_000);

            var report = TimingReport.format(List.of(timing));

            assertThat(report).contains("SECTION", "COUNT", "AVG ms", "MIN ms", "MAX ms", "TOTAL ms");
            assertThat(report).contains("render");
            // count and the millisecond conversions appear in the row.
            assertThat(report).contains("2", "1.500", "1.000", "2.000", "3.000");
        }
    }
}
