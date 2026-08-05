package kmlib.starsector.systems.claims;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;

import kmlib.starsector.factions.FactionCustomFixture;
import kmlib.starsector.testing.StarsectorSettingsFake;

import org.mockito.MockedStatic;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * The claim contest both claim-reading suites are driven against: one star system, its economy
 * and memory, and builders for the markets and factions that populate it.
 *
 * <p>Shared so the narrow claimant read and the full breakdown are exercised against the same
 * notion of a contest. The two are meant to resolve identically, which they cannot be shown to
 * do if each suite builds its own world.
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

    /** Hands the economy the markets present in the system, in the order it will list them. */
    void placeMarketsInSystem(MarketAPI... markets) {
        when(economyMock.getMarkets(systemMock))
            .thenReturn(List.of(markets));
    }

    /** Sets the system's claiming-faction memory flag, the override that settles a claim. */
    void overrideClaimingFaction(String factionId) {
        when(systemMemoryMock.getString(MemFlags.CLAIMING_FACTION))
            .thenReturn(factionId);
    }

    /** Raises a market's military flag, the condition behind vanilla's flat garrison bonus. */
    void markMarketAsMilitary(MarketAPI market) {
        when(market.getMemoryWithoutUpdate().getBoolean(MemFlags.MARKET_MILITARY))
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

    FactionAPI buildFaction(String id, boolean isTerritorial) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(id);
        when(factionMock.getCustom())
            .thenReturn(FactionCustomFixture.buildPunitiveExpeditionCustom(isTerritorial));

        return factionMock;
    }

    private MarketAPI createMarket(FactionAPI faction, int size, boolean isHidden) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(faction);
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
