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

    // How many markets have been built, so each takes an id no other one answers to.
    private static int builtMarketCount;

    private ColonyMarketFixture() {
        // fixture of static builders, no instances.
    }

    /** An ordinary colony: owned, open, on an entity the player has found. */
    public static MarketAPI buildVisibleColony(String factionId) {
        return buildVisibleColonyOfSize(factionId, DEFAULT_COLONY_SIZE);
    }

    public static MarketAPI buildVisibleColonyOfSize(String factionId, int size) {
        return buildVisibleColonyOfSize(buildFaction(factionId), size);
    }

    /**
     * The same colony under an owner the caller already holds, at a stated size.
     *
     * @param faction the owner, shared with whatever else the caller means it to hold
     * @param size    the colony size
     * @return the market mock
     */
    public static MarketAPI buildVisibleColonyOfSize(FactionAPI faction, int size) {
        return buildColonyOnItsOwnEntity(faction, size, false, false, false);
    }

    /** A base once raided: its entity is discovered, its market stays hidden for good. */
    public static MarketAPI buildFoundConcealedColony(String factionId) {
        return buildFoundConcealedColony(buildFaction(factionId), DEFAULT_COLONY_SIZE);
    }

    /**
     * The same base under an owner the caller already holds, at a stated size.
     *
     * @param faction the owner
     * @param size    the colony size
     * @return the market mock
     */
    public static MarketAPI buildFoundConcealedColony(FactionAPI faction, int size) {
        return buildColonyOnItsOwnEntity(faction, size, true, false, false);
    }

    /**
     * A base still to be found: concealed, and on an entity the player has not discovered.
     * Concealment and discovery agree here, so nothing whatever about it reaches the player.
     */
    public static MarketAPI buildUndiscoveredConcealedColony(String factionId) {
        return buildUndiscoveredConcealedColony(buildFaction(factionId), DEFAULT_COLONY_SIZE);
    }

    /**
     * The same base under an owner the caller already holds, at a stated size.
     *
     * @param faction the owner
     * @param size    the colony size
     * @return the market mock
     */
    public static MarketAPI buildUndiscoveredConcealedColony(FactionAPI faction, int size) {
        return buildColonyOnItsOwnEntity(faction, size, true, true, false);
    }

    /**
     * A derelict station's shape, and the sector's most common undiscovered one: nothing
     * conceals it, and its entity is still to be found. Concealment and discovery disagree here,
     * and the fog answers on discovery - declaring itself to an economy the player has no sight
     * of is not being seen.
     */
    public static MarketAPI buildUndiscoveredOpenColony(String factionId) {
        return buildUndiscoveredOpenColony(buildFaction(factionId), DEFAULT_COLONY_SIZE);
    }

    /**
     * The same colony under an owner the caller already holds, at a stated size.
     *
     * @param faction the owner
     * @param size    the colony size
     * @return the market mock
     */
    public static MarketAPI buildUndiscoveredOpenColony(FactionAPI faction, int size) {
        return buildColonyOnItsOwnEntity(faction, size, false, true, false);
    }

    /**
     * A derelict adrift: open, on an entity the player has found, and held by nobody - the neutral
     * faction every unowned station falls to. Every read but the condition takes it for a
     * settlement, which is the whole reason the kind is resolved at all.
     */
    public static MarketAPI buildDerelictStation() {
        return buildDerelictStation(DEFAULT_COLONY_SIZE);
    }

    /**
     * The same hulk at a stated size.
     *
     * <p>The neutral owner is the shape rather than a detail of it: it is what makes this a
     * derelict instead of a station somebody keeps, so it is fixed here rather than left to a
     * caller.
     *
     * @param size the colony size
     * @return the market mock
     */
    public static MarketAPI buildDerelictStation(int size) {
        return buildStationCarryingDerelictCondition(buildFaction(Factions.NEUTRAL), size);
    }

    /**
     * The same hulk on an entity the player has not found, which is what most of the sector's
     * derelicts are: nothing conceals it, and nobody has been near it.
     *
     * @return the market mock
     */
    public static MarketAPI buildUndiscoveredDerelictStation() {

        var marketMock = buildDerelictStation();

        when(marketMock.getPrimaryEntity().isDiscoverable())
            .thenReturn(true);

        return marketMock;
    }

    /**
     * A station somebody keeps: the same derelict condition on a market a real faction holds.
     * Nothing but the owner parts it from the hulk above, which is what makes the pair worth
     * posing together - a kind read splitting them on anything else would be reading the wrong
     * thing.
     */
    public static MarketAPI buildOutpost(String factionId) {
        return buildOutpost(buildFaction(factionId), DEFAULT_COLONY_SIZE);
    }

    /**
     * The same kept station under an owner the caller already holds, at a stated size.
     *
     * @param faction the faction keeping the station
     * @param size    the colony size
     * @return the market mock
     */
    public static MarketAPI buildOutpost(FactionAPI faction, int size) {
        return buildStationCarryingDerelictCondition(faction, size);
    }

    // The derelict condition on an ordinary colony's shape, which the owner then decides the kind
    // of. Built from the ordinary colony rather than from its flags, because the two are the same
    // shape: repeating the flag triple here would leave a second statement of what an open, found
    // colony is, free to drift from the one above.
    private static MarketAPI buildStationCarryingDerelictCondition(FactionAPI faction, int size) {

        var marketMock = buildVisibleColonyOfSize(faction, size);

        when(marketMock.hasCondition(Conditions.ABANDONED_STATION))
            .thenReturn(true);

        return marketMock;
    }

    /**
     * A bare planet's placeholder, the condition-only market every uninhabited world carries to
     * hold its hazard and atmosphere. Owned by nobody in particular, and rejected on that arm.
     */
    public static MarketAPI buildConditionOnlyMarket() {
        return buildConditionOnlyMarket(buildFaction(Factions.NEUTRAL), DEFAULT_COLONY_SIZE);
    }

    /**
     * The same placeholder under a stated owner and size, for a caller posing one beside the
     * colonies it must not be taken for.
     *
     * @param faction the faction the placeholder names
     * @param size    the colony size
     * @return the market mock
     */
    public static MarketAPI buildConditionOnlyMarket(FactionAPI faction, int size) {
        return buildColonyOnItsOwnEntity(faction, size, false, false, true);
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
    public static MarketAPI buildUndiscoveredUnsurveyedDecivilisedWorld() {
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
    //
    // The faction arrives built rather than named, because a caller posing several markets of one
    // owner needs them to share the very object: a mechanic that groups by faction identity, or a
    // palette stubbed on it, sees nothing if each market carries a faction of its own.
    //
    // Every market answers to an id of its own. Almost nothing reads it, and a mock answers null
    // until asked otherwise - which would have every market built here share one key in any
    // register keyed by colony id, so marking one seen would mark the lot.
    public static MarketAPI buildColonyOnItsOwnEntity(
            FactionAPI faction,
            int size,
            boolean isHidden,
            boolean isEntityDiscoverable,
            boolean isConditionOnly) {

        // The entity and the owner each finish their own stubbing before the market's opens, so
        // the two do not nest into an unfinished-stubbing error.
        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.isDiscoverable())
            .thenReturn(isEntityDiscoverable);

        var factionId = faction == null ? null : faction.getId();
        var marketMock = mock(MarketAPI.class);

        builtMarketCount++;

        when(marketMock.getId())
            .thenReturn("market_" + builtMarketCount);
        // The owner answers on both readings, as a real market's does - they are one fact in the
        // game, and a rule that tells owners apart would see none if only one of them answered.
        when(marketMock.getFaction())
            .thenReturn(faction);
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

    /**
     * The faction holding one of these markets.
     *
     * <p>Whether it is the neutral one is answered off the faction, as the engine answers it,
     * rather than left false: every place nobody has settled is handed to neutral as it is built,
     * so a fixture that had neutral deny being neutral would pose an unowned hulk as a station
     * somebody keeps.
     *
     * @param id the faction id
     * @return the faction mock, for a caller that means several markets to share one owner
     */
    public static FactionAPI buildFaction(String id) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(id);
        when(factionMock.isNeutralFaction())
            .thenReturn(Factions.NEUTRAL.equals(id));

        return factionMock;
    }

    // The same shape named by its owner rather than by the faction object, for a caller with one
    // market to pose and no reason to hold its owner.
    private static MarketAPI buildColonyOnItsOwnEntity(
            String factionId,
            int size,
            boolean isHidden,
            boolean isEntityDiscoverable,
            boolean isConditionOnly) {

        return buildColonyOnItsOwnEntity(
            buildFaction(factionId),
            size,
            isHidden,
            isEntityDiscoverable,
            isConditionOnly);
    }
}
