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

    /**
     * Hangs the colonies on the location's own bodies without saying where each one stands - the
     * half of siting that decides what an entity walk finds, and so how an unregistered colony is
     * found at all.
     *
     * @param location the location whose bodies carry them
     * @param colonies the colonies to hang
     */
    public static void hangColoniesOnEntitiesIn(LocationAPI location, MarketAPI... colonies) {
        MarketPlacementFixture.hangMarketsOnBodiesIn(location, colonies);
    }

    /**
     * Says where each colony stands, without putting it among the location's bodies - the other
     * half, and the one a rule asking where a colony is now reads.
     *
     * @param location the location the colonies stand in
     * @param colonies the colonies standing there
     */
    public static void standColoniesIn(LocationAPI location, MarketAPI... colonies) {
        MarketPlacementFixture.standMarketsIn(location, colonies);
    }

    /** Registers the colonies with the economy, in the order it will list them. */
    public static void listColonies(
            EconomyAPI economyMock,
            LocationAPI location,
            MarketAPI... colonies) {

        MarketPlacementFixture.listMarketsIn(economyMock, location, colonies);
    }
}
