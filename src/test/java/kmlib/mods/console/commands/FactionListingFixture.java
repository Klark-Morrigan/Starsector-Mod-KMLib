package kmlib.mods.console.commands;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.RelationshipAPI;

import kmlib.starsector.factions.FactionCustomFixture;
import kmlib.testfixtures.starsector.markets.colonies.ColonyPlacementFixture;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A sector posed the way the faction listing reads one, with the factions to fill it.
 *
 * <p>Shared by the two suites over the listing rather than living in either: the report's suite
 * poses a whole sector to assert on the text, and the command's poses one because a run cannot get
 * to the parsing without a sector behind it. Written twice, the two would be free to drift into
 * posing different sectors for the same run.
 *
 * <p>Factions are this fixture's own rather than a colony fixture's: the listing reads them for
 * their names, territoriality and player attitude, none of which a colony has business carrying.
 *
 * <p>Places are wired the way the game wires one - the economy lists the colonies, and the location
 * carries the entities they sit on - so both halves of the colony read find them. Each colony is
 * also told which system it is in, since that is what the systems clause is built from; a colony
 * sited in hyperspace is told nothing, which is exactly how the game answers for one.
 */
final class FactionListingFixture {

    private static final int NEUTRAL_REPUTATION = 0;

    private final EconomyAPI economyMock = mock(EconomyAPI.class);
    private final SectorAPI sectorMock = mock(SectorAPI.class);

    // Handed to the sector mock once and added to afterwards. Mockito answers the same list
    // instance every call, so a faction or system added later is still listed - which is what lets
    // a case read as "open a sector, then fill it".
    private final List<FactionAPI> factions = new ArrayList<>();
    private final List<StarSystemAPI> systems = new ArrayList<>();

    FactionListingFixture() {

        when(sectorMock.getAllFactions())
            .thenReturn(factions);
        when(sectorMock.getEconomy())
            .thenReturn(economyMock);
        when(sectorMock.getStarSystems())
            .thenReturn(systems);
    }

    /**
     * A faction the listing can name: not territorial, neutral to the player, and declaring no long
     * name - which most do not, and which is what keeps the cases about something else asserting on
     * one name rather than a pair.
     */
    static FactionAPI buildFaction(String id, String displayName) {
        return buildFaction(id, null, displayName, false);
    }

    /** A faction declaring both names, as the listing prints a pair for. */
    static FactionAPI buildNamedFaction(String id, String displayNameLong, String displayName) {
        return buildFaction(id, displayNameLong, displayName, false);
    }

    /** A faction that treats the space around its holdings as its own. */
    static FactionAPI buildTerritorialFaction(String id, String displayName) {
        return buildFaction(id, null, displayName, true);
    }

    SectorAPI getSector() {
        return sectorMock;
    }

    void addFaction(FactionAPI faction) {
        factions.add(faction);
    }

    void addSystemHolding(String systemId, MarketAPI... locationColonies) {

        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.getId())
            .thenReturn(systemId);

        // Each colony names the system it sits in, as a market does once its entity is in one; the
        // sector read itself never asks, so this is the listing's own input rather than the read's.
        for (var colony : locationColonies) {
            when(colony.getStarSystem())
                .thenReturn(systemMock);
        }
        placeColoniesIn(systemMock, locationColonies);
        systems.add(systemMock);
    }

    void setHyperspaceHolding(MarketAPI... locationColonies) {

        var hyperspaceMock = mock(LocationAPI.class);

        placeColoniesIn(hyperspaceMock, locationColonies);

        when(sectorMock.getHyperspace())
            .thenReturn(hyperspaceMock);
    }

    void setPlayerFaction(FactionAPI faction) {

        when(sectorMock.getPlayerFaction())
            .thenReturn(faction);
    }

    // A faction wired the way the listing reads one. The attitude is stubbed through the
    // live-relationship arm on purpose: the fallback arm ends at Misc's colour palette, which reads
    // from settings the test JVM never loads.
    private static FactionAPI buildFaction(
            String id,
            String displayNameLong,
            String displayName,
            boolean isTerritorial) {

        // The relationship finishes its own stubbing before the faction's opens, so the two do not
        // nest into an unfinished-stubbing error.
        var relationshipMock = mock(RelationshipAPI.class);

        when(relationshipMock.getLevel())
            .thenReturn(RepLevel.NEUTRAL);
        when(relationshipMock.getRepInt())
            .thenReturn(NEUTRAL_REPUTATION);

        var custom = isTerritorial
            ? FactionCustomFixture.buildPunitiveExpeditionCustom(true)
            : null;
        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(id);
        when(factionMock.getDisplayName())
            .thenReturn(displayName);
        when(factionMock.getDisplayNameLong())
            .thenReturn(displayNameLong);
        when(factionMock.getRelToPlayer())
            .thenReturn(relationshipMock);
        when(factionMock.getCustom())
            .thenReturn(custom);

        return factionMock;
    }

    // Sites the colonies in one location: the economy lists them, and the location carries the
    // entity each sits on. Both halves, since every case here poses ordinary registered colonies -
    // the listed-versus-unlisted split is the colony read's own suites' business.
    private void placeColoniesIn(LocationAPI location, MarketAPI[] locationColonies) {

        ColonyPlacementFixture.placeColonies(location, locationColonies);
        ColonyPlacementFixture.listColonies(economyMock, location, locationColonies);
    }
}
