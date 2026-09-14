package kmlib.starsector.ui.highlight;

import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import kmlib.starsector.ui.colour.StarsectorUiColour;

import java.awt.Color;
import java.util.Objects;

/**
 * Paragraph-shaped data: a string of text, a base colour, and the
 * per-substring {@link Highlight}s that should tint pieces of it. Two
 * render methods make the same data object work against the two
 * Starsector surfaces that consume highlights:
 * <ul>
 *   <li>{@link #addTo(TextPanelAPI)} creates a new paragraph on a
 *       {@code TextPanelAPI} and tints each highlight in one go.</li>
 *   <li>{@link #addTo(TooltipMakerAPI, float)} does the same for a
 *       tooltip element, where the highlight machinery lives on the
 *       returned label rather than on the panel.</li>
 *   <li>{@link #applyTo(LabelAPI)} sets the highlight tokens and
 *       colours on an already-rendered {@code LabelAPI}.</li>
 * </ul>
 *
 * <p>{@link #getBaseColour()} is non-null. The convenience constructor
 * that omits it defaults to {@link StarsectorUiColour#VANILLA_TEXT}, so
 * call sites only set a base colour when they want something different
 * (e.g. a grey section header).
 */
public final class HighlightedParagraph {
    private final String text;
    private final Color baseColour;
    private final Highlight[] highlights;

    public HighlightedParagraph(String text, Color baseColour, Highlight... highlights) {
        this.text = Objects.requireNonNull(text, "text");
        this.baseColour = Objects.requireNonNull(baseColour, "baseColour");
        this.highlights = highlights == null ? new Highlight[0] : highlights.clone();
        for (var i = 0; i < this.highlights.length; i++) {
            Objects.requireNonNull(this.highlights[i], "highlights[" + i + "]");
        }
    }

    /**
     * Defaults the base colour to {@link StarsectorUiColour#VANILLA_TEXT}
     * - the right pick for the vast majority of paragraphs, where only
     * individual highlights deviate from the default text colour.
     */
    public HighlightedParagraph(String text, Highlight... highlights) {
        this(text, StarsectorUiColour.VANILLA_TEXT.resolve(), highlights);
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

    /** Convenience projection - returns the highlight tokens as a
     *  flat string array. Useful for a caller that only cares about
     *  the text side of each pair - a log line, a summary. */
    public String[] getHighlightTexts() {
        String[] texts = new String[highlights.length];
        for (var i = 0; i < highlights.length; i++) {
            texts[i] = highlights[i].getText();
        }
        return texts;
    }

    /** Convenience projection - returns the highlight colours as a
     *  flat colour array, paired by index with {@link #getHighlightTexts()}. */
    public Color[] getHighlightColours() {
        Color[] colours = new Color[highlights.length];
        for (var i = 0; i < highlights.length; i++) {
            colours[i] = highlights[i].getColour();
        }
        return colours;
    }

    /**
     * Adds the paragraph to {@code panel}, tinting each highlight from
     * the paired {@link Highlight#getColour()}.
     */
    public LabelAPI addTo(TextPanelAPI panel) {
        Objects.requireNonNull(panel, "panel");

        if (highlights.length == 0) {
            return panel.addPara(text, baseColour);
        }

        String[] texts = new String[highlights.length];
        Color[] colours = new Color[highlights.length];
        for (var i = 0; i < highlights.length; i++) {
            texts[i] = highlights[i].getText();
            colours[i] = highlights[i].getColour();
        }

        // The third arg to addPara is the single fallback highlight
        // colour used when setHighlightColorsInLastPara does not cover
        // a slot. We always set every slot below, so the value only
        // matters as a defensive default - the first highlight's own
        // colour is the most sensible pick.
        var label = panel.addPara(text, baseColour, colours[0], texts);
        panel.setHighlightColorsInLastPara(colours);
        return label;
    }

    /**
     * Overload that adds the paragraph with no top padding - the
     * common case for tooltip rows that the caller is positioning
     * itself.
     */
    public LabelAPI addTo(TooltipMakerAPI tooltip) {
        return addTo(tooltip, 0f);
    }

    /**
     * Adds the paragraph to {@code tooltip} with the given top
     * padding, then routes highlights through the returned label.
     * Unlike {@link TextPanelAPI}, {@code TooltipMakerAPI} does not
     * expose a panel-side highlight-colour setter - the engine wants
     * highlights set on the {@link LabelAPI} that {@code addPara}
     * returns. {@link #applyTo(LabelAPI)} handles that side here.
     */
    public LabelAPI addTo(TooltipMakerAPI tooltip, float pad) {
        Objects.requireNonNull(tooltip, "tooltip");
        var label = tooltip.addPara(text, baseColour, pad);
        applyTo(label);
        return label;
    }

    /**
     * Tints an already-rendered label by setting its highlight tokens
     * and matching colours. The label's own text and base colour are
     * untouched - typical use is right after a renderer constructs the
     * label from {@link #getText()} and {@link #getBaseColour()}.
     */
    public void applyTo(LabelAPI label) {
        Objects.requireNonNull(label, "label");

        if (highlights.length == 0) {
            return;
        }

        String[] texts = new String[highlights.length];
        Color[] colours = new Color[highlights.length];
        for (var i = 0; i < highlights.length; i++) {
            texts[i] = highlights[i].getText();
            colours[i] = highlights[i].getColour();
        }
        label.setHighlight(texts);
        label.setHighlightColors(colours);
    }
}
