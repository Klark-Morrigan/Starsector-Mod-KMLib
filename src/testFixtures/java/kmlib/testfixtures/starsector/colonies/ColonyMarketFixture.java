package kmlib.testfixtures.starsector.colonies;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The colonies every colony read is posed against, built by what kind of colony they are.
 *
 * <p>Separate from the worlds they are placed in - a system, hyperspace, a whole sector - because
 * those differ per suite while a colony's shape does not. A suite posing its own world would
 * otherwise either reach into a fixture built for a different one, or build colonies of its own
 * that the colony set would never have yielded.
 *
 * <p>Built through named builders rather than by flag, so a case says which kind of colony it
 * poses. The flags themselves stay behind that boundary: three adjacent booleans are transposable
 * without failing, and no suite has business setting them directly.
 */
public final class ColonyMarketFixture {

    // The size every colony takes unless a case asks for another. Colony size only decides which
    // of two markets on one place wins, so a case not posing that collision is not about size.
    public static final int DEFAULT_COLONY_SIZE = 3;

    private ColonyMarketFixture() {
        // fixture of static builders, no instances.
    }

    /** An ordinary colony: owned, open, on an entity the player has found. */
    public static MarketAPI buildVisibleColony(String factionId) {
        return buildVisibleColonyOfSize(factionId, DEFAULT_COLONY_SIZE);
    }

    public static MarketAPI buildVisibleColonyOfSize(String factionId, int size) {
        return buildColonyOnItsOwnEntity(factionId, size, false, false, false);
    }

    /** A base once raided: its entity is discovered, its market stays hidden for good. */
    public static MarketAPI buildFoundConcealedColony(String factionId) {
        return buildColonyOnItsOwnEntity(factionId, DEFAULT_COLONY_SIZE, true, false, false);
    }

    /**
     * A base still to be found: concealed, and on an entity the player has not discovered.
     * Concealment and discovery agree here, so nothing whatever about it reaches the player.
     */
    public static MarketAPI buildUnfoundConcealedColony(String factionId) {
        return buildColonyOnItsOwnEntity(factionId, DEFAULT_COLONY_SIZE, true, true, false);
    }

    /**
     * A derelict station's shape, and the sector's most common undiscovered one: nothing
     * conceals it, and its entity is still to be found. Concealment and discovery disagree here,
     * and the fog answers on discovery - declaring itself to an economy the player has no sight
     * of is not being seen.
     */
    public static MarketAPI buildUnfoundOpenColony(String factionId) {
        return buildColonyOnItsOwnEntity(factionId, DEFAULT_COLONY_SIZE, false, true, false);
    }

    /**
     * A derelict adrift: open, on an entity the player has found, and held by nobody - the neutral
     * faction every unowned station falls to. Every read but the condition takes it for a
     * settlement, which is the whole reason the kind is resolved at all.
     */
    public static MarketAPI buildDerelictStation() {
        return buildStationCarryingDerelictCondition(Factions.NEUTRAL);
    }

    /**
     * A station somebody keeps: the same derelict condition on a market a real faction holds.
     * Nothing but the owner parts it from the hulk above, which is what makes the pair worth
     * posing together - a kind read splitting them on anything else would be reading the wrong
     * thing.
     */
    public static MarketAPI buildOutpost(String factionId) {
        return buildStationCarryingDerelictCondition(factionId);
    }

    // The derelict condition on an ordinary colony's shape, which the owner then decides the kind
    // of. Built from the ordinary colony rather than from its flags, because the two are the same
    // shape: repeating the flag triple here would leave a second statement of what an open, found
    // colony is, free to drift from the one above.
    private static MarketAPI buildStationCarryingDerelictCondition(String factionId) {

        var marketMock = buildVisibleColony(factionId);

        when(marketMock.hasCondition(Conditions.ABANDONED_STATION))
            .thenReturn(true);

        return marketMock;
    }

    /**
     * A bare planet's placeholder, the condition-only market every uninhabited world carries to
     * hold its hazard and atmosphere. Owned by nobody in particular, and rejected on that arm.
     */
    public static MarketAPI buildConditionOnlyMarket() {
        return buildColonyOnItsOwnEntity(Factions.NEUTRAL, DEFAULT_COLONY_SIZE, false, false, true);
    }

    /**
     * A world whose colony collapsed, surveyed closely enough for the player to see so: the
     * condition-only shell a collapse leaves behind, carrying the decivilised condition, on a
     * planet already found.
     *
     * <p>Nothing but that condition parts it from the bare placeholder above, which is what makes
     * the pair worth posing together - an admission splitting them on anything else would be
     * reading the wrong thing.
     */
    public static MarketAPI buildDecivilisedWorld() {
        return buildDecivilisedWorld(MarketAPI.SurveyLevel.FULL, false);
    }

    /**
     * The same world seen and no more - the lowest survey vanilla shows a condition at, and so the
     * one a case raising the map's own bar past vanilla's has to pose.
     */
    public static MarketAPI buildSeenDecivilisedWorld() {
        return buildDecivilisedWorld(MarketAPI.SurveyLevel.SEEN, false);
    }

    /**
     * The same world with nobody having looked at it at all: the condition is there, and the
     * player has no way of knowing it. Its planet is found all the same, discovery and survey
     * being independent - which is the pair a case about the survey bar turns on.
     */
    public static MarketAPI buildUnsurveyedDecivilisedWorld() {
        return buildDecivilisedWorld(MarketAPI.SurveyLevel.NONE, false);
    }

    /**
     * A collapsed colony whose planet is neither surveyed nor found - the world both fog arms hold
     * back at once, and so the case that shows each knob reaches its own arm and no other.
     */
    public static MarketAPI buildUnfoundUnsurveyedDecivilisedWorld() {
        return buildDecivilisedWorld(MarketAPI.SurveyLevel.NONE, true);
    }

    /**
     * A second market object on an existing colony's entity, under the same owner - the shape a
     * mod builds when it supersedes a colony by adding beside vanilla's rather than replacing.
     */
    public static MarketAPI buildSiblingMarketOn(MarketAPI colony, int size) {

        // Read off the sibling before the new mock's stubbing opens, so the two do not nest into
        // an unfinished-stubbing error.
        var faction = colony.getFaction();
        var factionId = colony.getFactionId();
        var entity = colony.getPrimaryEntity();
        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(faction);
        when(marketMock.getFactionId())
            .thenReturn(factionId);
        when(marketMock.getPrimaryEntity())
            .thenReturn(entity);
        when(marketMock.getSize())
            .thenReturn(size);

        return marketMock;
    }

    // A decivilised world at a stated survey level. Built on the bare placeholder's shape rather
    // than from its flags, the two being the same condition-only market with one condition between
    // them - a second statement of that shape here would be free to drift from the one above.
    //
    // The condition is posed as one needing no survey of its own, so the survey level alone
    // decides whether the ruins read: which arm reveals what is the market read's business, and a
    // case here about a colony set has no reason to pose both bars at once.
    private static MarketAPI buildDecivilisedWorld(
            MarketAPI.SurveyLevel surveyLevel,
            boolean isEntityDiscoverable) {

        var conditionMock = mock(MarketConditionAPI.class);
        var marketMock = buildColonyOnItsOwnEntity(
            Factions.NEUTRAL,
            DEFAULT_COLONY_SIZE,
            false,
            isEntityDiscoverable,
            true);

        when(marketMock.hasCondition(Conditions.DECIVILIZED))
            .thenReturn(true);
        when(marketMock.getSurveyLevel())
            .thenReturn(surveyLevel);
        when(marketMock.getFirstCondition(Conditions.DECIVILIZED))
            .thenReturn(conditionMock);

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

        // The owner answers on both readings, as a real market's does - they are one fact in the
        // game, and a rule that tells owners apart would see none if only one of them answered.
        when(marketMock.getFaction())
            .thenReturn(factionMock);
        when(marketMock.getFactionId())
            .thenReturn(factionId);
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

    // The faction holding one of these markets. Whether it is the neutral one is answered off the
    // faction, as the engine answers it, rather than left false: every place nobody has settled is
    // handed to neutral as it is built, so a fixture that had neutral deny being neutral would
    // pose an unowned hulk as a station somebody keeps.
    private static FactionAPI buildFaction(String id) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(id);
        when(factionMock.isNeutralFaction())
            .thenReturn(Factions.NEUTRAL.equals(id));

        return factionMock;
    }
}
