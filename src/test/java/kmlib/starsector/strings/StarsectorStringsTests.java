package kmlib.starsector.strings;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import kmlib.testfixtures.logging.LogAppenderFake;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class StarsectorStringsTests {

    private static final String CATEGORY = "test_category";
    private static final String KEY = "test_key";

    @Nested
    class Get {

        @Test
        void returnsConfiguredStringFromSource() {

            var value = StarsectorStrings.get(
                CATEGORY,
                KEY,
                (category, key) -> "Configured");

            assertThat(value)
                .isEqualTo("Configured");
        }

        @Test
        void forwardsCategoryAndKeyToSource() {
            // Locks in that the source sees both arguments unchanged - the
            // facade is supposed to be a pass-through, not a category
            // rewriter.
            var seen = new String[2];

            StarsectorStrings.get(
                CATEGORY,
                KEY,
                (category, key) -> {
                    seen[0] = category;
                    seen[1] = key;
                    return "ok";
                });

            assertThat(seen)
                .containsExactly(CATEGORY, KEY);
        }

        @Test
        void redactsWhenSourceReturnsNull() {

            var value = StarsectorStrings.get(
                CATEGORY,
                KEY,
                (category, key) -> null);

            assertThat(value)
                .isEqualTo(StarsectorStrings.REDACTED);
        }

        @Test
        void redactsWhenSourceReturnsBlankValue() {

            var value = StarsectorStrings.get(
                CATEGORY,
                KEY,
                (category, key) -> "  ");

            assertThat(value)
                .isEqualTo(StarsectorStrings.REDACTED);
        }

        @Test
        void redactsWhenSourceThrows() {

            var value = StarsectorStrings.get(
                CATEGORY,
                KEY,
                (category, key) -> {
                    throw new IllegalStateException("missing settings");
                });

            assertThat(value)
                .isEqualTo(StarsectorStrings.REDACTED);
        }
    }

    @Nested
    class Format {

        @Test
        void formatsConfiguredStringUsingRootLocale() {

            var value = StarsectorStrings.format(
                CATEGORY,
                KEY,
                (category, key) -> "%d configured %d",
                3,
                2);

            assertThat(value)
                .isEqualTo("3 configured 2");
        }

        @Test
        void redactsWhenConfiguredFormatIsInvalid() {

            var value = StarsectorStrings.format(
                CATEGORY,
                KEY,
                (category, key) -> "%q",
                3,
                2);

            assertThat(value)
                .isEqualTo(StarsectorStrings.REDACTED);
        }
    }

    @Nested
    class ListCategoryStrings {

        @AfterEach
        void clearSettings() {
            Global.setSettings(null);
        }

        @Test
        void readsEveryNonBlankStringOfTheCategoryInTheMergedFile() throws Exception {
            // The merged file, as a translation mod replacing the category leaves it; any other category
            // is some other text, and a blank is nothing a caller could draw.
            installMergedStrings("{ \"test_category\": { \"first\": \"Political map\", \"blank\": \"\" },"
                + " \"other\": { \"elsewhere\": \"Another text\" } }");

            assertThat(StarsectorStrings.listCategoryStrings(CATEGORY))
                .containsExactly("Political map");
        }

        @Test
        void answersNothingAndSaysSoWhereTheFileCannotBeRead() throws Exception {
            // A caller acting on what it read would otherwise act on nothing with no trace of why.
            var settingsMock = mock(SettingsAPI.class);

            when(settingsMock.getMergedJSON("data/strings/strings.json"))
                .thenThrow(new IOException("missing"));

            Global.setSettings(settingsMock);

            assertThat(readCategoryLoggingTheFailure())
                .isEmpty();
        }

        @Test
        void answersNothingAndSaysSoWhereNoModDeclaresTheCategory() throws Exception {
            // A category spelt differently from the file's reads as absent, which is worth a line too.
            installMergedStrings("{ \"other\": { \"elsewhere\": \"Another text\" } }");

            assertThat(readCategoryLoggingTheFailure())
                .isEmpty();
        }

        // Stands the merged strings file up as the given JSON.
        private void installMergedStrings(String mergedJson) throws Exception {

            var settingsMock = mock(SettingsAPI.class);

            when(settingsMock.getMergedJSON("data/strings/strings.json"))
                .thenReturn(new JSONObject(mergedJson));

            Global.setSettings(settingsMock);
        }

        // Reads the test category and pins that the one line logged is the read failure's.
        private List<String> readCategoryLoggingTheFailure() {

            var strings = new ArrayList<String>();
            var logFake = LogAppenderFake.captureLogOf(
                StarsectorStrings.class,
                () -> strings.addAll(StarsectorStrings.listCategoryStrings(CATEGORY)));

            assertThat(logFake.getMessages())
                .containsExactly("The strings of category 'test_category' in the merged data/strings/strings.json"
                    + " could not be read");

            return strings;
        }
    }
}
