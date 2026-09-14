package kmlib.testfixtures.logging;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins what a capture through {@link LogAppenderFake} is worth, which is a contract every KM mod's
 * suites rest on rather than one this library's own code calls.
 *
 * <p>What only this can show is the process-global state the capture takes and puts back: a
 * logger's level, its additivity and its appenders all outlive the call, so a capture that failed
 * to take one would read the wrong thing and one that failed to give it back would leave the next
 * suite reading the wrong thing. What the capture then hands over is read by every suite that uses
 * it, which is where the rest of its behaviour is covered.
 *
 * <p>The level is what made this worth pinning. A class guarding a line behind
 * {@code isDebugEnabled} wrote nothing under a capture that never raised the level, and every case
 * reading that capture passed or failed on whether some unrelated log4j configuration had reached
 * the classpath - which reads as the code having chosen not to write the line.
 */
final class LogAppenderFakeTest {

    private static final String FIRST_LINE = "first";
    private static final String SECOND_LINE = "second";

    // The level a capture has to see past. Above debug, so a debug line is dropped by any capture
    // that leaves the level where it found it.
    private static final Level ABOVE_DEBUG = Level.WARN;

    private final Logger log = Logger.getLogger(LogAppenderFakeTest.class);

    @AfterEach
    void restoreTheLoggersOwnSettings() {
        // Every case here poses a level and an additivity, and both outlive the case: they are
        // log4j's, not JUnit's, so whatever a case left standing is what the next suite in this
        // JVM would be read under.
        log.setLevel(null);
        log.setAdditivity(true);
    }

    @Nested
    class CaptureLogOf {

        @Test
        void collectsALineTheStandingLevelWouldHaveDropped() {
            // The whole reason the capture takes the level. A class that guards its trace behind
            // isDebugEnabled writes nothing while the level stands above debug, so a capture that
            // left the level alone would report silence for a line the class does write.
            log.setLevel(ABOVE_DEBUG);

            var appenderFake = LogAppenderFake.captureLogOf(
                LogAppenderFakeTest.class,
                () -> {
                    if (log.isDebugEnabled()) {
                        log.debug(FIRST_LINE);
                    }
                });

            assertThat(appenderFake.getMessages())
                .containsExactly(FIRST_LINE);
        }

        @Test
        void putsTheLoggersOwnLevelBackAfterwards() {

            log.setLevel(ABOVE_DEBUG);

            LogAppenderFake.captureLogOf(LogAppenderFakeTest.class, () -> { });

            assertThat(log.getLevel())
                .isEqualTo(ABOVE_DEBUG);
        }

        @Test
        void putsBackAnInheritedLevelAsInheritedRatherThanAsWhateverItResolvedTo() {
            // A logger stating no level of its own takes its ancestors', and that is a setting like
            // any other: a capture that put back the level it resolved to would pin this logger to
            // a level nobody chose, and the next change to an ancestor would stop reaching it.
            LogAppenderFake.captureLogOf(LogAppenderFakeTest.class, () -> { });

            assertThat(log.getLevel())
                .isNull();
        }

        @Test
        void putsAdditivityBackAfterwards() {
            // Off for the length of the capture, so a case's planted lines stay out of the run's
            // own output - and back afterwards, since a logger left detached from the root would
            // silence whatever this class writes for the rest of the JVM.
            LogAppenderFake.captureLogOf(LogAppenderFakeTest.class, () -> { });

            assertThat(log.getAdditivity())
                .isTrue();
        }

        @Test
        void takesTheAppenderBackOffWhenTheWorkThrows() {
            // A case whose subject is a fault still has to leave the logger clean: an appender left
            // attached goes on collecting whatever the rest of the suite writes, and the next case
            // reads a tally that is not its own.
            assertThatThrownBy(() -> LogAppenderFake.captureLogOf(
                LogAppenderFakeTest.class,
                () -> {
                    throw new IllegalStateException("the work faulted");
                }))
                .isInstanceOf(IllegalStateException.class);

            var afterTheFault =
                LogAppenderFake.captureLogOf(LogAppenderFakeTest.class, () -> log.warn(FIRST_LINE));

            assertThat(afterTheFault.getMessages())
                .containsExactly(FIRST_LINE);
        }

        @Test
        void collectsNothingWrittenAfterTheCaptureCloses() {

            var appenderFake =
                LogAppenderFake.captureLogOf(LogAppenderFakeTest.class, () -> log.warn(FIRST_LINE));

            log.warn(SECOND_LINE);

            assertThat(appenderFake.getMessages())
                .containsExactly(FIRST_LINE);
        }
    }

}
