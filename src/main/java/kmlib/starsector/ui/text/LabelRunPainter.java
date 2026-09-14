package kmlib.starsector.ui.text;

/**
 * What draws a label's runs: one method per kind a run can be, each handed the run and the anchor its
 * label measured out for it. A surface implements this once and paints every kind of sentence through
 * it - words, a mark set among them, a name withheld - without any of the three learning where it ends
 * up drawn.
 *
 * <p>A role rather than a branch at each surface, because {@link LabelRun}'s seal promises that a layout
 * reading the kind stops compiling when a new one arrives, and a branch cannot keep that promise here: a
 * pattern switch over a sealed set needs a language level above the one this library targets, so an
 * {@code instanceof} chain is all a surface could write - and a chain falls through in silence. The run
 * that fell through would still be charged its width by the polymorphic measurement, leaving a gap in the
 * line exactly where it should have drawn. A method per kind moves that failure to the compiler: a kind
 * added to the seal adds a method here, and every surface that paints runs stops building until it says
 * what the new kind looks like.
 *
 * <p>Each method takes the run's own left edge and nothing else of the geometry. Where the line sits, how
 * tall it is, and what face it speaks in are the surface's own and were settled before the walk began, so
 * they belong to whatever implements this rather than travelling through the call.
 */
public interface LabelRunPainter {

    /**
     * Draws a small image hung on the line, squared off the line's own height.
     *
     * @param imageSpan the image run to draw
     * @param runX      the run's left edge, as its label measured it out
     */
    void paintImageSpan(ImageSpan imageSpan, float runX);

    /**
     * Draws a withheld name as the blocks standing in for its words, in the run's own colour.
     *
     * @param redactedSpan the withheld run to draw
     * @param runX         the run's left edge, as its label measured it out
     */
    void paintRedactedSpan(RedactedSpan redactedSpan, float runX);

    /**
     * Draws a stretch of text in the colour the run states.
     *
     * @param textSpan the run of text to draw
     * @param runX     the run's left edge, as its label measured it out
     */
    void paintTextSpan(TextSpan textSpan, float runX);
}
