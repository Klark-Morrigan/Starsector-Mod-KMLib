package kmlib.starsector.ui.map.probes;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Pins that gathering the probes' warnings buys the one thing it is for - letting them all speak
 * again for a reader who arrives late - without costing the thing they were built for, which is that
 * each stays its own flag.
 *
 * <p>The registry is process-wide and the probes register at class-init, so these cases work through
 * warnings of their own rather than asserting on how many it holds: what the probes put there is not
 * this test's to know, and a count would break every time one is added.
 */
class MapProbeWarningsTest {

    private static final String MESSAGE = "the widget tree could not be walked";
    private static final String OTHER_MESSAGE = "the surface could not be identified";

    @Nested
    class RearmAllWarnings {

        @Test
        void rearmAllWarningsLetsAGatheredWarningBeSaidOnceMore() {

            var loggerMock = mock(Logger.class);
            var warning = MapProbeWarnings.createSharedWarning(loggerMock);

            warning.warnOnce(MESSAGE);
            MapProbeWarnings.rearmAllWarnings();
            warning.warnOnce(MESSAGE);

            verify(loggerMock, times(2))
                .warn(MESSAGE);
        }

        @Test
        void rearmAllWarningsReachesEveryGatheredWarning() {
            // The reason they are gathered at all. The reaches a trace is built from are spread over
            // several probes, so re-arming only the one the trace is named after would leave the
            // reader met with silence from whichever of the others actually broke.
            var loggerMock = mock(Logger.class);
            var otherLoggerMock = mock(Logger.class);
            var warning = MapProbeWarnings.createSharedWarning(loggerMock);
            var otherWarning = MapProbeWarnings.createSharedWarning(otherLoggerMock);

            warning.warnOnce(MESSAGE);
            otherWarning.warnOnce(OTHER_MESSAGE);
            MapProbeWarnings.rearmAllWarnings();
            warning.warnOnce(MESSAGE);
            otherWarning.warnOnce(OTHER_MESSAGE);

            verify(loggerMock, times(2))
                .warn(MESSAGE);
            verify(otherLoggerMock, times(2))
                .warn(OTHER_MESSAGE);
        }

        @Test
        void rearmAllWarningsSaysNothingByItself() {
            // A switch flipped on a session where nothing broke must not manufacture a warning.
            var loggerMock = mock(Logger.class);

            MapProbeWarnings.createSharedWarning(loggerMock);
            MapProbeWarnings.rearmAllWarnings();

            verify(loggerMock, times(0))
                .warn(MESSAGE);
        }
    }

    @Nested
    class CreateSharedWarning {

        @Test
        void createSharedWarningKeepsEachWarningItsOwnFlag() {
            // Gathering them must not pool them: one probe's broken read still cannot silence the
            // news of another's, which is the whole reason these are per holder rather than static.
            var loggerMock = mock(Logger.class);
            var otherLoggerMock = mock(Logger.class);
            var warning = MapProbeWarnings.createSharedWarning(loggerMock);
            var otherWarning = MapProbeWarnings.createSharedWarning(otherLoggerMock);

            warning.warnOnce(MESSAGE);

            assertThat(otherWarning.hasWarnedThisSession())
                .isFalse();
        }

        @Test
        void createSharedWarningHandsBackAWarningThatStillSaysThingsOnlyOnce() {

            var loggerMock = mock(Logger.class);
            var warning = MapProbeWarnings.createSharedWarning(loggerMock);

            warning.warnOnce(MESSAGE);
            warning.warnOnce(MESSAGE);

            verify(loggerMock, times(1))
                .warn(MESSAGE);
        }
    }
}
