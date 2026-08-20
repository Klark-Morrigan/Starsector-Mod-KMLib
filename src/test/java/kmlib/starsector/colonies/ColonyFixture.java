package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The system every colony read is posed against: one star system in a sector, its economy, and
 * builders for the colonies that populate it.
 *
 * <p>Shared so the direct per-system read and the per-pass index are exercised against the same
 * system. The index's whole contract is that it answers exactly what the direct read answers,
 * which cannot be shown if each suite builds its own world.
 *
 * <p>Presence and registration are stated separately, because that split is the thing under
 * test: a colony is sited in the system by {@link #placeColoniesInSystem} and registered with
 * the economy by {@link #listColoniesInEconomy}, and a colony given only the first is the
 * off-economy shape vanilla builds Galatia Academy in. Folding them into one call would leave
 * every case unable to pose that difference.
 *
 * <p>Colonies themselves are {@link ColonyMarketFixture}'s, forwarded here so a suite holding
 * this fixture builds and places them through the one object. What a colony is does not depend
 * on the world it is placed in, and a suite posing a different world - hyperspace, a whole
 * sector - shares those builders rather than restating them.
 */
public final class ColonyFixture {

    // The size every colony takes unless a case asks for another, named here as well so a suite
    // posing a system reads it off the fixture it is already holding.
    public static final int DEFAULT_COLONY_SIZE = ColonyMarketFixture.DEFAULT_COLONY_SIZE;

    private final EconomyAPI economyMock;
    private final SectorAPI sectorMock;
    private final StarSystemAPI systemMock;

    /**
     * Opens a sector holding exactly one system.
     *
     * @param systemId the system's id, as {@code StarSystemAPI#getId} reports it; null poses the
     *                 unkeyable system an id-keyed reader cannot memo on
     */
    public ColonyFixture(String systemId) {

        systemMock = mock(StarSystemAPI.class);

        when(systemMock.getId())
            .thenReturn(systemId);

        economyMock = mock(EconomyAPI.class);
        sectorMock = mock(SectorAPI.class);

        when(sectorMock.getEconomy())
            .thenReturn(economyMock);
        when(sectorMock.getStarSystems())
            .thenReturn(List.of(systemMock));
    }

    /** The sector every read in a suite is made against. */
    public SectorAPI getSector() {
        return sectorMock;
    }

    /** The one system the sector holds. */
    public StarSystemAPI getSystem() {
        return systemMock;
    }

    /**
     * Sites the colonies in the system, each on the entity it was built with - what the entity
     * walk finds, whether or not the economy also lists them.
     */
    public void placeColoniesInSystem(MarketAPI... colonies) {
        ColonyPlacementFixture.placeColonies(systemMock, colonies);
    }

    /** Registers the colonies with the economy, in the order it will list them. */
    public void listColoniesInEconomy(MarketAPI... colonies) {
        ColonyPlacementFixture.listColonies(economyMock, systemMock, colonies);
    }

    /**
     * Records the player as having been in the system, the way vanilla does when their fleet
     * arrives - the player's own route to having seen whatever stands here.
     *
     * <p>Stated rather than defaulted, because an unentered system is the interesting half: it
     * is where a colony that would otherwise leak has to be held back.
     */
    public void markSystemAsEntered() {
        when(systemMock.isEnteredByPlayer())
            .thenReturn(true);
    }

    // Each colony builder below forwards to the one named for it on ColonyMarketFixture, which
    // is where the shape is described. Restating those descriptions here would put two accounts
    // of one colony a rename apart, and a suite reading this fixture is one hop from the real
    // one either way.

    public MarketAPI buildConditionOnlyMarket() {
        return ColonyMarketFixture.buildConditionOnlyMarket();
    }

    public MarketAPI buildDerelictStation() {
        return ColonyMarketFixture.buildDerelictStation();
    }

    public MarketAPI buildOutpost(String factionId) {
        return ColonyMarketFixture.buildOutpost(factionId);
    }

    public MarketAPI buildFoundConcealedColony(String factionId) {
        return ColonyMarketFixture.buildFoundConcealedColony(factionId);
    }

    public MarketAPI buildSiblingMarketOn(MarketAPI colony, int size) {
        return ColonyMarketFixture.buildSiblingMarketOn(colony, size);
    }

    public MarketAPI buildUnfoundConcealedColony(String factionId) {
        return ColonyMarketFixture.buildUnfoundConcealedColony(factionId);
    }

    public MarketAPI buildUnfoundOpenColony(String factionId) {
        return ColonyMarketFixture.buildUnfoundOpenColony(factionId);
    }

    public MarketAPI buildVisibleColony(String factionId) {
        return ColonyMarketFixture.buildVisibleColony(factionId);
    }

    public MarketAPI buildVisibleColonyOfSize(String factionId, int size) {
        return ColonyMarketFixture.buildVisibleColonyOfSize(factionId, size);
    }
}
