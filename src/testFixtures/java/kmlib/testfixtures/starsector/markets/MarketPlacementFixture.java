package kmlib.testfixtures.starsector.markets;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Siting markets in a location, the way the game sites them.
 *
 * <p>Every read over a location walks it the same two ways - the economy is asked what it lists
 * there, and the location's own entities are walked for the markets hung on them - so every
 * suite posing one would otherwise wire the same two stubs. Stated once here so the worlds
 * different suites pose differ in what they hold rather than in how a location is put together.
 *
 * <p>Presence and registration stay separate calls, because that split is what several cases are
 * about: a market given only {@link #placeMarketsIn} is the off-economy shape vanilla builds
 * Galatia Academy in, and one given only {@link #listMarketsIn} is registered with nothing to
 * find by walking entities.
 *
 * <p>Separate from {@link MarketStateFixture}, which poses what state a market is in rather than
 * where it sits.
 */
public final class MarketPlacementFixture {

    private MarketPlacementFixture() {
        // fixture of static wiring, no instances.
    }

    /**
     * A market on a body of its own, fixed at a location and answering to an id - the pair a
     * nearest search ranks by and settles its ties on. Wired both ways, so the market is
     * reachable whether a read meets it through the economy or by walking the location.
     */
    public static MarketAPI buildMarketOnBodyAt(String bodyId, float x, float y) {
        return hangMarketOn(buildBodyAt(bodyId, x, y));
    }

    /**
     * A market on a body that sits nowhere in particular: an entity with an id and no location,
     * which is every case about what is present rather than what is nearest.
     */
    public static MarketAPI buildMarketOnBody(String bodyId) {

        var bodyMock = mock(SectorEntityToken.class);

        when(bodyMock.getId())
            .thenReturn(bodyId);

        return hangMarketOn(bodyMock);
    }

    /**
     * A market on a body of its own under a named owner - the pair
     * {@link kmlib.starsector.markets.Markets#isSamePlaceAndOwner} keys on, so two of them can be posed as one place or
     * as two.
     */
    public static MarketAPI buildOwnedMarketOnBody(String bodyId, String factionId) {

        // The faction finishes its own stubbing before the market's opens, so the two do not
        // nest into an unfinished-stubbing error.
        var factionMock = buildFaction(factionId);
        var marketMock = buildMarketOnBody(bodyId);

        when(marketMock.getFaction())
            .thenReturn(factionMock);

        return marketMock;
    }

    /**
     * A second market object on an existing market's body under the same owner, hung on that
     * body in the first one's place - the shape a mod builds when it supersedes a market by
     * adding beside vanilla's rather than replacing it.
     */
    public static MarketAPI buildSupplementaryMarketOn(MarketAPI market) {

        // Both halves are read off the existing market before the new one's stubbing opens, so
        // the two do not nest into an unfinished-stubbing error.
        var faction = market.getFaction();
        var body = market.getPrimaryEntity();
        var supplementaryMock = hangMarketOn(body);

        when(supplementaryMock.getFaction())
            .thenReturn(faction);

        return supplementaryMock;
    }

    /** A body with nothing on it - a gate, a beacon, a bare rock. */
    public static SectorEntityToken buildBodyAt(String bodyId, float x, float y) {

        var bodyMock = mock(SectorEntityToken.class);

        when(bodyMock.getId())
            .thenReturn(bodyId);
        when(bodyMock.getLocation())
            .thenReturn(new Vector2f(x, y));

        return bodyMock;
    }

    /** The player's fleet where it sits, which is what a nearest search measures from. */
    public static CampaignFleetAPI buildFleetAt(float x, float y) {

        var fleetMock = mock(CampaignFleetAPI.class);

        when(fleetMock.getLocation())
            .thenReturn(new Vector2f(x, y));

        return fleetMock;
    }

    /**
     * Sites the markets in {@code location}, each on the body it was built with - what the
     * entity walk finds, whether or not the economy also lists them.
     *
     * <p>Wired both ways, as siting always is here: the location lists the bodies, and each
     * market names the location back. A read asking a market where it stands would otherwise get
     * nothing out of a world that had plainly just put it somewhere.
     */
    public static void placeMarketsIn(LocationAPI location, MarketAPI... markets) {

        // Every body is read off its market before the stubbing opens, so calling a mock does
        // not land inside a stubbing in progress.
        var bodies = new ArrayList<SectorEntityToken>();

        for (var market : markets) {
            bodies.add(market.getPrimaryEntity());
        }
        when(location.getAllEntities())
            .thenReturn(bodies);

        for (var market : markets) {
            when(market.getContainingLocation())
                .thenReturn(location);
        }
    }

    /** Registers the markets with the economy as sitting in {@code location}, in listing order. */
    public static void listMarketsIn(
            EconomyAPI economyMock,
            LocationAPI location,
            MarketAPI... markets) {

        when(economyMock.getMarkets(location))
            .thenReturn(List.of(markets));
    }

    // A market and the body it sits on, wired both ways: the market names the body as its place
    // and the body carries the market, which is how an unregistered market is found at all. The
    // body's own stubbing is finished before the market's opens, so the two do not nest into an
    // unfinished-stubbing error.
    private static MarketAPI hangMarketOn(SectorEntityToken bodyMock) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getPrimaryEntity())
            .thenReturn(bodyMock);
        when(bodyMock.getMarket())
            .thenReturn(marketMock);

        return marketMock;
    }

    private static FactionAPI buildFaction(String id) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(id);

        return factionMock;
    }
}
