package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.starsector.geometry.StarsectorPoints;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Finding the markets that sit in one place, whatever kind of place it is - all of them, or the
 * one nearest something.
 *
 * <p>The search underneath every "what is in here" read. A market sits in a location - a star
 * system, or hyperspace - and neither half of finding one is system-specific: the economy is
 * asked per location, and the entities hung with markets are a location's own. Stating the
 * search once here leaves the star-system and hyperspace readers above it as the thin,
 * separately-named things they should be, rather than one read taking a kind of place as an
 * argument and each caller having to know which kinds are legal.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and
 * null-defensive like the rest of the library.
 */
public final class LocationMarkets {

    private LocationMarkets() {
        // utility class, no instances.
    }

    /**
     * The markets the economy places in {@code location}, in the order it lists them.
     *
     * <p>A location does not hold its own markets - the economy owns that mapping - so every
     * "what is in here" read has to go through the sector. Centralised so callers share one
     * traversal, and so an unreachable economy is handled once: outside a running game (or
     * before the economy exists) the read yields an empty list rather than throwing, which
     * reads as an empty place.
     *
     * <p>Economy order is preserved and load-bearing for some callers: vanilla's own claim
     * mechanic settles a tied contest on whichever market it meets first, so a caller mirroring
     * that rule depends on this order being the economy's, not one imposed here.
     *
     * @param sector   the sector whose economy is read; null (or a null economy) yields an
     *                 empty list
     * @param location the location to read; null yields an empty list
     * @return the location's markets in economy order; never null
     */
    public static List<MarketAPI> readMarkets(SectorAPI sector, LocationAPI location) {
        if (sector == null || location == null || sector.getEconomy() == null) {
            return List.of();
        }
        var markets = sector.getEconomy().getMarkets(location);

        if (markets == null) {
            return List.of();
        }
        // Every read of the economy's listing is counted, this one included when a
        // caller above has already made it: two reads of one place are two reads,
        // and a row that reported them as one would hide the second.
        SectorWalkCounters.countMarketsRead(markets.size());

        return markets;
    }

    /**
     * The markets hung on {@code location}'s own entities that the economy does not list, in
     * entity order - what {@link #readMarkets} cannot see, and nothing it can.
     *
     * <p>A colony can sit on a real entity, owned by a real faction, and never be registered
     * with the economy - vanilla builds Galatia Academy that way on purpose. A caller
     * <em>describing</em> what is in a place has to see it, or it reports a station flying a
     * faction's colours as belonging to nobody. A caller <em>computing</em> a mechanic vanilla
     * feeds off the economy must not, which is why this is a second read rather than a widening
     * of {@link #readMarkets}: that one's "the economy's list, in the economy's order" contract
     * is what a mirrored mechanic depends on.
     *
     * <p>Answering only what the economy leaves out, rather than the whole of what is present,
     * is what leaves a caller that needs both able to tell them apart by which read they came
     * from. A combined answer would have every such caller re-deriving that split against the
     * economy's list, which is a second implementation of the very comparison made here.
     *
     * <p>Sameness is the market object itself first and {@link Markets#isSamePlaceAndOwner}
     * after, taken against the economy's markets and against the ones already found, so a mod
     * hanging its own market beside vanilla's on one station yields nothing here rather than a
     * duplicate of the colony the economy already lists.
     *
     * @param sector   the sector whose economy is read; null (or a null economy) yields an empty
     *                 list - with no economy to compare against there is no telling a listed
     *                 market from an unlisted one
     * @param location the location to read; null yields an empty list
     * @return the location's markets the economy does not list, in entity order; never null
     */
    public static List<MarketAPI> readMarketsUnlistedByEconomy(
            SectorAPI sector,
            LocationAPI location) {

        if (sector == null || location == null || sector.getEconomy() == null) {
            return List.of();
        }
        // Seeded with the economy's own markets so one scan answers both halves of sameness - a
        // market the economy lists, and one an earlier entity already yielded - then dropped from
        // the answer, the caller having read those from the economy itself.
        var listedMarkets = readMarkets(sector, location);
        var seenMarkets = new ArrayList<>(listedMarkets);
        var unlistedMarkets = new ArrayList<MarketAPI>();

        var entities = location.getAllEntities();

        for (var entity : entities) {
            var market = entity == null ? null : entity.getMarket();

            if (market != null && !isAlreadyPresent(seenMarkets, market)) {
                seenMarkets.add(market);
                unlistedMarkets.add(market);
            }
        }
        // The entities are what this read costs - everything in the place is looked
        // at - while the markets are what it produced. Both, since neither answers
        // the other: a place with one unlisted colony among two thousand bodies is
        // a cheap answer to state and an expensive one to find.
        SectorWalkCounters.countEntitiesVisited(entities.size());
        SectorWalkCounters.countMarketsRead(unlistedMarkets.size());

        return unlistedMarkets;
    }

    /**
     * The market in {@code location} that {@code eligibility} admits whose body sits closest to
     * {@code from} - the "act on the one by me" resolution a targeted operation is pointed at
     * with, rather than a listing to pick out of.
     *
     * <p>Both listings are searched, and which one a market came from is not part of the answer.
     * That is not thoroughness: a body carrying only survey data is never registered with the
     * economy, so a search of the economy's own listing could not find one to colonise at all,
     * and vanilla builds real colonies off the economy too. What a caller is looking for is
     * stated by the predicate it hands over, which is the only thing that decides.
     *
     * <p>A market with no primary entity, or one whose entity has no location, cannot be ranked
     * and is passed over - there is no place to measure to, so admitting it would make the
     * answer "nearest" only by default. Ties settle by
     * {@link StarsectorPoints#isNearerThan}'s rule, so one pass over one place answers the same
     * market every time.
     *
     * @param sector      the sector whose economy is read; null (or a null economy) yields empty
     * @param location    the location to search; null yields empty
     * @param from        what nearness is measured from, the entity a caller is acting out of;
     *                    null (or one with no location) yields empty, there being nothing to
     *                    measure against
     * @param eligibility what makes a market a candidate at all; null yields empty rather than
     *                    admitting every market present
     * @return the nearest admitted market, or empty when the location holds none
     */
    public static Optional<MarketAPI> findNearestMarket(
            SectorAPI sector,
            LocationAPI location,
            SectorEntityToken from,
            Predicate<MarketAPI> eligibility) {

        if (from == null || from.getLocation() == null || eligibility == null) {
            return Optional.empty();
        }
        var presentMarkets = new ArrayList<>(readMarkets(sector, location));
        presentMarkets.addAll(readMarketsUnlistedByEconomy(sector, location));

        RankedMarket nearest = null;

        for (var market : presentMarkets) {

            var candidate = rankMarket(market, from, eligibility);

            if (candidate != null && candidate.isNearerThan(nearest)) {
                nearest = candidate;
            }
        }
        return nearest == null ? Optional.empty() : Optional.of(nearest.market());
    }

    // A market with what it takes to rank it, or null when it does not rank at all - rejected by
    // the caller's rule, or standing for no place a distance can be taken to. The market, its
    // body and its distance travel together so a pass cannot advance one of the three and leave
    // the others behind.
    private static RankedMarket rankMarket(
            MarketAPI market,
            SectorEntityToken from,
            Predicate<MarketAPI> eligibility) {

        if (market == null || !eligibility.test(market)) {
            return null;
        }
        var body = market.getPrimaryEntity();

        if (body == null || body.getLocation() == null) {
            return null;
        }
        return new RankedMarket(market, body, StarsectorPoints.computeDistanceBetween(body, from));
    }

    // Whether one of the markets already found stands for the same colony under the same owner,
    // which is what makes a mod's supplementary market on a listed station not a second colony.
    private static boolean isAlreadyPresent(List<MarketAPI> presentMarkets, MarketAPI market) {

        for (var present : presentMarkets) {
            if (MarketColocation.isSamePlaceAndOwner(present, market)) {
                return true;
            }
        }
        return false;
    }

    // One candidate in a nearest search: the market, the body its distance was taken to, and
    // that distance.
    private record RankedMarket(
        MarketAPI market,
        SectorEntityToken body,
        double distance) {

        // Whether this candidate displaces the best found so far, nothing found yet included.
        private boolean isNearerThan(RankedMarket incumbent) {
            return incumbent == null
                || StarsectorPoints.isNearerThan(
                    distance,
                    incumbent.distance,
                    body,
                    incumbent.body);
        }
    }
}
