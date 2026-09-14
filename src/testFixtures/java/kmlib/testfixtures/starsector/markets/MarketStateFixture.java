package kmlib.testfixtures.starsector.markets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The markets a state read is posed against, built by what kind of market they are.
 *
 * <p>Two flags decide most of these - whether the market carries only a planet's conditions, and
 * whether the economy has it registered - and a suite setting them directly says nothing about
 * which of the resulting shapes it meant. Two adjacent booleans are also transposable without
 * failing, so the named builders keep them behind this boundary and a case reads as the shape it
 * poses. The shapes a condition marks rather than a flag - a derelict, a decivilised world - are built
 * the same way for the same reason: a bare condition ID in a case says nothing about what kind of
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
        // Built from the unlisted-colony shape rather than from the flags, because that is
        // precisely what a derelict is on every axis but the condition: an owned market the
        // economy never registered. The composition is the point the doc above makes.
        return stubConditionOn(
            buildColonyUnlistedByEconomy(Factions.NEUTRAL),
            Conditions.ABANDONED_STATION);
    }

    /**
     * A bare world's placeholder: the condition-only market procgen hangs on every uninhabited
     * planet and never registers, which is what colonisation turns into a colony.
     */
    public static MarketAPI buildColonisableBody() {
        return buildMarket(buildFaction(Factions.NEUTRAL), true, false);
    }

    /** An ordinary colony: a faction holds it and the economy lists it. */
    public static MarketAPI buildColony(String factionId) {
        return buildMarket(buildFaction(factionId), false, true);
    }

    /**
     * A real colony under a real faction that the economy does not list - the shape vanilla builds
     * Galatia Academy in, though the Academy's own market is concealed besides, which this poses
     * nothing of.
     */
    public static MarketAPI buildColonyUnlistedByEconomy(String factionId) {
        return buildMarket(buildFaction(factionId), false, false);
    }

    /**
     * A decivilised world's shape: a colony nobody runs any more, carrying the decivilised
     * condition and nothing else a colony would. Posed against the derelict above, because both are
     * unlisted neutral markets marked by a condition - so a read keying on the wrong one admits
     * both.
     */
    public static MarketAPI buildDecivilisedWorld() {
        // A decivilised world is a bare world's placeholder with the condition on it - the polity
        // that ran the place is gone, and what stayed behind is the market holding the planet's own
        // conditions. Composed for that reason rather than to save the two flags.
        return stubConditionOn(buildColonisableBody(), Conditions.DECIVILIZED);
    }

    /**
     * Survey data the economy has registered - a shape nothing vanilla builds, and the one only
     * a registration read tells apart from a colonisable body.
     */
    public static MarketAPI buildRegisteredSurveyData() {
        return buildMarket(buildFaction(Factions.NEUTRAL), true, true);
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

    // Hangs one condition on an already-built market - named for what it does to a market it is
    // given, rather than as a builder, which it is not. Only the named condition answers true, so
    // a read keying on a different one has to fail rather than passing on "carries something".
    private static MarketAPI stubConditionOn(MarketAPI marketMock, String conditionId) {

        when(marketMock.hasCondition(conditionId))
            .thenReturn(true);

        return marketMock;
    }

    // The faction holding one of these markets. Whether it is the neutral one is answered off the
    // faction, as the engine answers it, rather than left false: every market nobody has settled
    // is handed to neutral as it is built, so a fixture that had neutral deny being neutral would
    // pose a derelict station and a bare world's placeholder as places somebody lives.
    private static FactionAPI buildFaction(String id) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(id);
        when(factionMock.isNeutralFaction())
            .thenReturn(Factions.NEUTRAL.equals(id));

        return factionMock;
    }
}
