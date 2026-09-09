package kmlib.testfixtures.profiling;

import kmlib.profiling.ActiveProfiler;
import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.SilentProfiler;
import kmlib.profiling.recording.RecordingProfiler;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the one thing about {@link RecordedCapture} its callers cannot see: the profiler holder it
 * binds through is process-wide, so a binding left behind follows the next suite into a capture it
 * never asked for, and one never made records nothing the walkers counted.
 *
 * <p>What the capture then hands back is read by every suite that uses it, which is where the rest
 * of its behaviour is covered.
 */
final class RecordedCaptureTest {

    private static final String COUNTER_NAME = "test.recordedCapture.systems";

    private static final ProfileCounter SYSTEMS_COUNTER =
        ProfileCounter.registerCounter(COUNTER_NAME);

    // Nothing here is a duration, so one reading answers every clock read.
    private static final long FIXED_CLOCK_NANOS = 0L;

    private static final long TWO_SYSTEMS = 2L;

    @Nested
    class RecordWhile {

        @Test
        void bindsTheProfilerTheWorkIsMeasuredThrough() {
            // The point of the whole fixture: the walkers reach the holder rather than anything a
            // caller hands them, so a capture that did not bind would record nothing they counted.
            var capture = RecordedCapture.recordWhile(
                profilerReadingAFixedClock(),
                () -> ActiveProfiler.resolveProfiler().addCountToOpenScope(
                    SYSTEMS_COUNTER, TWO_SYSTEMS));

            assertThat(ProfileCounts.readTotalOf(
                    capture.findNode(ProfileSection.UNSCOPED_COUNTS.getName()),
                    SYSTEMS_COUNTER))
                .isEqualTo(TWO_SYSTEMS);
        }

        @Test
        void putsTheSilentProfilerBackAfterwards() {
            // The holder is process-wide, so a binding left standing goes on recording into a
            // capture nothing reads and follows this suite into whatever runs next.
            RecordedCapture.recordWhile(profilerReadingAFixedClock(), () -> { });

            assertThat(ActiveProfiler.resolveProfiler())
                .isSameAs(SilentProfiler.INSTANCE);
        }

        @Test
        void putsTheSilentProfilerBackWhenTheWorkThrows() {
            // A case whose subject is a fault leaves the holder as bound as any other would.
            assertThatThrownBy(() -> RecordedCapture.recordWhile(
                profilerReadingAFixedClock(),
                () -> {
                    throw new IllegalStateException("the work faulted");
                }))
                .isInstanceOf(IllegalStateException.class);

            assertThat(ActiveProfiler.resolveProfiler())
                .isSameAs(SilentProfiler.INSTANCE);
        }
    }

    private static RecordingProfiler profilerReadingAFixedClock() {
        return new RecordingProfiler(() -> FIXED_CLOCK_NANOS);
    }
}
