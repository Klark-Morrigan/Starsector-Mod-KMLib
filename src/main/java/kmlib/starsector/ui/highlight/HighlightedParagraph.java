package kmlib.starsector.ui.highlight;

import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import kmlib.starsector.ui.colour.StarsectorUiColour;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Paragraph-shaped data: a string of text, a base colour, and the per-substring {@link Highlight}s that tint pieces
 * of it. One data object renders to every Starsector surface that consumes highlights:
 * <ul>
 *   <li>{@link #addTo(TextPanelAPI)} adds a paragraph to a text panel.</li>
 *   <li>{@link #addTo(TooltipMakerAPI, float)} adds a paragraph to a tooltip and tints the label it returns.</li>
 *   <li>{@link #addTo(MessageIntel)} adds a line to a campaign message.</li>
 *   <li>{@link #applyTo(LabelAPI)} tints a label that is already rendered.</li>
 * </ul>
 *
 * <p>{@link #getBaseColour()} is non-null. The constructor that omits it defaults to
 * {@link StarsectorUiColour#VANILLA_TEXT}, so call sites only set a base colour when they want something different
 * (e.g. a grey section header).
 */
public final class HighlightedParagraph {

    private final String text;
    private final Color baseColour;
    private final Highlight[] highlights;
    private final String[] highlightTextsInTextOrder;
    private final Color[] highlightColoursInTextOrder;

    public HighlightedParagraph(String text, Color baseColour, Highlight... highlights) {

        this.text = Objects.requireNonNull(text, "text");
        this.baseColour = Objects.requireNonNull(baseColour, "baseColour");
        this.highlights = highlights == null
            ? new Highlight[0]
            : highlights.clone();

        for (var i = 0; i < this.highlights.length; i++) {
            Objects.requireNonNull(this.highlights[i], "highlights[" + i + "]");
        }

        var orderedHighlights = orderHighlightsByPositionInText(text, this.highlights);

        this.highlightTextsInTextOrder = listTexts(orderedHighlights);
        this.highlightColoursInTextOrder = listColours(orderedHighlights);
    }

    /**
     * Defaults the base colour to {@link StarsectorUiColour#VANILLA_TEXT} - the right pick for the vast majority of
     * paragraphs, where only individual highlights deviate from the default text colour.
     */
    public HighlightedParagraph(String text, Highlight... highlights) {
        this(text, StarsectorUiColour.VANILLA_TEXT.resolve(), highlights);
    }

    /**
     * A paragraph in the default text colour whose runs all share one highlight colour - the common shape of a line
     * that states a few figures, each picked out the same way.
     *
     * @param text            the paragraph text
     * @param highlightColour the colour every run is tinted with
     * @param highlightTexts  the runs to tint, as they stand in the text
     * @return the paragraph
     */
    public static HighlightedParagraph createUniformlyHighlightedParagraph(
            String text,
            Color highlightColour,
            String... highlightTexts) {

        Objects.requireNonNull(highlightColour, "highlightColour");

        // Null means no runs, as it does for the constructor's varargs.
        var highlights = highlightTexts == null
            ? new Highlight[0]
            : Arrays
                .stream(highlightTexts)
                .map(highlightText -> Highlight.of(highlightText, highlightColour))
                .toArray(Highlight[]::new);

        return new HighlightedParagraph(text, highlights);
    }

    public String getText() {
        return text;
    }

    public Color getBaseColour() {
        return baseColour;
    }

    /** Defensive copy so callers cannot mutate the paragraph after construction. */
    public Highlight[] getHighlights() {
        return highlights.clone();
    }

    /**
     * Adds the paragraph to {@code panel}, tinting each highlight from the paired {@link Highlight#getColour()}.
     */
    public LabelAPI addTo(TextPanelAPI panel) {

        Objects.requireNonNull(panel, "panel");

        if (highlights.length == 0) {
            return panel.addPara(text, baseColour);
        }

        // The third argument is only a fallback colour: every slot is set right after.
        var label = panel.addPara(
            text,
            baseColour,
            highlightColoursInTextOrder[0],
            highlightTextsInTextOrder.clone());

        panel.setHighlightColorsInLastPara(highlightColoursInTextOrder.clone());
        return label;
    }

    /**
     * Overload that adds the paragraph with no top padding - the common case for tooltip rows that the caller is
     * positioning itself.
     */
    public LabelAPI addTo(TooltipMakerAPI tooltip) {
        return addTo(tooltip, 0f);
    }

    /**
     * Adds the paragraph to {@code tooltip} with the given top padding, then tints the label it returns.
     * {@code TooltipMakerAPI}, unlike {@link TextPanelAPI}, has no panel-side highlight-colour setter.
     */
    public LabelAPI addTo(TooltipMakerAPI tooltip, float pad) {

        Objects.requireNonNull(tooltip, "tooltip");

        var label = tooltip.addPara(text, baseColour, pad);

        applyTo(label);
        return label;
    }

    /**
     * Adds the paragraph to {@code intel} as one line of the message.
     */
    public void addTo(MessageIntel intel) {

        Objects.requireNonNull(intel, "intel");

        if (highlights.length == 0) {
            intel.addLine(text, baseColour);
            return;
        }

        intel.addLine(text, baseColour, highlightTextsInTextOrder.clone(), highlightColoursInTextOrder.clone());
    }

    /**
     * Tints an already-rendered label by setting its highlight tokens and matching colours. The label's own text and
     * base colour are untouched - typical use is right after a renderer constructs the label from {@link #getText()}
     * and {@link #getBaseColour()}.
     */
    public void applyTo(LabelAPI label) {

        Objects.requireNonNull(label, "label");

        if (highlights.length == 0) {
            return;
        }

        label.setHighlight(highlightTextsInTextOrder.clone());
        label.setHighlightColors(highlightColoursInTextOrder.clone());
    }

    private static Color[] listColours(Highlight[] highlights) {

        return Arrays
            .stream(highlights)
            .map(Highlight::getColour)
            .toArray(Color[]::new);
    }

    private static String[] listTexts(Highlight[] highlights) {

        return Arrays
            .stream(highlights)
            .map(Highlight::getText)
            .toArray(String[]::new);
    }

    // The game searches for each run from where the previous one matched, so a run handed over after one
    // that stands later in the text is never found. A translation may order its slots differently from the
    // English it translates, so the runs are handed over in the order they stand in the text. A run named
    // twice takes its next occurrence each time, and a run the text lacks goes last, in the order given.
    private static Highlight[] orderHighlightsByPositionInText(String text, Highlight[] highlights) {

        var positions = new int[highlights.length];
        var searchStartsByRunText = new HashMap<String, Integer>();

        for (var i = 0; i < highlights.length; i++) {

            var runText = highlights[i].getText();
            var position = text.indexOf(runText, searchStartsByRunText.getOrDefault(runText, 0));

            positions[i] = position == -1
                ? Integer.MAX_VALUE
                : position;

            if (position != -1) {
                searchStartsByRunText.put(runText, position + runText.length());
            }
        }
        // Boxed indices, so the sort is stable and runs at the same position keep the order given.
        return IntStream
            .range(0, highlights.length)
            .boxed()
            .sorted(Comparator.comparingInt(index -> positions[index]))
            .map(index -> highlights[index])
            .toArray(Highlight[]::new);
    }
}
