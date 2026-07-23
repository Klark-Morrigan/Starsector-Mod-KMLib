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
 * <p>The map icon is drawn in world space, so it scales with the map zoom - large zoomed in, a dot
 * zoomed out. The widget's own hit test hides this: it compares a screen-space distance
 * ({@code distance(cursor, anchor) * factor}) against an icon radius that is itself
 * {@code radius * factor} scaled, so the map's per-vertex {@code factor} cancels and the test
 * reduces to a fixed <em>world</em> radius. The reconstruction works in that reduced form directly:
 * the overlay already holds the cursor as an unprojected world point, so it needs no screen
 * conversion. A small screen-pixel term the widget adds does not cancel and stays as a floor,
 * {@code / factor} in world terms, which keeps a zoomed-out icon hittable rather than collapsing to
 * a sub-pixel point.
 *
 * <p>The world icon radius is a value of the system - its star's radius (or a fixed base for a
 * nebula), scaled per the widget's sizing - so it is computed once from the star and reused, rather
 * than reconstructed per frame. {@link #computeIconWorldRadius} is that reconstruction; the
 * hit test itself is then a plain distance compare.
 *
 * <p>Pure arithmetic over plain coordinates: no GL, no Starsector types, no per-frame state, so the
 * one part of the derivation that is easy to get subtly wrong is verifiable without a live map. The
 * sizing constants are the map widget's, and are the surface a caller tunes against the live map if
 * the reconstructed gate sits slightly off the drawn icon.
 */
public final class StarIconHitTest {

    // The widget sizes its icon as radius * factor * 10, then halves it (a diameter-to-radius step
    // folded into its maths), so the world radius per unit of star radius is 10 * 0.5 = 5.
    private static final float ICON_RADIUS_WORLD_SCALE = 5f;

    // A nebula centre's icon is sized off a fixed base rather than its (huge, diffuse) radius, so
    // the widget substitutes this in place of the star radius for one.
    private static final float NEBULA_ICON_BASE_RADIUS = 200f;

    // The widget shrinks a true star's icon relative to other bodies of the same radius.
    private static final float STAR_ICON_SCALE = 0.75f;

    // The fixed screen-pixel term the widget adds to the icon radius. Unlike the rest it does not
    // scale with the map, so in the reduced world-space test it stays a screen floor - it divides
    // by factor to reach world units, growing as the map zooms out so a tiny icon stays hittable.
    private static final float ICON_SCREEN_MARGIN = 5f;

    private StarIconHitTest() {
    }

    /**
     * Whether the cursor's world point sits within the star's map icon.
     *
     * @param worldPoint     the cursor's unprojected position in world (hyperspace) coordinates
     * @param anchor         the star's hyperspace anchor in the same coordinates
     * @param iconWorldRadius the icon's radius in world units, from {@link #computeIconWorldRadius}
     * @param factor         the map render pass's per-vertex scale, needed only for the screen-pixel
     *                       floor; must be positive, as it is on any live map
     * @return whether the point falls inside the icon
     */
    public static boolean isWorldPointOverStarIcon(
            Vector2f worldPoint, Vector2f anchor, float iconWorldRadius, float factor) {
        return Points.computeDistance(worldPoint, anchor)
                < iconWorldRadius + ICON_SCREEN_MARGIN / factor;
    }

    /**
     * The star icon's radius in world units - the zoom-independent size the widget draws it at,
     * before the per-frame screen scaling. A nebula centre uses a fixed base in place of its radius;
     * a true star is drawn a shade smaller than another body of the same radius.
     *
     * @param bodyRadius       the star (or other body) radius the icon is sized by; ignored for a
     *                         nebula centre
     * @param scaleMultMapIcon the body's per-spec map-icon scale multiplier
     * @param isStar           whether the body is a true star, which the widget draws smaller
     * @param isNebulaCentre   whether the body is a nebula centre, sized off the fixed base instead
     * @return the icon's radius in world units
     */
    public static float computeIconWorldRadius(
            float bodyRadius, float scaleMultMapIcon, boolean isStar, boolean isNebulaCentre) {
        var base = isNebulaCentre ? NEBULA_ICON_BASE_RADIUS : bodyRadius;
        var worldRadius = ICON_RADIUS_WORLD_SCALE * base * scaleMultMapIcon;
        return isStar ? worldRadius * STAR_ICON_SCALE : worldRadius;
    }
}
