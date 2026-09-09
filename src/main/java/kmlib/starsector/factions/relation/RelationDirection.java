package kmlib.starsector.factions.relation;

import java.util.Comparator;
import java.util.function.Predicate;

/**
 * Which end of a set of relations is being asked about: the most hostile of them, or the
 * friendliest.
 *
 * <p>Carries the two readings a direction has, because they are one choice said twice and a caller
 * holding them apart is free to disagree with itself - taking the most hostile relation of a set
 * while sorting that same set by goodwill, or asking whether a set is uniform on one side of
 * indifference after picking its deciding member from the other. Bound together here, choosing the
 * direction settles both at once.
 *
 * <p>The first reading is the order relations decide in, most deciding first. It is what an end of
 * the set is taken over, and it is also the order a surface listing those relations wants, so the
 * line that decided what the player is looking at leads.
 *
 * <p>The second is the band the direction is about - ill will for {@link #MOST_HOSTILE}, goodwill
 * for {@link #FRIENDLIEST}. A caller asking whether a set is all of one kind asks the direction
 * rather than naming a threshold of its own, so one rule covers both ends instead of two rules that
 * happen to look alike.
 *
 * <p>Neither band covers indifference: a relation inside the neutral band satisfies no direction,
 * which is what keeps a set of wholly indifferent factions from reading as uniformly hostile in one
 * direction and uniformly friendly in the other.
 */
public enum RelationDirection {

    /** The worst relation of a set decides it, and its band is ill will. */
    MOST_HOSTILE(
        FactionRelation::isBelowNeutral,
        Comparator.<FactionRelation>comparingInt(FactionRelation::reputation)),

    /** The best relation of a set decides it, and its band is goodwill. */
    FRIENDLIEST(
        FactionRelation::isAboveNeutral,
        Comparator.<FactionRelation>comparingInt(FactionRelation::reputation).reversed());

    private final Predicate<FactionRelation> bandTest;
    private final Comparator<FactionRelation> decidingOrder;

    RelationDirection(
            Predicate<FactionRelation> bandTest,
            Comparator<FactionRelation> decidingOrder) {

        this.bandTest = bandTest;
        this.decidingOrder = decidingOrder;
    }

    /**
     * Whether a relation sits in the band this direction is about.
     *
     * <p>No relation is read of nothing, and membership is a positive claim: an absent relation
     * answers false rather than taking the benefit of the doubt, so nothing counts a relation it
     * never read as being of the kind it was looking for.
     *
     * @param relation the relation to place; nothing is placed of no relation
     * @return true where the relation falls on this direction's side of indifference
     */
    public boolean isWithinBand(FactionRelation relation) {

        return relation != null && bandTest.test(relation);
    }

    /**
     * The order relations decide in under this direction, the most deciding first.
     *
     * <p>Offered rather than kept private because taking an end of a set and listing that set are
     * the same question at two shapes, and a surface writing its own ordering would be free to
     * rank the relations against the one it reports as deciding.
     *
     * @return the order, ranking the relation that decides ahead of every other
     */
    public Comparator<FactionRelation> resolveDecidingOrder() {

        return decidingOrder;
    }
}
