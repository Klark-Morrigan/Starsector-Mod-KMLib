package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;

import kmlib.starsector.entities.EntityMapIcons;
import kmlib.starsector.entities.EntityNameplate;

import java.util.Optional;

/**
 * Queries over what one market is.
 *
 * <p>Centralises reads a market only answers indirectly - walking its connected
 * entities, normalising a raw stat against its vanilla band, or picking the more
 * durable of the several markings vanilla leaves for one fact - so KM* mods (and
 * any external caller) share one implementation of a check like "does this colony
 * have a station", "how stable is it, as a fraction" or "is anybody actually aboard"
 * rather than re-deriving the scan, the band arithmetic or the choice of marking each
 * time.
 *
 * <p>What may be <em>said</em> about a market is {@link MarketVisibility}'s, and which
 * of several markets speaks for a place is {@link MarketColocation}'s. Both were once
 * here, and both are questions a caller can reach for by accident while wanting a plain
 * state read - so each states its own subject in its own name.
 *
 * <p>Nothing in this package changes a market, and the layering gate holds it that way -
 * the reads may not import either operation, while an operation reads freely. What can be
 * <em>done</em> to a market lives a package in, named for the operation it is -
 * {@link kmlib.starsector.markets.colonisation} and
 * {@link kmlib.starsector.markets.ownership} - each holding the seam through which an
 * installed mod takes that operation over. Reading and mutating a market were once the
 * same package under names of the same shape, where a class that answered a question and
 * a class that rewrote a colony were told apart only by opening them.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance
 * state. Matches {@link kmlib.starsector.systems.StarSystems}'s shape, and is
 * null-market defensive like the rest of the library.
 */
public final class Markets {

    // Vanilla's opt-out tag for a "station"-tagged entity that must not be treated
    // as a market's orbital station. No Tags constant exists for it, so the literal
    // is vanilla's own contract - OrbitalStation's station scan tests it the same way.
    private static final String NO_ORBITAL_STATION_TAG = "NO_ORBITAL_STATION";

    // Stability's vanilla 0..10 band, the denominator of the stability fraction. A
    // modded market can report a value outside it, so the fraction clamps: an
    // out-of-band reading scales as "none" or "full" rather than overshooting.
    private static final float MAX_STABILITY_VALUE = 10.0f;

    private Markets() {
        // utility class, no instances.
    }

    /**
     * A market's attached defensive station, as the entity itself.
     *
     * <p>Reads the market's connected entities - the ownership link the game
     * maintains, so a station found here is this market's own rather than a rival's
     * or an abandoned hulk sharing the orbit - and qualifies each on the two halves
     * vanilla's own station reads test: a {@code "station"}-tagged entity not opted
     * out via {@code NO_ORBITAL_STATION}, which additionally has a station fleet.
     * Keying on the tag and the fleet captures vanilla and modded stations alike,
     * so no industry ids are read.
     *
     * <p>The fleet half is what makes the answer a defensive station rather than a
     * place built on one. A market sited on a station is connected to its own
     * primary entity, which carries the {@code "station"} tag for what it is, so the
     * tag alone would have every such market defended by itself. The station fleet
     * is raised by an actual orbital-station industry, so requiring it is what tells
     * a colony with a battlestation apart from a colony that is a hab ring.
     *
     * <p>Yields the entity rather than a verdict, so a caller that has to name the
     * station - or read its faction, orbit, or own market - can, while one that only
     * asks whether there is one reads {@link #hasAttachedStation} over this same
     * scan. A second scan for the entity would be free to disagree with the verdict;
     * folding both onto one read makes that impossible.
     *
     * <p>The first qualifying entity wins. A market with two of them is not a shape
     * vanilla builds, and there is no ordering among connected entities that would
     * make one of the pair the "real" station, so picking a winner is arbitrary
     * either way.
     *
     * @param market the market to inspect; null (or one with no connected entities)
     *               yields empty
     * @return the market's orbital station, or empty when it owns none
     */
    public static Optional<SectorEntityToken> findAttachedStation(MarketAPI market) {
        if (market == null || market.getConnectedEntities() == null) {
            return Optional.empty();
        }
        for (var entity : market.getConnectedEntities()) {
            // The opt-out is tested before the fleet read: it is the cheaper answer, and
            // vanilla's fleet lookup dereferences the entity's memory, which an opted-out
            // decoration has no reason to carry.
            if (entity.hasTag(Tags.STATION)
                    && !entity.hasTag(NO_ORBITAL_STATION_TAG)
                    && Misc.getStationFleet(entity) != null) {
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    /**
     * A market's stability as a fraction of its vanilla 0..10 band, clamped to
     * [0, 1].
     *
     * <p>The reusable half of any "scale something by how stable this colony is"
     * read: 0 at no stability, 1 at full, linear between. The clamp keeps a modded
     * market that reports outside the band from scaling past "none" or "full", so a
     * caller multiplying by the fraction never overshoots or flips sign.
     *
     * @param market the market to read; null yields 0 (no stability)
     * @return the market's stability in [0, 1]
     */
    public static double getStabilityFraction(MarketAPI market) {
        if (market == null) {
            return 0.0;
        }
        var fraction = market.getStabilityValue() / MAX_STABILITY_VALUE;
        return Math.min(Math.max(fraction, 0.0f), 1.0f);
    }

    /**
     * Whether a market owns an attached defensive station.
     *
     * <p>The verdict half of {@link #findAttachedStation}, which owns the scan and
     * documents what counts as a station. Kept as its own read because most callers
     * weigh only the station's presence - a defence score, a fill rule - and reading
     * that as an {@code isPresent()} at every such site says less than the question
     * being asked.
     *
     * @param market the market to inspect; null (or one with no connected entities)
     *               yields false
     * @return true when one of the market's connected entities is its orbital station
     */
    public static boolean hasAttachedStation(MarketAPI market) {
        return findAttachedStation(market).isPresent();
    }

    /**
     * Whether a market is an abandoned station - a derelict hulk with a storage locker
     * bolted on, rather than a place anybody lives.
     *
     * <p>Reads the {@code abandoned_station} condition
     * ({@link Conditions#ABANDONED_STATION}) that vanilla's own station abandonment adds,
     * rather than the {@code $abandonedStation} flag it sets on the entity in the same
     * breath. The condition is the better of the two readings on both counts: it travels
     * with the market, so a market moved onto another entity is still what it was, and it
     * is the test the game's own market panel makes when deciding a place has nothing to
     * show for itself.
     *
     * <p>Says nothing about ownership or about the fog. A derelict is owned by whatever
     * faction the station belonged to and is not condition-only, so it passes
     * {@link #isOwnedColony} exactly as a settlement does - which is why what a market is
     * has to be asked separately from whether somebody holds it.
     *
     * @param market the market to test; null yields false
     * @return true when the market carries vanilla's abandoned-station condition
     */
    public static boolean isAbandonedStation(MarketAPI market) {
        return market != null && market.hasCondition(Conditions.ABANDONED_STATION);
    }

    /**
     * Whether a market is a military one - a garrison rather than a plain colony.
     *
     * <p>Delegates to {@link Misc#isMilitary}, which reads the
     * {@link MemFlags#MARKET_MILITARY} ({@code $military}) flag a market's military
     * industries raise. The read is delegated rather than reproduced: the flag and
     * the conditions that raise it are vanilla's to change, and a second
     * implementation of the same rule would be free to drift from it.
     *
     * <p>Broader than {@link MarketPatrols#fieldsPatrols}, and the two answer different
     * questions. A market is military by virtue of what it <em>is</em>; it fields
     * patrols by virtue of a patrol industry currently running there. A caller weighing
     * military standing - claim scoring, threat estimates - wants this one; a caller
     * asking whether fleets actually launch from here wants the patrol flag.
     *
     * @param market the market to test; null (or one with no memory) yields false
     * @return true when the market counts as military
     */
    public static boolean isMilitary(MarketAPI market) {
        // The memory guard is this library's, not vanilla's: Misc reads the flag straight off
        // the market's memory and would throw on a market that has none. Its neighbours here
        // absorb that case, so this one does too rather than being the single read a caller
        // has to defend against.
        if (market == null || market.getMemoryWithoutUpdate() == null) {
            return false;
        }
        return Misc.isMilitary(market);
    }

    /**
     * Whether a market is held by the named faction.
     *
     * <p>Reads the market's own faction id rather than its faction object, that being what an
     * ownership change writes and therefore what is answering for the owner a moment after one.
     * The ids are compared exactly: faction ids are keys rather than names, and a read that
     * matched loosely would have two mods' factions answer for each other.
     *
     * <p>Its callers are the operations that must not act twice. Handing a colony to the faction
     * already holding it, or founding under one, is a call whose subject has already happened -
     * and where such a call would still change the colony, this is the read that stops it.
     *
     * @param market    the market to test; null yields false
     * @param factionId the faction id to test for; null yields false, no market being held by
     *                  nobody in the sense this asks about
     * @return true when the market flies that faction's flag
     */
    public static boolean isOwnedBy(MarketAPI market, String factionId) {
        return market != null
            && factionId != null
            && factionId.equals(market.getFactionId());
    }

    /**
     * Whether a market is a colony a faction owns, rather than a bare planet's
     * placeholder.
     *
     * <p>The "counts as a colony" filter map and territory logic share: a faction
     * must own it, and it must not be the condition-only market every uninhabited
     * planet carries to hold its hazard and atmosphere conditions. This is
     * ownership alone - it says nothing about whether the player has found the
     * colony yet; compose it with {@link MarketVisibility#isDiscoveredByPlayer} when
     * visibility matters.
     *
     * @param market the market to test; null (or one with no owning faction) yields
     *               false
     * @return true when a faction owns the market and it is not condition-only
     */
    public static boolean isOwnedColony(MarketAPI market) {
        return market != null
            && market.getFaction() != null
            && !market.isPlanetConditionMarketOnly();
    }

    /**
     * Whether a market is a place somebody has settled, rather than one merely standing where
     * nobody has.
     *
     * <p>Narrower than {@link #isOwnedColony} by one arm, and that arm is what "somebody" means.
     * Every market has an owner whether or not anyone lives there: a bare world's placeholder, a
     * decivilised world's remains and a derelict station alike are handed to the neutral faction
     * as they are built, so a read asking only whether a faction holds the market finds one
     * holding all three. Asking whether that faction is anybody is what separates a settled place
     * from a flag flown over an empty one.
     *
     * <p>The neutral question is put to the faction rather than answered by comparing an id here,
     * which is how the engine's own code asks it. The id it settles on is the engine's own to
     * change, and a library comparing its own copy of that id would be a second answer free to
     * disagree with the game's.
     *
     * <p>Ownership is the whole of the rule. Whether the economy lists the colony is no part of
     * it, a real colony being buildable unregistered, and neither is whether the player has found
     * it - compose with {@link MarketVisibility#isDiscoveredByPlayer} when visibility matters.
     *
     * @param market the market to test; null (or one with no owning faction) yields false
     * @return true when a faction other than neutral holds a market that is not condition-only
     */
    public static boolean isSettledColony(MarketAPI market) {
        return isOwnedColony(market) && !market.getFaction().isNeutralFaction();
    }

    /**
     * How a market is identified to a reader: the name it goes by, and the glyph the sector map
     * marks it with.
     *
     * <p>The two come from different places - a market is named in its own right while the glyph
     * belongs to the entity it sits on - which is exactly why the pairing is made here rather than
     * at each surface printing a list of colonies. Read apart, one colony's name is one call away
     * from being drawn beside another's glyph.
     *
     * @param market the market to identify; null yields {@link EntityNameplate#BLANK}, the
     *               null-defensive shape the rest of the class holds to, and one with no primary
     *               entity is named with no glyph
     * @return the market's nameplate
     */
    public static EntityNameplate readNameplate(MarketAPI market) {
        if (market == null) {
            return EntityNameplate.BLANK;
        }
        return new EntityNameplate(
            market.getName(),
            EntityMapIcons.resolveMapIcon(market.getPrimaryEntity()));
    }

    /**
     * The plugin keeping one of a market's trading counters, as the kind of plugin the caller
     * means - or null where the market has no such counter, or has one kept by something else.
     *
     * <p>Speaking to a counter is a three-step reach: the counter, its plugin, and whether that
     * plugin is the one the caller is about to speak to. Every step can come back empty on a
     * modded market - the counter may be absent, and the plugin behind a counter vanilla defines
     * is a plugin any mod may replace - so an operation that skipped the checks would fail on an
     * install rather than on a mistake.
     *
     * <p>The kind is asked for rather than assumed for the same reason. A caller wanting vanilla's
     * own plugin names that class and gets nothing on an install that replaced it; a caller
     * wanting only the interface a step is declared on names that instead, and is answered by
     * whatever is actually keeping the counter.
     *
     * <p>The one read here that is about how a market is put together rather than about what it
     * is. It sits with the others because its callers are the operations on a market, and an
     * operation reaching a counter's plugin for itself would be the place all three of those
     * steps get skipped.
     *
     * @param market       the market whose counter is being reached; null yields null
     * @param submarketId  the counter's submarket id
     * @param pluginType   the kind of plugin the caller is about to speak to
     * @param <T>          that kind
     * @return the counter's plugin as that kind, or null where there is none to speak to
     */
    public static <T> T readSubmarketPlugin(
            MarketAPI market,
            String submarketId,
            Class<T> pluginType) {

        if (market == null) {
            return null;
        }

        var submarket = market.getSubmarket(submarketId);

        if (submarket == null) {
            return null;
        }

        var plugin = submarket.getPlugin();

        return pluginType.isInstance(plugin)
            ? pluginType.cast(plugin)
            : null;
    }
}
