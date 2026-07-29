package kmlib.starsector.ui.highlight;

import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import kmlib.starsector.testing.StarsectorSettingsFake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.lang.reflect.Field;
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

    @Test
    void getLinesReturnsTheConstructorParagraphsInOrder() {
        HighlightedParagraph first = new HighlightedParagraph("first");
        HighlightedParagraph second = new HighlightedParagraph("second");

        HighlightedMessage message = new HighlightedMessage(first, second);

        assertThat(message.getLines()).containsExactly(first, second);
    }

    @Test
    void toMessageIntelEmitsOneLinePerParagraph() throws Exception {
        HighlightedMessage message = new HighlightedMessage(
                new HighlightedParagraph("first line"),
                new HighlightedParagraph("second line"));

        MessageIntel intel = message.toMessageIntel();

        List<String> texts = readLineTexts(intel);
        assertThat(texts).containsExactly("first line", "second line");
    }

    @Test
    void toMessageIntelPropagatesHighlightsAndColorsPerParagraph() throws Exception {
        HighlightedParagraph paragraph = new HighlightedParagraph(
                "construction complete on Test Prime.",
                Color.GRAY,
                Highlight.of("complete", Color.GREEN),
                Highlight.of("Test Prime", Color.YELLOW));

        MessageIntel intel = new HighlightedMessage(paragraph).toMessageIntel();

        Object line = readLines(intel).get(0);
        assertThat(readLineField(line, "text"))
                .isEqualTo("construction complete on Test Prime.");
        assertThat(readLineField(line, "color")).isEqualTo(Color.GRAY);
        assertThat((String[]) readLineField(line, "highlights"))
                .containsExactly("complete", "Test Prime");
        assertThat((Color[]) readLineField(line, "colors"))
                .containsExactly(Color.GREEN, Color.YELLOW);
    }

    // MessageIntel keeps line data on a protected `lines` field of
    // protected `MessageLineData` instances; nothing public surfaces
    // the text or highlights once they are in the intel. Tests reach
    // in reflectively so we can pin the actual rendered shape, not
    // just "addLine was called N times".

    private static List<?> readLines(MessageIntel intel) throws Exception {
        Field lines = MessageIntel.class.getDeclaredField("lines");
        lines.setAccessible(true);
        return (List<?>) lines.get(intel);
    }

    private static List<String> readLineTexts(MessageIntel intel) throws Exception {
        List<?> lines = readLines(intel);
        List<String> texts = new java.util.ArrayList<>(lines.size());
        for (Object line : lines) {
            texts.add((String) readLineField(line, "text"));
        }
        return texts;
    }

    private static Object readLineField(Object line, String name) throws Exception {
        Field field = line.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(line);
    }
}
