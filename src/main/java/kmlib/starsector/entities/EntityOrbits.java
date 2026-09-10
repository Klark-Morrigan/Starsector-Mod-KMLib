package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.SectorEntityToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Orbit mechanics for sector entities, so KM* mods (and console tools) share one
 * implementation of how fast a body should circle and how to place it on that
 * orbit. {@link EntitySpawner} owns creating the entity; this owns the orbit it
 * is put on.
 *
 * <p>Three concerns live here. Speed: {@link #deriveBaseSpeedDegPerDay} answers
 * "how fast should a body at this radius drift" the vanilla way (constant
 * tangential speed), and {@link #applyJitter} widens any speed - that
 * radius-derived default or an explicit one - by vanilla's random spread. Orbit:
 * {@link #applyCircularOrbit} puts an entity onto the circle an
 * {@link OrbitPlacement} describes, or pins it where that placement is a
 * standstill. Chain: {@link #readFocusChain} and
 * {@link #computeOrbitalDistanceTo} walk the orbit-focus chain, which is the one
 * traversal any question about where a body sits relative to another has to make.
 *
 * <p>Speed is in degrees per day throughout, the intuitive knob; the orbital
 * period {@code setCircularOrbit} wants is derived from it (period = 360 / speed).
 *
 * <p>The reads sit here beside the writes on purpose. {@link #readSpeedDegPerDay}
 * inverts the very arithmetic {@link #applyCircularOrbit} performs, and a reader
 * that stated that inverse for itself would be free to disagree with the writer
 * about what 360 means.
 */
public final class EntityOrbits {

    /**
     * Vanilla's own orbital spread: it jitters the base divisor up by as much as
     * a quarter (20 + random*5), so this is what a caller that has no reason to
     * pick its own spread widens a speed by.
     */
    public static final float VANILLA_JITTER_FRACTION = 0.25f;

    private static final float DEGREES_PER_CIRCLE = 360f;

    // Vanilla's base orbital divisor for planets and moons: orbital period in
    // days is radius / divisor, so a larger divisor means a shorter period and a
    // higher tangential speed.
    private static final float BASE_ORBIT_DIVISOR = 20f;

    // A hang guard for a malformed cyclic orbit chain, not a domain limit: a real
    // chain nests only a few links (station -> planet -> star), so a walk halts on a
    // null focus long before this. The value is generous headroom above any real
    // nesting, chosen only to bound a pathological cycle rather than to model one.
    private static final int MAX_ORBIT_CHAIN_DEPTH = 32;

    private EntityOrbits() {
        // utility class, no instances.
    }

    /**
     * Returns the base orbital speed, in degrees per day, for a body orbiting at
     * {@code orbitRadius}, matching vanilla's constant-tangential-speed
     * convention before any random spread.
     *
     * <p>Vanilla procgen sets a body's orbital period as {@code radius / divisor}
     * (a base divisor of {@value #BASE_ORBIT_DIVISOR} for planets and moons),
     * which holds the tangential speed roughly constant across the system - a body
     * twice as far out takes twice as long to circle. A flat degrees-per-day rate
     * does the opposite, so close-in bodies look sluggish and distant ones too
     * fast. Pair with {@link #applyJitter} to add vanilla's random widening. A
     * non-positive radius is degenerate (the body sits on its focus), so it pins
     * the body in place by returning {@code 0}.
     *
     * @param orbitRadius the orbit radius from the focus, in game units
     * @return the base orbital speed in degrees per day, or {@code 0} when the
     *         radius is non-positive
     */
    public static float deriveBaseSpeedDegPerDay(float orbitRadius) {
        if (orbitRadius <= 0f) {
            return 0f;
        }
        // period = radius / divisor (days); speed = 360 / period.
        return DEGREES_PER_CIRCLE * BASE_ORBIT_DIVISOR / orbitRadius;
    }

    /**
     * Widens a speed by vanilla's random orbital spread: the rate is scaled by a
     * random factor in {@code [1, 1 + jitterFraction]} (vanilla jitters the
     * divisor up by up to a quarter, the {@link #VANILLA_JITTER_FRACTION}
     * default). A fraction of {@code 0} - or a negative one - leaves the speed
     * unchanged. This applies to any base rate, the radius-derived default or an
     * explicit speed, since the jitter is just a spread on the final rate; a
     * non-positive speed stays non-positive, so a pinned body stays pinned.
     *
     * @param speedDegPerDay the base orbital speed to widen
     * @param jitterFraction the upper bound of the random widening; negatives are
     *                       treated as no jitter
     * @param random         the randomness source for the jitter sample
     * @return the widened orbital speed in degrees per day
     */
    public static float applyJitter(float speedDegPerDay, float jitterFraction, Random random) {
        var jitter = Math.max(0f, jitterFraction);
        return speedDegPerDay * (1f + random.nextFloat() * jitter);
    }

    /**
     * Puts an existing entity onto the circle {@code placement} describes around
     * {@code focus}, or fixes it at that point when the placement is a
     * standstill. Used by {@link EntitySpawner} and by callers placing entities
     * created elsewhere, e.g. a jump point built through the factory.
     *
     * @param entity    the entity to place
     * @param focus     the entity to orbit
     * @param placement where around the focus the entity goes, and how fast
     */
    public static void applyCircularOrbit(
            SectorEntityToken entity,
            SectorEntityToken focus,
            OrbitPlacement placement) {

        if (placement.isPinned()) {
            var focusLocation = focus.getLocation();
            var radians = Math.toRadians(placement.startAngleDegrees());
            entity.setFixedLocation(
                focusLocation.x + (float) (Math.cos(radians) * placement.orbitDistance()),
                focusLocation.y + (float) (Math.sin(radians) * placement.orbitDistance()));
            return;
        }
        var orbitalPeriodDays = DEGREES_PER_CIRCLE / placement.speedDegPerDay();
        entity.setCircularOrbit(
            focus,
            placement.startAngleDegrees(),
            placement.orbitDistance(),
            orbitalPeriodDays);
    }

    /**
     * The chain of bodies a body orbits through, itself first, then its orbit focus, that
     * body's focus, and so on until nothing is orbited.
     *
     * <p>The one traversal every orbit-relative question makes - how far out a body sits, what
     * it hangs off, which bodies a tree has to keep to show it. Stated once so the cycle guard
     * is stated once too: a malformed chain that orbits itself is bounded here rather than at
     * each call site, where one site guarding by depth and another by a visited set is two
     * answers to a question with one.
     *
     * @param body the body to walk up from; null yields an empty chain
     * @return the body and everything it orbits through, outermost first; never null
     */
    public static List<SectorEntityToken> readFocusChain(SectorEntityToken body) {

        var chain = new ArrayList<SectorEntityToken>();

        for (var orbiter = body;
                orbiter != null && chain.size() < MAX_ORBIT_CHAIN_DEPTH;
                orbiter = orbiter.getOrbitFocus()) {

            chain.add(orbiter);
        }
        return chain;
    }

    /**
     * How far a body sits from a reference along its orbit, summing the circular-orbit radii up
     * the body's orbit-focus chain until the chain reaches the reference - typically the
     * system's centremost star.
     *
     * <p>Reads the orbit rather than the body's live position, so the value does not drift as
     * the body revolves: a planet is as far out as its orbit, a moon adds its planet's orbit, a
     * station on a planet adds the planet's too. A body that never reaches the reference sums
     * its whole chain - still a stable depth. The reference's own orbit is not added, so a body
     * sitting on the reference reads zero.
     *
     * @param body      the body to measure; null yields {@link Double#POSITIVE_INFINITY}, since
     *                  a body with no orbit to read sits at no measurable distance
     * @param reference the body the chain is summed up to; null sums the whole chain to its root
     * @return the summed orbit-chain distance, or positive infinity when {@code body} is null
     */
    public static double computeOrbitalDistanceTo(
            SectorEntityToken body,
            SectorEntityToken reference) {

        if (body == null) {
            return Double.POSITIVE_INFINITY;
        }
        var distance = 0.0;

        for (var orbiter : readFocusChain(body)) {
            if (orbiter == reference) {
                break;
            }
            distance += orbiter.getCircularOrbitRadius();
        }
        return distance;
    }

    /**
     * How fast a body actually circles its focus, in degrees per day - the orbit it is on,
     * rather than the one {@link #deriveBaseSpeedDegPerDay} would have given it.
     *
     * <p>The inverse of what {@link #applyCircularOrbit} writes, which is why it lives beside
     * it: both turn on the same 360 degrees, and a reader stating that conversion for itself
     * could drift from the writer it is meant to undo.
     *
     * @param entity the body to read; null, or one on no orbit, yields {@code 0} - the same
     *               reading a pinned body has
     * @return the body's orbital speed in degrees per day
     */
    public static float readSpeedDegPerDay(SectorEntityToken entity) {

        var orbit = entity == null ? null : entity.getOrbit();

        if (orbit == null || orbit.getOrbitalPeriod() <= 0f) {
            return 0f;
        }
        return DEGREES_PER_CIRCLE / orbit.getOrbitalPeriod();
    }
}
