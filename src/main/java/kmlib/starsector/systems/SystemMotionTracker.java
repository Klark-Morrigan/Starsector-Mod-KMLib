package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.math.motion.MotionTracker;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Tracks which star systems move across hyperspace between polls, among the systems
 * a caller-supplied predicate selects.
 *
 * <p>Some mods make a system a mobile entity that rewrites its own
 * {@code getLocation()} and drifts, which breaks any layout that assumes a fixed
 * position - a map partition, a proximity index. This observes each selected
 * system's live position each poll and reports the ones that moved, so a caller can
 * leave a mover out of its layout rather than chase it. Detection is by observation
 * only, with no coupling to how a system is moved; see {@link MotionTracker}.
 *
 * <p>The predicate is the caller's alone: it decides which systems are in scope -
 * those a given feature draws, owns, or otherwise cares about - so this stays
 * agnostic to any one feature's membership rule while sharing the walk and the
 * detection.
 */
public final class SystemMotionTracker {
    // The floor a system's hyperspace position must shift between polls to read as
    // motion rather than float noise. Star systems sit thousands of units apart, so a
    // one-unit floor separates a genuine move from jitter without risking a real move
    // slipping under it.
    private static final double MOTION_THRESHOLD = 1.0;

    private final MotionTracker motionTracker = new MotionTracker(MOTION_THRESHOLD);

    /**
     * Observes the live hyperspace position of every selected system and reports
     * whether the moving set changed.
     *
     * @param sector        the sector to walk; null observes nothing and reports no
     *                      change
     * @param shouldInclude which systems are in scope for this poll; null scopes in
     *                      every system, tracking the whole sector's motion
     * @return true when a selected system started or stopped moving since the last
     *         poll; false while the moving set holds steady, including a system that
     *         merely keeps moving - it is already excluded, so nothing changes
     */
    public boolean updateMovingSystems(SectorAPI sector, Predicate<StarSystemAPI> shouldInclude) {
        if (sector == null) {
            return false;
        }
        return motionTracker.observe(SectorStarSystems.collectPositionsById(sector, shouldInclude));
    }

    /**
     * @return the ids of the systems currently moving - an immutable snapshot safe to
     *         read from another thread; empty until the first poll observes anything
     */
    public Set<String> getMovingSystemIds() {
        return motionTracker.getMovingKeys();
    }

    /**
     * Drops every baseline so the next poll starts fresh. Called when the sector is
     * replaced (a save load), so a system id shared with the previous sector is not
     * compared against its old position.
     */
    public void clearObservations() {
        motionTracker.clearObservations();
    }
}
