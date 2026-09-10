package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;

import java.util.Objects;

/**
 * One custom entity no market hangs on - a derelict station, a habitat, a defensive platform, a
 * cargo pod - and the plain facts it carries about itself.
 *
 * <p>Named for what it can actually select, and for what it refuses. The interesting subset is the
 * man-made places a player has a word for, bigger than a pod and smaller than a colony, but where
 * that line falls is a judgement whatever is listing them draws rather than a fact any entity
 * carries - so a name claiming the subset would promise a selection this makes no attempt at. The
 * market is what it does insist on, and a bare entity noun would promise the whole family while
 * throwing on an ordinary part of it.
 *
 * <p>Refuses an entity carrying a market, which is the mirror of a colony insisting on one. Such
 * an entity is a colony: its owner is the market's and so is its discovery, and reading either off
 * the entity here would produce a second answer free to disagree with the colony's - which is also
 * what dropping the qualifier from the name would cost.
 *
 * <p>Carries no location and no place in a listing, for the same reason a colony does not: where
 * the entity was found is the knowledge of whatever asked, and a second copy of it here would be
 * free to disagree with the first.
 *
 * <p>Every fact is read back off the entity rather than stored beside it, so one can never report
 * a state its own entity contradicts. That is also why the entity is required rather than absorbed
 * when absent: with no entity there is no owner, no type and no discovery, and each of the reads
 * below would have to invent an answer.
 *
 * <p>Says nothing about whether the entity is worth listing, nor about what the player may be told
 * of it. Both are classifications drawn for a purpose rather than facts the sector holds, so they
 * belong to whatever is drawing them.
 *
 * @param entity the entity the facts are read off; mandatory, there being no facts without one
 */
public record MarketlessEntity(
    CustomCampaignEntityAPI entity) {

    /** Refuses an entity that is absent, or one a colony's market hangs on. */
    public MarketlessEntity {

        Objects.requireNonNull(
            entity, "A market-less entity needs an entity: it has no facts without one.");

        if (entity.getMarket() != null) {
            throw new IllegalArgumentException(
                "An entity carrying a market is a colony's, not a market-less entity: "
                    + entity.getCustomEntityType());
        }
    }

    /**
     * Who owns the entity, as a faction id.
     *
     * <p>Live and unconcealed. A caller withholding what the player could not plausibly know
     * applies its own concealment over this, there being no way back to the true owner once a
     * reading has substituted a neutral one.
     *
     * @return the owning faction's id, or null where the entity names no faction
     */
    public String readOwnerId() {
        return Entities.readFactionId(entity);
    }

    /**
     * Which kind of entity this is, as the id the game builds it from.
     *
     * <p>The id rather than the spec's name, because this is what a classification keyed on entity
     * type matches against; a caller printing a label reads the spec instead.
     *
     * @return the custom entity type id, or null where the entity carries none
     */
    public String readTypeId() {
        return entity.getCustomEntityType();
    }

    /**
     * Whether the player has found this entity - the entity's own fact, and the base every rule
     * about showing one starts from.
     *
     * @return true when the entity has been found
     */
    public boolean isDiscoveredByPlayer() {
        return Entities.isDiscoveredByPlayer(entity);
    }
}
