package kmlib.math.motion;

import kmlib.math.geometry.Points;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Detects which of a set of keyed points moved between successive observations, by
 * comparing each key's current position against the one seen last time.
 *
 * <p>Motion is decided by observation alone: a key whose point shifts past a
 * caller-supplied noise floor from one {@link #observe} to the next is reported
 * moving, with no knowledge of what moves it or why. This holds equally for a
 * steady drift and a per-observation teleport - both differ from their last sample -
 * so a caller need not model the motion. A key seen for the first time has no
 * baseline, so its motion is decided on the next observation, not this one; a key
 * that stops being observed drops its baseline, so it is judged afresh if it
 * returns.
 *
 * <p>The moving set is republished through a volatile field on each observation, so
 * a reader on another thread sees each publication whole without locking. The
 * baseline map is mutated only by {@link #observe}, so one writer paired with any
 * number of snapshot readers is safe.
 */
public final class MotionTracker {
    // The squared distance a point must shift between observations to count as
    // motion rather than numerical jitter. Squared so the check avoids a sqrt.
    private final double motionThresholdSquared;
    // Each key's position as seen last observation, the baseline the next
    // observation compares against. Mutated only by observe().
    private final Map<String, double[]> lastObservedPositionByKey = new LinkedHashMap<>();
    // The keys observed to be moving, republished each observation. Volatile so a
    // reader sees each new publication as a whole.
    private volatile Set<String> publishedMovingKeys = Set.of();

    /**
     * @param motionThreshold the distance a point must shift between observations to
     *                        count as motion; a shift below it is treated as noise
     *                        and the key held still. The right floor is
     *                        domain-specific - how far apart the points sit, how much
     *                        jitter their source carries - so the caller sets it
     *                        rather than the tracker assuming one
     */
    public MotionTracker(double motionThreshold) {
        this.motionThresholdSquared = motionThreshold * motionThreshold;
    }

    /**
     * @return the keys currently moving - an immutable snapshot safe to read from
     *         another thread; empty until the first observation sees anything move
     */
    public Set<String> getMovingKeys() {
        return publishedMovingKeys;
    }

    /**
     * Drops every baseline, so a key observed after this is a first sighting again
     * rather than being compared against a stale position. Called when the observed
     * world is replaced wholesale - for instance a save load reusing a key from a
     * previous world.
     */
    public void clearObservations() {
        lastObservedPositionByKey.clear();
        publishedMovingKeys = Set.of();
    }

    /**
     * Records the current position of each observed key, republishes the moving set,
     * and reports whether that set changed.
     *
     * @param currentPositionByKey each observed key's current position; a key absent
     *                             from this map is no longer observed, so its baseline
     *                             is dropped
     * @return true when the moving set gained or lost a member since the last
     *         observation, so a caller watching for transitions can react; false
     *         while it holds steady - including a key that simply keeps moving, which
     *         is already in the set, so nothing changes
     */
    public boolean observe(Map<String, double[]> currentPositionByKey) {
        var movingKeys = new LinkedHashSet<String>();
        for (var entry : currentPositionByKey.entrySet()) {
            var lastPosition = lastObservedPositionByKey.get(entry.getKey());
            // A first-seen key has no baseline, so it cannot be judged moving yet;
            // only a position past the noise floor from its baseline marks motion.
            if (lastPosition != null
                    && Points.computeDistanceSquared(lastPosition, entry.getValue())
                            > motionThresholdSquared) {
                movingKeys.add(entry.getKey());
            }
        }
        // Replace the baseline with this observation (dropping keys no longer
        // present) so the next observation measures change from here.
        lastObservedPositionByKey.clear();
        lastObservedPositionByKey.putAll(currentPositionByKey);
        var hasMovingSetChanged = !movingKeys.equals(publishedMovingKeys);
        publishedMovingKeys = Set.copyOf(movingKeys);
        return hasMovingSetChanged;
    }
}
