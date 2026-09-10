package kmlib.settings;

import kmlib.logging.KmLogging;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shipped settings table against the code that reads it, because every way it can drift
 * fails silently in play rather than loudly at build.
 *
 * <p>A field id renamed on either side leaves the read finding nothing and falling back, so the
 * player's pick simply does nothing. A default that disagrees with the library's own means the level
 * before a player has ever touched the screen is not the level the code says it is. And an option
 * label is a stored key wearing the costume of a caption - tidying its wording resets that setting
 * for everyone who had chosen it - so the option list is held against the level names log4j will
 * actually parse.
 *
 * <p>Reads the real data file rather than a fixture: a fixture would agree with the code while the
 * shipped table did not, which is precisely the failure.
 */
class LunaSettingsCsvIntegrationTest {

    private static final Path SETTINGS_CSV = Path.of("data", "config", "LunaSettings.csv");

    private static final String LOG_LEVEL_FIELD = "kmlib_logLevel";

    // Every level log4j can parse, in the order the dropdown offers them. Stated here rather than
    // derived, since the CSV's job is to offer exactly these and nothing else.
    private static final List<String> LOG_LEVEL_OPTIONS =
        List.of("OFF", "ERROR", "WARN", "INFO", "DEBUG", "ALL");

    private static String readLogLevelRow() {
        try {
            return Files.readAllLines(SETTINGS_CSV, StandardCharsets.UTF_8).stream()
                .filter(line -> line.startsWith(LOG_LEVEL_FIELD + ","))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                    "No row in " + SETTINGS_CSV + " declares " + LOG_LEVEL_FIELD));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    @Nested
    class LogLevelRow {

        @Test
        void shipsARowForTheFieldTheBindingReads() {
            // The binding asks LunaLib for this id; a row under any other name is a setting the
            // player can change and the library will never read.
            assertThat(readLogLevelRow()).startsWith(LOG_LEVEL_FIELD + ",");
        }

        @Test
        void defaultsToTheLibrarysOwnFallbackLevel() {
            // The screen's default and the code's fallback are two different values that have to
            // agree, or a player who never opens the settings screen gets one level and a player
            // who opens it and changes nothing gets another.
            assertThat(readLogLevelRow())
                .contains(",Radio," + KmLogging.DEFAULT_LEVEL.toString() + ",");
        }

        @Test
        void offersEveryLevelAndOnlyLevelsLog4jParses() {
            // An option log4j cannot parse resolves to the fallback, so the player picks a level and
            // silently gets a different one.
            assertThat(readLogLevelRow())
                .contains("\"" + String.join(", ", LOG_LEVEL_OPTIONS) + "\"");
        }
    }
}
