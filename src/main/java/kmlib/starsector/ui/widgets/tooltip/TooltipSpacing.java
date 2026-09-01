package kmlib.starsector.ui.widgets.tooltip;

import kmlib.starsector.ui.layout.TooltipBoxLayout;

/**
 * Every answer a tooltip has to "how much room stands here": between two lines of one block, between
 * two blocks, and between two blocks nested inside one. The three are one subject - what a box spends
 * on nothing - and they are read together by whatever stacks the content, so they travel as one value
 * rather than as three measurements a caller holds separately.
 *
 * <p>Grouped rather than left on {@link TooltipStyle} beside the faces because two of them are bare
 * floats of the same unit standing next to a third, {@code levelShrink}, which is also one. Four such
 * fields in a row can be handed over transposed and still compile, and a box laid out with its block
 * parting where its shrink belongs is wrong in a way only a screenshot shows. With the room in a value
 * of its own, the typography holds one shrink and one spacing, and that whole class of mix-up stops
 * compiling.
 *
 * <p>The line gap is a {@link TooltipLineGaps} rather than a float of its own because it is the one of
 * the three that varies by how deep the line above it sits; the two block partings are facts about
 * boundaries, which are the same width wherever they fall.
 *
 * @param lineGaps     how far apart two lines of one block stand, by the tier of the line above the gap
 * @param sectionBreak the room taken above a block for the one above it, in UI units - what parts two
 *                     blocks, where two lines of one block sit a plain line gap apart
 * @param groupBreak   the room taken above a block nested inside another for the nested block before
 *                     it, in UI units - narrower than the section break, since a run inside a block
 *                     should read as set apart from its neighbour rather than as a block of its own
 */
public record TooltipSpacing(
    TooltipLineGaps lineGaps,
    float sectionBreak,
    float groupBreak) {

    // How far apart blocks stand unless a box says otherwise: half a line of body text past the gap two
    // lines of one block already sit at, which reads as a parted block without looking like a dropped
    // line. Stated as one measurement rather than derived from whichever line happens to open a block,
    // so every parting in a box is the same width whatever sizes its blocks begin at.
    private static final float DEFAULT_SECTION_BREAK = 11.5f;

    // What parts two groups inside a block unless a box says otherwise: half the break between blocks,
    // so a nested run is visibly set off from its neighbour while the block boundary above it still
    // reads as the stronger of the two. A box wanting no inner parting at all asks for zero.
    private static final float DEFAULT_GROUP_BREAK = 5.75f;

    // How far apart two lines of one block stand unless a box says otherwise: the box geometry's own
    // line gap at every tier, so a box that never names a tier spaces its lines exactly as one did
    // before there was anything to name.
    private static final TooltipLineGaps DEFAULT_LINE_GAPS =
        TooltipLineGaps.createGaps(TooltipBoxLayout.LINE_GAP);

    /**
     * Builds the standard room a box spends: its lines a plain gap apart at every tier, its blocks at
     * the standard parting, and the groups nested in them at the narrower one. What a box wants beyond
     * that it layers on with {@link #stackedAt}, {@link #partedBy}, or {@link #groupedBy}, so a caller
     * states only what differs from the baseline.
     *
     * @return the standard spacing
     */
    public static TooltipSpacing createSpacing() {
        return new TooltipSpacing(DEFAULT_LINE_GAPS, DEFAULT_SECTION_BREAK, DEFAULT_GROUP_BREAK);
    }

    /**
     * Returns a copy of this spacing stacking the lines of a block at {@code lineGaps} - the room spent
     * where no block boundary falls, which is most of a listing.
     *
     * @param lineGaps how far apart two lines of one block stand, by the tier of the line above the gap
     * @return an otherwise-identical spacing stacking its lines at those gaps
     */
    public TooltipSpacing stackedAt(TooltipLineGaps lineGaps) {
        return new TooltipSpacing(lineGaps, sectionBreak, groupBreak);
    }

    /**
     * Returns a copy of this spacing whose blocks stand {@code sectionBreak} apart - a tighter box for a
     * dense list, a wider one where the blocks answer separate questions.
     *
     * @param sectionBreak the room taken above a block for the one above it, in UI units
     * @return an otherwise-identical spacing parting its blocks by that much
     */
    public TooltipSpacing partedBy(float sectionBreak) {
        return new TooltipSpacing(lineGaps, sectionBreak, groupBreak);
    }

    /**
     * Returns a copy of this spacing whose nested blocks stand {@code groupBreak} apart - the parting
     * spent inside a block, between one group of lines and the next.
     *
     * <p>Its own measurement rather than the section break reused, because the two boundaries are not
     * the same statement: one block ends where the box changes subject, while a group ends where one
     * item of the same list does. Drawn at one width they would read as equals, and the box would lose
     * the shape its blocks give it.
     *
     * @param groupBreak the room taken above a nested block for the nested block before it, in UI units
     * @return an otherwise-identical spacing parting its nested blocks by that much
     */
    public TooltipSpacing groupedBy(float groupBreak) {
        return new TooltipSpacing(lineGaps, sectionBreak, groupBreak);
    }

    /**
     * Returns a copy of this spacing keeping {@code gapShare} of the room it stands between two lines of
     * a block, at every tier it holds one for - what a box spends where it has been compressed to fit
     * the room it has.
     *
     * <p>Only the line gaps come down. The two block partings mark where the box changes subject, which
     * reads as a boundary at whatever size the lines around it are drawn; a compressed box that gave
     * them up would save a parting per block and read as one undifferentiated run for it. Every tier is
     * scaled by the one share, so a run a box holds tighter than the rest stays the tighter of the two.
     *
     * @param gapShare how much of each line gap the box keeps, 0..1
     * @return an otherwise-identical spacing standing its lines that much of their room apart
     */
    public TooltipSpacing tightenedBy(float gapShare) {
        return new TooltipSpacing(lineGaps.tightenedBy(gapShare), sectionBreak, groupBreak);
    }

    /**
     * Answers how much room stands under a line that sits {@code subordinationLevel} steps under the
     * box's own voice, where no block boundary falls between it and the line below.
     *
     * @param subordinationLevel how many steps under the box's own voice the line above the gap stands
     * @return the room spent under that line, in UI units
     */
    public float resolveLineGapAfter(int subordinationLevel) {
        return lineGaps.resolveGapAfter(subordinationLevel);
    }
}
