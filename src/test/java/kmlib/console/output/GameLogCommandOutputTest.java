package kmlib.console.output;

import kmlib.testfixtures.logging.LogAppenderFake;

import org.apache.log4j.Level;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

            var appenderFake = LogAppenderFake.captureLogOf(
                GameLogCommandOutput.class, () -> GameLogCommandOutput.INSTANCE.showMessage(TABLE));

            assertThat(appenderFake.getEvents())
                .hasSize(1);
            assertThat(appenderFake.getEvents().get(0).getLevel())
                .isEqualTo(Level.INFO);
            assertThat(appenderFake.getMessages())
                .containsExactly(TABLE);
        }
    }
}
