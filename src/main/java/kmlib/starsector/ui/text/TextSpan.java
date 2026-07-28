package kmlib.starsector.ui.text;

import kmlib.text.KmlibStrings;

import java.awt.Color;
import java.util.Objects;

/**
 * One run of text and the colour it draws in. The pair named once as a value, so a piece of content
 * carrying several runs carries several spans rather than a text field and a colour field per run
 * threaded side by side - two fields kept in step by their names alone are two fields that eventually
 * stop agreeing, and neither of them can be handed anywhere on its own.
 *
 * <p>A span carries no face and no size, the way {@link TextStyle} carries no text: the face comes from
 * the host's style for the kind of line the span sits on, so one style decides how a whole stack of
 * spans is spoken while each span says only what it says and in what colour. That split is what lets
 * content be authored by whatever knows the subject matter and never by whatever knows the typography.
 *
 * <p>Nothing-to-draw is spelled as blank text rather than as a null span or a null string, so a place
 * that draws nothing still holds a span with a colour and is measured, styled, and drawn by the same
 * path as one that draws something. {@link #hasText} is the single rule deciding which of the two a
 * span is, so no caller has to invent its own reading of an empty run.
 *
 * @param text   the run as its author wrote it, before any casing the host's style applies to it
 * @param colour the colour the run draws in, before any opacity fade the host applies
 */
public record TextSpan(String text, Color colour) {

    // How a span with nothing to draw spells its text. Held here so the blank spelling has one home
    // rather than being open-coded wherever an absent run is built.
    private static final String NO_TEXT = "";

    /**
     * Rejects nulls at construction, where the caller that built the span is still on the stack: a
     * null text or colour otherwise surfaces as a failure inside a measurement or a draw call, well
     * past the point that could say what was meant. Nothing-to-draw has its own spelling
     * ({@link #createBlank}), so a null is a mistake rather than an unstated absence.
     */
    public TextSpan {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(colour, "colour");
    }

    /**
     * Builds a span with nothing to draw, holding only the colour it would have drawn in. For a part
     * of a layout that is always present but not always filled: it keeps a colour rather than taking
     * null so that whatever measures or styles it needs no branch for the empty case.
     *
     * @param colour the colour the span would draw in were it filled
     * @return a span carrying no text
     */
    public static TextSpan createBlank(Color colour) {
        return new TextSpan(NO_TEXT, colour);
    }

    /**
     * Whether the span has anything worth drawing. Whitespace-only text reads as nothing, so a caller
     * assembling a run from parts and coming up empty gets the absence it should rather than a gap
     * reserved in front of no glyphs.
     *
     * @return true when the span carries text to draw
     */
    public boolean hasText() {
        return KmlibStrings.hasText(text);
    }
}
