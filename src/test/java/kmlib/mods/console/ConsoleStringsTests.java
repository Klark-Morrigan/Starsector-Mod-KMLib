package kmlib.mods.console;

import kmlib.starsector.strings.StarsectorStrings;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins which part of a console string the read answers: the English copy after the marker where there is one,
 * else the whole string.
 */
final class ConsoleStringsTests {

    private static final String CATEGORY = "kmu";

    // "Opened" (U+5DF2 U+6253 U+5F00): a translation the console's fonts would draw as a row of question marks.
    private static final String CHINESE_OPENED = "已打开";

    // "%2$s received %1$s" (U+6536 U+5230 U+4E86 for "received"): slots numbered the other way round.
    private static final String CHINESE_RECEIVED_TEMPLATE = "%2$s 收到了 %1$s";

    @AfterEach
    void clearSettings() {
        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class Get {

        @Test
        void answersTheEnglishCopyAfterTheMarker() {

            installStrings(Map.of("console_opened", CHINESE_OPENED + "\n\n-- English --\nOpened."));

            assertThat(ConsoleStrings.get(CATEGORY, "console_opened"))
                .isEqualTo("Opened.");
        }

        @Test
        void answersAStringCarryingNoMarkerWhole() {

            installStrings(Map.of("console_opened", "Opened."));

            assertThat(ConsoleStrings.get(CATEGORY, "console_opened"))
                .isEqualTo("Opened.");
        }

        @Test
        void redactsAMissingString() {

            installStrings(Map.of());

            assertThat(ConsoleStrings.get(CATEGORY, "console_opened"))
                .isEqualTo(StarsectorStrings.REDACTED);
        }
    }

    @Nested
    class Format {

        @Test
        void fillsTheEnglishCopyAlone() {
            // The translation numbers its slots in its own order; only the copy is filled, so its order is used.
            installStrings(Map.of(
                "console_moved",
                CHINESE_RECEIVED_TEMPLATE + "\n\n-- English --\nMoved %s to %s."));

            assertThat(ConsoleStrings.format(CATEGORY, "console_moved", "Jangala", "Hegemony"))
                .isEqualTo("Moved Jangala to Hegemony.");
        }

        @Test
        void redactsATemplateTheArgsDoNotFill() {

            installStrings(Map.of("console_moved", "Moved %s to %s."));

            assertThat(ConsoleStrings.format(CATEGORY, "console_moved", "Jangala"))
                .isEqualTo(StarsectorStrings.REDACTED);
        }
    }

    private static void installStrings(Map<String, String> stringsByKey) {

        StarsectorSettingsFake.installSettings((category, key) -> CATEGORY.equals(category)
            ? stringsByKey.get(key)
            : null);
    }
}
