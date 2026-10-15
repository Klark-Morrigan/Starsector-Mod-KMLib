package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The markets in one place, held apart by which of the place's two listings each came from: the
 * economy's own list, and the markets hung on the place's entities that the economy never
 * registered.
 *
 * <p>Two lists rather than one because they answer different callers. A mechanic mirrored from
 * vanilla feeds off the economy's list alone and depends on its order; a surface describing the
 * place has to see both. Handed the two apart, a caller that needs both can still tell them
 * apart by which list a market stands in, without comparing one against the other to find out.
 *
 * <p>Both lists are copied in and read-only, so a listing handed to several readers in one pass
 * reports the same thing to each of them.
 *
 * @param listedByEconomy   the economy's markets in the place, in the economy's order; null reads
 *                          as empty
 * @param unlistedByEconomy the markets the economy does not list, in entity order; null reads as
 *                          empty
 */
public record LocationMarketListings(
    List<MarketAPI> listedByEconomy,
    List<MarketAPI> unlistedByEconomy) {

    /** A place holding no market under either listing. */
    public static final LocationMarketListings NONE = new LocationMarketListings(List.of(), List.of());

    public LocationMarketListings {
        listedByEconomy = copyMarkets(listedByEconomy);
        unlistedByEconomy = copyMarkets(unlistedByEconomy);
    }

    /**
     * Every market present in the place, the economy's first in its order and the unlisted ones
     * after in entity order - for a caller asking what is here at all, rather than which listing
     * it came from.
     *
     * @return both listings as one read-only list; never null
     */
    public List<MarketAPI> listPresentMarkets() {

        var presentMarkets = new ArrayList<MarketAPI>(listedByEconomy.size() + unlistedByEconomy.size());

        presentMarkets.addAll(listedByEconomy);
        presentMarkets.addAll(unlistedByEconomy);

        return Collections.unmodifiableList(presentMarkets);
    }

    // Copied by hand rather than through List.copyOf, which refuses a null element: the economy's
    // list is the game's own and this is not the place to fail on what a mod put in it.
    private static List<MarketAPI> copyMarkets(List<MarketAPI> markets) {

        if (markets == null) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(markets));
    }
}
