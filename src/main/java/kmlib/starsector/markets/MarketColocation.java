package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Which market speaks for a place once several sit on one entity.
 *
 * <p>A colony is one place however many market objects the game has hung on its entity,
 * and mods supersede a market by adding rather than replacing - IndEvo attaches its own
 * Galatia Academy market to the station entity that already carries vanilla's, both
 * owned by the same faction. A caller that sums per market banks that colony twice and
 * reads its owner as holding twice what it holds.
 *
 * <p>Held apart from {@link Markets} because the question is about a <em>pair</em> of
 * markets, not about one. Every read here needs the identity two markets would have to
 * share, which is the private machinery below; nothing else in the package wants it, and
 * leaving it beside the single-market reads made it look like one of them.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state.
 * Matches the shape of its neighbours in this package, and is null-market defensive like
 * the rest of the library.
 */
public final class MarketColocation {

    private MarketColocation() {
        // utility class, no instances.
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
     * The markets that speak for their places once several share one entity: the
     * largest per owning faction, in the order the input first names each place.
     *
     * <p>Resolved per faction rather than per entity outright: an entity carrying
     * markets of two owners is not a shape vanilla builds, but collapsing to one winner
     * would silently drop a faction's only foothold there if a mod ever built it.
     *
     * <p>Largest wins, because size is the reading a weight is built on. The first of an
     * equal-sized pair wins, so a caller passing the economy's own order resolves the
     * same way on every pass rather than alternating between them.
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
