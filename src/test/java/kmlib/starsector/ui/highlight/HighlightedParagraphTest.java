package kmlib.starsector.ui.highlight;

import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import kmlib.starsector.testing.StarsectorSettingsFake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        // The default constructor resolves TEXT_WHITE via
        // StarsectorUiColorProvider, which needs SettingsAPI installed
        // before Misc.<clinit> runs.
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
    void defaultConstructorPicksTextWhiteAsBaseColor() {
        HighlightedParagraph paragraph = new HighlightedParagraph("text");

        // Routes through StarsectorUiColorProvider -> Misc.getTextColor(),
        // which the fake-installed proxy returns as Color.WHITE for
        // any color slot.
        assertThat(paragraph.getBaseColor()).isEqualTo(Color.WHITE);
    }

    @Test
    void explicitBaseColorOverridesTheDefault() {
        HighlightedParagraph paragraph = new HighlightedParagraph("text", Color.GRAY);

        assertThat(paragraph.getBaseColor()).isEqualTo(Color.GRAY);
    }

    @Test
    void rejectsNullBaseColor() {
        assertThatThrownBy(() -> new HighlightedParagraph("text", (Color) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("baseColor");
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
        HighlightedParagraph paragraph = new HighlightedParagraph("Plain text", Color.WHITE);

        paragraph.addTo(panel);

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

        paragraph.addTo(panel);

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
    void addToTooltipWithoutPadDefaultsToZero() {
        TooltipMakerAPI tooltip = mock(TooltipMakerAPI.class);
        when(tooltip.addPara(anyString(), any(Color.class), eq(0f))).thenReturn(label);
        HighlightedParagraph paragraph = new HighlightedParagraph("text", Color.WHITE);

        paragraph.addTo(tooltip);

        verify(tooltip).addPara("text", Color.WHITE, 0f);
    }

    @Test
    void addToTooltipDelegatesAddParaThenAppliesHighlightsToTheReturnedLabel() {
        TooltipMakerAPI tooltip = mock(TooltipMakerAPI.class);
        when(tooltip.addPara(anyString(), any(Color.class), eq(8f))).thenReturn(label);
        HighlightedParagraph paragraph = new HighlightedParagraph(
                "text",
                Color.WHITE,
                new Highlight("a", Color.RED),
                new Highlight("b", Color.YELLOW));

        LabelAPI returned = paragraph.addTo(tooltip, 8f);

        assertThat(returned).isSameAs(label);
        verify(tooltip).addPara("text", Color.WHITE, 8f);
        // Highlights flow through the returned label, not through a
        // panel-side setter the way TextPanelAPI handles them.
        ArgumentCaptor<String[]> texts = ArgumentCaptor.forClass(String[].class);
        verify(label).setHighlight(texts.capture());
        assertThat(texts.getValue()).containsExactly("a", "b");
        ArgumentCaptor<Color[]> colors = ArgumentCaptor.forClass(Color[].class);
        verify(label).setHighlightColors(colors.capture());
        assertThat(colors.getValue()).containsExactly(Color.RED, Color.YELLOW);
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
