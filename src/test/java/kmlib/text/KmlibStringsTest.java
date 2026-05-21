package kmlib.text;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KmlibStringsTest {

    @Test
    void hasTextIsFalseOnNull() {
        assertThat(KmlibStrings.hasText(null)).isFalse();
    }

    @Test
    void hasTextIsFalseOnEmpty() {
        assertThat(KmlibStrings.hasText("")).isFalse();
    }

    @Test
    void hasTextIsFalseOnWhitespaceOnly() {
        // Mix of space, tab, newline so the per-char loop runs over
        // each whitespace flavour the predicate is meant to dismiss.
        assertThat(KmlibStrings.hasText(" \t\n ")).isFalse();
    }

    @Test
    void hasTextIsTrueOnPlainText() {
        assertThat(KmlibStrings.hasText("Independent")).isTrue();
    }

    @Test
    void hasTextIsTrueOnLeadingAndTrailingWhitespace() {
        // The predicate accepts any string containing at least one
        // non-whitespace character - it is not a "trim then check"
        // wrapper, so surrounding whitespace stays in the input.
        assertThat(KmlibStrings.hasText("  word  ")).isTrue();
    }
}
