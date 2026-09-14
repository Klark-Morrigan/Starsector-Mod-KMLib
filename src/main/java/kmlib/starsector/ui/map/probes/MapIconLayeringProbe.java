package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin;

import kmlib.starsector.ui.map.MapIconLayering;

import java.util.Map;

/**
 * Answers where one entity's icon sits relative to the map's own nebula icons - the question anything
 * trying to paint over the fog actually has, as opposed to the proxies for it.
 *
 * <p>The widget seeds its icon map from the location's entities first and appends its synthetic
 * per-system nebulae after all of them, so an entity that was in its location when the map opened is
 * drawn under the fog. Whether it still is can only be answered by reading the order back: the map
 * is re-seeded per open, an open is not the only thing that re-seeds it, and no published call
 * reports any of that.
 *
 * <p>Reading the state rather than inferring it from an event is the point. A rule keyed on "a map
 * has just opened" is a guess that stays wrong for the rest of a session the moment one open goes
 * unobserved, with nothing to correct it; a rule keyed on this recovers by itself, whatever the
 * reason the icon ended up buried.
 *
 * <p>Nebulae are matched by plugin type rather than by terrain type, for the reason the icon trace
 * reports both: a terrain reports the type the engine gates its drawing on, which is not always the
 * type it was registered as, so only the plugin reliably says what draws an icon.
 */
public final class MapIconLayeringProbe {

    private MapIconLayeringProbe() {
    }

    /**
     * Where an entity's icon sits in the order the map widget on screen will draw.
     *
     * <p>Costs a walk down the widget tree and a pass over the icon map, so a caller on a per-frame
     * path should ask only while it would act on the answer.
     *
     * @param entity the entity whose icon to place; null answers
     *        {@link MapIconLayering#UNREADABLE}
     * @return whether that icon clears the map's nebulae, is buried under them, or cannot be placed
     */
    public static MapIconLayering readLayeringOf(SectorEntityToken entity) {
        var icons = entity == null ? null : MapWidgetIcons.readIconMapOfShownMap();
        return icons == null
            ? MapIconLayering.UNREADABLE
            : readLayeringIn(icons, entity);
    }

    /**
     * Places one entity within an icon map already read.
     *
     * <p>An entity with no icon at all is {@link MapIconLayering#UNREADABLE} rather than buried: it
     * is the state between being put back into its location and the next frame seeding an icon for
     * it, and reading it as buried would have a caller acting on a placement that does not exist yet.
     *
     * @param icons the widget's icon map, keyed by the entity each icon draws
     * @param entity the entity to place
     * @return that entity's layering within this map
     */
    static MapIconLayering readLayeringIn(Map<?, ?> icons, SectorEntityToken entity) {
        var entityPosition = -1;
        var lastNebulaPosition = -1;
        var position = 0;
        for (var iconKey : icons.keySet()) {
            if (iconKey == entity) {
                entityPosition = position;
            } else if (isNebulaIcon(iconKey)) {
                lastNebulaPosition = position;
            }
            position++;
        }
        if (entityPosition < 0) {
            return MapIconLayering.UNREADABLE;
        }
        // A map holding no nebulae at all leaves nothing to clear, which counts as clear: there is
        // no fog over this icon and no move that could improve it.
        return entityPosition > lastNebulaPosition
            ? MapIconLayering.CLEAR_OF_NEBULAE
            : MapIconLayering.BURIED_UNDER_NEBULAE;
    }

    private static boolean isNebulaIcon(Object iconKey) {
        return iconKey instanceof CampaignTerrainAPI terrain
            && terrain.getPlugin() instanceof NebulaTerrainPlugin;
    }
}
