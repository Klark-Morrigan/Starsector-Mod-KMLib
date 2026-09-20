package kmlib.settings;

import kmlib.logging.KmLogging;
import kmlib.testfixtures.starsector.settings.LunaSettingsTable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static kmlib.testfixtures.starsector.settings.LunaSettingsTable.RADIO_FIELD_TYPE;
import static kmlib.testfixtures.starsector.settings.LunaSettingsTable.SETTINGS_CSV;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shipped settings table against the code that reads it, because every way it can drift
 * fails silently in play rather than loudly at build.
 *
 * <p>A field ID renamed on either side leaves the read finding nothing and falling back, so the
 * player's pick simply does nothing. A default that disagrees with the library's own means the level
 * before a player has ever touched the screen is not the level the code says it is. And an option
 * label is a stored key wearing the costume of a caption - tidying its wording resets that setting
 * for everyone who had chosen it - so the option list is held against the level names log4j will
 * actually parse.
 *
 * <p>Read through {@link LunaSettingsTable}, which is the reading this library offers every mod on
 * its conventions, rather than through a parser of this suite's own. The table is the file's shape
 * as LunaLib defines it, so the library holding its one row to some other reading of it would be
 * shipping a tool it did not use.
 *
 * <p>Reads the real data file rather than a fixture: a fixture would agree with the code while the
 * shipped table did not, which is precisely the failure.
 */
class LunaSettingsCsvIntegrationTest {

    private static final String LOG_LEVEL_FIELD = "kmlib_logLevel";

    // What every one of this library's field IDs starts with. One row carries it today; the prefix
    // is what keeps the reading exhaustive rather than aimed at that row.
    private static final String FIELD_ID_PREFIX = "kmlib_";

    // Every level log4j can parse, in the order the dropdown offers them. Stated here rather than
    // derived, since the CSV's job is to offer exactly these and nothing else.
    private static final List<String> LOG_LEVEL_OPTIONS =
        List.of("OFF", "ERROR", "WARN", "INFO", "DEBUG", "ALL");

    private static final LunaSettingsTable SETTINGS_TABLE =
        new LunaSettingsTable(SETTINGS_CSV, FIELD_ID_PREFIX);

    @Nested
    class LogLevelRow {

        @Test
        void shipsARowForTheFieldTheBindingReads() {
            // The binding asks LunaLib for this ID; a row under any other name is a setting the
            // player can change and the library will never read.
            assertThat(SETTINGS_TABLE.readDeclaredFieldIds())
                .as("field ids declared in %s", SETTINGS_CSV)
                .contains(LOG_LEVEL_FIELD);
        }

        @Test
        void defaultsToTheLibrarysOwnFallbackLevel() {
            // The screen's default and the code's fallback are two different values that have to
            // agree, or a player who never opens the settings screen gets one level and a player
            // who opens it and changes nothing gets another.
            assertThat(SETTINGS_TABLE.readDefaultValue(LOG_LEVEL_FIELD, RADIO_FIELD_TYPE))
                .as("default of %s in %s", LOG_LEVEL_FIELD, SETTINGS_CSV)
                .isEqualTo(KmLogging.DEFAULT_LEVEL.toString());
        }

        @Test
        void offersEveryLevelAndOnlyLevelsLog4jParses() {
            // An option log4j cannot parse resolves to the fallback, so the player picks a level and
            // silently gets a different one.
            assertThat(SETTINGS_TABLE.readOptions(LOG_LEVEL_FIELD))
                .as("options of %s in %s", LOG_LEVEL_FIELD, SETTINGS_CSV)
                .isEqualTo(LOG_LEVEL_OPTIONS);
        }
    }
}
