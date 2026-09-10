package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.CampaignObjective;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import java.util.Objects;

/**
 * One built structure - a comm relay, a nav buoy, a sensor array - and the plain facts it carries
 * about itself.
 *
 * <p>Carries no location and no place in a listing, for the same reason a colony does not: where
 * the structure was found is the knowledge of whatever asked, and a second copy of it here would
 * be free to disagree with the first.
 *
 * <p>Every fact is read back off the entity rather than stored beside it, so a structure can never
 * report a state its own entity contradicts. That is also why the entity is required rather than
 * absorbed when absent: a structure with no entity has no holder, no type and no state, and each
 * of the reads below would have to invent an answer.
 *
 * <p>Says nothing about whether the player may be told any of it. Concealment over a structure is
 * a framing drawn for a purpose rather than a fact the sector holds, so it belongs to whatever is
 * drawing it.
 *
 * @param entity the entity the structure stands on; mandatory, there being no facts without one
 */
public record Structure(
    SectorEntityToken entity) {

    /** Refuses a structure with no entity, there being nothing for its own reads to answer off. */
    public Structure {
        Objects.requireNonNull(entity, "A structure needs an entity: it has no facts without one.");
    }

    /**
     * Who holds the structure, as a faction id.
     *
     * <p>Live and unconcealed. Vanilla's map item substitutes a neutral faction here when the
     * player is elsewhere, which is a display rule rather than a reading of the entity; a caller
     * wanting that concealment applies its own over this.
     *
     * @return the holding faction's id, or null where the entity names no faction
     */
    public String readHolderFactionId() {

        var faction = entity.getFaction();

        return faction == null
            ? null
            : faction.getId();
    }

    /**
     * What kind of thing this is in the game's own words - the entity spec's default name, which
     * is what an entity procgen has given a name of its own no longer says anywhere else.
     *
     * <p>The spec rather than the entity's name, because the name is the one that gets replaced.
     * Read off the entity's own spec rather than looked up by type through the settings, so the
     * answer needs nothing but the entity.
     *
     * @return the spec's default name, or null where the entity carries no custom spec
     */
    public String readTypeName() {

        var spec = entity.getCustomEntitySpec();

        return spec == null
            ? null
            : spec.getDefaultName();
    }

    /**
     * Whether the player has found this structure - the entity's own fact, and the base every
     * rule about showing one starts from.
     *
     * <p>There is no {@code isDiscovered}: an entity stops being discoverable once found, so the
     * inclusion gate is the negation of that flag.
     *
     * @return true when the entity has been found
     */
    public boolean isDiscoveredByPlayer() {
        return !entity.isDiscoverable();
    }

    /**
     * Whether this is one of the improvised variants rather than a full installation.
     *
     * <p>Read off the tag rather than off the entity id, so a mod's own makeshift variant reads
     * true without this having to know its id. The word is in the spec's default name as well,
     * which is what a caller printing a label uses; this is what a caller reasoning about the
     * structure uses.
     *
     * @return true when the entity is tagged makeshift
     */
    public boolean isMakeshift() {
        return entity.hasTag(Tags.MAKESHIFT);
    }

    /**
     * Whether the structure is out of action of its own accord - built broken by procgen, or
     * never repaired since.
     *
     * @return true when the entity carries the non-functional flag
     */
    public boolean isNonFunctional() {

        if (resolveObjectivePlugin() == null) {
            return false;
        }
        var memory = entity.getMemoryWithoutUpdate();

        return memory != null && memory.getBoolean(MemFlags.OBJECTIVE_NON_FUNCTIONAL);
    }

    /**
     * Whether the structure has been knocked out by a factory reset - a lapsing state somebody
     * did to it, as opposed to the standing one {@link #isNonFunctional} reports.
     *
     * @return true when the objective plugin reports a reset in force
     */
    public boolean isDisrupted() {

        var objective = resolveObjectivePlugin();

        return objective != null && Boolean.TRUE.equals(objective.isReset());
    }

    /**
     * Whether a sniffer is running on the structure - the state the player's own hack puts it in,
     * for as long as the hack lasts.
     *
     * @return true when the objective plugin reports a hack in force
     */
    public boolean isHacked() {

        var objective = resolveObjectivePlugin();

        return objective != null && Boolean.TRUE.equals(objective.isHacked());
    }

    // The entity's plugin where it is the one that owns the three state flags, and null
    // otherwise. All three go through it, the standing flag included: an entity tagged as a
    // structure but driven by somebody else's plugin is making none of those claims, so reading
    // its memory for a key that plugin never sets would be guessing rather than reading. Every
    // state read degrading to false there keeps a foreign structure listable instead of throwing.
    private CampaignObjective resolveObjectivePlugin() {

        var plugin = entity.getCustomPlugin();

        return plugin instanceof CampaignObjective objective
            ? objective
            : null;
    }
}
