package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The markets a state read is posed against, built by what kind of market they are.
 *
 * <p>Two flags decide every one of these - whether the market carries only a planet's
 * conditions, and whether the economy has it registered - and a suite setting them directly
 * says nothing about which of the four resulting shapes it meant. Two adjacent booleans are
 * also transposable without failing, so the named builders keep them behind this boundary and a
 * case reads as the shape it poses.
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

    private static FactionAPI buildFaction(String id) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(id);

        return factionMock;
    }
}
