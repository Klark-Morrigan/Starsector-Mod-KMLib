package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import kmlib.math.geometry.Points;
import kmlib.starsector.geometry.StarsectorPoints;
import kmlib.starsector.markets.LocationMarkets;
import kmlib.text.KmlibStrings;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Queries over one star system: what it is called, what is in it, and whether it can be
 * reached.
 *
 * <p>Every read here takes the system it is about. Questions posed of the sector's whole set of
 * systems - indexing them, locating them, finding the player's - are {@link SectorStarSystems}'
 * instead, because the two are asked by different callers at different moments and a class
 * answering both grows with no rule for what else belongs in it.
 *
 * <p>Final class with a private constructor: pure-function utility, no
 * instance state. Matches {@link kmlib.starsector.scripts.SectorScripts}'s
 * shape, and is null-sector defensive like the rest of the library.
 */
public final class StarSystems {

    private StarSystems() {
        // utility class, no instances.
    }

    /**
     * The name a surface titles {@code system} by - vanilla's own composed name, less a type word
     * the name already ends on.
     *
     * <p>Vanilla composes a system's name as its base name plus its type, so a system named after
     * its star ends the first on the word the second opens with: {@code getName()} over Penelope's
     * Star reads "Penelope's Star Star System". Dropping the type outright would answer "Penelope's
     * Star", losing the one word telling a reader the subject is the system rather than the star in
     * it; dropping the repetition keeps both, which is what a person writing the title by hand would
     * have put.
     *
     * <p>Only the words past the name proper are ever in reach. Left unconstrained the scan cannot
     * tell a name's own repetition from the collision vanilla's composition created, so a system
     * named "Ko Ko" would be titled "Ko Star System" - a name no longer anyone's. Protecting the
     * name proper makes that impossible by construction rather than by a rule about where a repeat
     * is allowed to fall, which is also what holds the read good if vanilla ever composes a longer
     * type.
     *
     * <p>Where the name proper is not what the name opens with, the name is answered untouched.
     * {@code getNameWithNoType} strips its type word globally, so a nebula carrying that word in the
     * middle of its base name comes back as something the name does not begin with - and the word
     * count would then protect the wrong words. A title keeping vanilla's own wart is worse than a
     * title; a title with a word cut out of the middle of a name is wrong.
     *
     * @param system the system to name; null yields a blank name, there being no name to read
     * @return the system's name with a repeated type word dropped, or vanilla's own name where
     *         nothing repeats or the name proper cannot be told apart within it
     */
    public static String readDisplayName(StarSystemAPI system) {
        var name = system == null ? null : system.getName();
        if (name == null) {
            return "";
        }
        var nameProper = system.getNameWithNoType();
        if (!KmlibStrings.hasText(nameProper) || !name.startsWith(nameProper)) {
            return name;
        }
        // The name proper is what this system is called and the words past it are what vanilla
        // appended, so protecting exactly that many words puts only the type in reach - which is
        // what makes a general repeat scan safe over a name.
        return KmlibStrings.dropAdjacentRepeatedWords(
            name,
            KmlibStrings.splitIntoWords(nameProper).size());
    }

    /**
     * Every star in {@code system}, in the system's planet order - the set a
     * caller must disambiguate between when "the center" alone is ambiguous.
     * A single-star system has one unambiguous center to orbit; binary and
     * trinary systems orbit a shared, invisible center rather than any one
     * star, so a caller wanting a concrete focus has to pick a star itself.
     *
     * @param system the star system to read; null yields an empty list
     * @return each {@link PlanetAPI} in {@code system} for which
     *         {@link PlanetAPI#isStar()} holds, empty when none
     */
    public static List<PlanetAPI> getStars(StarSystemAPI system) {
        var stars = new ArrayList<PlanetAPI>();
        if (system == null) {
            return stars;
        }
        for (var planet : system.getPlanets()) {
            if (planet.isStar()) {
                stars.add(planet);
            }
        }
        return stars;
    }

    /**
     * The markets the economy places in {@code system}, in the order it lists them.
     *
     * <p>The star-system half of {@link LocationMarkets#readMarkets}, which owns the search and
     * documents what it guarantees - economy order in particular, which a caller mirroring
     * vanilla's claim mechanic depends on. Kept as its own read so a star system is asked for
     * its markets by a method that accepts nothing else, rather than through a location read a
     * caller has to know a system may be passed to.
     *
     * @param sector the sector whose economy is read; null (or a null economy) yields an empty
     *               list
     * @param system the system to read; null yields an empty list
     * @return the system's markets in economy order; never null
     */
    public static List<MarketAPI> readMarkets(SectorAPI sector, StarSystemAPI system) {
        return LocationMarkets.readMarkets(sector, system);
    }

    /**
     * The markets hung on {@code system}'s own entities that the economy does not list, in
     * entity order.
     *
     * <p>The star-system half of {@link LocationMarkets#readMarketsUnlistedByEconomy}, which
     * owns the search and documents why the markets the economy leaves out are answered apart
     * from the ones it lists.
     *
     * @param sector the sector whose economy is read; null (or a null economy) yields an empty
     *               list
     * @param system the system to read; null yields an empty list
     * @return the system's markets the economy does not list, in entity order; never null
     */
    public static List<MarketAPI> readMarketsUnlistedByEconomy(
            SectorAPI sector,
            StarSystemAPI system) {

        return LocationMarkets.readMarketsUnlistedByEconomy(sector, system);
    }

    /**
     * The market in {@code system} that {@code eligibility} admits whose body sits closest to
     * {@code from}.
     *
     * <p>The star-system half of {@link LocationMarkets#findNearestMarket}, which owns the
     * search and documents what it ranks, what it passes over, and how it settles a tie. Kept
     * as its own read for the same reason its neighbours are: a star system is searched by a
     * method that accepts nothing else.
     *
     * @param sector      the sector whose economy is read; null (or a null economy) yields empty
     * @param system      the system to search; null yields empty
     * @param from        what nearness is measured from; null (or one with no location) yields
     *                    empty
     * @param eligibility what makes a market a candidate; null yields empty
     * @return the nearest admitted market, or empty when the system holds none
     */
    public static Optional<MarketAPI> findNearestMarket(
            SectorAPI sector,
            StarSystemAPI system,
            SectorEntityToken from,
            Predicate<MarketAPI> eligibility) {

        return LocationMarkets.findNearestMarket(sector, system, from, eligibility);
    }

    /**
     * The faction id decreed as {@code system}'s claimant by its
     * {@link MemFlags#CLAIMING_FACTION} ({@code $claimingFaction}) memory flag.
     *
     * <p>This is the imposed claim only - a flag a script or mod sets to hand a system
     * to a faction outright. It is not "who claims this system": vanilla resolves an
     * unflagged system's claimant by scoring the markets in it, so most claimed systems
     * report null here. A caller wanting the resolved claimant wants that computation,
     * not this flag.
     *
     * @param system the system to read; null (or one with no memory) yields null
     * @return the decreed claimant's faction id, or null when no claim is imposed
     */
    public static String readFactionClaimOverride(StarSystemAPI system) {
        if (system == null || system.getMemoryWithoutUpdate() == null) {
            return null;
        }
        return system.getMemoryWithoutUpdate().getString(MemFlags.CLAIMING_FACTION);
    }

    /**
     * The star closest to the system centre - the reference a distance-from-centre read measures
     * from. This is the star nearest the centre, not one presumed to sit at it: a single-star
     * system yields its one star, which does sit at the centre, but a binary or trinary system
     * orbits a shared invisible centre, so the closest star is the busy inner system's, the
     * natural reference rather than a distant companion. An equal-mass binary's two stars sit
     * equally far out, so the tie settles by {@link StarsectorPoints#isNearerThan}'s rule and
     * the same system answers the same star on every pass. Falls back to the centre token
     * itself when the system has no star.
     *
     * @param system the star system to read; null yields null
     * @return the star closest to the centre, the centre token when the system is starless, or
     *         null when {@code system} is null
     */
    public static SectorEntityToken getCentremostStar(StarSystemAPI system) {
        if (system == null) {
            return null;
        }

        var centre = system.getCenter();
        var centreLocation = centre == null ? null : centre.getLocation();
        var nearestDistance = Double.POSITIVE_INFINITY;

        SectorEntityToken nearestStar = null;

        for (var star : getStars(system)) {
            var distance = centreLocation == null
                ? 0.0
                : Points.computeDistance(centreLocation, star.getLocation());
            if (StarsectorPoints.isNearerThan(distance, nearestDistance, star, nearestStar)) {
                nearestDistance = distance;
                nearestStar = star;
            }
        }
        return nearestStar != null ? nearestStar : centre;
    }

    /**
     * Decides whether a star system has a normal means of arrival.
     *
     * <p>Access is defined by the means of arrival a system actually offers, not
     * by trusting the {@code SYSTEM_CUT_OFF_FROM_HYPER} tag - that tag is set by
     * specific procgen paths, so a hand-built hidden system can be unreachable
     * without ever carrying it. An <em>active</em> gate always grants access.
     * Otherwise the system must be wired into hyperspace by at least one jump
     * point and not be flagged cut off. A transverse-only system - reachable
     * solely via a nascent gravity well, with no jump point - confers no access
     * and is not reachable, even though the engine never tags it cut off; a
     * present but inactive gate does not rescue it. Gate activation flips
     * {@link GateEntityPlugin#isActive}, so reachability tracks the real state.
     *
     * <p>An installed mod may supply a further means of arrival that the engine
     * does not model - one that moves fleets in without a jump point, so that a
     * system entered only that way would otherwise read as cut off. Such a route
     * grants access the same way an active gate does, bypassing both the
     * jump-point and cut-off checks. Which routes this install has is
     * {@link SystemAccessRoutes}' to hold: the mods that supply them register
     * there, so nothing named here is a mod, and an install with none answers
     * from the vanilla reads alone.
     *
     * @param system the system being asked about; null is not reachable, there being no system to
     *               arrive at
     * @return true when the player has a normal means of reaching it
     */
    public static boolean isReachable(StarSystemAPI system) {
        if (system == null) {
            return false;
        }
        // A lit gate or an installed access route reaches the system regardless
        // of jump connectivity, so either overrides the cut-off flag and the
        // absence of jump points.
        if (hasActiveGate(system) || SystemAccessRoutes.isReachedByAnyRoute(system)) {
            return true;
        }
        // No gate: the system must be reachable by ordinary hyperspace travel.
        // The cut-off flag rejects a system whose jump points are disabled; an
        // empty jump-point list rejects a transverse-only system whose sole
        // entry is a nascent gravity well (not a jump point).
        if (system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) {
            return false;
        }
        return !system.getJumpPoints().isEmpty();
    }

    /**
     * The first entity in {@code system} carrying {@code entityTag} whose id
     * equals {@code id} - the targeted "find this one tagged entity" lookup
     * console commands and scripts need (e.g. a specific gate or comm relay),
     * so callers do not re-walk {@code getEntitiesWithTag} themselves.
     *
     * @param system    the star system to search; null yields null
     * @param entityTag the entity tag to filter on (e.g. {@code Tags.GATE})
     * @param id        the entity id to match exactly; null or blank yields null
     * @return the first matching entity, or null when none in {@code system}
     *         carries {@code entityTag} with that id
     */
    public static SectorEntityToken findTaggedEntity(
            StarSystemAPI system,
            String entityTag,
            String id) {

        if (system == null || !KmlibStrings.hasText(id)) {
            return null;
        }
        for (var entity : system.getEntitiesWithTag(entityTag)) {
            if (id.equals(entity.getId())) {
                return entity;
            }
        }
        return null;
    }

    // Whether any gate in the system is lit. An inactive gate (unscanned, or the
    // network not yet activated) grants nothing.
    private static boolean hasActiveGate(StarSystemAPI system) {
        for (var gate : system.getEntitiesWithTag(Tags.GATE)) {
            if (GateEntityPlugin.isActive(gate)) {
                return true;
            }
        }
        return false;
    }
}
