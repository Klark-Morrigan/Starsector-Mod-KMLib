package kmlib.starsector.geometry;

import com.fs.starfarer.api.campaign.SectorEntityToken;

import kmlib.math.geometry.Points;

/**
 * Distance and bearing between Starsector campaign entities, and which
 * of two entities a nearest search should keep - the game-typed sibling
 * of {@link Points}.
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

    /**
     * Whether {@code candidate}, that far from whatever is being measured from, is a nearer
     * answer than the best found so far - strictly closer, or exactly as close and carrying
     * the lower id.
     *
     * <p>The tie-break is what makes a nearest search answer the same entity twice running.
     * Equidistant bodies are ordinary in a star system - a shared orbit, a mirrored pair -
     * and with distance alone the winner is whichever the traversal happened to meet first,
     * which is a listing order that differs between installs and between passes. An ID is
     * stable, so it settles the tie rather than leaving it to be settled arbitrarily.
     *
     * <p>Stated here, beside the distance the search ranks by, so every nearest search
     * settles a tie the same way instead of each deciding for itself.
     *
     * @param candidateDistance how far the candidate sits from the point measured from
     * @param incumbentDistance how far the best answer so far sits from it
     * @param candidate         the entity being weighed
     * @param incumbent         the best answer so far; null - nothing found yet - is beaten
     *                          by anything
     * @return true when the candidate should displace the incumbent
     */
    public static boolean isNearerThan(
            double candidateDistance,
            double incumbentDistance,
            SectorEntityToken candidate,
            SectorEntityToken incumbent) {

        if (candidateDistance != incumbentDistance) {
            return candidateDistance < incumbentDistance;
        }
        return incumbent == null || candidate.getId().compareTo(incumbent.getId()) < 0;
    }
}
