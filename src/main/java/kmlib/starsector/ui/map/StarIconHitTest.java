package kmlib.starsector.ui.map;

import kmlib.math.geometry.Points;

import org.lwjgl.util.vector.Vector2f;

/**
 * Answers "is the cursor directly on a star system's icon?" for an overlay painting on the sector
 * (M) map. The map widget resolves the hovered star from a mouse-move event it handles itself and
 * keeps the result in a private field, so an overlay that wants the same answer - to step aside for
 * the vanilla star tooltip, say - has no route to read it and has to derive it. This is the one
 * place that derivation lives, a reconstruction of the widget's own hyperspace hit test.
 *
 * <p>The widget compares a screen-space distance: the cursor's distance to the star's hyperspace
 * anchor, scaled by the map's per-vertex {@code factor} into pixels, against the icon's pixel
 * radius. The overlay already works in the unprojected world coordinates the anchor is expressed in
 * and already holds {@code factor}, so the same test falls out with no extra map read. The icon
 * radius is not exposed either, so it too is reconstructed - from the star's radius, the input the
 * widget sizes its icon by, with a floor so even the smallest star keeps a hittable icon.
 *
 * <p>Pure arithmetic over plain numbers: no GL, no Starsector types, no per-frame state, so the one
 * part of the derivation that is easy to get subtly wrong is verifiable without a live map. The
 * icon-size constants are the map widget's, and are the surface a caller tunes against the live map
 * if the reconstructed gate sits slightly off the drawn icon.
 */
public final class StarIconHitTest {

    // The map widget's base icon diameter in screen pixels, before the per-star radius scaling.
    // Halved to a radius for the hit test, since the icon is drawn centred on the anchor.
    private static final float ICON_BASE_DIAMETER = 40f;

    // The star radius the icon scaling saturates at: at or above it the icon is full size, and the
    // scale is a straight ratio below it. The widget divides the radius by this same reference.
    private static final float ICON_SCALE_REFERENCE_RADIUS = 100f;

    // The smallest the radius scaling drops to, so a tiny star still draws - and so answers a
    // hover with - an icon big enough to point at rather than a vanishing one.
    private static final float MIN_ICON_SCALE = 0.5f;
    private static final float MAX_ICON_SCALE = 1f;

    private StarIconHitTest() {
    }

    /**
     * Whether the cursor's world point sits within the star's map icon.
     *
     * @param worldPoint the cursor's unprojected position in world (hyperspace) coordinates
     * @param anchor     the star's hyperspace anchor in the same coordinates
     * @param starRadius the star's radius, which the icon is sized by
     * @param factor     the map render pass's per-vertex scale, turning a world distance into the
     *                   pixels the icon radius is measured in
     * @return whether the point falls inside the icon
     */
    public static boolean isWorldPointOverStarIcon(
            Vector2f worldPoint, Vector2f anchor, float starRadius, float factor) {
        return Points.computeDistance(worldPoint, anchor) * factor < computeIconRadius(starRadius);
    }

    /**
     * The star icon's radius in screen pixels: the base radius scaled by how big the star is,
     * bounded so the icon neither vanishes for a tiny star nor grows without limit for a huge one.
     *
     * @param starRadius the star's radius
     * @return the icon's radius in screen pixels
     */
    static float computeIconRadius(float starRadius) {
        var radiusScale = Math.min(
                MAX_ICON_SCALE,
                Math.max(MIN_ICON_SCALE, starRadius / ICON_SCALE_REFERENCE_RADIUS));
        return ICON_BASE_DIAMETER * radiusScale / 2f;
    }
}
