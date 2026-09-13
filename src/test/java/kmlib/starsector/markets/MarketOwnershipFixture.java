package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;

import kmlib.testfixtures.starsector.markets.MarketPlacementFixture;
import kmlib.testfixtures.starsector.markets.MarketStateFixture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * The colonies an ownership change is posed against, and the reads that say what one became.
 *
 * <p>Stateful where its neighbours are not, and that is the point: an ownership rule is judged by
 * what the market reads as afterwards, so a colony here answers from what has been done to it
 * rather than from what a case stubbed in advance. A colony that kept answering its original owner
 * would let a rule that changed nothing pass, and a colony that could not be asked twice would
 * leave the round trip - player, faction, player again - unwritable, which is the run an asymmetry
 * shows up in.
 *
 * <p>Two owners exist here, the player and one faction, and their tariff rates differ. Equal rates
 * would let a run that never recomputed the tariff pass, since the number already on the market
 * would be the number expected of it. A colony posed under any other ID has no faction to read.
 *
 * <p>Separate from {@link MarketStateFixture}, which poses what kind of market a state read is
 * asked about, and from {@link MarketPlacementFixture}, which poses where one sits. Neither is
 * mutable, and neither carries the submarkets, entities and tariff an ownership change touches.
 *
 * <p>The siblings that are not separate are the ones posing an operation that <em>ends</em> in an
 * ownership change, since such an operation poses a market that changes hands:
 * {@link kmlib.starsector.markets.colonisation.MarketColonisationFixture} composes the wirings below rather than stubbing its own, and
 * {@link kmlib.starsector.markets.ownership.MarketTransferFixture} hangs what an outgoing owner leaves behind on the colonies built
 * here.
 */
public final class MarketOwnershipFixture {

    /** The faction a colony is posed under whenever the case is not about the player. */
    public static final String FACTION_OWNER_ID = "hegemony";

    // What each owner levies, as the fraction a colony's tariff modifier is set to. Distinct per
    // owner so a case can tell whose rate is on the market.
    private static final Map<String, Float> TARIFF_FRACTIONS_BY_OWNER = Map.of(
        Factions.PLAYER,
        0.3f,
        FACTION_OWNER_ID,
        0.2f);

    private MarketOwnershipFixture() {
        // fixture of static builders, no instances.
    }

    /** A colony under an owner, trading through nothing and running no industry. */
    public static MarketAPI buildColonyHeldBy(String factionId) {
        return buildColony(factionId, Set.of(), List.of(), true);
    }

    /** A colony whose one industry is the given one - what a submarket verdict turns on. */
    public static MarketAPI buildColonyRunning(String factionId, String industryId) {
        return buildColony(factionId, Set.of(industryId), List.of(), true);
    }

    /** A colony already trading through the given submarkets, which is the state to change from. */
    public static MarketAPI buildColonyTradingThrough(String factionId, String... submarketIds) {
        return buildColony(factionId, Set.of(), List.of(submarketIds), true);
    }

    /**
     * A colony that does not list its own body among its connected entities - the case that tells
     * whether the body is re-flagged in its own right or only by being swept up with the rest.
     */
    public static MarketAPI buildColonyNotListingItsOwnBody(String factionId) {
        return buildColony(factionId, Set.of(), List.of(), false);
    }

    /**
     * A colony with no body and nothing connected to it - the shape a market carries when it
     * stands for a place the game never gave an entity, which is what the flag half of an
     * ownership change has nothing to say to.
     */
    public static MarketAPI buildColonyWithNothingAttached(String factionId) {

        var market = buildColony(factionId, Set.of(), List.of(), true);

        // Re-stubbed rather than built absent, so the one shape that differs from every other
        // colony here says so in one place instead of threading a flag through the builder.
        doReturn(null)
            .when(market)
            .getPrimaryEntity();

        doReturn(null)
            .when(market)
            .getConnectedEntities();

        return market;
    }

    /** The submarkets the colony trades through now, in the order it opened them. */
    public static List<String> readSubmarketIds(MarketAPI market) {
        return market.getSubmarketsCopy().stream()
            .map(SubmarketAPI::getSpecId)
            .toList();
    }

    /** The storage counter's plugin, which is what records that the player has paid to open it. */
    public static StoragePlugin readStoragePlugin(MarketAPI market) {
        return (StoragePlugin) market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin();
    }

    /** The flag an entity flies now, as the faction ID, or null while it flies none. */
    public static String readFactionId(SectorEntityToken entity) {
        return entity.getFaction() == null
            ? null
            : entity.getFaction().getId();
    }

    // A colony wired to answer from its own state: the owner it was last given, the submarkets it
    // has been left with, and a tariff that carries whatever was last written to it.
    private static MarketAPI buildColony(
            String factionId,
            Set<String> industryIds,
            Collection<String> submarketIds,
            boolean isOwnBodyListedAsConnected) {

        // Everything the market answers with finishes its own stubbing before the market's opens,
        // so the two do not nest into an unfinished-stubbing error.
        var factionsById = buildFactions();
        var ownBody = buildEntity(factionsById);
        var orbitalStation = buildEntity(factionsById);
        var connectedEntities = isOwnBodyListedAsConnected
            ? new LinkedHashSet<>(List.of(ownBody, orbitalStation))
            : new LinkedHashSet<>(List.of(orbitalStation));

        var submarkets = new LinkedHashMap<String, SubmarketAPI>();

        for (var submarketId : submarketIds) {
            submarkets.put(submarketId, buildSubmarket(submarketId));
        }

        var marketMock = mock(MarketAPI.class);

        stubEntities(marketMock, ownBody, connectedEntities);
        stubOwner(marketMock, factionsById, factionId);
        stubIndustries(marketMock, industryIds);
        stubSubmarkets(marketMock, submarkets);
        stubTariff(marketMock);

        return marketMock;
    }

    // Where the colony sits: the body it stands on, and everything flying its flag alongside.
    // Both are fixed for the colony's life - an ownership change re-flags them rather than
    // exchanging them - so these are the one part of the market that answers a plain value.
    private static void stubEntities(
            MarketAPI marketMock,
            SectorEntityToken ownBody,
            Set<SectorEntityToken> connectedEntities) {

        when(marketMock.getPrimaryEntity())
            .thenReturn(ownBody);
        when(marketMock.getConnectedEntities())
            .thenReturn(connectedEntities);
    }

    // Who holds the colony, as the two reads that have to move together: the ID it was last
    // given, and the mark saying that ID is the player's. Held apart in the market's own state
    // rather than derived from each other, so a rule that sets one and forgets the other is
    // visible to a case instead of being papered over here.
    //
    // Package-visible, as are the submarket, tariff and faction wirings below: an ownership
    // change is not the only mutation posed against a market in this package, and a second
    // fixture stubbing these by hand would be a second answer to what changing hands does to a
    // market - free to disagree with this one.
    public static void stubOwner(
            MarketAPI marketMock,
            Map<String, FactionAPI> factionsById,
            String factionId) {

        var ownerId = new AtomicReference<>(factionId);

        // Posed in step with the owner the colony is built under, and moved only by the setter
        // afterwards. A colony built as the player's that read as nobody's until something set the
        // flag would stand for a state no save holds, and any rule keying on player ownership
        // before it changes hands - the account its outgoing owner is billed for, among them -
        // would be posed against the wrong colony.
        var isPlayerOwned = new AtomicBoolean(Factions.PLAYER.equals(factionId));

        when(marketMock.getFaction())
            .thenAnswer(invocation -> factionsById.get(ownerId.get()));
        when(marketMock.getFactionId())
            .thenAnswer(invocation -> ownerId.get());

        doAnswer(invocation -> {
                ownerId.set(invocation.getArgument(0));
                return null;
            })
            .when(marketMock)
            .setFactionId(anyString());

        when(marketMock.isPlayerOwned())
            .thenAnswer(invocation -> isPlayerOwned.get());

        doAnswer(invocation -> {
                isPlayerOwned.set(invocation.getArgument(0));
                return null;
            })
            .when(marketMock)
            .setPlayerOwned(anyBoolean());
    }

    // What the colony runs, which is read-only here: no ownership change builds or closes an
    // industry, it only reads them to decide which counters the new owner trades over.
    private static void stubIndustries(MarketAPI marketMock, Set<String> industryIds) {
        when(marketMock.hasIndustry(anyString()))
            .thenAnswer(invocation -> industryIds.contains(invocation.getArgument(0)));
    }

    // The counters the colony trades over, opened and closed as it changes hands. A counter
    // opened twice yields the same one back, because the map is keyed by submarket ID - which is
    // what lets a case tell a counter left alone from one closed and opened again.
    public static void stubSubmarkets(
            MarketAPI marketMock,
            Map<String, SubmarketAPI> submarkets) {

        when(marketMock.hasSubmarket(anyString()))
            .thenAnswer(invocation -> submarkets.containsKey(invocation.getArgument(0)));

        doAnswer(invocation -> {
                String submarketId = invocation.getArgument(0);
                submarkets.computeIfAbsent(submarketId, MarketOwnershipFixture::buildSubmarket);
                return null;
            })
            .when(marketMock)
            .addSubmarket(anyString());

        doAnswer(invocation -> {
                submarkets.remove(invocation.<String>getArgument(0));
                return null;
            })
            .when(marketMock)
            .removeSubmarket(anyString());

        when(marketMock.getSubmarket(anyString()))
            .thenAnswer(invocation -> submarkets.get(invocation.getArgument(0)));
        when(marketMock.getSubmarketsCopy())
            .thenAnswer(invocation -> new ArrayList<>(submarkets.values()));
    }

    // The tax rate, as the engine's own stat rather than a stubbed number: what a case reads back
    // is then what the modifier arithmetic produced, including a rate written over an earlier one.
    public static void stubTariff(MarketAPI marketMock) {
        when(marketMock.getTariff())
            .thenReturn(new MutableStat(0f));
    }

    // An entity that remembers the flag it was last given, so a case reads what it flies rather
    // than counting the times it was told to change.
    private static SectorEntityToken buildEntity(Map<String, FactionAPI> factionsById) {

        var flownFactionId = new AtomicReference<String>();
        var entityMock = mock(SectorEntityToken.class);

        doAnswer(invocation -> {
                flownFactionId.set(invocation.getArgument(0));
                return null;
            })
            .when(entityMock)
            .setFaction(anyString());

        when(entityMock.getFaction())
            .thenAnswer(invocation -> factionsById.get(flownFactionId.get()));

        return entityMock;
    }

    // A trading counter, named by the submarket it is. Two of them carry more than a bare plugin,
    // those being the counters whose plugin an operation on a colony speaks to: storage records
    // that the player has paid to open it, and local resources keeps the account a colony's
    // outgoing owner is billed for. The account is posed through the listener interface the billing
    // step is declared on rather than through the engine's own plugin class, which reads several
    // game settings while it loads and would have every case here standing a game up to have one.
    private static SubmarketAPI buildSubmarket(String submarketId) {

        var pluginMock = buildSubmarketPlugin(submarketId);
        var submarketMock = mock(SubmarketAPI.class);

        when(submarketMock.getSpecId())
            .thenReturn(submarketId);
        when(submarketMock.getPlugin())
            .thenReturn(pluginMock);

        return submarketMock;
    }

    // The plugin behind one counter, carrying whatever that counter is spoken to about.
    private static SubmarketPlugin buildSubmarketPlugin(String submarketId) {

        if (Submarkets.SUBMARKET_STORAGE.equals(submarketId)) {
            return mock(StoragePlugin.class);
        }

        if (Submarkets.LOCAL_RESOURCES.equals(submarketId)) {
            return mock(
                SubmarketPlugin.class,
                withSettings().extraInterfaces(EconomyTickListener.class));
        }

        return mock(SubmarketPlugin.class);
    }

    // The owners a colony can be posed under, each carrying the tariff fraction it levies.
    public static Map<String, FactionAPI> buildFactions() {

        var factionsById = new LinkedHashMap<String, FactionAPI>();

        TARIFF_FRACTIONS_BY_OWNER.forEach(
            (ownerId, tariffFraction) ->
                factionsById.put(ownerId, buildFaction(ownerId, tariffFraction)));

        return factionsById;
    }

    private static FactionAPI buildFaction(String id, float tariffFraction) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(id);
        when(factionMock.getTariffFraction())
            .thenReturn(tariffFraction);

        return factionMock;
    }
}
