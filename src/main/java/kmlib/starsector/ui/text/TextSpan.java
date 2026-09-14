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
 * path as one that draws something. {@link #hasContent} is the single rule deciding which of the two a
 * span is, so no caller has to invent its own reading of an empty run.
 *
 * <p>It is one of the things a label's line can be made of ({@link LabelRun}), beside an image set among
 * the words and a name withheld from them. Being a member of that set rather than the whole of it is
 * what lets a caller write a crest into the middle of a sentence without the surfaces that lay labels
 * out learning a second way to compose one.
 *
 * @param text                  the run as its author wrote it, before any casing the host's style
 *                              applies to it
 * @param colour                the colour the run draws in, before any opacity fade the host applies
 * @param isJoinedToPreviousRun whether the run butts against the one before it rather than standing a
 *                              word space clear of it, for a stretch picked out of the middle of
 *                              somebody else's text ({@link LabelRun#isJoinedToPreviousRun})
 */
public record TextSpan(
    String text,
    Color colour,
    boolean isJoinedToPreviousRun) implements LabelRun {

    // How a span with nothing to draw spells its text. Held here so the blank spelling has one home
    // rather than being open-coded wherever an absent run is built.
    private static final String NO_TEXT = "";

    // The two readings a run can take of its own left edge - a word of its own, spaced from whatever
    // precedes it, or the tail of the word before it. Named so the constructor and the refinement
    // below say which they take rather than passing bare booleans a reader has to count off against
    // the components.
    private static final boolean IS_ITS_OWN_WORD = false;
    private static final boolean IS_JOINED_TO_PREVIOUS_RUN = true;

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
     * Builds the ordinary run: a word of the sentence, standing a word space clear of whatever
     * precedes it. Butting against the run before is asked for afterwards ({@link #joinsPreviousRun}),
     * so an author states what its run <em>is</em> rather than passing a spacing decision at every
     * call site that has none to make.
     *
     * @param text   the run as its author wrote it
     * @param colour the colour the run draws in
     */
    public TextSpan(String text, Color colour) {
        this(text, colour, IS_ITS_OWN_WORD);
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
     * Returns a copy of this span butting against the run before it, with none of the word space a
     * label parts its runs by - so the two draw as one word in two colours.
     *
     * <p>Stated as a refinement rather than at construction because it says nothing about the run
     * itself: the same words in the same colour read as a word of their own or as the tail of the one
     * before, and only whatever is composing the label knows which. A caller building a plain sentence
     * therefore never states a spacing decision, and one splitting a name at a match states it exactly
     * where the split is made.
     *
     * @return an otherwise-identical span that spends no gap in front of itself
     */
    public TextSpan joinsPreviousRun() {
        return new TextSpan(text, colour, IS_JOINED_TO_PREVIOUS_RUN);
    }

    /**
     * The width the span's glyphs measure in the look the measurement is bound to. A span that came out
     * blank is charged nothing, so a caller assembling a run from parts and coming up empty gets the
     * line it would have had without it rather than a gap held open in front of no glyphs.
     *
     * <p>The line height is not read: a run of text is as tall as the face it draws in, so what sizes
     * an image run against its line has no bearing on this one.
     */
    @Override
    public float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer) {
        if (!hasContent()) {
            return LabelRun.NO_WIDTH;
        }
        return (float) spanMeasurer.measureSpanWidth(this);
    }

    /**
     * Whether the span has anything worth drawing. Whitespace-only text reads as nothing, so a caller
     * assembling a run from parts and coming up empty gets the absence it should rather than a gap
     * reserved in front of no glyphs.
     *
     * @return true when the span carries text to draw
     */
    @Override
    public boolean hasContent() {
        return KmlibStrings.hasText(text);
    }

    /**
     * Hands this run to {@code labelRunPainter} as the kind it is, so a surface paints a stretch of text
     * by saying what one looks like rather than by testing what the run happens to be.
     */
    @Override
    public void paintRun(LabelRunPainter labelRunPainter, float runX) {
        labelRunPainter.paintTextSpan(this, runX);
    }
}
