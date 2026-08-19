package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;

import kmlib.starsector.entities.EntityMapIcons;
import kmlib.starsector.entities.EntityNameplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Queries over a market's state, and over which of several markets speaks for a place.
 *
 * <p>Centralises reads a market only answers indirectly - walking its connected
 * entities, or normalising a raw stat against its vanilla band - so KM* mods (and
 * any external caller) share one implementation of a check like "does this colony
 * have a station" or "how stable is it, as a fraction" rather than re-deriving the
 * scan or the band arithmetic each time.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance
 * state. Matches {@link kmlib.starsector.systems.StarSystems}'s shape, and is
 * null-market defensive like the rest of the library.
 */
public final class Markets {

    // Vanilla's opt-out tag for a "station"-tagged entity that must not be treated
    // as a market's orbital station. No Tags constant exists for it, so the literal
    // is vanilla's own contract - OrbitalStation's station scan tests it the same way.
    private static final String NO_ORBITAL_STATION_TAG = "NO_ORBITAL_STATION";

    // Stability's vanilla 0..10 band, the denominator of the stability fraction. A
    // modded market can report a value outside it, so the fraction clamps: an
    // out-of-band reading scales as "none" or "full" rather than overshooting.
    private static final float MAX_STABILITY_VALUE = 10.0f;

    private Markets() {
        // utility class, no instances.
    }

    /**
     * Whether a market is a colony a faction owns, rather than a bare planet's
     * placeholder.
     *
     * <p>The "counts as a colony" filter map and territory logic share: a faction
     * must own it, and it must not be the condition-only market every uninhabited
     * planet carries to hold its hazard and atmosphere conditions. This is
     * ownership alone - it says nothing about whether the player has found the
     * colony yet; compose it with {@link #isKnownToPlayer} when visibility matters.
     *
     * @param market the market to test; null (or one with no owning faction) yields
     *               false
     * @return true when a faction owns the market and it is not condition-only
     */
    public static boolean isOwnedColony(MarketAPI market) {
        return market != null
            && market.getFaction() != null
            && !market.isPlanetConditionMarketOnly();
    }

    /**
     * How a market is identified to a reader: the name it goes by, and the glyph the sector map
     * marks it with.
     *
     * <p>The two come from different places - a market is named in its own right while the glyph
     * belongs to the entity it sits on - which is exactly why the pairing is made here rather than
     * at each surface printing a list of colonies. Read apart, one colony's name is one call away
     * from being drawn beside another's glyph.
     *
     * @param market the market to identify; null yields {@link EntityNameplate#BLANK}, the
     *               null-defensive shape the rest of the class holds to, and one with no primary
     *               entity is named with no glyph
     * @return the market's nameplate
     */
    public static EntityNameplate readNameplate(MarketAPI market) {
        if (market == null) {
            return EntityNameplate.BLANK;
        }
        return new EntityNameplate(
            market.getName(),
            EntityMapIcons.resolveMapIcon(market.getPrimaryEntity()));
    }

    /**
     * A market's attached defensive station, as the entity itself.
     *
     * <p>Reads the market's connected entities - the ownership link the game
     * maintains, so a station found here is this market's own rather than a rival's
     * or an abandoned hulk sharing the orbit - and qualifies each on the two halves
     * vanilla's own station reads test: a {@code "station"}-tagged entity not opted
     * out via {@code NO_ORBITAL_STATION}, which additionally has a station fleet.
     * Keying on the tag and the fleet captures vanilla and modded stations alike,
     * so no industry ids are read.
     *
     * <p>The fleet half is what makes the answer a defensive station rather than a
     * place built on one. A market sited on a station is connected to its own
     * primary entity, which carries the {@code "station"} tag for what it is, so the
     * tag alone would have every such market defended by itself. The station fleet
     * is raised by an actual orbital-station industry, so requiring it is what tells
     * a colony with a battlestation apart from a colony that is a hab ring.
     *
     * <p>Yields the entity rather than a verdict, so a caller that has to name the
     * station - or read its faction, orbit, or own market - can, while one that only
     * asks whether there is one reads {@link #hasAttachedStation} over this same
     * scan. A second scan for the entity would be free to disagree with the verdict;
     * folding both onto one read makes that impossible.
     *
     * <p>The first qualifying entity wins. A market with two of them is not a shape
     * vanilla builds, and there is no ordering among connected entities that would
     * make one of the pair the "real" station, so picking a winner is arbitrary
     * either way.
     *
     * @param market the market to inspect; null (or one with no connected entities)
     *               yields empty
     * @return the market's orbital station, or empty when it owns none
     */
    public static Optional<SectorEntityToken> findAttachedStation(MarketAPI market) {
        if (market == null || market.getConnectedEntities() == null) {
            return Optional.empty();
        }
        for (var entity : market.getConnectedEntities()) {
            // The opt-out is tested before the fleet read: it is the cheaper answer, and
            // vanilla's fleet lookup dereferences the entity's memory, which an opted-out
            // decoration has no reason to carry.
            if (entity.hasTag(Tags.STATION)
                    && !entity.hasTag(NO_ORBITAL_STATION_TAG)
                    && Misc.getStationFleet(entity) != null) {
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    /**
     * Whether a market owns an attached defensive station.
     *
     * <p>The verdict half of {@link #findAttachedStation}, which owns the scan and
     * documents what counts as a station. Kept as its own read because most callers
     * weigh only the station's presence - a defence score, a fill rule - and reading
     * that as an {@code isPresent()} at every such site says less than the question
     * being asked.
     *
     * @param market the market to inspect; null (or one with no connected entities)
     *               yields false
     * @return true when one of the market's connected entities is its orbital station
     */
    public static boolean hasAttachedStation(MarketAPI market) {
        return findAttachedStation(market).isPresent();
    }

    /**
     * A market's stability as a fraction of its vanilla 0..10 band, clamped to
     * [0, 1].
     *
     * <p>The reusable half of any "scale something by how stable this colony is"
     * read: 0 at no stability, 1 at full, linear between. The clamp keeps a modded
     * market that reports outside the band from scaling past "none" or "full", so a
     * caller multiplying by the fraction never overshoots or flips sign.
     *
     * @param market the market to read; null yields 0 (no stability)
     * @return the market's stability in [0, 1]
     */
    public static double getStabilityFraction(MarketAPI market) {
        if (market == null) {
            return 0.0;
        }
        var fraction = market.getStabilityValue() / MAX_STABILITY_VALUE;
        return Math.min(Math.max(fraction, 0.0f), 1.0f);
    }

    /**
     * Whether the player knows this market exists - the base fog every display of a
     * colony asks permission of.
     *
     * <p>Knowledge is discovery and nothing besides: the player has found the market's
     * entity. A market's public listing is not an arm of the rule, because a colony
     * listed in an economy the player has no sight of is not something the player has
     * any way of knowing about, and admitting it shows every undiscovered market that
     * merely omits to hide itself - every derelict station in the sector among them -
     * from the first frame of a campaign. The case the listing would be admitting for,
     * a colony surfaced by a story reveal ahead of a fleet reaching it, is one vanilla
     * marks found by clearing the entity's discoverable flag, which this rule already
     * reads.
     *
     * <p>Named apart from {@link #isDiscoveredByPlayer} because the two answer
     * different questions: this is the fog a display asks permission of, that is the
     * fact about the entity the fog is made of. A caller weighing whether it may show
     * something reads this; a caller wanting the entity's own state reads that.
     *
     * <p>Neither reads the owner or the intel directory, so a faction hidden from the
     * directory is not barred, and a concealed base on an entity the player has found
     * (a raided pirate base) reads known.
     *
     * @param market the market to test; null yields false
     * @return true when the player knows the market exists
     */
    public static boolean isKnownToPlayer(MarketAPI market) {
        return isDiscoveredByPlayer(market);
    }

    /**
     * Whether the player has physically found this market's entity.
     *
     * <p>Discovery lives on the entity, not the market: an entity stops being
     * {@code discoverable} once found. A market with no entity reads discovered - there is
     * nothing left to find, so nothing to withhold.
     *
     * <p>Hiddenness is a separate axis and is deliberately not read here. A concealed base
     * the player has raided is discovered and permanently hidden; a colony surfaced by a
     * story reveal is un-hidden while its entity may be either. A caller asking what the
     * player has found must get that answer alone, and a caller weighing concealment reads
     * {@code isHidden} for itself.
     *
     * @param market the market to test; null yields false
     * @return true when the market's entity has been found, or it has no entity
     */
    public static boolean isDiscoveredByPlayer(MarketAPI market) {
        if (market == null) {
            return false;
        }
        var entity = market.getPrimaryEntity();
        return entity == null || !entity.isDiscoverable();
    }

    /**
     * Whether a market counts as a known owned colony - the "counts on the map" filter a
     * presence or dominance read admits a market by.
     *
     * <p>Composes {@link #isOwnedColony} with {@link #isKnownToPlayer} so "counts as a known
     * colony" means one thing across every caller rather than each re-deriving the pair and
     * drifting. The dev reveal drops the visibility arm, admitting a colony the player has not
     * yet found so an undiscovered faction still paints under it.
     *
     * @param market                           the market to test; null yields false
     * @param shouldIncludeUndiscoveredMarkets whether an undiscovered colony still counts (the
     *                                         "show all factions" dev reveal); false applies the
     *                                         normal known-to-player filter
     * @return true when a faction owns the market and it is either known or the reveal is on
     */
    public static boolean isCountedAsColony(
            MarketAPI market,
            boolean shouldIncludeUndiscoveredMarkets) {
                
        if (!isOwnedColony(market)) {
            return false;
        }
        return shouldIncludeUndiscoveredMarkets || isKnownToPlayer(market);
    }

    /**
     * The markets that speak for their places once several share one entity: the
     * largest per owning faction, in the order the input first names each place.
     *
     * <p>A colony is one place however many market objects the game has hung on its
     * entity, and mods supersede a market by adding rather than replacing - IndEvo
     * attaches its own Galatia Academy market to the station entity that already
     * carries vanilla's, both owned by the same faction. A caller that sums per market
     * banks that colony twice and reads its owner as holding twice what it holds.
     *
     * <p>Resolved per faction rather than per entity outright: an entity carrying
     * markets of two owners is not a shape vanilla builds, but collapsing to one winner
     * would silently drop a faction's only foothold there if a mod ever built it.
     *
     * <p>Largest wins, because size is the reading the weights above are built on. The
     * first of an equal-sized pair wins, so a caller passing the economy's own order
     * resolves the same way on every pass rather than alternating between them.
     *
     * <p>A market with no entity, or no owner to read, cannot collide with anything and
     * passes through untouched. Grouping those together would merge places whose only
     * shared trait is the missing key.
     *
     * @param markets the markets to resolve; null yields an empty list, and null
     *                elements are dropped
     * @return one market per place-and-owner, in first-appearance order
     */
    public static List<MarketAPI> readLargestMarketsPerFaction(Collection<MarketAPI> markets) {
        if (markets == null) {
            return List.of();
        }
        // Insertion-ordered rather than hashed: re-putting a winner on a key it already
        // holds leaves that key in its original slot, so a place stays where the caller
        // first named it however late its largest market arrives.
        var winnersByPlace = new LinkedHashMap<PlaceKey, MarketAPI>();
        for (var market : markets) {
            if (market == null) {
                continue;
            }
            var place = buildPlaceKey(market);
            var incumbent = winnersByPlace.get(place);
            if (incumbent == null || market.getSize() > incumbent.getSize()) {
                winnersByPlace.put(place, market);
            }
        }
        return new ArrayList<>(winnersByPlace.values());
    }

    /**
     * Whether two markets stand for the same colony under the same owner - the identity
     * {@link #readLargestMarketsPerFaction} groups by, offered as a plain test for a caller
     * merging two listings of markets rather than resolving one.
     *
     * <p>The market object itself is asked first, so a market is always the same place as
     * itself. A market with no entity, or no owner whose id can be read, answers for nothing
     * but itself thereafter: a missing key is not a key two markets can share.
     *
     * @param left  one market; null is the same place as nothing at all
     * @param right the other market; null is the same place as nothing at all
     * @return true when both stand for one colony under one faction
     */
    public static boolean isSamePlaceAndOwner(MarketAPI left, MarketAPI right) {
        if (left == null || right == null) {
            return false;
        }
        if (left == right) {
            return true;
        }
        return buildPlaceKey(left).equals(buildPlaceKey(right));
    }

    /**
     * Whether a market is a military one - a garrison rather than a plain colony.
     *
     * <p>Delegates to {@link Misc#isMilitary}, which reads the
     * {@link MemFlags#MARKET_MILITARY} ({@code $military}) flag a market's military
     * industries raise. The read is delegated rather than reproduced: the flag and
     * the conditions that raise it are vanilla's to change, and a second
     * implementation of the same rule would be free to drift from it.
     *
     * <p>Broader than {@link MarketPatrols#fieldsPatrols}, and the two answer different
     * questions. A market is military by virtue of what it <em>is</em>; it fields
     * patrols by virtue of a patrol industry currently running there. A caller weighing
     * military standing - claim scoring, threat estimates - wants this one; a caller
     * asking whether fleets actually launch from here wants the patrol flag.
     *
     * @param market the market to test; null (or one with no memory) yields false
     * @return true when the market counts as military
     */
    public static boolean isMilitary(MarketAPI market) {
        // The memory guard is this library's, not vanilla's: Misc reads the flag straight off
        // the market's memory and would throw on a market that has none. Its neighbours here
        // absorb that case, so this one does too rather than being the single read a caller
        // has to defend against.
        if (market == null || market.getMemoryWithoutUpdate() == null) {
            return false;
        }
        return Misc.isMilitary(market);
    }

    // What makes two markets the same place under the same owner, or - for a market
    // missing either half - what makes it answer for nothing but itself.
    private static PlaceKey buildPlaceKey(MarketAPI market) {
        var entity = market.getPrimaryEntity();
        var faction = market.getFaction();
        if (entity == null || faction == null || faction.getId() == null) {
            return new UnkeyablePlace();
        }
        return new PlaceAndOwner(entity, faction.getId());
    }

    // What may key the per-place resolution: a place under an owner, or a market with
    // neither to read. Sealed so the map's key type states those two cases rather than
    // admitting anything, and so a third case cannot be added without being handled.
    private sealed interface PlaceKey permits PlaceAndOwner, UnkeyablePlace {
    }

    // The identity two market objects must share before the larger can stand for both.
    // The entity compares by whatever equality it defines, which for vanilla's entities
    // is identity, so two distinct stations never merge however alike they read.
    private record PlaceAndOwner(SectorEntityToken entity, String factionId)
        implements PlaceKey {
    }

    // A market with no entity, or no owner whose id can be read. A class rather than a
    // record because identity equality is the whole point: every instance equals only
    // itself, so such a market forms its own group and passes through. A record here
    // would make all instances equal and pool every unkeyable market into one.
    private static final class UnkeyablePlace implements PlaceKey {
    }
}
