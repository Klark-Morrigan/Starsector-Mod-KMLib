package kmlib.starsector.systems.claims;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomEntitySpecAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;

import kmlib.starsector.factions.FactionCustomFixture;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.mockito.MockedStatic;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * The claim contest the claim-reading cases are driven against: one star system, its economy and
 * memory, and builders for the markets and factions that populate it.
 *
 * <p>Markets and factions here carry the state vanilla classifies on - real memory, real custom
 * data - rather than having the classifying reads stubbed out. A garrison in a contest is one
 * the game would call a garrison, and a territorial faction one the game would call territorial,
 * so the scores are those of a real contest. {@code Global.getSector} is the one static stood
 * in for, since the economy is reached through it.
 *
 * <p>Closeable because that static mock must be released in a suite's teardown; the fixture is
 * spent once closed.
 */
final class ClaimContestFixture implements AutoCloseable {

    private final MockedStatic<Global> globalMock;
    private final EconomyAPI economyMock;
    private final MemoryAPI systemMemoryMock;
    private final StarSystemAPI systemMock;

    ClaimContestFixture() {

        systemMemoryMock = mock(MemoryAPI.class);
        systemMock = mock(StarSystemAPI.class);

        when(systemMock.getMemoryWithoutUpdate())
            .thenReturn(systemMemoryMock);

        economyMock = mock(EconomyAPI.class);
        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getEconomy())
            .thenReturn(economyMock);

        // Misc's static initialiser reads Global.getSettings(), so the no-op proxy is installed
        // before Global is stood in for and before any read loads the class.
        StarsectorSettingsFake.installSettings();

        globalMock = mockStatic(Global.class);
        globalMock
            .when(Global::getSector)
            .thenReturn(sectorMock);
    }

    /** The system every read in a suite is posed against. */
    StarSystemAPI getSystem() {
        return systemMock;
    }

    /**
     * Hands the economy the markets present in the system, in the order it will list them, and
     * has each market name the system back - a colony that could not say where it stands would
     * be unreachable to any rule asking who else is in its system.
     */
    void placeMarketsInSystem(MarketAPI... markets) {

        when(economyMock.getMarkets(systemMock))
            .thenReturn(List.of(markets));

        for (var market : markets) {
            when(market.getContainingLocation())
                .thenReturn(systemMock);
        }
    }

    /**
     * Hangs the given markets on entities of the system without registering any of them with the
     * economy - the shape vanilla builds Galatia Academy in, a real colony on a real station that
     * the mechanic's own walk never reaches.
     */
    void placeOffEconomyMarketsInSystem(MarketAPI... markets) {

        var entities = new ArrayList<SectorEntityToken>(markets.length);

        for (var market : markets) {
            var entityMock = mock(SectorEntityToken.class);

            when(entityMock.getMarket())
                .thenReturn(market);

            // Each market names the system back, as the listed ones do. A colony that could not
            // say where it stands reads as standing nowhere, and a rule asking whether anybody has
            // seen it answers that nobody could have been in a system it is not in - which shows
            // as an unvisited derelict reporting itself known.
            when(market.getContainingLocation())
                .thenReturn(systemMock);

            entities.add(entityMock);
        }
        when(systemMock.getAllEntities())
            .thenReturn(entities);
    }

    /** Sets the system's claiming-faction memory flag, the override that settles a claim. */
    void overrideClaimingFaction(String factionId) {
        when(systemMemoryMock.getString(MemFlags.CLAIMING_FACTION))
            .thenReturn(factionId);
    }

    /** Names a market, the label a standing carries for the colonies it is made up of. */
    void nameMarket(MarketAPI market, String name) {
        when(market.getName())
            .thenReturn(name);
    }

    /**
     * Gives a market the identity the engine knows it by, which is a separate fact from the name a
     * reader is shown: vanilla names a station colony and its defending station alike, so a case
     * about the identity has to be able to state the two apart.
     */
    void identifyMarket(MarketAPI market, String marketId) {
        when(market.getId())
            .thenReturn(marketId);
    }

    /**
     * Hangs a map glyph on a market's own entity - a custom-entity spec's authored path and colour,
     * which is where vanilla keeps a station's icon. A plain market built here has no primary entity
     * at all, so a colony is unmarked unless a case says otherwise.
     */
    void giveMarketAMapIcon(MarketAPI market, String iconName, Color iconColour) {

        // The spec is built and stubbed before the entity's own stubbing opens, so the two do not
        // nest into an unfinished-stubbing error.
        var entitySpecMock = mock(CustomEntitySpecAPI.class);

        when(entitySpecMock.getIconName())
            .thenReturn(iconName);
        when(entitySpecMock.getIconColor())
            .thenReturn(iconColour);

        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.getCustomEntitySpec())
            .thenReturn(entitySpecMock);
        when(market.getPrimaryEntity())
            .thenReturn(entityMock);
    }

    /** Raises a market's military flag, the condition behind vanilla's flat garrison bonus. */
    void markMarketAsMilitary(MarketAPI market) {
        when(market.getMemoryWithoutUpdate().getBoolean(MemFlags.MARKET_MILITARY))
            .thenReturn(true);
    }

    /**
     * Hangs vanilla's abandoned-station condition on a market - a derelict nobody has ever lived
     * on. The mechanic weighs it like any other colony; what the condition changes is only
     * whether a surface is entitled to name it.
     */
    void markMarketAsAbandonedStation(MarketAPI market) {
        when(market.hasCondition(Conditions.ABANDONED_STATION))
            .thenReturn(true);
    }

    /**
     * Records the player as having been in the system, the way vanilla does when their fleet
     * arrives. Left unset otherwise, since an unvisited system is where a colony that would
     * leak has to be held back.
     */
    void markSystemAsEntered() {
        when(systemMock.isEnteredByPlayer())
            .thenReturn(true);
    }

    /**
     * Makes a faction the player's own - the one presence the mechanic scores but never lets
     * claim, whatever its configured territoriality says.
     */
    void markFactionAsPlayer(FactionAPI faction) {
        when(faction.isPlayerFaction())
            .thenReturn(true);
    }

    /** A market of the given faction and size, visible to the player. */
    MarketAPI buildMarket(FactionAPI faction, int size) {
        return createMarket(faction, size, false);
    }

    /**
     * A market the economy does not surface publicly - a base, not an undiscovered colony;
     * hiddenness and the entity's discovery are independent, and the mechanic reads only this.
     * It is present for its faction's sibling count but is never scored on its own account,
     * which is the asymmetry worth stating at a call site.
     */
    MarketAPI buildHiddenMarket(FactionAPI faction, int size) {
        return createMarket(faction, size, true);
    }

    /**
     * The condition-only market every uninhabited planet carries to hold its hazard and atmosphere
     * conditions - owned by a real faction, hung on the entity, never registered with the economy.
     * A survey therefore puts one of these in reach of any read that walks a system's entities, and
     * they outnumber real colonies by a wide margin, so admitting one is how a system reports a
     * faction that is not there at all.
     */
    MarketAPI buildConditionOnlyMarket(FactionAPI faction, int size) {

        var marketMock = createMarket(faction, size, false);

        when(marketMock.isPlanetConditionMarketOnly())
            .thenReturn(true);

        return marketMock;
    }

    FactionAPI buildFaction(String id, boolean isTerritorial) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(id);
        when(factionMock.getCustom())
            .thenReturn(FactionCustomFixture.buildPunitiveExpeditionCustom(isTerritorial));

        // Whether this is the neutral faction is answered off the faction, as the engine answers
        // it: the colony kind read parts an unowned derelict from a station somebody keeps on
        // exactly this question, so leaving it false would pose every derelict here as a manned
        // outpost.
        when(factionMock.isNeutralFaction())
            .thenReturn(Factions.NEUTRAL.equals(id));

        return factionMock;
    }

    private MarketAPI createMarket(FactionAPI faction, int size, boolean isHidden) {

        // The owner's ID is read before the market's stubbing opens, so the two mocks do not nest
        // into an unfinished-stubbing error.
        var factionId = faction == null ? null : faction.getId();
        var marketMock = mock(MarketAPI.class);

        // The owner answers on both readings, as a real market's does - they are one fact in the
        // game, and a rule that tells owners apart would see none if only one of them answered.
        when(marketMock.getFaction())
            .thenReturn(faction);
        when(marketMock.getFactionId())
            .thenReturn(factionId);
        when(marketMock.getSize())
            .thenReturn(size);
        when(marketMock.isHidden())
            .thenReturn(isHidden);

        // Every market carries memory, so the military read runs for real against it and a
        // market is a garrison only once its flag is actually raised.
        when(marketMock.getMemoryWithoutUpdate())
            .thenReturn(mock(MemoryAPI.class));

        return marketMock;
    }

    @Override
    public void close() {
        globalMock.close();
        StarsectorSettingsFake.clearSettings();
    }
}
