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

    private TextPanelAPI panelMock;
    private LabelAPI labelMock;

    @BeforeEach
    void setUp() {
        // The default constructor calls VANILLA_TEXT.resolve(), which
        // routes through Misc. SettingsAPI must be installed before
        // Misc.<clinit> runs.
        StarsectorSettingsFake.installSettings();
        panelMock = mock(TextPanelAPI.class);
        labelMock = mock(LabelAPI.class);
        when(panelMock.addPara(anyString(), any(Color.class))).thenReturn(labelMock);
        when(panelMock.addPara(anyString(), any(Color.class), any(Color.class), any(String[].class)))
                .thenReturn(labelMock);
    }

    @AfterEach
    void tearDown() {
        StarsectorSettingsFake.clearSettings();
    }

    @Test
    void defaultConstructorPicksTextWhiteAsBaseColor() {
        var paragraph = new HighlightedParagraph("text");

        // Routes through VANILLA_TEXT.resolve() -> Misc.getTextColor(),
        // which the fake-installed proxy returns as Color.WHITE for
        // any color slot.
        assertThat(paragraph.getBaseColor()).isEqualTo(Color.WHITE);
    }

    @Test
    void explicitBaseColorOverridesTheDefault() {
        var paragraph = new HighlightedParagraph("text", Color.GRAY);

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
        var original = new Highlight("token", Color.RED);
        var paragraph = new HighlightedParagraph("text", original);

        var copy = paragraph.getHighlights();
        copy[0] = new Highlight("other", Color.WHITE);

        assertThat(paragraph.getHighlights()[0]).isSameAs(original);
    }

    @Test
    void addToWithoutHighlightsUsesTheSimpleAddParaOverload() {
        var paragraph = new HighlightedParagraph("Plain text", Color.WHITE);

        paragraph.addTo(panelMock);

        verify(panelMock).addPara("Plain text", Color.WHITE);
        // The highlight-bearing overload must not fire when there is
        // nothing to highlight - otherwise the engine would receive
        // an empty highlight array unnecessarily.
        verify(panelMock, never()).addPara(
                anyString(), any(Color.class), any(Color.class), any(String[].class));
        verify(panelMock, never()).setHighlightColorsInLastPara(any(Color[].class));
    }

    @Test
    void addToWithHighlightsFansThePairsIntoParallelArrays() {
        var paragraph = new HighlightedParagraph(
                "host: %s pad: Landing Pad cost: %s",
                Color.WHITE,
                new Highlight("the Hegemony's", Color.RED),
                new Highlight("Landing Pad", Color.YELLOW),
                new Highlight("5,000 cr", Color.YELLOW));

        paragraph.addTo(panelMock);

        var highlights = ArgumentCaptor.forClass(String[].class);
        verify(panelMock).addPara(
                eq("host: %s pad: Landing Pad cost: %s"),
                eq(Color.WHITE),
                eq(Color.RED), // first highlight's colour - safe fallback
                highlights.capture());
        assertThat(highlights.getValue()).containsExactly(
                "the Hegemony's", "Landing Pad", "5,000 cr");

        var colors = ArgumentCaptor.forClass(Color[].class);
        verify(panelMock).setHighlightColorsInLastPara(colors.capture());
        assertThat(colors.getValue()).containsExactly(Color.RED, Color.YELLOW, Color.YELLOW);
    }

    @Test
    void addToTooltipWithoutPadDefaultsToZero() {
        var tooltipMock = mock(TooltipMakerAPI.class);
        when(tooltipMock.addPara(anyString(), any(Color.class), eq(0f))).thenReturn(labelMock);
        var paragraph = new HighlightedParagraph("text", Color.WHITE);

        paragraph.addTo(tooltipMock);

        verify(tooltipMock).addPara("text", Color.WHITE, 0f);
    }

    @Test
    void addToTooltipDelegatesAddParaThenAppliesHighlightsToTheReturnedLabel() {
        var tooltipMock = mock(TooltipMakerAPI.class);
        when(tooltipMock.addPara(anyString(), any(Color.class), eq(8f))).thenReturn(labelMock);
        var paragraph = new HighlightedParagraph(
                "text",
                Color.WHITE,
                new Highlight("a", Color.RED),
                new Highlight("b", Color.YELLOW));

        var returned = paragraph.addTo(tooltipMock, 8f);

        assertThat(returned).isSameAs(labelMock);
        verify(tooltipMock).addPara("text", Color.WHITE, 8f);
        // Highlights flow through the returned label, not through a
        // panel-side setter the way TextPanelAPI handles them.
        var texts = ArgumentCaptor.forClass(String[].class);
        verify(labelMock).setHighlight(texts.capture());
        assertThat(texts.getValue()).containsExactly("a", "b");
        var colors = ArgumentCaptor.forClass(Color[].class);
        verify(labelMock).setHighlightColors(colors.capture());
        assertThat(colors.getValue()).containsExactly(Color.RED, Color.YELLOW);
    }

    @Test
    void applyToFansHighlightsIntoTheParallelLabelSetters() {
        var paragraph = new HighlightedParagraph(
                "text",
                new Highlight("a", Color.RED),
                new Highlight("b", Color.WHITE));

        paragraph.applyTo(labelMock);

        var texts = ArgumentCaptor.forClass(String[].class);
        verify(labelMock).setHighlight(texts.capture());
        assertThat(texts.getValue()).containsExactly("a", "b");

        var colors = ArgumentCaptor.forClass(Color[].class);
        verify(labelMock).setHighlightColors(colors.capture());
        assertThat(colors.getValue()).containsExactly(Color.RED, Color.WHITE);
    }

    @Test
    void applyToIsANoopForAParagraphWithNoHighlights() {
        var paragraph = new HighlightedParagraph("text");

        paragraph.applyTo(labelMock);

        verify(labelMock, never()).setHighlight(any(String[].class));
        verify(labelMock, never()).setHighlightColors(any(Color[].class));
    }
}
