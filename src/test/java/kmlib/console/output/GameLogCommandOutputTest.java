package kmlib.console.output;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link GameLogCommandOutput}: what a command hands it reaches the game log whole, in one
 * entry and at info - a table split across entries would arrive in the file interleaved with
 * whatever else the frame logged, and a diagnostic a player was asked for is an answer rather than
 * a warning.
 */
final class GameLogCommandOutputTest {

    // Two lines, because the one thing a table cannot survive is being written a line at a time.
    private static final String TABLE = "SECTION  TOTAL ms\nmapLayer.prepare  49.440";

    @Nested
    class ShowMessage {

        @Test
        void writesTheWholeMessageToTheGameLogInOneEntryAtInfo() {

            var events = new ArrayList<LoggingEvent>();
            var appenderFake = new LogAppenderFake(events);
            var logger = Logger.getLogger(GameLogCommandOutput.class);

            logger.addAppender(appenderFake);
            try {
                GameLogCommandOutput.INSTANCE.showMessage(TABLE);
            } finally {
                logger.removeAppender(appenderFake);
            }

            assertThat(events).hasSize(1);
            assertThat(events.get(0).getLevel()).isEqualTo(Level.INFO);
            assertThat(events.get(0).getRenderedMessage()).isEqualTo(TABLE);
        }
    }

    // Stands in for the game's own appender, keeping what was written so a case can read it.
    private static final class LogAppenderFake extends AppenderSkeleton {

        private final List<LoggingEvent> events;

        private LogAppenderFake(List<LoggingEvent> events) {
            this.events = events;
        }

        @Override
        public void close() {
            // Nothing is held open: what was written is the list the case reads.
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }

        @Override
        protected void append(LoggingEvent event) {
            events.add(event);
        }
    }
}
