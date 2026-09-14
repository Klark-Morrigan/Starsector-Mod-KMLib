package kmlib.starsector.factions.relation;

import com.fs.starfarer.api.campaign.RepLevel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Relations built the way the game would report them, for the cases that care about where a
 * reputation falls on the scale rather than about how one was read off a faction.
 *
 * <p>Shared so a band boundary is stated as one number in one place. Written per suite, the level a
 * boundary case lands in is derived independently each time, and a suite pinning the boundary at +9
 * beside one pinning it at +10 would look like disagreement about the rule rather than about the
 * arithmetic.
 *
 * <p>Every relation comes out with its level and its reputation agreeing, since both are derived
 * from the one relationship value. A sample whose level contradicted its number would pass a subject
 * reading either one and pin neither.
 */
final class RelationSamples {

    // Any shade will do wherever a sample is used: no subject taking these reads the colour, and the
    // ones that do care about a shade name their own.
    private static final Color RAMP_CENTRE = new Color(125, 125, 125);

    // The scale the engine's signed hundred is counted on, so a case may be written at the number a
    // player is shown rather than at the fraction the engine stores.
    private static final float REPUTATION_SCALE = 100f;

    private RelationSamples() {
    }

    /**
     * The relation the game would report for a raw relationship value, so a band boundary is stated
     * as the number a save actually holds rather than as the level it is expected to land in.
     */
    static FactionRelation createRelationAt(float relationship) {

        return new FactionRelation(
            RepLevel.getLevelFor(relationship),
            RepLevel.getRepInt(relationship),
            RAMP_CENTRE);
    }

    /**
     * Several relations at the signed hundred a player is shown, in a list an ordering case may sort
     * in place.
     */
    static List<FactionRelation> createRelationsAt(int... reputations) {

        var relations = new ArrayList<FactionRelation>();

        for (var reputation : reputations) {
            relations.add(createRelationAt(reputation / REPUTATION_SCALE));
        }
        return relations;
    }
}
