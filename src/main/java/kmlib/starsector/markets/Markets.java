package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
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
 * entities, or normalising a raw stat against its vanilla band - so KM* mods (and
 * any external caller) share one implementation of a check like "does this colony
 * have a station" or "how stable is it, as a fraction" rather than re-deriving the
 * scan or the band arithmetic each time.
 *
 * <p>What may be <em>said</em> about a market is {@link MarketVisibility}'s, and which
 * of several markets speaks for a place is {@link MarketColocation}'s. Both were once
 * here, and both are questions a caller can reach for by accident while wanting a plain
 * state read - so each states its own subject in its own name.
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
     * Whether a market is a colony a faction owns, rather than a bare planet's
     * placeholder.
     *
     * <p>The "counts as a colony" filter map and territory logic share: a faction
     * must own it, and it must not be the condition-only market every uninhabited
     * planet carries to hold its hazard and atmosphere conditions. This is
     * ownership alone - it says nothing about whether the player has found the
     * colony yet; compose it with {@link MarketVisibility#isKnownToPlayer} when
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
