package kmlib.starsector.systems;

import kmlib.math.motion.MotionTracker;

import java.util.Map;
import java.util.Set;

/**
 * Tracks which star systems move across hyperspace between polls, among the systems whose
 * positions a caller hands it: {@link MotionTracker} bound to {@link SystemKey} and to the floor a
 * hyperspace shift has to clear to read as motion rather than float noise.
 *
 * <p>Keyed by {@link SystemKey} rather than by system id because an id is not unique: two systems
 * co-located under one id would be one observation, and a move by either would read as a move by
 * the one recorded last. Which systems are in scope is the caller's alone, so this stays agnostic
 * to any one feature's membership rule.
 */
public final class SystemMotionTracker {
    // The floor a system's hyperspace position must shift between polls to read as
    // motion rather than float noise. Star systems sit thousands of units apart, so a
    // one-unit floor separates a genuine move from jitter without risking a real move
    // slipping under it.
    private static final double MOTION_THRESHOLD = 1.0;

    private final MotionTracker<SystemKey> motionTracker = new MotionTracker<>(MOTION_THRESHOLD);

    /**
     * Observes positions the caller has already read and reports whether the moving
     * set changed.
     *
     * <p>Takes the positions rather than the sector they were read off, so a caller sharing
     * one reading of the sector with other readers hands over what that reading already holds
     * instead of this walking the system list a second time for the same answer.
     *
     * @param positionByKey each observed system's live hyperspace position keyed by
     *                      {@link SystemKey}, as of this poll; an empty map observes an
     *                      empty sector and reports every system that had been moving as
     *                      stopped
     * @return true when an observed system started or stopped moving since the last
     *         poll; false while the moving set holds steady, including a system that
     *         merely keeps moving - it is already excluded, so nothing changes
     */
    public boolean updateMovingSystems(Map<SystemKey, double[]> positionByKey) {
        return motionTracker.observe(positionByKey);
    }

    /**
     * @return the keys of the systems currently moving - an immutable snapshot safe to
     *         read from another thread; empty until the first poll observes anything
     */
    public Set<SystemKey> getMovingSystemKeys() {
        return motionTracker.getMovingKeys();
    }

    /**
     * Drops every baseline so the next poll starts fresh. Called when the sector is
     * replaced (a save load), so a system key carried over from the previous sector is not
     * compared against its old position.
     */
    public void clearObservations() {
        motionTracker.clearObservations();
    }
}
