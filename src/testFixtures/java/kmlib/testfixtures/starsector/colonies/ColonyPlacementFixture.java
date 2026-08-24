package kmlib.testfixtures.starsector.colonies;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.testfixtures.starsector.markets.MarketPlacementFixture;

/**
 * Siting colonies in a location, the way the game sites them.
 *
 * <p>A colony is sited by siting the market that stands for it, so the wiring is
 * {@link MarketPlacementFixture}'s and this says only that a colony is what is being sited.
 * Kept as its own surface because a colony suite reads better posing colonies than markets, and
 * because what a colony <em>is</em> stays {@link ColonyMarketFixture}'s alongside it.
 *
 * <p>Presence and registration stay separate calls, because that split is what several cases
 * are about: a colony given only {@link #placeColonies} is the off-economy shape vanilla builds
 * Galatia Academy in, and one given only {@link #listColonies} is the economy-registered shape
 * with nothing to find by walking entities.
 */
public final class ColonyPlacementFixture {

    private ColonyPlacementFixture() {
        // fixture of static wiring, no instances.
    }

    /**
     * Sites the colonies in {@code location}, each on the entity it was built with - what the
     * entity walk finds, whether or not the economy also lists them.
     */
    public static void placeColonies(LocationAPI location, MarketAPI... colonies) {
        MarketPlacementFixture.placeMarketsIn(location, colonies);
    }

    /** Registers the colonies with the economy, in the order it will list them. */
    public static void listColonies(
            EconomyAPI economyMock,
            LocationAPI location,
            MarketAPI... colonies) {

        MarketPlacementFixture.listMarketsIn(economyMock, location, colonies);
    }
}
