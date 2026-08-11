package kmlib.starsector.ui.widgets.tooltip;

/**
 * The stretch a tooltip row rules between the end of its label and the start of its value - the hairline
 * that carries a reader's eye across the gap the value column opens, so a value read off the right edge
 * of a wide box can be traced back to the name it belongs to without the line being lost on the way.
 *
 * <p>A stretch rather than a rectangle, because where it sits vertically is not a fact the box's geometry
 * holds: the rule is set on the optical line of the words it runs between, which is a statement about the
 * face those words are drawn in and so is resolved by whatever holds a loaded face. What the layout can
 * answer - where the label stopped and where the value starts, both already measured to place the row -
 * is exactly what this carries, and no more.
 *
 * <p>How far it stands off the text at each end, and how short a run is not worth ruling at all, are
 * settled where the stretch is measured ({@link CursorTooltip}) rather than here: both are facts about the
 * columns a box laid out, and a value that arrived already measured cannot be asked to re-decide them.
 *
 * @param leftX  where the rule starts, in UI coordinates, already stood off the label's last glyph
 * @param rightX where the rule ends, in UI coordinates, already stood off the value's first glyph
 */
public record TooltipLeaderLine(
    float leftX,
    float rightX) {

    // The two ends of a rule that runs nowhere. Any pair meeting or crossing would do - the width is what
    // is read - but one shared value spells the absence once, so a row with no rule and a row whose
    // columns closed up are not two different nothings.
    private static final float NO_RUN = 0f;

    /**
     * The stretch a row rules nothing along - what a row with no value to point at carries, and what a
     * row whose label and value have closed up too far to be worth joining carries too.
     *
     * <p>A value rather than a null, so a row's placement holds a rule either way and nothing measuring
     * or painting one has to read a missing rule and an empty one as the same thing. It is the same
     * reading a run of no length gets: {@link #isRuled} answers from the run itself, so an absence
     * spelled this way and one that arrived as a degenerate pair cannot be treated differently.
     */
    public static final TooltipLeaderLine NONE = new TooltipLeaderLine(NO_RUN, NO_RUN);

    /**
     * Whether there is anything to rule here. Answered from the run's own length rather than from a flag
     * set beside it, so a stretch that came out empty and one stated as {@link #NONE} read alike and no
     * caller can hold a rule that says it draws while having nothing to draw across.
     *
     * @return true when the rule has length to draw
     */
    public boolean isRuled() {
        return rightX > leftX;
    }

    /**
     * How far the rule runs, in UI units - what a paint fills across, and nothing at all for a rule that
     * runs nowhere.
     *
     * @return the rule's length
     */
    public float computeWidth() {
        return isRuled()
            ? rightX - leftX
            : NO_RUN;
    }
}
