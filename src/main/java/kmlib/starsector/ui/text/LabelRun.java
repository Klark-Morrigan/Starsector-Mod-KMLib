package kmlib.starsector.ui.text;

/**
 * One run of a label: a stretch of text in the colour it draws in, or a small image set into the
 * sentence beside it. A label is a list of these read as one line, so picking a word out in another
 * colour and setting a crest mid-sentence are the same move - a run added to what is already there -
 * rather than a colour feature and an icon feature each surface has to grow separately.
 *
 * <p>A sealed pair rather than an open interface, because these two are all a sentence is made of and
 * a layout that branches on the kind must stop compiling when a third arrives. It is deliberately
 * narrower than {@link kmlib.starsector.ui.widgets.RowSlot}: a tick box or a sort marker is a
 * control's state shown in a column of its own, and neither has any reading mid-word, so neither can
 * be spelled here at all.
 *
 * <p>Runs flow where slots are columns - a run starts a word gap past where the one before it measured
 * out, while a slot is reserved at one width across a whole stack so the labels between them line up.
 * That is why the two stay separate sets even though both can hold an image: a crest in a leading slot
 * aligns down a stack of rows, and the same crest as a run sits wherever the sentence puts it.
 *
 * <p>Every run answers its own width for a line of a given height, because the arithmetic differs per
 * kind: a stretch of text is as wide as its glyphs measure, while an image squares off the line so it
 * sits level with the words around it whatever face they draw in. What a run does <em>not</em> answer
 * is the gap in front of it or where it lands - those are facts about a label rather than about any one
 * run of it, so they stay with {@link LabelRuns}.
 */
public sealed interface LabelRun
    permits ImageSpan, TextSpan {

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
}
