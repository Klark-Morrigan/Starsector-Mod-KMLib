package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The markets a state read is posed against, built by what kind of market they are.
 *
 * <p>Two flags decide most of these - whether the market carries only a planet's conditions, and
 * whether the economy has it registered - and a suite setting them directly says nothing about
 * which of the resulting shapes it meant. Two adjacent booleans are also transposable without
 * failing, so the named builders keep them behind this boundary and a case reads as the shape it
 * poses. The shapes a condition marks rather than a flag - a derelict, a dead world - are built
 * the same way for the same reason: a bare condition id in a case says nothing about what kind of
 * place it makes.
 *
 * <p>Separate from {@link MarketPlacementFixture}, which poses where a market sits rather than
 * what state it is in. No case needs both, because nothing that ranks markets by distance also
 * asks what kind they are.
 */
public final class MarketStateFixture {

    private MarketStateFixture() {
        // fixture of static builders, no instances.
    }

    /**
     * A derelict station's shape: owned by the faction whose station it was, never registered
     * with the economy, and carrying the condition vanilla marks an abandoned station with. Reads
     * as an owned colony on every other axis, which is exactly why the condition has to be asked.
     */
    public static MarketAPI buildAbandonedStation() {
        return buildMarketWithCondition(
            buildMarket(buildFaction("neutral"), false, false),
            Conditions.ABANDONED_STATION);
    }

    /**
     * A bare world's placeholder: the condition-only market procgen hangs on every uninhabited
     * planet and never registers, which is what colonisation turns into a colony.
     */
    public static MarketAPI buildColonisableBody() {
        return buildMarket(buildFaction("neutral"), true, false);
    }

    /** An ordinary colony: a faction holds it and the economy lists it. */
    public static MarketAPI buildColony(String factionId) {
        return buildMarket(buildFaction(factionId), false, true);
    }

    /**
     * Galatia Academy's shape: a real colony under a real faction that vanilla deliberately
     * never registers with the economy.
     */
    public static MarketAPI buildColonyUnlistedByEconomy(String factionId) {
        return buildMarket(buildFaction(factionId), false, false);
    }

    /**
     * A dead colony's shape: ruins nobody holds, carrying the decivilised condition and nothing
     * else a colony would. Posed against the derelict above, because both are unlisted neutral
     * markets marked by a condition - so a read keying on the wrong one admits both.
     */
    public static MarketAPI buildDecivilisedWorld() {
        return buildMarketWithCondition(
            buildMarket(buildFaction("neutral"), true, false),
            Conditions.DECIVILIZED);
    }

    /**
     * Survey data the economy has registered - a shape nothing vanilla builds, and the one only
     * a registration read tells apart from a colonisable body.
     */
    public static MarketAPI buildRegisteredSurveyData() {
        return buildMarket(buildFaction("neutral"), true, true);
    }

    /** A market no faction holds, which is nobody's colony however else it reads. */
    public static MarketAPI buildUnownedMarket() {
        return buildMarket(null, false, true);
    }

    private static MarketAPI buildMarket(
            FactionAPI faction,
            boolean isConditionOnly,
            boolean isInEconomy) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(faction);
        when(marketMock.isPlanetConditionMarketOnly())
            .thenReturn(isConditionOnly);
        when(marketMock.isInEconomy())
            .thenReturn(isInEconomy);

        return marketMock;
    }

    // Hangs one condition on an already-built market. Only the named condition answers true, so
    // a read keying on a different one has to fail rather than passing on "carries something".
    private static MarketAPI buildMarketWithCondition(MarketAPI marketMock, String conditionId) {

        when(marketMock.hasCondition(conditionId))
            .thenReturn(true);

        return marketMock;
    }

    private static FactionAPI buildFaction(String id) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(id);

        return factionMock;
    }
}
