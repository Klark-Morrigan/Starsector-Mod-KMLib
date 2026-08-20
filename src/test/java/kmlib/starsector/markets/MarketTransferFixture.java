package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.econ.RecentUnrest;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The colonies a takeover is posed against - each one carrying what the owner it is leaving left
 * behind.
 *
 * <p>Built on {@link MarketOwnershipFixture}'s colonies rather than beside them, since the half of
 * a takeover that is about the incoming owner <em>is</em> an ownership change: a colony posed here
 * changes hands exactly as one posed there does, and a case reading the counters or the flag
 * afterwards reads them through that fixture. What is added is only what an owner leaves: the
 * administrator they posted, the free port they opened, the stockpiling they turned on, the unrest
 * their rule accrued, and the account at the counter their colony's own production was sold over.
 *
 * <p>Each of those answers from its own state, for the reason that fixture gives - a takeover is
 * judged by what the colony reads as afterwards, and a colony answering what a case stubbed in
 * advance would let a run that detached nothing pass. The two that are verified as calls rather
 * than read back are the unrest and the account, both being plugins a colony holds rather than
 * values it carries.
 */
public final class MarketTransferFixture {

    /** The faction a colony is posed under whenever the case is not about the player. */
    public static final String FACTION_OWNER_ID = MarketOwnershipFixture.FACTION_OWNER_ID;

    private MarketTransferFixture() {
        // fixture of static builders, no instances.
    }

    /**
     * A colony the player has been running: trading over the counters a player colony has, and
     * carrying every arrangement its owner made. The one shape that has an outstanding account to
     * settle, local resources being the player's own counter.
     */
    public static MarketAPI buildPlayerColonyAsItsOwnerLeftIt() {

        return stubLeavings(
            MarketOwnershipFixture.buildColonyTradingThrough(
                Factions.PLAYER,
                Submarkets.LOCAL_RESOURCES,
                Submarkets.SUBMARKET_STORAGE),
            true);
    }

    /**
     * A colony a faction has been running: trading over the counters an NPC colony has, and
     * carrying the same arrangements. Nothing here keeps an account, which is what a colony taken
     * from a faction has none of to settle.
     */
    public static MarketAPI buildFactionColonyAsItsOwnerLeftIt() {

        return stubLeavings(
            MarketOwnershipFixture.buildColonyTradingThrough(
                FACTION_OWNER_ID,
                Submarkets.SUBMARKET_OPEN,
                Submarkets.SUBMARKET_BLACK),
            true);
    }

    /**
     * A colony whose owner was never resented - no unrest condition on it at all, which is the
     * state a read that clears unrest must not create one in.
     */
    public static MarketAPI buildContentedFactionColony() {

        return stubLeavings(
            MarketOwnershipFixture.buildColonyTradingThrough(
                FACTION_OWNER_ID,
                Submarkets.SUBMARKET_OPEN),
            false);
    }

    /**
     * The player's colony with a local resources counter that keeps no account - a plugin some
     * other mod put there, which never took anything on credit and has no month-end billing step
     * to be asked for. The shape that tells whether the settling reaches for the account or
     * assumes one.
     */
    public static MarketAPI buildPlayerColonyWhoseCounterKeepsNoAccount() {

        var market = buildPlayerColonyAsItsOwnerLeftIt();
        var counterMock = mock(SubmarketAPI.class);

        when(counterMock.getSpecId())
            .thenReturn(Submarkets.LOCAL_RESOURCES);
        when(counterMock.getPlugin())
            .thenReturn(mock(SubmarketPlugin.class));

        // Named rather than added to the colony's own counters, so the one counter this shape is
        // about answers a plugin of its own while every other read of the market is unchanged.
        doReturn(counterMock)
            .when(market)
            .getSubmarket(Submarkets.LOCAL_RESOURCES);

        return market;
    }

    /**
     * The account kept at the colony's local resources counter, as what settles it: the listener
     * the month-end billing step is declared on. Null where the colony has no such counter.
     */
    public static EconomyTickListener readLocalResourcesAccount(MarketAPI market) {

        var localResources = market.getSubmarket(Submarkets.LOCAL_RESOURCES);

        return localResources == null
            ? null
            : (EconomyTickListener) localResources.getPlugin();
    }

    /** The unrest the colony carries, or null where it carries none. */
    public static RecentUnrest readRecentUnrest(MarketAPI market) {

        var condition = market.getCondition(Conditions.RECENT_UNREST);

        return condition == null
            ? null
            : (RecentUnrest) condition.getPlugin();
    }

    // Everything an owner leaves behind, hung on a colony that already trades and flies a flag.
    private static MarketAPI stubLeavings(MarketAPI marketMock, boolean isResented) {

        stubAdministrator(marketMock);
        stubFreePort(marketMock);
        stubStockpileUse(marketMock);
        stubRecentUnrest(marketMock, isResented);

        return marketMock;
    }

    // The person the outgoing owner posted here, and the post they can be removed from. Held as
    // state so a case reads the empty post a takeover leaves rather than counting the removal.
    private static void stubAdministrator(MarketAPI marketMock) {

        var administrator = new AtomicReference<>(mock(PersonAPI.class));

        when(marketMock.getAdmin())
            .thenAnswer(invocation -> administrator.get());

        doAnswer(invocation -> {
                administrator.set(invocation.getArgument(0));
                return null;
            })
            .when(marketMock)
            .setAdmin(any());
    }

    // The trading arrangement the outgoing owner made, open to begin with so a case reading it
    // closed is reading what the takeover did rather than the state it started in.
    private static void stubFreePort(MarketAPI marketMock) {

        var isFreePort = new AtomicBoolean(true);

        when(marketMock.isFreePort())
            .thenAnswer(invocation -> isFreePort.get());

        doAnswer(invocation -> {
                isFreePort.set(invocation.getArgument(0));
                return null;
            })
            .when(marketMock)
            .setFreePort(anyBoolean());
    }

    // The instruction the outgoing owner gave about covering shortages from stockpiles, on to
    // begin with for the same reason the free port is open.
    private static void stubStockpileUse(MarketAPI marketMock) {

        var isUsingStockpiles = new AtomicBoolean(true);

        when(marketMock.isUseStockpilesForShortages())
            .thenAnswer(invocation -> isUsingStockpiles.get());

        doAnswer(invocation -> {
                isUsingStockpiles.set(invocation.getArgument(0));
                return null;
            })
            .when(marketMock)
            .setUseStockpilesForShortages(anyBoolean());
    }

    // How resented the outgoing owner was, as the condition that carries it. A colony that carries
    // none answers null to the read, which is the shape a takeover must not turn into a condition.
    private static void stubRecentUnrest(MarketAPI marketMock, boolean isResented) {

        if (!isResented) {
            return;
        }

        var conditionMock = mock(MarketConditionAPI.class);

        when(conditionMock.getPlugin())
            .thenReturn(mock(RecentUnrest.class));
        when(marketMock.getCondition(Conditions.RECENT_UNREST))
            .thenReturn(conditionMock);
    }
}
