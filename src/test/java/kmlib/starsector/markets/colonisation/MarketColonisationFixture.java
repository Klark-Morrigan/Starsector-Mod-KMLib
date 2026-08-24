package kmlib.starsector.markets.colonisation;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.ConstructionQueue;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import kmlib.starsector.markets.MarketOwnershipFixture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The bodies a colonisation is posed against, and the sector its economy is registered with.
 *
 * <p>Stateful, for the same reason {@link MarketOwnershipFixture} is: founding a colony is judged
 * by what the placeholder market reads as afterwards - no longer survey data, populated, sized,
 * named after its body - and a market answering what a case stubbed in advance would let a run
 * that changed nothing pass. The ownership half of a colonisation is wired by that fixture rather
 * than restated here, so a market posed for founding changes hands exactly as one posed for a
 * takeover does.
 *
 * <p>What it adds is everything founding touches and an ownership change does not: the
 * condition-only flag that <em>is</em> colonisation, the conditions a body carries and whether
 * they have been looked at, the industries and size a colony gains, and the construction queue it
 * starts with. Industries are mutable here where the ownership fixture holds them fixed, founding
 * being the one operation that builds one.
 *
 * <p>Separate from {@link kmlib.testfixtures.starsector.markets.MarketStateFixture}, which poses the four combinations of the two flags
 * a colonisation-eligibility read asks about and nothing else - a market that cannot be mutated
 * says nothing about what founding did to it, and a colonisation case needs both.
 */
public final class MarketColonisationFixture {

    /** The faction a colony is posed under whenever the case is not about the player. */
    public static final String FACTION_OWNER_ID = MarketOwnershipFixture.FACTION_OWNER_ID;

    /** What the body is called, which is the name the colony founded on it takes. */
    public static final String BODY_NAME = "Hesperus";

    // What a placeholder market reads as before anyone settles the body: no size of its own, and
    // an age no founding could produce. Neither is a value a founding sets, which is what keeps a
    // case asserting the size and age founding produced from passing on the state it started in.
    private static final int UNCOLONISED_SIZE = 0;
    private static final float UNCOLONISED_AGE = Float.NaN;

    private MarketColonisationFixture() {
        // fixture of static builders, no instances.
    }

    /** A sector whose economy is there to register a colony with. */
    public static SectorAPI buildSectorWithEconomy() {

        var economyMock = mock(EconomyAPI.class);
        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getEconomy())
            .thenReturn(economyMock);

        return sectorMock;
    }

    /**
     * A sector with no economy - what a caller holds outside a running game, and the one shape
     * that leaves a colony nothing to be registered with.
     */
    public static SectorAPI buildSectorWithoutEconomy() {
        return mock(SectorAPI.class);
    }

    /** The economy a colony is registered with, which is what a case verifies the founding on. */
    public static EconomyAPI readEconomy(SectorAPI sector) {
        return sector.getEconomy();
    }

    /** A bare world: the condition-only market procgen hangs on a planet nobody has settled. */
    public static MarketAPI buildColonisableWorld() {
        return buildColonisableMarket(mock(PlanetAPI.class), Set.of());
    }

    /**
     * A bare world already carrying the given conditions, unsurveyed - what a body reads as
     * before anyone has looked at it, and what a ruined world carries from the colony it was.
     */
    public static MarketAPI buildColonisableWorldCarrying(String... conditionIds) {
        return buildColonisableMarket(mock(PlanetAPI.class), Set.of(conditionIds));
    }

    /**
     * A colonisable place whose body is not a planet. Modded bodies take this shape, and it is
     * the one a report named for colonising a planet has nothing to carry.
     */
    public static MarketAPI buildColonisableStation() {
        return buildColonisableMarket(mock(SectorEntityToken.class), Set.of());
    }

    /**
     * A colonisable place the game gave no body at all - nothing to take a name from, and
     * nothing to point back at the colony.
     */
    public static MarketAPI buildColonisablePlaceWithNoBody() {
        return buildColonisableMarket(null, Set.of());
    }

    /** The body the colony stands on, or null where the place has none. */
    public static SectorEntityToken readBody(MarketAPI market) {
        return market.getPrimaryEntity();
    }

    /**
     * The body as the planet it is - what the game's colonisation report carries, and so what a
     * case about that report has to name.
     */
    public static PlanetAPI readWorld(MarketAPI market) {
        return (PlanetAPI) market.getPrimaryEntity();
    }

    /**
     * The conditions marked as looked at. A colony whose own conditions read as unknown to its
     * owner is the shape founding has to avoid leaving behind, so the case reads what the market
     * was left holding rather than counting the calls that got there.
     */
    public static List<String> readSurveyedConditionIds(MarketAPI market) {
        return market.getConditions().stream()
            .filter(MarketConditionAPI::isSurveyed)
            .map(MarketConditionAPI::getId)
            .toList();
    }

    // A placeholder market wired to answer from its own state. The owner, the trading counters
    // and the tariff are MarketOwnershipFixture's wiring, since a colonisation ends in exactly
    // the ownership change that fixture poses; everything founding alone touches is stubbed here.
    private static MarketAPI buildColonisableMarket(
            SectorEntityToken body,
            Set<String> conditionIds) {

        // Everything the market answers with finishes its own stubbing before the market's opens,
        // so the two do not nest into an unfinished-stubbing error.
        var factionsById = MarketOwnershipFixture.buildFactions();
        var conditionsById = new LinkedHashMap<String, MarketConditionAPI>();

        for (var conditionId : conditionIds) {
            conditionsById.put(conditionId, buildCondition(conditionId));
        }

        var marketMock = mock(MarketAPI.class);

        MarketOwnershipFixture.stubOwner(marketMock, factionsById, Factions.NEUTRAL);
        MarketOwnershipFixture.stubSubmarkets(marketMock, new LinkedHashMap<>());
        MarketOwnershipFixture.stubTariff(marketMock);

        stubBody(marketMock, body);
        stubColonisationState(marketMock);
        stubConditions(marketMock, conditionsById);
        stubIndustries(marketMock);
        stubConstructionQueue(marketMock);

        return marketMock;
    }

    // The body the placeholder is hung on. Survey data already points at its body, so the read
    // answers one from the start rather than only once founding has pointed the two at each
    // other - which is what leaves a case free to check that founding stated the link anyway.
    private static void stubBody(MarketAPI marketMock, SectorEntityToken body) {

        if (body != null) {
            when(body.getName())
                .thenReturn(BODY_NAME);
        }

        when(marketMock.getPrimaryEntity())
            .thenReturn(body);
    }

    // What tells survey data from a colony, and what a colony gains by being founded. One wiring
    // per read, each pairing a getter with the setter that moves it, so a case reads back what
    // founding left rather than what the fixture decided. The economy's own listing is not among
    // them: it is held fixed at unregistered, and a case that wants a registered market poses one
    // rather than reaching this one after a founding has moved it.
    private static void stubColonisationState(MarketAPI marketMock) {

        stubConditionOnlyFlag(marketMock);
        stubName(marketMock);
        stubAge(marketMock);
        stubSize(marketMock);
        stubSurveyLevel(marketMock);
    }

    // The flag whose clearing is colonisation itself, set here from survey data's own answer.
    private static void stubConditionOnlyFlag(MarketAPI marketMock) {

        var isConditionMarketOnly = new AtomicBoolean(true);

        when(marketMock.isPlanetConditionMarketOnly())
            .thenAnswer(invocation -> isConditionMarketOnly.get());

        doAnswer(invocation -> {
                isConditionMarketOnly.set(invocation.getArgument(0));
                return null;
            })
            .when(marketMock)
            .setPlanetConditionMarketOnly(anyBoolean());
    }

    // What the place is called. Unnamed to begin with, so a case reading the body's name off the
    // market afterwards is reading what founding put there.
    private static void stubName(MarketAPI marketMock) {

        var name = new AtomicReference<String>();

        when(marketMock.getName())
            .thenAnswer(invocation -> name.get());

        doAnswer(invocation -> {
                name.set(invocation.getArgument(0));
                return null;
            })
            .when(marketMock)
            .setName(anyString());
    }

    // How long the colony has existed. It starts at a value no founding produces, so a case
    // asserting the age founding set cannot pass on the state the market started in.
    private static void stubAge(MarketAPI marketMock) {

        var daysInExistence = new AtomicReference<>(UNCOLONISED_AGE);

        when(marketMock.getDaysInExistence())
            .thenAnswer(invocation -> daysInExistence.get());

        doAnswer(invocation -> {
                daysInExistence.set(invocation.getArgument(0));
                return null;
            })
            .when(marketMock)
            .setDaysInExistence(anyFloat());
    }

    // How many people the place holds, which survey data has no answer to until it is settled.
    private static void stubSize(MarketAPI marketMock) {

        var size = new AtomicInteger(UNCOLONISED_SIZE);

        when(marketMock.getSize())
            .thenAnswer(invocation -> size.get());

        doAnswer(invocation -> {
                size.set(invocation.getArgument(0));
                return null;
            })
            .when(marketMock)
            .setSize(anyInt());
    }

    // How much of the body is known. Held as state rather than verified as a call, since what a
    // case is about is a colony whose conditions its owner can read.
    private static void stubSurveyLevel(MarketAPI marketMock) {

        var surveyLevel = new AtomicReference<>(MarketAPI.SurveyLevel.NONE);

        when(marketMock.getSurveyLevel())
            .thenAnswer(invocation -> surveyLevel.get());

        doAnswer(invocation -> {
                surveyLevel.set(invocation.getArgument(0));
                return null;
            })
            .when(marketMock)
            .setSurveyLevel(any());
    }

    // The conditions the body carries, added and removed as they are here: a founding both adds
    // one and swaps another out, so a case reads the set it was left with rather than the calls
    // that produced it.
    private static void stubConditions(
            MarketAPI marketMock,
            Map<String, MarketConditionAPI> conditionsById) {

        when(marketMock.hasCondition(anyString()))
            .thenAnswer(invocation -> conditionsById.containsKey(invocation.getArgument(0)));

        when(marketMock.addCondition(anyString()))
            .thenAnswer(invocation -> {
                String conditionId = invocation.getArgument(0);
                conditionsById.computeIfAbsent(
                    conditionId,
                    MarketColonisationFixture::buildCondition);
                return conditionId;
            });

        doAnswer(invocation -> {
                conditionsById.remove(invocation.<String>getArgument(0));
                return null;
            })
            .when(marketMock)
            .removeCondition(anyString());

        when(marketMock.getConditions())
            .thenAnswer(invocation -> new ArrayList<>(conditionsById.values()));
    }

    // What the colony runs, mutable here: founding is the one operation that builds an industry,
    // and the ownership rule that follows it reads the industries to decide which counters the
    // owner trades over - so an industry added by founding has to be visible to it.
    private static void stubIndustries(MarketAPI marketMock) {

        var industryIds = new LinkedHashSet<String>();

        when(marketMock.hasIndustry(anyString()))
            .thenAnswer(invocation -> industryIds.contains(invocation.getArgument(0)));

        doAnswer(invocation -> {
                industryIds.add(invocation.getArgument(0));
                return null;
            })
            .when(marketMock)
            .addIndustry(anyString());
    }

    // What the colony has queued to build. The engine's own queue rather than a stand-in: it is a
    // plain list holding what was added to it, so a case asks the queue itself what is in it.
    private static void stubConstructionQueue(MarketAPI marketMock) {
        when(marketMock.getConstructionQueue())
            .thenReturn(new ConstructionQueue());
    }

    // One condition, remembering whether anyone has looked at it.
    private static MarketConditionAPI buildCondition(String conditionId) {

        var isSurveyed = new AtomicBoolean(false);
        var conditionMock = mock(MarketConditionAPI.class);

        when(conditionMock.getId())
            .thenReturn(conditionId);
        when(conditionMock.isSurveyed())
            .thenAnswer(invocation -> isSurveyed.get());

        doAnswer(invocation -> {
                isSurveyed.set(invocation.getArgument(0));
                return null;
            })
            .when(conditionMock)
            .setSurveyed(anyBoolean());

        return conditionMock;
    }
}
