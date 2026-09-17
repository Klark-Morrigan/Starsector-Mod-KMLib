package kmlib.starsector.strings;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class StarsectorStringsTest {

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
}
