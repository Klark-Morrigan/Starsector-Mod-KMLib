package kmlib.starsector.ui.text;

/**
 * One run of a label: a stretch of text in the colour it draws in, a small image set into the sentence
 * beside it, or a name withheld from it. A label is a list of these read as one line, so picking a word
 * out in another colour, setting a crest mid-sentence, and blocking a name out are the same move - a run
 * added to what is already there - rather than a colour feature, an icon feature, and a redaction feature
 * each surface has to grow separately.
 *
 * <p>A sealed set rather than an open interface, because these are all a sentence is made of and a
 * surface that reads the kind must stop compiling when a new one arrives. That promise is kept by
 * {@link LabelRunPainter} rather than by any branch: a run is handed to a painter that names every kind
 * ({@link #paintRun}), so a kind added here adds a method there and every surface that draws runs stops
 * building until it says what the new one looks like. It is deliberately narrower than
 * {@link kmlib.starsector.ui.widgets.RowSlot}: a tick box or a sort marker is a control's state shown in
 * a column of its own, and neither has any reading mid-word, so neither can be spelled here at all.
 *
 * <p>Runs flow where slots are columns - a run starts a word gap past where the one before it measured
 * out, while a slot is reserved at one width across a whole stack so the labels between them line up.
 * That is why the two stay separate sets even though both can hold an image: a crest in a leading slot
 * aligns down a stack of rows, and the same crest as a run sits wherever the sentence puts it.
 *
 * <p>Every run answers its own width for a line of a given height, because the arithmetic differs per
 * kind: a stretch of text is as wide as its glyphs measure, an image squares off the line so it sits
 * level with the words around it whatever face they draw in, and a withheld name is charged the
 * characters it stands for. A run also answers whether the gap in
 * front of it is spent at all ({@link #isJoinedToPreviousRun}). How wide that gap is and where the run
 * lands it does <em>not</em> answer - those are facts about a label rather than about any one run of
 * it, so they stay with {@link LabelRuns}.
 */
public sealed interface LabelRun
    permits ImageSpan, RedactedSpan, TextSpan {

    /**
     * What a run with nothing to draw is charged: no width, and no gap in front of it either. Named
     * here rather than per member so a blank run and an absent one cannot be charged two different
     * nothings, and so a caller comparing a measured width against it reads the intent rather than a
     * bare zero.
     */
    float NO_WIDTH = 0f;

    /**
     * How wide this run draws on a line {@code lineHeight} tall, before the gap the label adds in front
     * of it. Sized off the line height rather than a fixed step so an image run scales with the text it
     * is set among rather than towering over a small face or vanishing beside a large one.
     *
     * @param lineHeight   the height of the line the run sits on, in UI units
     * @param spanMeasurer the width measurement already bound to the face the label draws in, spent
     *                     only by a run whose width depends on its glyphs
     * @return the run's width, in UI units, and {@link #NO_WIDTH} for a run with nothing to draw
     */
    float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer);

    /**
     * Whether the run has anything worth drawing. One rule for both what a label charges the run and
     * what paints it, so a run that came out blank cannot be reserved room it never fills - and asked
     * of the run rather than measured, since a caller deciding that holds no face to measure against.
     *
     * @return true when the run draws something
     */
    boolean hasContent();

    /**
     * Draws this run at {@code runX} through {@code labelRunPainter}, by handing itself to the painter's
     * method for the kind it is. The run states which kind that is and the painter states what the kind
     * looks like, so neither has to test the other.
     *
     * <p>Dispatched rather than branched on so the seal above bites: a surface writes one method per kind
     * and a kind added later takes its method with it, where a branch would fall through in silence and
     * leave a gap on the line the size of the run it failed to draw.
     *
     * @param labelRunPainter what draws each kind of run on the surface the label is laid on
     * @param runX            the run's left edge, as its label measured it out
     */
    void paintRun(LabelRunPainter labelRunPainter, float runX);

    /**
     * Whether a single-line reading of the label ({@link LabelRuns#resolveLineText}) may leave this run
     * out without losing what it says. True by default, which covers both runs that are not left out at
     * all - a stretch of text puts its own glyphs in the line - and runs whose absence costs the line
     * nothing, an image among the words being decoration the sentence still reads without.
     *
     * <p>False is for a run whose whole meaning is in its own draw. Flattened, such a run does not come
     * out plainer - it comes out missing, and the surface that flattened it shows a line with a hole in it
     * that reads as though nothing had been there. That is worth refusing rather than documenting, which
     * is why the flattening asks this instead of dropping every run it cannot spell.
     *
     * @return true when the flattened line is whole without this run
     */
    default boolean canBeLeftOutOfLineText() {
        return true;
    }

    /**
     * Whether this run continues the one before it with no word space between them - so the two read as
     * one word drawn in more than one colour rather than as two words of a sentence.
     *
     * <p>For a label picking a stretch out of a name it does not own: gilding a word inside
     * <em>Abandoned-Station</em> splits the name into runs at the match, and a word space charged at
     * each split would have the label draw a name its subject is not called. Joined runs let the split
     * be made at exact character positions, so what is drawn is the text as its author spelled it
     * whatever the match happened to land beside.
     *
     * <p>A run of its own by default, which is what every run of an ordinary sentence is. The exception
     * has to be asked for, so a caller composing a line from independent parts cannot lose the spacing
     * between them by omission.
     *
     * @return true when the run butts against the one before it
     */
    default boolean isJoinedToPreviousRun() {
        return false;
    }
}
