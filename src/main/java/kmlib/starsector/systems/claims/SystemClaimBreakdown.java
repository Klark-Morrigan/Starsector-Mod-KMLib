package kmlib.starsector.systems.claims;

import java.util.List;

/**
 * Why a star system is claimed by whoever claims it: the resolved claimant, the memory-flag
 * override that can impose one outright, and the per-faction standings the contest was settled
 * on.
 *
 * <p>The claim mechanic publishes only its winner, which is enough to colour a map but not to
 * explain it. This carries the whole computation instead, so a caller can show the ranking
 * behind a claim - who else is present, how close they are, who is barred from claiming - while
 * still reading the winner out of that same pass. An explanation built from this can therefore
 * never disagree with the claim it explains.
 *
 * @param overrideFactionId the faction id imposed by the system's claiming-faction memory flag,
 *                          or null when no override is set. An override takes the system
 *                          outright, without being scored for it.
 * @param claimantFactionId the id of the faction claiming the system: the override when one is
 *                          set, else the top-scoring territorial faction, else null when
 *                          nobody claims it
 * @param scores            every faction whose presence can be scored, ordered by score
 *                          descending. Ties keep economy iteration order, which is the order
 *                          the mechanic itself settles a tied contest on.
 */
public record SystemClaimBreakdown(
        String overrideFactionId,
        String claimantFactionId,
        List<FactionClaimScore> scores) {

    /** An unreadable or wholly empty system: nobody present, nobody claiming, no override. */
    public static final SystemClaimBreakdown NONE =
        new SystemClaimBreakdown(
            null,
            null,
            List.of());

    /**
     * Takes an immutable copy of the standings, and reads a null list as an empty one, so a
     * breakdown handed around a render pass cannot change under its readers.
     */
    public SystemClaimBreakdown {
        scores = scores == null ? List.of() : List.copyOf(scores);
    }
}
