package kmlib.starsector.ui.highlight;

import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;

import java.util.List;
import java.util.Objects;

/**
 * Ordered list of {@link HighlightedParagraph}s rendered as a single
 * Starsector campaign message ({@link MessageIntel}). One paragraph
 * maps to one {@code MessageIntel.addLine(...)} call, preserving the
 * paragraph's base colour and per-substring highlights.
 *
 * <p>Lives alongside {@link HighlightedParagraph} because it is the
 * same "describe text + highlights once, render to a specific
 * Starsector surface" pattern - the paragraph type handles
 * {@code TextPanelAPI} / {@code TooltipMakerAPI} / {@code LabelAPI},
 * and this type extends the family to the campaign side panel via
 * {@link MessageIntel}. Keeping all four render targets in one
 * package means a new contributor finds them together when grepping
 * for "highlight".
 *
 * <p>Multi-line messages are useful when a single sentence would be
 * cramped: e.g. "facility complete" on line one, "income starts next
 * month" on line two. For the common single-paragraph case, the
 * varargs constructor stays as terse as a direct {@code MessageIntel}
 * call.
 *
 * <p>{@code MessageIntel}'s icon / sound / extra fields are not yet
 * exposed - the KM mod set has no callers that need them. When the
 * first caller does, add overloads (or a small builder) rather than
 * adding nullable parameters to the current ctor; the varargs shape
 * here is deliberately as light as possible.
 */
public final class HighlightedMessage {
    private final List<HighlightedParagraph> lines;

    public HighlightedMessage(HighlightedParagraph... lines) {
        Objects.requireNonNull(lines, "lines");
        if (lines.length == 0) {
            throw new IllegalArgumentException("HighlightedMessage requires at least one paragraph");
        }
        for (var i = 0; i < lines.length; i++) {
            Objects.requireNonNull(lines[i], "lines[" + i + "]");
        }
        // List.of is immutable and creates a defensive copy, so callers
        // cannot mutate the message after construction.
        this.lines = List.of(lines);
    }

    public List<HighlightedParagraph> getLines() {
        return lines;
    }

    /**
     * Materialises the message as a {@link MessageIntel} ready for
     * {@link com.fs.starfarer.api.campaign.CampaignUIAPI#addMessage}.
     * Each paragraph becomes one {@code addLine} call so multi-line
     * messages render with the same per-line spacing the engine uses
     * for vanilla intel notifications.
     */
    public MessageIntel toMessageIntel() {
        var intel = new MessageIntel();
        for (HighlightedParagraph line : lines) {
            // getHighlightTexts / getHighlightColors return zero-length
            // arrays when the paragraph carries no highlights, which
            // MessageIntel.addLine accepts as "no highlights on this
            // line" - no special-casing needed here.
            intel.addLine(
                line.getText(),
                line.getBaseColor(),
                line.getHighlightTexts(),
                line.getHighlightColors());
        }
        return intel;
    }
}
