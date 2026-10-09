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

    /**
     * Whether two sets are friendly - true only where every pair drawn across them is.
     *
     * @param factionIds      one set, each faction asked as the first of a pair
     * @param otherFactionIds the other set
     * @return true where every pair is above neutral
     */
    public boolean areSetsFriendly(Set<String> factionIds, Set<String> otherFactionIds) {

        // Friendliness is a positive statement, so a set of nobody leaves nothing to make it of:
        // answered false rather than as the vacuous truth no pairs would report. The other side being
        // empty needs no guard of its own: nobody is friendly with nobody, so every member is
        // counted at odds below.
        return !factionIds.isEmpty()
            && countMembersAtOddsWith(factionIds, otherFactionIds) == 0;
    }

    /**
     * How many of one set's factions are not friendly with the whole of another.
     *
     * <p>Counted per member rather than per pair, so a faction at odds with two of the other set
     * counts once: what is asked is how much of this set the statement is false of.
     *
     * @param factionIds      the set being counted
     * @param otherFactionIds the set it is measured against, whole
     * @return how many members fall short of friendly
     */
    public int countMembersAtOddsWith(Set<String> factionIds, Set<String> otherFactionIds) {

        var atOddsCount = 0;

        for (var factionId : factionIds) {

            if (!isFactionFriendlyWithAll(factionId, otherFactionIds)) {
                atOddsCount++;
            }
        }
        return atOddsCount;
    }

    /**
     * How many of a set's factions one faction is not friendly with - the far side of the walk
     * {@link #countMembersAtOddsWith} counts the near side of.
     *
     * @param factionId       the faction being counted for
     * @param otherFactionIds the set it is measured against
     * @return how many of that set it falls short of friendly with
     */
    public int countFactionsAtOddsWith(String factionId, Set<String> otherFactionIds) {

        var atOddsCount = 0;

        for (var otherFactionId : otherFactionIds) {

            if (!factionFriendliness.test(factionId, otherFactionId)) {
                atOddsCount++;
            }
        }
        return atOddsCount;
    }

    // Whether one faction is above neutral with every one of a set - the reading every public answer
    // is composed from, so no two of them are worked out of pairs read in opposite directions. An
    // empty set is nobody to be friendly with, which is why this is not simply the count reaching
    // nought.
    private boolean isFactionFriendlyWithAll(String factionId, Set<String> otherFactionIds) {

        return !otherFactionIds.isEmpty()
            && countFactionsAtOddsWith(factionId, otherFactionIds) == 0;
    }
}
