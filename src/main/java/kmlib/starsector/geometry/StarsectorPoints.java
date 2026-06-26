package kmlib.starsector.geometry;

import com.fs.starfarer.api.campaign.SectorEntityToken;

import kmlib.math.geometry.Points;


/**
 * Distance and bearing between Starsector campaign entities - the
 * game-typed sibling of {@link Points}.
 *
 * <p>{@code Misc.getDistance} accepts these same types, but its
 * declaring class has a static initialiser that needs a booted game, so
 * it drags the full runtime into any caller. {@link SectorEntityToken}
 * is a plain interface, so unpacking its {@link
 * SectorEntityToken#getLocation() location} and handing it to
 * {@link Points} keeps entity-distance math at the runtime-free math
 * layer.
 */
public final class StarsectorPoints {

    private StarsectorPoints() {
    }

    /**
     * The Euclidean distance between the locations of {@code a} and
     * {@code b} in their current container's coordinate space. The
     * caller is responsible for the two entities sharing one (same
     * system, both in hyperspace, ...); cross-space comparisons are
     * meaningless here, exactly as they are for {@code Misc.getDistance}.
     */
    public static double computeDistanceBetween(SectorEntityToken a, SectorEntityToken b) {
        return Points.computeDistance(a.getLocation(), b.getLocation());
    }

    /**
     * The bearing in degrees from {@code from} to {@code to}, measured
     * counter-clockwise from the positive x-axis, in the range
     * {@code (-180, 180]}. Coincident locations yield 0.
     */
    public static double computeAngleDegreesBetween(SectorEntityToken from, SectorEntityToken to) {
        return Points.computeAngleDegrees(from.getLocation(), to.getLocation());
    }
}
