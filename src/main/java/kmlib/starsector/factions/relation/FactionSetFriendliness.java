package kmlib.starsector.factions.relation;

import com.fs.starfarer.api.campaign.SectorAPI;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Whether one set of factions is friendly with another: every faction of the one disposed above
 * neutral toward every faction of the other.
 *
 * <p>A disposition is a fact about two factions, and a statement about two groups of them needs a
 * rule for composing it. Unanimity rather than a majority or a lead member, because either of those
 * puts a statement over factions it is false of. A set this answers no for is not thereby hostile:
 * no single statement is true of it.
 *
 * <p>How much of a set is at odds is answered here too, off the same walk over the same pairs as the
 * yes-or-no, so a caller stating a count beside a verdict cannot draw one contradicting the other.
 *
 * <p>Every answer reads its pairs from the faction named first. A disposition is a fact one faction
 * holds about another, so a rule asking the same pair from either end could sort a faction one way
 * and count it the other wherever a disposition is not returned alike both ways round.
 *
 * <p>Pure rule: the sets arrive as plain IDs and the faction-level disposition as a predicate over a
 * pair of them, so it holds no game state and the live read binds where the two meet.
 */
public final class FactionSetFriendliness {

    private final BiPredicate<String, String> factionFriendliness;

    /**
     * @param factionFriendliness whether the first faction is disposed above neutral toward the
     *                            second
     */
    public FactionSetFriendliness(BiPredicate<String, String> factionFriendliness) {

        this.factionFriendliness = Objects.requireNonNull(
            factionFriendliness,
            "factionFriendliness");
    }

    /**
     * The rule over one sector's own relations, read per call through
     * {@link FactionRelations#createDispositionReader}.
     *
     * @param sector the sector the factions are looked up in; no sector makes no pair friendly
     * @return the rule bound to that sector
     */
    public static FactionSetFriendliness createForSector(SectorAPI sector) {
        return new FactionSetFriendliness(FactionRelations.createDispositionReader(sector));
    }
}
