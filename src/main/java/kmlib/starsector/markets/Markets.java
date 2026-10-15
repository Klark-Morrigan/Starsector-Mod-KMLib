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
 * Reads of what one market is.
 *
 * <p>Each read answers a question the market only answers indirectly: by walking its connected
 * entities, by normalising a raw stat against its vanilla band, or by choosing between the markings
 * vanilla leaves for one fact. Stating each once keeps every caller on the same answer.
 *
 * <p>What may be said about a market is {@link MarketVisibility}'s, which of several markets speaks
 * for a place is {@link MarketColocation}'s, and how a market's trading counters are reached is
 * {@link MarketSubmarkets}'s.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and
 * null-market defensive like the rest of the library.
 */
public final class Markets {

    // Stability's vanilla 0..10 band, the denominator of the stability fraction. A
    // modded market can report a value outside it, so the fraction clamps: an
    // out-of-band reading scales as "none" or "full" rather than overshooting.
    private static final double MAX_STABILITY_VALUE = 10.0;

    // Vanilla's opt-out tag for a "station"-tagged entity that must not be treated
    // as a market's orbital station. No Tags constant exists for it, so the literal
    // is vanilla's own contract - OrbitalStation's station scan tests it the same way.
    private static final String NO_ORBITAL_STATION_TAG = "NO_ORBITAL_STATION";

    private Markets() {
    }

    /**
     * A market's attached defensive station, as the entity itself.
     *
     * <p>Scans the market's connected entities, the ownership link the game maintains, so a rival's
     * station or a derelict sharing the orbit is never found. An entity qualifies on the two tests
     * vanilla's own station reads make: a {@code "station"} tag not opted out via
     * {@code NO_ORBITAL_STATION}, and a station fleet. Neither test reads an industry ID, so modded
     * stations qualify too.
     *
     * <p>The fleet test tells a colony with a battlestation from a colony that is a hab ring. A market
     * sited on a station is connected to its own primary entity, which carries the {@code "station"}
     * tag. Only an orbital-station industry raises the fleet.
     *
     * <p>The first qualifying entity wins. Vanilla never builds a market with two, and connected
     * entities have no order that would make either one the real station.
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

        return Math.min(Math.max(fraction, 0.0), 1.0);
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
     * Whether a market is an abandoned station - a derelict with a storage locker
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

        return market != null
            && market.hasCondition(Conditions.ABANDONED_STATION);
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
     * <p>Reads the market's own faction ID rather than its faction object, that being what an
     * ownership change writes and therefore what is answering for the owner a moment after one.
     * The IDs are compared exactly: faction IDs are keys rather than names, and a read that
     * matched loosely would have two mods' factions answer for each other.
     *
     * <p>Its callers are the operations that must not act twice. Handing a colony to the faction
     * already holding it, or founding under one, is a call whose subject has already happened -
     * and where such a call would still change the colony, this is the read that stops it.
     *
     * @param market    the market to test; null yields false
     * @param factionId the faction ID to test for; null yields false, no market being held by
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
     * Whether a market is a place somebody has settled, rather than a flag flown over an empty one.
     *
     * <p>Narrower than {@link #isOwnedColony} by one test. The game hands a bare world's placeholder,
     * a decivilised world's remains and a derelict station to the neutral faction as it builds them,
     * so every market has an owner whether or not anyone lives there. Ruling out the neutral owner is
     * what leaves the settled places.
     *
     * <p>The faction is asked whether it is neutral, as the engine's own code asks it, rather than its
     * ID compared here. A copy of that ID would be a second answer free to disagree with the game's.
     *
     * <p>Ownership is the whole rule. The economy listing is no part of it, since a real colony can be
     * built unregistered, and neither is discovery - compose with
     * {@link MarketVisibility#isDiscoveredByPlayer} when visibility matters.
     *
     * @param market the market to test; null (or one with no owning faction) yields false
     * @return true when a faction other than neutral holds a market that is not condition-only
     */
    public static boolean isSettledColony(MarketAPI market) {

        return isOwnedColony(market)
            && !market.getFaction().isNeutralFaction();
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
}
