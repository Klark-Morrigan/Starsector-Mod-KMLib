package kmlib.starsector.ui.widgets;

/**
 * What draws a row's flanking slot: one method per kind a slot can be, each handed the slot it is to
 * draw. A surface implements this once and paints every kind through it - a crest, a value, a value
 * picked out in shades of its own, a tick, a direction marker, an unfilled column - without any of them
 * learning where it ends up drawn.
 *
 * <p>A role rather than a branch at each surface, for the reason
 * {@link kmlib.starsector.ui.text.LabelRunPainter} is one: {@link RowSlot}'s seal promises that a
 * layout reading the kind stops compiling when a new one arrives, and a branch cannot keep that promise
 * here. A pattern switch over a sealed set needs a language level above the one this library targets,
 * so an {@code instanceof} chain is all a surface could write - and a chain falls through in silence.
 * The slot that fell through would still be charged its width by the polymorphic measurement, leaving a
 * reserved column with nothing painted into it. A method per kind moves that failure to the compiler: a
 * kind added to the seal adds a method here, and every surface that paints slots stops building until
 * it says what the new kind looks like.
 *
 * <p>A surface that draws nothing for a kind implements that method empty, which is how "no tick is
 * shown here" comes to be written down rather than merely left out.
 *
 * <p>No geometry travels through these calls. Where the row sits, how tall it is, and what face it
 * speaks in are the surface's own and were settled before the slot was reached, so they belong to
 * whatever implements this - one painter per row, bound to that row's placement.
 */
public interface RowSlotPainter {

    /**
     * Draws a row's unfilled column. Almost always nothing, the column being reserved by whichever rows
     * do fill it; a surface marking an empty cell states that here.
     */
    void paintEmptySlot();

    /**
     * Draws a small image hung on the row, squared off the row's own line.
     *
     * @param imageSlot the image slot to draw, carrying the tint it is multiplied by
     */
    void paintImageSlot(RowSlot.Image imageSlot);

    /**
     * Draws a value made of several runs, each in the colour its own run states.
     *
     * @param textRunsSlot the runs slot to draw, in reading order
     */
    void paintTextRunsSlot(RowSlot.TextRuns textRunsSlot);

    /**
     * Draws a value of one run, in the colour that run states.
     *
     * @param textSlot the text slot to draw
     */
    void paintTextSlot(RowSlot.Text textSlot);

    /**
     * Draws a tick box, ticked or clear as the slot states.
     *
     * @param tickSlot the tick slot to draw
     */
    void paintTickSlot(RowSlot.Tick tickSlot);

    /**
     * Draws a direction triangle, pointing as the slot states.
     *
     * @param triangleSlot the triangle slot to draw
     */
    void paintTriangleSlot(RowSlot.Triangle triangleSlot);
}
