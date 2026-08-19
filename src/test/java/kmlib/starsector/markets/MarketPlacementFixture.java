package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
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

        // The body finishes its own stubbing before the market's opens, so the two do not nest
        // into an unfinished-stubbing error.
        var bodyMock = buildBodyAt(bodyId, x, y);
        var marketMock = mock(MarketAPI.class);

        when(marketMock.getPrimaryEntity())
            .thenReturn(bodyMock);
        when(bodyMock.getMarket())
            .thenReturn(marketMock);

        return marketMock;
    }

    /**
     * A market standing for a body that is nowhere in particular: an entity with an id and no
     * location, which a distance cannot be taken to.
     */
    public static MarketAPI buildMarketOnPlacelessBody(String bodyId) {

        var bodyMock = mock(SectorEntityToken.class);

        when(bodyMock.getId())
            .thenReturn(bodyId);

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getPrimaryEntity())
            .thenReturn(bodyMock);
        when(bodyMock.getMarket())
            .thenReturn(marketMock);

        return marketMock;
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
    }

    /** Registers the markets with the economy as sitting in {@code location}, in listing order. */
    public static void listMarketsIn(
            EconomyAPI economyMock,
            LocationAPI location,
            MarketAPI... markets) {

        when(economyMock.getMarkets(location))
            .thenReturn(List.of(markets));
    }
}
