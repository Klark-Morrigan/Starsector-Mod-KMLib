package kmlib.profiling.recording;

import kmlib.profiling.ProfileLevel;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins, of {@link RecordingProfiler}, that a profiler answers the level it was built to keep.
 */
final class RecordingProfilerGetRecordedLevelTest {

    @Nested
    class GetRecordedLevel {

        @Test
        void answersTheLevelItWasBuiltAt() {
            // What a caller choosing what to bind compares its knob against, so an unrelated
            // settings change does not throw the capture away and start a new one.
            assertThat(new RecordingProfiler(ProfileLevel.COARSE).getRecordedLevel())
                .isEqualTo(ProfileLevel.COARSE);
        }

        @Test
        void keepsEveryLevelWhereTheCallerNamedNone() {
            // A profiler asked for without a level keeps what it is handed: the level is a
            // reader's restriction, not a default the library imposes.
            assertThat(new RecordingProfiler().getRecordedLevel())
                .isEqualTo(ProfileLevel.FINE);
        }
    }
}
