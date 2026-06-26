package kmlib.math.geometry;

import org.lwjgl.util.vector.Vector2f;

/**
 * Operations on 2D points.
 *
 * <p>Pure 2D math: no rendering and no Starsector types, so it can be reasoned
 * about and verified on its own - and called from code that must stay free of
 * the game runtime (e.g. {@code com.fs.starfarer.api.util.Misc}, whose static
 * initialiser needs a booted game, is not an option in such contexts).
 *
 * <p>The {@link Vector2f} overloads are exempt from that rule: LWJGL's vector
 * is a plain struct with no game-requiring static initialiser, so it does not
 * pull in the runtime the contract above guards against. They exist only to
 * spare callers the {@code .x}/{@code .y} unpacking at every site.
 */
public final class Points {

    private Points() {
    }

    /**
     * The Euclidean distance between {@code (x1, y1)} and {@code (x2, y2)}.
     */
    public static double computeDistance(double x1, double y1, double x2, double y2) {
        var deltaX = x1 - x2;
        var deltaY = y1 - y2;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    /**
     * The Euclidean distance between two {@code {x, y}} points.
     */
    public static double computeDistance(double[] a, double[] b) {
        return computeDistance(a[0], a[1], b[0], b[1]);
    }

    /**
     * The Euclidean distance between two points {@code a} and {@code b}.
     */
    public static double computeDistance(Vector2f a, Vector2f b) {
        return computeDistance(a.x, a.y, b.x, b.y);
    }

}
