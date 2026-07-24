package kmlib.starsector.ui.map;

import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import org.lwjgl.util.vector.Vector2f;

/**
 * A star system's reconstructed sector-map icon: where its icon sits, how big it is in world units,
 * and which kind of body sized it. The whole generic reconstruction of the vanilla map icon in one
 * place - resolving the body the widget sizes the icon from, classifying it, and applying the
 * vanilla sizing - so a caller gets the raw vanilla figures and applies any of its own adjustment
 * (a settings-driven scale, say) on top, rather than the reconstruction reaching into a caller's
 * configuration.
 *
 * @param anchor      the icon's centre in world (hyperspace) coordinates
 * @param worldRadius the vanilla icon radius in world units, before any caller adjustment
 * @param bodyKind    which kind of body sized the icon, so a caller can adjust per kind
 */
public record StarMapIcon(Vector2f anchor, float worldRadius, StarIconBodyKind bodyKind) {

    /**
     * Reconstructs a system's map icon, or {@code null} when it has none to reconstruct - no
     * hyperspace anchor to place it at, or no body to size it from.
     *
     * @param system the star system to read
     * @return the reconstructed icon, or {@code null} when the system has no placeable icon
     */
    public static StarMapIcon reconstructFor(StarSystemAPI system) {
        var anchor = system.getHyperspaceAnchor();
        if (anchor == null || anchor.getLocation() == null) {
            return null;
        }
        var body = resolveIconBody(system, anchor);
        if (body == null) {
            return null;
        }
        var bodyKind = resolveBodyKind(body);
        var scaleMultMapIcon = body instanceof PlanetAPI planet
                ? planet.getSpec().getScaleMultMapIcon()
                : 1f;
        var worldRadius = StarIconHitTest.computeIconWorldRadius(
                body.getRadius(),
                scaleMultMapIcon,
                bodyKind);
                
        // Copy the anchor's live location: the token hands back its own mutable vector, so aliasing
        // it would let the system's motion rewrite a captured icon.
        return new StarMapIcon(
                new Vector2f(anchor.getLocation()),
                worldRadius,
                bodyKind);
    }

    // The body the map sizes the icon from: the hyperspace anchor's destination visual entity, the
    // same one the vanilla icon wraps, so the radius matches the drawn icon rather than the (often
    // larger) primary star. Falls back to the star when the anchor is not a jump point, so a system
    // without a jump-point anchor still resolves a body.
    private static SectorEntityToken resolveIconBody(StarSystemAPI system, SectorEntityToken anchor) {
        if (anchor instanceof JumpPointAPI jumpPoint
                && jumpPoint.getDestinationVisualEntity() != null) {
            return jumpPoint.getDestinationVisualEntity();
        }
        return system.getStar();
    }

    // Classifies the body for sizing: a nebula centre and a black hole read off their spec, then a
    // true star, then anything else. A body with no planet spec (a bare token) can only be a star
    // or other by its own flag.
    private static StarIconBodyKind resolveBodyKind(SectorEntityToken body) {
        if (body instanceof PlanetAPI planet) {
            var spec = planet.getSpec();
            if (spec.isNebulaCenter()) {
                return StarIconBodyKind.NEBULA_CENTRE;
            }
            if (spec.isBlackHole()) {
                return StarIconBodyKind.BLACK_HOLE;
            }
        }
        return body.isStar() ? StarIconBodyKind.STAR : StarIconBodyKind.OTHER;
    }
}
