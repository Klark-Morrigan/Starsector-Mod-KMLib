package kmlib.starsector.ui.highlight;

import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;

import kmlib.starsector.ui.color.StarsectorUiColor;
import kmlib.starsector.ui.color.StarsectorUiColorProvider;

import java.awt.Color;
import java.util.Objects;

/**
 * Paragraph-shaped data: a string of text, an optional base colour,
 * and the per-substring {@link Highlight}s that should tint pieces of
 * it. Two render methods make the same data object work against the
 * two Starsector surfaces that consume highlights:
 * <ul>
 *   <li>{@link #addTo(TextPanelAPI)} / {@link #addTo(TextPanelAPI, Color)}
 *       creates a new paragraph on a {@code TextPanelAPI} and tints
 *       each highlight in one go.</li>
 *   <li>{@link #applyTo(LabelAPI)} sets the highlight tokens and
 *       colours on an already-rendered {@code LabelAPI}.</li>
 * </ul>
 *
 * <p>{@link #getBaseColor()} returns {@code null} when the paragraph
 * wants the renderer's default base colour rather than overriding it.
 * The {@code addTo} overloads honour that intent by falling back to
 * the supplied default (or to {@link StarsectorUiColor#TEXT_WHITE}
 * when no default is provided).
 */
public final class HighlightedParagraph {
    private final String text;
    private final Color baseColor;
    private final Highlight[] highlights;

    public HighlightedParagraph(String text, Color baseColor, Highlight... highlights) {
        this.text = Objects.requireNonNull(text, "text");
        this.baseColor = baseColor;
        this.highlights = highlights == null ? new Highlight[0] : highlights.clone();
        for (int i = 0; i < this.highlights.length; i++) {
            Objects.requireNonNull(this.highlights[i], "highlights[" + i + "]");
        }
    }

    /** Overload for paragraphs that want the renderer's default base colour. */
    public HighlightedParagraph(String text, Highlight... highlights) {
        this(text, null, highlights);
    }

    public String getText() {
        return text;
    }

    /** Returns {@code null} when the paragraph wants the renderer's default. */
    public Color getBaseColor() {
        return baseColor;
    }

    /** Defensive copy so callers cannot mutate the paragraph after construction. */
    public Highlight[] getHighlights() {
        return highlights.clone();
    }

    /** Convenience projection - returns the highlight tokens as a
     *  flat string array. Useful for assertions that only care about
     *  the text side of each pair (tests, snapshot logging). */
    public String[] getHighlightTexts() {
        String[] texts = new String[highlights.length];
        for (int i = 0; i < highlights.length; i++) {
            texts[i] = highlights[i].getText();
        }
        return texts;
    }

    /** Convenience projection - returns the highlight colours as a
     *  flat colour array, paired by index with {@link #getHighlightTexts()}. */
    public Color[] getHighlightColors() {
        Color[] colors = new Color[highlights.length];
        for (int i = 0; i < highlights.length; i++) {
            colors[i] = highlights[i].getColor();
        }
        return colors;
    }

    /**
     * Adds the paragraph to {@code panel} using
     * {@link StarsectorUiColor#TEXT_WHITE} as the fallback base colour.
     * Convenience overload for the common case; pass an explicit colour
     * through the two-arg overload when the dialog uses a non-default
     * base.
     */
    public LabelAPI addTo(TextPanelAPI panel) {
        return addTo(panel, StarsectorUiColorProvider.get(StarsectorUiColor.TEXT_WHITE));
    }

    /**
     * Adds the paragraph to {@code panel}, tinting each highlight from
     * the paired {@link Highlight#getColor()} rather than from a single
     * default. The paragraph's own {@link #getBaseColor()} wins over
     * {@code defaultBaseColor} when present.
     */
    public LabelAPI addTo(TextPanelAPI panel, Color defaultBaseColor) {
        Objects.requireNonNull(panel, "panel");
        Objects.requireNonNull(defaultBaseColor, "defaultBaseColor");

        Color effectiveBase = baseColor != null ? baseColor : defaultBaseColor;

        if (highlights.length == 0) {
            return panel.addPara(text, effectiveBase);
        }

        String[] texts = new String[highlights.length];
        Color[] colors = new Color[highlights.length];
        for (int i = 0; i < highlights.length; i++) {
            texts[i] = highlights[i].getText();
            colors[i] = highlights[i].getColor();
        }

        // The third arg to addPara is the single fallback highlight
        // colour used when setHighlightColorsInLastPara does not cover
        // a slot. We always set every slot below, so the value only
        // matters as a defensive default - the first highlight's own
        // colour is the most sensible pick.
        LabelAPI label = panel.addPara(text, effectiveBase, colors[0], texts);
        panel.setHighlightColorsInLastPara(colors);
        return label;
    }

    /**
     * Tints an already-rendered label by setting its highlight tokens
     * and matching colours. The label's own text and base colour are
     * untouched - typical use is right after a renderer constructs the
     * label from {@link #getText()} and {@link #getBaseColor()}.
     */
    public void applyTo(LabelAPI label) {
        Objects.requireNonNull(label, "label");

        if (highlights.length == 0) {
            return;
        }

        String[] texts = new String[highlights.length];
        Color[] colors = new Color[highlights.length];
        for (int i = 0; i < highlights.length; i++) {
            texts[i] = highlights[i].getText();
            colors[i] = highlights[i].getColor();
        }
        label.setHighlight(texts);
        label.setHighlightColors(colors);
    }
}
