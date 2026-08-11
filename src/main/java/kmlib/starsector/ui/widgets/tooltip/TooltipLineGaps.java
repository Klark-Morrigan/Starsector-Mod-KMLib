package kmlib.starsector.ui.widgets.tooltip;

import java.util.HashMap;
import java.util.Map;

/**
 * How far apart two lines of one block stand, by how deep the line above them sits. It is the spacing
 * counterpart to {@link TooltipStyle}'s level shrink - that answers how large a line at some depth
 * draws, this answers how much room is spent after it - so a box can tighten a run of like lines
 * without touching the size they are drawn at.
 *
 * <p>The gap belongs to the tier of the line just drawn rather than to the line about to be. A run of
 * lines at one depth is the thing a reader takes in as a unit, and it is the run that is tightened or
 * opened up; resolved from the line below instead, the first line of a run would take its own tier's
 * gap and shift the whole run away from the line it belongs under.
 *
 * <p>A map rather than a field per tier because a listing goes as deep as its subject matter does, and
 * a type naming a gap per level would run out at whichever depth its author imagined. A tier nobody
 * stated a gap for falls back on the base gap, so a box states only the runs it wants held apart from
 * the rest.
 *
 * @param baseGap                   the room spent after a line whose tier was never stated, in UI
 *                                  units - the spacing every box has before it asks for anything
 * @param gapsBySubordinationLevel  the room spent after a line at each stated tier, keyed by how many
 *                                  steps under the box's own voice that line stands, in UI units
 */
public record TooltipLineGaps(
    float baseGap,
    Map<Integer, Float> gapsBySubordinationLevel) {

    // What a box holds until it names a tier: nothing, so every line resolves the base gap and the
    // stack reads at one spacing. Named so the factory reads as "no tier is held apart yet" rather
    // than as an unexplained empty map.
    private static final Map<Integer, Float> NO_TIER_GAPS = Map.of();

    /**
     * Copies the stated tiers at construction, so a caller that goes on to edit the map it passed
     * cannot change the spacing of a box already built from it.
     */
    public TooltipLineGaps {
        gapsBySubordinationLevel = Map.copyOf(gapsBySubordinationLevel);
    }

    /**
     * Builds the plainest spacing there is: one gap after every line, whatever depth it sits at. What a
     * box wants beyond that it layers on with {@link #gappedAtLevel}, so a caller states only the tiers
     * that differ from the baseline.
     *
     * @param baseGap the room spent after a line, in UI units
     * @return the spacing holding every line that far apart
     */
    public static TooltipLineGaps createGaps(float baseGap) {
        return new TooltipLineGaps(baseGap, NO_TIER_GAPS);
    }

    /**
     * Returns a copy of this spacing that spends {@code gap} after a line standing
     * {@code subordinationLevel} steps under the box's own voice, leaving every other tier as it was.
     *
     * <p>Stating the same tier twice is the later statement winning rather than an error, since the
     * refinements are chained and a caller layering a player-set gap over a built-in one is naming a
     * tier that was already named on purpose.
     *
     * @param subordinationLevel how many steps under the box's own voice the line above the gap stands
     * @param gap                the room spent after a line at that tier, in UI units
     * @return an otherwise-identical spacing holding that tier's lines that far apart
     */
    public TooltipLineGaps gappedAtLevel(int subordinationLevel, float gap) {
        var gapsByLevel = new HashMap<>(gapsBySubordinationLevel);
        gapsByLevel.put(floorAtTheBoxsVoice(subordinationLevel), gap);
        return new TooltipLineGaps(baseGap, gapsByLevel);
    }

    /**
     * Answers how much room is spent after a line standing {@code subordinationLevel} steps under the
     * box's own voice - that tier's gap where one was stated, and the base gap where none was.
     *
     * @param subordinationLevel how many steps under the box's own voice the line above the gap stands
     * @return the room spent after that line, in UI units
     */
    public float resolveGapAfter(int subordinationLevel) {
        return gapsBySubordinationLevel.getOrDefault(
            floorAtTheBoxsVoice(subordinationLevel),
            baseGap);
    }

    // Reads a level above the box's own voice as speaking in it, at both ends: a row floors its own
    // level there, but these two are public and are handed whatever a caller holds. Normalised in one
    // place so a tier stated at a negative level is the same tier the lookup later finds, rather than
    // an entry nothing can reach.
    private static int floorAtTheBoxsVoice(int subordinationLevel) {
        return Math.max(TooltipRow.TableRow.NO_SUBORDINATION, subordinationLevel);
    }
}
