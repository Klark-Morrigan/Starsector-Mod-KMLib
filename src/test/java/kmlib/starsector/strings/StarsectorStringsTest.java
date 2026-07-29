package kmlib.starsector.strings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StarsectorStringsTest {

    private static final String CATEGORY = "test_category";
    private static final String KEY = "test_key";

    @Test
    void returnsConfiguredStringFromSource() {
        String value = StarsectorStrings.get(
                CATEGORY,
                KEY,
                (category, key) -> "Configured");

        assertThat(value).isEqualTo("Configured");
    }

    @Test
    void forwardsCategoryAndKeyToSource() {
        // Locks in that the source sees both arguments unchanged - the
        // facade is supposed to be a pass-through, not a category
        // rewriter.
        String[] seen = new String[2];

        StarsectorStrings.get(
                CATEGORY,
                KEY,
                (category, key) -> {
                    seen[0] = category;
                    seen[1] = key;
                    return "ok";
                });

        assertThat(seen).containsExactly(CATEGORY, KEY);
    }

    @Test
    void redactsWhenSourceReturnsNull() {
        String value = StarsectorStrings.get(
                CATEGORY,
                KEY,
                (category, key) -> null);

        assertThat(value).isEqualTo(StarsectorStrings.REDACTED);
    }

    @Test
    void redactsWhenSourceReturnsBlankValue() {
        String value = StarsectorStrings.get(
                CATEGORY,
                KEY,
                (category, key) -> "  ");

        assertThat(value).isEqualTo(StarsectorStrings.REDACTED);
    }

    @Test
    void redactsWhenSourceThrows() {
        String value = StarsectorStrings.get(
                CATEGORY,
                KEY,
                (category, key) -> {
                    throw new IllegalStateException("missing settings");
                });

        assertThat(value).isEqualTo(StarsectorStrings.REDACTED);
    }

    @Test
    void formatsConfiguredStringUsingRootLocale() {
        String value = StarsectorStrings.format(
                CATEGORY,
                KEY,
                (category, key) -> "%d configured %d",
                3,
                2);

        assertThat(value).isEqualTo("3 configured 2");
    }

    @Test
    void redactsWhenConfiguredFormatIsInvalid() {
        String value = StarsectorStrings.format(
                CATEGORY,
                KEY,
                (category, key) -> "%q",
                3,
                2);

        assertThat(value).isEqualTo(StarsectorStrings.REDACTED);
    }
}
