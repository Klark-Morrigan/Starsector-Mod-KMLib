package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.SectorEntityToken;

import java.util.Random;

/**
 * Orbit mechanics for sector entities, so KM* mods (and console tools) share one
 * implementation of how fast a body should circle and how to place it on that
 * orbit. {@link EntitySpawner} owns creating the entity; this owns the orbit it
 * is put on.
 *
 * <p>Two concerns live here. Speed: {@link #deriveBaseSpeedDegPerDay} answers
 * "how fast should a body at this radius drift" the vanilla way (constant
 * tangential speed), and {@link #applyJitter} widens any speed - that
 * radius-derived default or an explicit one - by vanilla's random spread. Orbit:
 * {@link #applyCircularOrbit} puts an entity onto a circular orbit at a given
 * degrees-per-day rate (or pins it when the rate is non-positive). Speed is in
 * degrees per day throughout, the intuitive knob; the orbital period
 * {@code setCircularOrbit} wants is derived from it (period = 360 / speed).
 */
public final class EntityOrbits {
    private static final float DEGREES_PER_CIRCLE = 360f;

    // Vanilla's base orbital divisor for planets and moons: orbital period in
    // days is radius / divisor, so a larger divisor means a shorter period and a
    // higher tangential speed.
    private static final float BASE_ORBIT_DIVISOR = 20f;

    // Vanilla jitters the divisor up by up to a quarter (20 + random*5), so this
    // is the default spread for an unspecified jitter.
    public static final float VANILLA_JITTER_FRACTION = 0.25f;

    private EntityOrbits() {
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
     * Puts an existing entity into a circular orbit around {@code focus} (or pins
     * it in place when the speed is non-positive). Used by {@link EntitySpawner}
     * and by callers placing entities created elsewhere, e.g. a jump point built
     * through the factory.
     *
     * @param entity            the entity to place
     * @param focus             the entity to orbit
     * @param orbitDistance     orbit radius from the focus
     * @param speedDegPerDay    orbital angular speed; 0 or less means static
     * @param startAngleDegrees angle from the focus at which the entity starts
     */
    public static void applyCircularOrbit(
            SectorEntityToken entity,
            SectorEntityToken focus,
            float orbitDistance,
            float speedDegPerDay,
            float startAngleDegrees) {

        if (speedDegPerDay <= 0f) {
            var focusLocation = focus.getLocation();
            var radians = Math.toRadians(startAngleDegrees);
            entity.setFixedLocation(
                focusLocation.x + (float) (Math.cos(radians) * orbitDistance),
                focusLocation.y + (float) (Math.sin(radians) * orbitDistance));
            return;
        }
        var orbitalPeriodDays = DEGREES_PER_CIRCLE / speedDegPerDay;
        entity.setCircularOrbit(
            focus,
            startAngleDegrees,
            orbitDistance,
            orbitalPeriodDays);
    }
}
