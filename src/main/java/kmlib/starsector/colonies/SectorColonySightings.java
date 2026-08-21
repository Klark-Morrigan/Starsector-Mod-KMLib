package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.markets.LocationMarkets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The sighting register kept in the sector's own memory: reading it, adding to it when the player
 * is somewhere, and shedding what it no longer describes.
 *
 * <p>Sector memory rather than anything of vanilla's, because vanilla keeps no such fact. It
 * serialises into the save alongside everything else there, so a sighting survives reload the way
 * the visit that produced it does.
 *
 * <p>Recording is done where the player is, one place at a time, rather than by a script sweeping
 * the sector. A sighting only ever changes when the player moves, so paying per day of a campaign
 * to answer a question that changes per journey would be paying for nothing on almost every day.
 *
 * <p>Only star systems are recorded. A colony standing anywhere else reads sighted whatever the
 * register says - there is no system to have been in - so recording out there would buy nothing,
 * and hyperspace holds by far the largest entity list in the sector to walk for it.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and
 * null-defensive like the rest of the library.
 */
public final class SectorColonySightings {

    // The save-serialised identity of the register. Stable once shipped: renaming it silently
    // drops every sighting in every existing save, and with them every colony a gate holds back.
    private static final String SIGHTINGS_KEY = "$kmlib_colony_sightings";

    private SectorColonySightings() {
        // utility class, no instances.
    }

    /**
     * Opens the sector's sighting register for reading.
     *
     * <p>Handed out as a lookup over the stored map rather than as a copy of it. The register is
     * read once per place a colony set is selected for, which is per system and per pass, and a
     * copy taken there would cost the whole sector's sightings to answer about one place.
     *
     * @param sector the sector whose memory holds the register; null - or one holding no
     *               register yet - yields {@link ColonySightings#NONE}
     * @return what the player has seen, by colony id; never null
     */
    public static ColonySightings readSightings(SectorAPI sector) {

        var storedSightings = readStoredSightings(sector);

        if (storedSightings == null) {
            return ColonySightings.NONE;
        }
        return storedSightings::get;
    }

    /**
     * Records every colony standing in {@code location} as seen there.
     *
     * <p>Every market present is recorded rather than the resolved colony set, because which
     * market wins a place is decided when the place is read and can change between one reading
     * and the next - a sighting held only for the winner of the day would be missing for the
     * market that succeeds it.
     *
     * @param sector   the sector whose memory holds the register; null is a no-op
     * @param location where the player is; anything that is not a star system is a no-op, a
     *                 colony outside one reading sighted whatever the register holds
     */
    public static void recordSightingsIn(SectorAPI sector, LocationAPI location) {

        if (!(location instanceof StarSystemAPI system)) {
            return;
        }
        var locationId = system.getId();
        var storedSightings = locationId == null ? null : openStoredSightings(sector);

        if (storedSightings == null) {
            return;
        }
        for (var market : readMarketsIn(sector, system)) {

            var colonyId = market.getId();

            if (colonyId != null) {
                storedSightings.put(colonyId, locationId);
            }
        }
    }

    /**
     * Drops every sighting whose colony is no longer anywhere in the sector.
     *
     * <p>Run once against a loaded save. A sighting outliving the colony it was about would go on
     * answering for whatever next took the id, which is a sighting the player never made.
     *
     * @param sector the sector to reconcile the register against; null is a no-op
     */
    public static void dropSightingsOfAbsentColonies(SectorAPI sector) {

        var storedSightings = readStoredSightings(sector);

        if (storedSightings == null || storedSightings.isEmpty()) {
            return;
        }
        storedSightings.keySet().retainAll(readColonyIdsIn(sector));
    }

    /**
     * Brings the register into step with a loaded save: sheds the sightings whose colonies have
     * gone, then records what stands where the player currently is.
     *
     * <p>The second half is what a load owes the register. A save opened in a system produces no
     * location change until the player leaves it, so without this the place they are looking at is
     * the one place the register has nothing to say about - and on the first load of a save written
     * before any sighting was ever made, that is the only place it could learn anything at all.
     *
     * <p>Nothing here seeds the register from vanilla's memory of which systems have been entered.
     * That fact says the player was once here, not that they saw what is here now, so importing it
     * would put back the very reading the register exists to replace.
     *
     * @param sector the sector to reconcile; null is a no-op
     */
    public static void reconcileWithLoadedSave(SectorAPI sector) {

        dropSightingsOfAbsentColonies(sector);

        if (sector != null) {
            recordSightingsIn(sector, sector.getCurrentLocation());
        }
    }

    // The markets present in one place, both listings together: the economy's, and the ones hung
    // on the place's own entities that it never registered. A sighting is about being seen rather
    // than about being registered, so the unlisted colonies - which is the shape a gated one is
    // most often built in - have to be walked too.
    private static List<MarketAPI> readMarketsIn(SectorAPI sector, LocationAPI location) {

        var markets = new ArrayList<MarketAPI>(LocationMarkets.readMarkets(sector, location));

        markets.addAll(LocationMarkets.readMarketsUnlistedByEconomy(sector, location));

        return markets;
    }

    // Every colony id the sector still holds, hyperspace included. Read the same two ways a
    // sighting is recorded, so the reconciliation cannot drop a colony merely for having been
    // found by a listing the walk here forgot about.
    private static Set<String> readColonyIdsIn(SectorAPI sector) {

        var colonyIds = new HashSet<String>();

        if (sector == null) {
            return colonyIds;
        }
        var systems = sector.getStarSystems();

        if (systems != null) {
            for (var system : systems) {
                collectColonyIdsIn(sector, system, colonyIds);
            }
        }
        collectColonyIdsIn(sector, sector.getHyperspace(), colonyIds);

        return colonyIds;
    }

    private static void collectColonyIdsIn(
            SectorAPI sector,
            LocationAPI location,
            Set<String> colonyIds) {

        for (var market : readMarketsIn(sector, location)) {

            var colonyId = market.getId();

            if (colonyId != null) {
                colonyIds.add(colonyId);
            }
        }
    }

    // The stored register as it stands, or null when there is nothing to read - no sector, no
    // memory, or a save written before any sighting was made. A caller reading null treats it as
    // "nothing has been seen", which is the answer an absent record deserves.
    private static Map<String, String> readStoredSightings(SectorAPI sector) {

        if (sector == null) {
            return null;
        }
        var memory = sector.getMemoryWithoutUpdate();

        if (memory == null || !memory.contains(SIGHTINGS_KEY)) {
            return null;
        }
        return castStoredSightings(memory.get(SIGHTINGS_KEY));
    }

    // The stored register, created and stored on first use. Writers go through here rather than
    // through the read above so that the first sighting of a campaign has somewhere to land.
    private static Map<String, String> openStoredSightings(SectorAPI sector) {

        var storedSightings = readStoredSightings(sector);

        if (storedSightings != null) {
            return storedSightings;
        }
        var memory = sector == null ? null : sector.getMemoryWithoutUpdate();

        if (memory == null) {
            return null;
        }
        var openedSightings = new HashMap<String, String>();

        memory.set(SIGHTINGS_KEY, openedSightings);

        return openedSightings;
    }

    // Sector memory is untyped, so what comes back out of it is taken on trust - and refused
    // outright when it is not a map at all, since another party writing over the key would
    // otherwise take down every read of the register rather than merely emptying it.
    @SuppressWarnings("unchecked")
    private static Map<String, String> castStoredSightings(Object storedValue) {

        if (!(storedValue instanceof Map)) {
            return null;
        }
        return (Map<String, String>) storedValue;
    }
}
