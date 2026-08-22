package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
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
    private final Map<String, String> sightedLocationIdsByColonyId = new HashMap<>();
    private final StarSystemAPI systemMock;

    private int namedColonyCount;

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
     * What the player has seen and where, as a colony set reads it. Live rather than a snapshot,
     * so a case may build its set first and record the sighting after.
     */
    public ColonySightings getSightings() {
        return sightedLocationIdsByColonyId::get;
    }

    /**
     * Backs the sector's memory with a real map, so the production recorders write where they
     * really write and a later read finds it there.
     *
     * <p>What a case exercising the recorders needs, as against one merely stating what has been
     * seen: {@link #markColoniesAsSighted} hands a colony set an answer, while a recorder has to
     * be given somewhere to put one.
     */
    public void openSectorMemory() {

        var storedValues = new HashMap<String, Object>();
        var memoryMock = mock(MemoryAPI.class);

        when(memoryMock.contains(anyString()))
            .thenAnswer(invocation -> storedValues.containsKey(invocation.getArgument(0)));
        when(memoryMock.get(anyString()))
            .thenAnswer(invocation -> storedValues.get(invocation.getArgument(0)));

        doAnswer(invocation -> storedValues.put(
                invocation.getArgument(0),
                invocation.getArgument(1)))
            .when(memoryMock)
            .set(anyString(), any());

        when(sectorMock.getMemoryWithoutUpdate())
            .thenReturn(memoryMock);
    }

    /**
     * Records the player as having seen these colonies standing in this system - their own route
     * to having heard of whatever is here.
     *
     * <p>Stated rather than defaulted, because an unseen colony is the interesting half: it is
     * where a colony that would otherwise leak has to be held back.
     *
     * <p>Per colony rather than per system, which is the whole of what the sighting rule turns
     * on: a colony that has since moved, or was founded after the player passed through, is one
     * this system carries no sighting of however often the player has crossed it.
     */
    public void markColoniesAsSighted(MarketAPI... colonies) {

        for (var colony : colonies) {
            sightedLocationIdsByColonyId.put(nameColony(colony), systemMock.getId());
        }
    }

    /**
     * Records the player as having seen these colonies in some other system - a sighting that no
     * longer describes where they stand, which is what a colony that has moved since carries.
     */
    public void markColoniesAsSightedElsewhere(String otherSystemId, MarketAPI... colonies) {

        for (var colony : colonies) {
            sightedLocationIdsByColonyId.put(nameColony(colony), otherSystemId);
        }
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

    // The id a sighting is kept against, given to the colony here if it has none. Colonies are
    // built without one because almost nothing reads it, and a mock answers null until asked to
    // answer otherwise - which would have every unnamed colony share one entry in the register.
    private String nameColony(MarketAPI colony) {

        var colonyId = colony.getId();

        if (colonyId != null) {
            return colonyId;
        }
        namedColonyCount++;

        var namedColonyId = "colony_" + namedColonyCount;

        when(colony.getId())
            .thenReturn(namedColonyId);

        return namedColonyId;
    }
}
