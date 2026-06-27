package kmlib.starsector.ui.highlight;

import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;

import kmlib.starsector.testing.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HighlightedMessageTest {

    @BeforeEach
    void setUp() {
        // HighlightedParagraph's default-color path resolves Misc.getTextColor,
        // whose <clinit> reads from SettingsAPI. Install the fake first so
        // any paragraph constructed below stays inert.
        StarsectorSettingsFake.installSettings();
    }

    @AfterEach
    void tearDown() {
        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class Constructor {
        @Test
        void rejectsEmptyParagraphList() {
            assertThatThrownBy(() -> new HighlightedMessage())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejectsNullParagraph() {
            assertThatThrownBy(() -> new HighlightedMessage(
                    new HighlightedParagraph("ok"),
                    null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class GetLines {
        @Test
        void getLinesReturnsTheConstructorParagraphsInOrder() {
            var first = new HighlightedParagraph("first");
            var second = new HighlightedParagraph("second");

            var message = new HighlightedMessage(first, second);

            assertThat(message.getLines()).containsExactly(first, second);
        }
    }

    @Nested
    class ToMessageIntel {
        @Test
        void toMessageIntelEmitsOneLinePerParagraph() throws Exception {
            var message = new HighlightedMessage(
                    new HighlightedParagraph("first line"),
                    new HighlightedParagraph("second line"));

            var intel = message.toMessageIntel();

            var texts = readLineTexts(intel);
            assertThat(texts).containsExactly("first line", "second line");
        }

        @Test
        void toMessageIntelPropagatesHighlightsAndColorsPerParagraph() throws Exception {
            var paragraph = new HighlightedParagraph(
                    "construction complete on Test Prime.",
                    Color.GRAY,
                    Highlight.of("complete", Color.GREEN),
                    Highlight.of("Test Prime", Color.YELLOW));

            var intel = new HighlightedMessage(paragraph).toMessageIntel();

            var line = readLines(intel).get(0);
            assertThat(readLineField(line, "text"))
                    .isEqualTo("construction complete on Test Prime.");
            assertThat(readLineField(line, "color")).isEqualTo(Color.GRAY);
            assertThat((String[]) readLineField(line, "highlights"))
                    .containsExactly("complete", "Test Prime");
            assertThat((Color[]) readLineField(line, "colors"))
                    .containsExactly(Color.GREEN, Color.YELLOW);
        }
    }

    // MessageIntel keeps line data on a protected `lines` field of
    // protected `MessageLineData` instances; nothing public surfaces
    // the text or highlights once they are in the intel. Tests reach
    // in reflectively so we can pin the actual rendered shape, not
    // just "addLine was called N times".

    private static List<?> readLines(MessageIntel intel) throws Exception {
        var lines = MessageIntel.class.getDeclaredField("lines");
        lines.setAccessible(true);
        return (List<?>) lines.get(intel);
    }

    private static List<String> readLineTexts(MessageIntel intel) throws Exception {
        var lines = readLines(intel);
        var texts = new java.util.ArrayList<String>(lines.size());
        for (Object line : lines) {
            texts.add((String) readLineField(line, "text"));
        }
        return texts;
    }

    private static Object readLineField(Object line, String name) throws Exception {
        var field = line.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(line);
    }
}
