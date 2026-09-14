package kmlib.starsector.map;

import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import kmlib.starsector.systems.SystemKey;

import java.util.HashSet;
import java.util.Set;

/**
 * Records which star systems show a star on the vanilla hyperspace map, so a
 * caller can mirror that visibility.
 *
 * <p>The map draws a system's star from its hyperspace star-anchor jump point.
 * The engine suppresses that draw with the {@code star_hidden_on_map} tag for
 * abyssal rogue-stellar objects - a neutron star, magnetar, or black hole whose
 * one-way gravity-well exit is meant to stay hidden - while leaving the system
 * its (fringe) jump points. The tag lives on the star anchor, not on the system
 * or its in-system jump points, so it is only reachable by scanning hyperspace.
 *
 * <p>This scans hyperspace once and resolves each visible (untagged) star anchor
 * to the system it leads into via {@link JumpPointAPI#getDestinationStarSystem},
 * indexing those systems by {@link SystemKey}. Resolving by identity, not by
 * coordinates, is what makes a barycenter or multi-star system match: its
 * {@code getLocation} returns an empty centre offset from where the star anchor
 * actually sits, so a position-based lookup would miss it. Building the index
 * once keeps the per-system query an O(1) lookup, so callers that walk every
 * system do not rescan hyperspace per system.
 *
 * <p>Keyed by {@link SystemKey} rather than by system ID, because an ID is not
 * unique and the systems that share one here are exactly the systems this read
 * exists to separate: vanilla's abyssal pair holds one system whose anchor is
 * tagged hidden and one whose anchor is not, so an ID-keyed index answers
 * "visible" for both on the strength of the untagged one alone - which is a
 * hidden star drawn as an ordinary one by whatever composes this.
 */
public final class VisibleStars {

    private final Set<SystemKey> visibleStarSystemKeys;

    private VisibleStars(Set<SystemKey> visibleStarSystemKeys) {
        this.visibleStarSystemKeys = visibleStarSystemKeys;
    }

    /**
     * Scans the sector's hyperspace for the systems whose star the map draws.
     *
     * @param sector the sector to scan; null, or a sector with no hyperspace,
     *               yields an empty index (no star treated as visible)
     * @return an index of the keys of systems whose star the map draws
     */
    public static VisibleStars scan(SectorAPI sector) {
        var visibleStarSystemKeys = new HashSet<SystemKey>();
        var hyperspace = sector == null ? null : sector.getHyperspace();
        if (hyperspace != null) {
            indexVisibleStarSystems(hyperspace, visibleStarSystemKeys);
        }
        return new VisibleStars(visibleStarSystemKeys);
    }

    /**
     * @param system the system to test; null reads as not visible, there being no
     *               system whose star could be drawn
     * @return true when the vanilla map draws this system's star - a visible
     *         star anchor leads into it
     */
    public boolean isStarVisibleForSystem(StarSystemAPI system) {
        return visibleStarSystemKeys.contains(SystemKey.readKeyOf(system));
    }

    private static void indexVisibleStarSystems(LocationAPI hyperspace, Set<SystemKey> keys) {
        for (Object entity : hyperspace.getEntities(JumpPointAPI.class)) {
            var jumpPoint = (JumpPointAPI) entity;
            // Only a star anchor draws a system's star; one tagged hidden (an
            // abyssal rogue object) does not, so the system it leads into is left
            // out of the index and reads as map-invisible.
            if (!jumpPoint.isStarAnchor() || jumpPoint.hasTag(Tags.STAR_HIDDEN_ON_MAP)) {
                continue;
            }
            var system = jumpPoint.getDestinationStarSystem();
            // A star anchor always leads into a system; the null guard keeps a
            // malformed anchor from failing the whole scan rather than expecting it.
            //
            // A system the sector states nothing at all about carries the blank key, which every
            // other such system also carries; two of them share this entry, as they shared the
            // empty ID before. That is the one shape a key cannot separate, and indexing it anyway
            // keeps a lone arm-less system with a drawn star reading as visible.
            if (system != null) {
                keys.add(SystemKey.readKeyOf(system));
            }
        }
    }
}
