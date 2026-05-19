package kmlib.starsector.ui.highlight;

import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import kmlib.starsector.testing.StarsectorSettingsFake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HighlightedParagraphTest {

    private TextPanelAPI panel;
    private LabelAPI label;

    @BeforeEach
    void setUp() {
        // addTo's no-arg overload calls Misc.getTextColor(), which
        // needs SettingsAPI installed before Misc.<clinit> runs.
        StarsectorSettingsFake.installSettings();
        panel = mock(TextPanelAPI.class);
        label = mock(LabelAPI.class);
        when(panel.addPara(anyString(), any(Color.class))).thenReturn(label);
        when(panel.addPara(anyString(), any(Color.class), any(Color.class), any(String[].class)))
                .thenReturn(label);
    }

    @AfterEach
    void tearDown() {
        StarsectorSettingsFake.clearSettings();
    }

    @Test
    void getBaseColorIsNullByDefault() {
        HighlightedParagraph paragraph = new HighlightedParagraph("text");

        assertThat(paragraph.getBaseColor()).isNull();
    }

    @Test
    void getHighlightsReturnsADefensiveCopy() {
        Highlight original = new Highlight("token", Color.RED);
        HighlightedParagraph paragraph = new HighlightedParagraph("text", original);

        Highlight[] copy = paragraph.getHighlights();
        copy[0] = new Highlight("other", Color.WHITE);

        assertThat(paragraph.getHighlights()[0]).isSameAs(original);
    }

    @Test
    void addToWithoutHighlightsUsesTheSimpleAddParaOverload() {
        HighlightedParagraph paragraph = new HighlightedParagraph("Plain text");

        paragraph.addTo(panel, Color.WHITE);

        verify(panel).addPara("Plain text", Color.WHITE);
        // The highlight-bearing overload must not fire when there is
        // nothing to highlight - otherwise the engine would receive
        // an empty highlight array unnecessarily.
        verify(panel, never()).addPara(
                anyString(), any(Color.class), any(Color.class), any(String[].class));
        verify(panel, never()).setHighlightColorsInLastPara(any(Color[].class));
    }

    @Test
    void addToWithHighlightsFansThePairsIntoParallelArrays() {
        HighlightedParagraph paragraph = new HighlightedParagraph(
                "host: %s pad: Landing Pad cost: %s",
                Color.WHITE,
                new Highlight("the Hegemony's", Color.RED),
                new Highlight("Landing Pad", Color.YELLOW),
                new Highlight("5,000 cr", Color.YELLOW));

        paragraph.addTo(panel, Color.GRAY);

        ArgumentCaptor<String[]> highlights = ArgumentCaptor.forClass(String[].class);
        verify(panel).addPara(
                eq("host: %s pad: Landing Pad cost: %s"),
                eq(Color.WHITE),
                eq(Color.RED), // first highlight's colour - safe fallback
                highlights.capture());
        assertThat(highlights.getValue()).containsExactly(
                "the Hegemony's", "Landing Pad", "5,000 cr");

        ArgumentCaptor<Color[]> colors = ArgumentCaptor.forClass(Color[].class);
        verify(panel).setHighlightColorsInLastPara(colors.capture());
        assertThat(colors.getValue()).containsExactly(Color.RED, Color.YELLOW, Color.YELLOW);
    }

    @Test
    void addToUsesTheParagraphBaseColorWhenSet() {
        HighlightedParagraph paragraph = new HighlightedParagraph("text", Color.WHITE);

        paragraph.addTo(panel, Color.GRAY);

        // Paragraph's own base colour wins over the supplied default.
        verify(panel).addPara("text", Color.WHITE);
    }

    @Test
    void addToFallsBackToTheSuppliedDefaultBaseColor() {
        HighlightedParagraph paragraph = new HighlightedParagraph("text"); // baseColor null

        paragraph.addTo(panel, Color.GRAY);

        verify(panel).addPara("text", Color.GRAY);
    }

    @Test
    void applyToFansHighlightsIntoTheParallelLabelSetters() {
        HighlightedParagraph paragraph = new HighlightedParagraph(
                "text",
                new Highlight("a", Color.RED),
                new Highlight("b", Color.WHITE));

        paragraph.applyTo(label);

        ArgumentCaptor<String[]> texts = ArgumentCaptor.forClass(String[].class);
        verify(label).setHighlight(texts.capture());
        assertThat(texts.getValue()).containsExactly("a", "b");

        ArgumentCaptor<Color[]> colors = ArgumentCaptor.forClass(Color[].class);
        verify(label).setHighlightColors(colors.capture());
        assertThat(colors.getValue()).containsExactly(Color.RED, Color.WHITE);
    }

    @Test
    void applyToIsANoopForAParagraphWithNoHighlights() {
        HighlightedParagraph paragraph = new HighlightedParagraph("text");

        paragraph.applyTo(label);

        verify(label, never()).setHighlight(any(String[].class));
        verify(label, never()).setHighlightColors(any(Color[].class));
    }
}
