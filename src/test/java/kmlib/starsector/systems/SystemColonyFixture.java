package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.ArrayList;
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
 * <p>Colonies are built through named builders rather than by flag, so a case says which kind of
 * colony it poses. The flags themselves stay behind that boundary: three adjacent booleans are
 * transposable without failing, and no suite has business setting them directly.
 */
final class SystemColonyFixture {

    // The size every colony takes unless a case asks for another. Colony size only decides which
    // of two markets on one place wins, so a case not posing that collision is not about size.
    static final int DEFAULT_COLONY_SIZE = 3;

    private final EconomyAPI economyMock;
    private final SectorAPI sectorMock;
    private final StarSystemAPI systemMock;

    /**
     * Opens a sector holding exactly one system.
     *
     * @param systemId the system's id, as {@code StarSystemAPI#getId} reports it; null poses the
     *                 unkeyable system an id-keyed reader cannot memo on
     */
    SystemColonyFixture(String systemId) {

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
    SectorAPI getSector() {
        return sectorMock;
    }

    /** The one system the sector holds. */
    StarSystemAPI getSystem() {
        return systemMock;
    }

    /**
     * Sites the colonies in the system, each on the entity it was built with - what the entity
     * walk finds, whether or not the economy also lists them.
     */
    void placeColoniesInSystem(MarketAPI... colonies) {

        var entities = new ArrayList<SectorEntityToken>();
        for (var colony : colonies) {
            entities.add(colony.getPrimaryEntity());
        }
        when(systemMock.getAllEntities())
            .thenReturn(entities);
    }

    /** Registers the colonies with the economy, in the order it will list them. */
    void listColoniesInEconomy(MarketAPI... colonies) {
        when(economyMock.getMarkets(systemMock))
            .thenReturn(List.of(colonies));
    }

    /** An ordinary colony: owned, open, on an entity the player has found. */
    MarketAPI buildVisibleColony(String factionId) {
        return buildVisibleColonyOfSize(factionId, DEFAULT_COLONY_SIZE);
    }

    MarketAPI buildVisibleColonyOfSize(String factionId, int size) {
        return buildColonyOnItsOwnEntity(factionId, size, false, false, false);
    }

    /** A base once raided: its entity is discovered, its market stays hidden for good. */
    MarketAPI buildFoundConcealedColony(String factionId) {
        return buildColonyOnItsOwnEntity(factionId, DEFAULT_COLONY_SIZE, true, false, false);
    }

    /**
     * A base still to be found: concealed and on a discoverable entity, so it fails both arms of
     * the known read - the one shape the fog has to keep back.
     */
    MarketAPI buildUnfoundConcealedColony(String factionId) {
        return buildColonyOnItsOwnEntity(factionId, DEFAULT_COLONY_SIZE, true, true, false);
    }

    /**
     * A bare planet's placeholder, the condition-only market every uninhabited world carries to
     * hold its hazard and atmosphere. Owned by nobody in particular, and rejected on that arm.
     */
    MarketAPI buildConditionOnlyMarket() {
        return buildColonyOnItsOwnEntity("neutral", DEFAULT_COLONY_SIZE, false, false, true);
    }

    /**
     * A second market object on an existing colony's entity, under the same owner - the shape a
     * mod builds when it supersedes a colony by adding beside vanilla's rather than replacing.
     */
    MarketAPI buildSiblingMarketOn(MarketAPI colony, int size) {

        // Read off the sibling before the new mock's stubbing opens, so the two do not nest into
        // an unfinished-stubbing error.
        var faction = colony.getFaction();
        var entity = colony.getPrimaryEntity();
        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(faction);
        when(marketMock.getPrimaryEntity())
            .thenReturn(entity);
        when(marketMock.getSize())
            .thenReturn(size);

        return marketMock;
    }

    // A market on an entity of its own, wired both ways: the market names the entity as its
    // place, and the entity carries the market - which is how an unlisted colony is found at all.
    private static MarketAPI buildColonyOnItsOwnEntity(
            String factionId,
            int size,
            boolean isHidden,
            boolean isEntityDiscoverable,
            boolean isConditionOnly) {

        // The entity and the faction each finish their own stubbing before the market's opens,
        // so the two do not nest into an unfinished-stubbing error.
        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.isDiscoverable())
            .thenReturn(isEntityDiscoverable);

        var factionMock = buildFaction(factionId);
        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(factionMock);
        when(marketMock.getPrimaryEntity())
            .thenReturn(entityMock);
        when(marketMock.getSize())
            .thenReturn(size);
        when(marketMock.isHidden())
            .thenReturn(isHidden);
        when(marketMock.isPlanetConditionMarketOnly())
            .thenReturn(isConditionOnly);
        when(entityMock.getMarket())
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
