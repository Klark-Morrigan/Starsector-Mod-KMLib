package kmlib.starsector.factions.relation;

import java.util.Collection;
import java.util.Optional;

/**
 * Which of several relations decides. A surface that has to report one relation where several
 * factions are in play answers with an end of the set - the most hostile of them, or the
 * friendliest - never an average, which would state a relation nobody actually holds.
 *
 * <p>One statement of that, taken over a {@link RelationDirection}, because the same question is
 * asked at two scales: of the factions found somewhere, to pick whose relation is reported, and of
 * a group's members, to say where the group as a whole stands. Written once, the two cannot take
 * opposite ends of the same set.
 *
 * <p>Ties fall to the first relation the collection yields. Equal readings are equally true, so the
 * rule is stated as iteration order rather than left to whichever one a fold happens to keep -
 * which makes a caller's own ordering the thing that settles them.
 *
 * <p>An empty set answers no relation rather than a reputation of nought. Nought is not a spare
 * value to signal absence with - it sits in the middle of the band the scale calls indifference,
 * which every faction nobody has any history with reads at - so a set with nobody in it and a set
 * of factions who do not care would arrive as the same fact. The same distinction a single pair
 * read draws by handing back no relation at all.
 *
 * <p>Stateless - every entry point is a static method, no instance needed.
 */
public final class RelationExtremes {

    private RelationExtremes() {
    }

    /**
     * Resolves the relation that decides, at whichever end of the scale the direction names.
     *
     * @param direction which end decides; nothing decides in no direction, so none is resolved
     * @param relations the relations to take an end of, in the order ties are settled by; entries
     *                  that hold no relation are passed over rather than competing at nought
     * @return the deciding relation, or none where there is nothing to decide between
     */
    public static Optional<FactionRelation> resolveExtreme(
            RelationDirection direction,
            Collection<FactionRelation> relations) {

        if (direction == null || relations == null) {
            return Optional.empty();
        }
        var decidingOrder = direction.resolveDecidingOrder();
        FactionRelation deciding = null;

        for (var relation : relations) {
            if (relation != null
                    && (deciding == null || decidingOrder.compare(relation, deciding) < 0)) {
                deciding = relation;
            }
        }
        return Optional.ofNullable(deciding);
    }

    /**
     * Resolves the friendliest of several relations.
     *
     * <p>The named form, for a caller whose direction is fixed rather than chosen.
     *
     * @param relations the relations to take the friendly end of
     * @return the friendliest relation, or none where there is nothing to decide between
     */
    public static Optional<FactionRelation> resolveBest(Collection<FactionRelation> relations) {

        return resolveExtreme(RelationDirection.FRIENDLIEST, relations);
    }

    /**
     * Resolves the most hostile of several relations.
     *
     * <p>The named form, for a caller whose direction is fixed rather than chosen - which is what
     * a group asked how it stands with somebody outside any direction of its own answers with: its
     * least friendly member, since safety is what a relation read on its own is being asked for.
     *
     * @param relations the relations to take the hostile end of
     * @return the most hostile relation, or none where there is nothing to decide between
     */
    public static Optional<FactionRelation> resolveWorst(Collection<FactionRelation> relations) {

        return resolveExtreme(RelationDirection.MOST_HOSTILE, relations);
    }
}
