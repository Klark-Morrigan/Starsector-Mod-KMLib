package kmlib.starsector.ui.widgets.tooltip;

import kmlib.starsector.ui.layout.TooltipBoxLayout;
import kmlib.starsector.ui.text.TextStyle;

/**
 * The typography of one tooltip: the look each {@link TooltipLineStyle} draws in, and how far apart its
 * {@link TooltipSection blocks} stand. It is the other half of the row model's content-versus-look split
 * - rows carry which kind of line they are, this carries what each kind looks like - so a box restyles a
 * whole class of its lines at once and no row has to name a face.
 *
 * <p>It sits beside the row model rather than with any renderer because a tooltip is drawn on more than
 * one surface: a raw-GL box reads these styles into its own measuring and draws, while a vanilla-widget
 * tooltip reads the same two into {@code setTitleFont} / {@code setParaFont} and their colours. Both
 * surfaces want the same answer to "how does a heading look here", so the answer cannot live inside
 * either one. The parting between blocks is here for the same reason - every surface that stacks blocks
 * parts them, so it is not GL chrome the way an opacity or a border is.
 *
 * <p>Held as complete {@link TextStyle}s rather than as the parts they differ in, because the kinds
 * genuinely differ in more than one part at once - vanilla's headings are a different typeface at a
 * different size from its body text - and a caller wanting two of them identical simply passes one style
 * twice.
 *
 * @param headerStyle    the look of a line that heads the box
 * @param paragraphStyle the look of a line of the box's body
 * @param footnoteStyle  the look of a note at the box's foot; a box with nothing to note never resolves
 *                       it, so it defaults to the body look rather than being stated by every caller
 * @param levelShrink    how much smaller each step under the box's own voice draws than the step above
 *                       it, in UI units; zero draws every level at its kind's own size
 * @param lineGaps       how far apart two lines of one block stand, by the tier of the line above the
 *                       gap - the spacing spent everywhere no block boundary falls
 * @param sectionBreak   the room taken above a block for the one above it, in UI units - what parts two
 *                       blocks, where two lines of one block sit a plain line gap apart
 * @param groupBreak     the room taken above a block nested inside another for the nested block before
 *                       it, in UI units - narrower than the section break, since a run inside a block
 *                       should read as set apart from its neighbour rather than as a block of its own
 */
public record TooltipStyle(
    TextStyle headerStyle,
    TextStyle paragraphStyle,
    TextStyle footnoteStyle,
    float levelShrink,
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

    // What a box demotes a subordinate line by unless it asks for something: nothing at all, so a stack
    // of rows reads at one size until a box states that its levels should read as levels.
    private static final float NO_LEVEL_SHRINK = 0f;

    // How far apart two lines of one block stand unless a box says otherwise: the box geometry's own
    // line gap at every tier, so a style that never names a tier spaces its lines exactly as a box did
    // before there was anything to name.
    private static final TooltipLineGaps DEFAULT_LINE_GAPS =
        TooltipLineGaps.createGaps(TooltipBoxLayout.LINE_GAP);

    // The smallest a demoted line is allowed to reach. A deep enough stack would otherwise arrive at a
    // size no atlas can render legibly, and then at zero and below - so the shrink stops here and the
    // deepest levels share a size rather than vanishing.
    private static final double SMALLEST_SUBORDINATE_SIZE = 7d;

    /**
     * Builds the plainest typography there is: the two looks a box always has, with its lines a plain
     * gap apart, blocks and the groups nested in them parted by the standard breaks, and a note at the
     * foot set in the body look. What a box wants beyond that it layers on with {@link #partedBy},
     * {@link #groupedBy}, {@link #stackedAt}, or {@link #footnotedIn}, so a caller states only what
     * differs from the baseline.
     *
     * <p>The footnote defaults rather than being asked for because most boxes note nothing at all, and
     * one that does not never resolves the look - so demanding a third face here would have every caller
     * name a face for a line it will not draw.
     *
     * @param headerStyle    the look of a line that heads the box
     * @param paragraphStyle the look of a line of the box's body
     * @return the typography drawing those looks at the standard parting
     */
    public static TooltipStyle createStyle(TextStyle headerStyle, TextStyle paragraphStyle) {
        return new TooltipStyle(
            headerStyle,
            paragraphStyle,
            paragraphStyle,
            NO_LEVEL_SHRINK,
            DEFAULT_LINE_GAPS,
            DEFAULT_SECTION_BREAK,
            DEFAULT_GROUP_BREAK);
    }

    /**
     * Returns a copy of this typography setting notes at the box's foot in {@code footnoteStyle} - the
     * smaller, quieter face such a line is set apart from the content above it by.
     *
     * @param footnoteStyle the look of a note at the box's foot
     * @return an otherwise-identical typography setting its footnotes in that look
     */
    public TooltipStyle footnotedIn(TextStyle footnoteStyle) {
        return rebuildOnTheSameFaces(footnoteStyle, levelShrink, lineGaps, sectionBreak, groupBreak);
    }

    /**
     * Returns a copy of this typography drawing each step under its own voice {@code levelShrink}
     * smaller than the step above it, so how far a line stands under the box is legible from its size as
     * well as from its indent.
     *
     * <p>One step rather than a look per level, because that is what the rule actually is: a stack of
     * rows goes as deep as its subject matter, and a style naming a look per level would run out at
     * whichever depth its author happened to imagine. How far a line stands under the box is its own
     * ({@code TooltipRow.TableRow}), and it is not the same as its indent - a group's members are inset
     * without being demoted, and read at their group's size.
     *
     * @param levelShrink how much smaller each step draws than the one above it, in UI units
     * @return an otherwise-identical typography shrinking its levels by that much
     */
    public TooltipStyle shrunkPerLevel(float levelShrink) {
        return rebuildOnTheSameFaces(footnoteStyle, levelShrink, lineGaps, sectionBreak, groupBreak);
    }

    /**
     * Returns a copy of this typography stacking its lines at {@code lineGaps} - the room spent between
     * two lines of one block, which a box can hold at one width throughout or vary by how deep the line
     * above the gap sits.
     *
     * <p>Its own value type rather than a measurement per level held here, because the tiers a box
     * spaces are as many as its subject matter goes deep: a style naming a gap per level would run out
     * at whichever depth its author imagined, and every box would then restate the arithmetic of which
     * tier it meant.
     *
     * @param lineGaps how far apart two lines of one block stand, by the tier of the line above the gap
     * @return an otherwise-identical typography stacking its lines at those gaps
     */
    public TooltipStyle stackedAt(TooltipLineGaps lineGaps) {
        return rebuildOnTheSameFaces(footnoteStyle, levelShrink, lineGaps, sectionBreak, groupBreak);
    }

    /**
     * Returns a copy of this typography whose blocks stand {@code sectionBreak} apart - a tighter box
     * for a dense list, a wider one where the blocks answer separate questions.
     *
     * @param sectionBreak the room taken above a block for the one above it, in UI units
     * @return an otherwise-identical typography parting its blocks by that much
     */
    public TooltipStyle partedBy(float sectionBreak) {
        return rebuildOnTheSameFaces(footnoteStyle, levelShrink, lineGaps, sectionBreak, groupBreak);
    }

    /**
     * Returns a copy of this typography whose nested blocks stand {@code groupBreak} apart - the parting
     * spent inside a block, between one group of lines and the next.
     *
     * <p>Its own measurement rather than the section break reused, because the two boundaries are not the
     * same statement: one block ends where the box changes subject, while a group ends where one item of
     * the same list does. Drawn at one width they would read as equals, and the box would lose the shape
     * its blocks give it.
     *
     * @param groupBreak the room taken above a nested block for the nested block before it, in UI units
     * @return an otherwise-identical typography parting its nested blocks by that much
     */
    public TooltipStyle groupedBy(float groupBreak) {
        return rebuildOnTheSameFaces(footnoteStyle, levelShrink, lineGaps, sectionBreak, groupBreak);
    }

    /**
     * Answers what a line of {@code lineStyle} standing {@code subordinationLevel} steps under the box's
     * own voice draws in - its kind's look, shrunk once per step by whatever the box asked for.
     *
     * <p>The one lookup there is, since a row carries both facts. Resolved here rather than by the
     * renderer so the size a row is measured at and the size it is painted at come from one answer, and
     * so a box that asked for no shrink resolves exactly the look its kind names.
     *
     * <p>The kind alone is not answerable from outside, which is deliberate: a caller that resolved a
     * look without the level would silently get the box's own voice, and a demoted line measured at one
     * size and painted at another overlaps its own words. Making the level unskippable is what keeps the
     * two sides of that in step.
     *
     * @param lineStyle          the kind of line being laid out or drawn
     * @param subordinationLevel how many steps that line stands under the box's own voice; zero for a
     *                           line speaking in it
     * @return the look that line draws in
     */
    public TextStyle resolveStyleFor(TooltipLineStyle lineStyle, int subordinationLevel) {
        var lineStyleLook = resolveLineStyleLook(lineStyle);
        if (subordinationLevel <= TooltipRow.TableRow.NO_SUBORDINATION
                || levelShrink <= NO_LEVEL_SHRINK) {
            return lineStyleLook;
        }
        // Floored rather than allowed to run down: a stack deep enough would otherwise resolve a size no
        // atlas renders, and then a negative one - so the deepest levels share the smallest size instead
        // of disappearing.
        return lineStyleLook.sizedAt(Math.max(
            SMALLEST_SUBORDINATE_SIZE,
            lineStyleLook.face().size() - subordinationLevel * levelShrink));
    }

    /**
     * Answers how much room stands under a line that sits {@code subordinationLevel} steps under the
     * box's own voice, where no block boundary falls between it and the line below.
     *
     * <p>Delegated here rather than left to callers to read off the gaps, so that a surface stacking a
     * box asks its typography one question per line - the same shape as {@link #resolveStyleFor} - and
     * a box's spacing stays reachable from the one thing every surface already holds.
     *
     * @param subordinationLevel how many steps under the box's own voice the line above the gap stands
     * @return the room spent under that line, in UI units
     */
    public float resolveLineGapAfter(int subordinationLevel) {
        return lineGaps.resolveGapAfter(subordinationLevel);
    }

    // Rebuilds the typography around whatever a refinement changed, carrying the two looks a box always
    // has over untouched. Shared rather than each refinement restating the parts it leaves alone - which
    // is where a fourth part, and then a fifth, eventually gets restated wrongly in one of them.
    private TooltipStyle rebuildOnTheSameFaces(
            TextStyle footnoteStyle,
            float levelShrink,
            TooltipLineGaps lineGaps,
            float sectionBreak,
            float groupBreak) {

        return new TooltipStyle(
            headerStyle,
            paragraphStyle,
            footnoteStyle,
            levelShrink,
            lineGaps,
            sectionBreak,
            groupBreak);
    }

    // The look one kind of line names, before any demotion is applied to it - the half of the lookup
    // above that reads the styles, kept apart from the half that shrinks them so each is one decision.
    //
    // Matched value by value rather than by a map or an ordinal so that a new line style is a compile
    // error here - the one place that would otherwise silently hand it the body look and leave the new
    // kind indistinguishable from a paragraph on screen.
    private TextStyle resolveLineStyleLook(TooltipLineStyle lineStyle) {
        return switch (lineStyle) {
            case HEADER -> headerStyle;
            case PARAGRAPH -> paragraphStyle;
            case FOOTNOTE -> footnoteStyle;
        };
    }
}
