package kmlib.starsector.ui.highlight;

import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

class HighlightedParagraphTests {

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

        when(panelMock.addPara(anyString(), any(Color.class)))
            .thenReturn(labelMock);
        when(panelMock.addPara(anyString(), any(Color.class), any(Color.class), any(String[].class)))
            .thenReturn(labelMock);
    }

    @AfterEach
    void tearDown() {

        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class GetBaseColour {

        @Test
        void defaultConstructorPicksTextWhiteAsBaseColour() {

            var paragraph = new HighlightedParagraph("text");

            // Routes through VANILLA_TEXT.resolve() -> Misc.getTextColor(),
            // which the fake-installed proxy returns as Color.WHITE for
            // any colour slot.
            assertThat(paragraph.getBaseColour())
                .isEqualTo(Color.WHITE);
        }

        @Test
        void explicitBaseColourOverridesTheDefault() {

            var paragraph = new HighlightedParagraph("text", Color.GRAY);

            assertThat(paragraph.getBaseColour())
                .isEqualTo(Color.GRAY);
        }

        @Test
        void rejectsNullBaseColour() {

            assertThatThrownBy(() -> new HighlightedParagraph("text", (Color) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("baseColour");
        }
    }

    @Nested
    class GetHighlights {

        @Test
        void returnsADefensiveCopy() {

            var original = new Highlight("token", Color.RED);
            var paragraph = new HighlightedParagraph("text", original);
            var copy = paragraph.getHighlights();

            copy[0] = new Highlight("other", Color.WHITE);

            assertThat(paragraph.getHighlights()[0])
                .isSameAs(original);
        }
    }

    @Nested
    class AddToTextPanel {

        @Test
        void addToWithoutHighlightsUsesTheSimpleAddParaOverload() {

            var paragraph = new HighlightedParagraph("Plain text", Color.WHITE);

            paragraph.addTo(panelMock);

            verify(panelMock)
                .addPara("Plain text", Color.WHITE);

            // The highlight-bearing overload must not fire when there is
            // nothing to highlight - otherwise the engine would receive
            // an empty highlight array unnecessarily.
            verify(panelMock, never())
                .addPara(anyString(), any(Color.class), any(Color.class), any(String[].class));

            verify(panelMock, never())
                .setHighlightColorsInLastPara(any(Color[].class));
        }

        @Test
        void addToWithHighlightsFansThePairsIntoParallelArrays() {

            var paragraph = new HighlightedParagraph(
                "host: the Hegemony's pad: Landing Pad cost: 5,000 cr",
                Color.WHITE,
                new Highlight("the Hegemony's", Color.RED),
                new Highlight("Landing Pad", Color.YELLOW),
                new Highlight("5,000 cr", Color.YELLOW));

            paragraph.addTo(panelMock);

            var highlights = ArgumentCaptor.forClass(String[].class);

            verify(panelMock)
                .addPara(
                    eq("host: the Hegemony's pad: Landing Pad cost: 5,000 cr"),
                    eq(Color.WHITE),
                    eq(Color.RED), // first highlight's colour - safe fallback
                    highlights.capture());

            assertThat(highlights.getValue())
                .containsExactly("the Hegemony's", "Landing Pad", "5,000 cr");

            var colours = ArgumentCaptor.forClass(Color[].class);

            verify(panelMock)
                .setHighlightColorsInLastPara(colours.capture());

            assertThat(colours.getValue())
                .containsExactly(Color.RED, Color.YELLOW, Color.YELLOW);
        }
    }

    @Nested
    class AddToTooltip {

        @Test
        void withoutPadDefaultsToZero() {

            var tooltipMock = mock(TooltipMakerAPI.class);

            when(tooltipMock.addPara(anyString(), any(Color.class), eq(0f)))
                .thenReturn(labelMock);

            var paragraph = new HighlightedParagraph("text", Color.WHITE);

            paragraph.addTo(tooltipMock);

            verify(tooltipMock)
                .addPara("text", Color.WHITE, 0f);
        }

        @Test
        void delegatesAddParaThenAppliesHighlightsToTheReturnedLabel() {

            var tooltipMock = mock(TooltipMakerAPI.class);

            when(tooltipMock.addPara(anyString(), any(Color.class), eq(8f)))
                .thenReturn(labelMock);

            var paragraph = new HighlightedParagraph(
                "text",
                Color.WHITE,
                new Highlight("a", Color.RED),
                new Highlight("b", Color.YELLOW));

            var returned = paragraph.addTo(tooltipMock, 8f);

            assertThat(returned)
                .isSameAs(labelMock);

            verify(tooltipMock)
                .addPara("text", Color.WHITE, 8f);

            // Highlights flow through the returned label, not through a
            // panel-side setter the way TextPanelAPI handles them.
            var texts = ArgumentCaptor.forClass(String[].class);

            verify(labelMock)
                .setHighlight(texts.capture());

            assertThat(texts.getValue())
                .containsExactly("a", "b");

            var colours = ArgumentCaptor.forClass(Color[].class);

            verify(labelMock)
                .setHighlightColors(colours.capture());

            assertThat(colours.getValue())
                .containsExactly(Color.RED, Color.YELLOW);
        }
    }

    @Nested
    class ApplyTo {

        @Test
        void fansHighlightsIntoTheParallelLabelSetters() {

            var paragraph = new HighlightedParagraph(
                "text",
                new Highlight("a", Color.RED),
                new Highlight("b", Color.WHITE));

            paragraph.applyTo(labelMock);

            var texts = ArgumentCaptor.forClass(String[].class);

            verify(labelMock)
                .setHighlight(texts.capture());
            assertThat(texts.getValue())
                .containsExactly("a", "b");

            var colours = ArgumentCaptor.forClass(Color[].class);

            verify(labelMock)
                .setHighlightColors(colours.capture());
            assertThat(colours.getValue())
                .containsExactly(Color.RED, Color.WHITE);
        }

        @Test
        void handsTheRunsOverInTheOrderTheyStandInTheText() {

            // A translation whose sentence runs the other way puts the second slot first; the game finds
            // each run only after the previous one, so the order handed over must be the text's.
            var paragraph = new HighlightedParagraph(
                "beta, then alpha",
                new Highlight("alpha", Color.RED),
                new Highlight("beta", Color.WHITE));

            paragraph.applyTo(labelMock);

            var texts = ArgumentCaptor.forClass(String[].class);

            verify(labelMock)
                .setHighlight(texts.capture());

            assertThat(texts.getValue())
                .containsExactly("beta", "alpha");

            var colours = ArgumentCaptor.forClass(Color[].class);

            verify(labelMock)
                .setHighlightColors(colours.capture());

            assertThat(colours.getValue())
                .containsExactly(Color.WHITE, Color.RED);
        }

        @Test
        void aRunNamedTwiceTakesItsNextOccurrenceEachTime() {

            var paragraph = new HighlightedParagraph(
                "x a y a",
                new Highlight("y", Color.WHITE),
                new Highlight("a", Color.RED),
                new Highlight("a", Color.YELLOW));

            paragraph.applyTo(labelMock);

            var colours = ArgumentCaptor.forClass(Color[].class);

            verify(labelMock)
                .setHighlightColors(colours.capture());

            assertThat(colours.getValue())
                .containsExactly(Color.RED, Color.WHITE, Color.YELLOW);
        }

        @Test
        void aRunTheTextLacksGoesLast() {

            var paragraph = new HighlightedParagraph(
                "only beta here",
                new Highlight("alpha", Color.RED),
                new Highlight("beta", Color.WHITE));

            paragraph.applyTo(labelMock);

            var texts = ArgumentCaptor.forClass(String[].class);

            verify(labelMock)
                .setHighlight(texts.capture());
            assertThat(texts.getValue())
                .containsExactly("beta", "alpha");
        }

        @Test
        void isANoopForAParagraphWithNoHighlights() {

            var paragraph = new HighlightedParagraph("text");

            paragraph.applyTo(labelMock);

            verify(labelMock, never())
                .setHighlight(any(String[].class));
            verify(labelMock, never())
                .setHighlightColors(any(Color[].class));
        }
    }
}
