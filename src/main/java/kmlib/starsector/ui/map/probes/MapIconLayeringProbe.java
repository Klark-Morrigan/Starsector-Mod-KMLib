package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin;

import org.apache.log4j.Logger;

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

    private static final Logger LOG = Global.getLogger(MapIconLayeringProbe.class);

    // Says once per session that this stopped working, since a caller handed UNREADABLE cannot tell
    // a screen with no map from a reach that broke.
    private static final SessionWarning WARNING = new SessionWarning(LOG);

    private MapIconLayeringProbe() {
    }

    /**
     * Where an entity's icon sits in the order the map widget on screen will draw.
     *
     * <p>Costs a walk down the widget tree and a pass over the icon map, so a caller on a per-frame
     * path should ask only while it would act on the answer.
     *
     * @param entity the entity whose icon to place; null answers {@link Layering#UNREADABLE}
     * @return whether that icon clears the map's nebulae, is buried under them, or cannot be placed
     */
    public static Layering readLayeringOf(SectorEntityToken entity) {
        try {
            var mapTab = ShownMapTab.resolveShownMapTab();
            if (mapTab == null || entity == null) {
                // No map on screen is the ordinary state off the map screens rather than a reach
                // that stopped working, so it is not reported.
                return Layering.UNREADABLE;
            }
            var icons = MapWidgetIcons.readIconMapUnder(mapTab);
            if (icons == null) {
                warnOnce("no component under the map tab answers its icon accessor", null);
                return Layering.UNREADABLE;
            }
            return readLayeringIn(icons, entity);
        } catch (Throwable failure) {
            // Swallowed rather than raised: a caller asks this from inside a frame, and a reach that
            // cannot read the tree must not take that frame down with it.
            warnOnce("the map widget's icon map could not be read by reflection", failure);
            return Layering.UNREADABLE;
        }
    }

    /**
     * Places one entity within an icon map already read.
     *
     * <p>An entity with no icon at all is {@link Layering#UNREADABLE} rather than buried: it is the
     * state between being put back into its location and the next frame seeding an icon for it, and
     * reading it as buried would have a caller acting on a placement that does not exist yet.
     *
     * @param icons the widget's icon map, keyed by the entity each icon draws
     * @param entity the entity to place
     * @return that entity's layering within this map
     */
    static Layering readLayeringIn(Map<?, ?> icons, SectorEntityToken entity) {
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
            return Layering.UNREADABLE;
        }
        // A map holding no nebulae at all leaves nothing to clear, which counts as clear: there is
        // no fog over this icon and no move that could improve it.
        return entityPosition > lastNebulaPosition
            ? Layering.CLEAR_OF_NEBULAE
            : Layering.BURIED_UNDER_NEBULAE;
    }

    private static boolean isNebulaIcon(Object iconKey) {
        return iconKey instanceof CampaignTerrainAPI terrain
            && terrain.getPlugin() instanceof NebulaTerrainPlugin;
    }

    // On this library's own logger, since a reach that stopped fitting the game is the library's
    // news rather than the consuming mod's. Both failures word the same consequence, so both go
    // through one warning and the first of them silences the rest.
    private static void warnOnce(String reason, Throwable failure) {
        WARNING.warnOnce(
            "Could not place a map icon in the widget's draw order: " + reason
                + ". Nothing will be lifted over the map's nebulae this session.",
            failure);
    }

    /** Where an icon sits relative to the nebulae the map widget appends after its own entities. */
    public enum Layering {

        /** Drawn after every nebula icon, so the map's fog does not paint over it. */
        CLEAR_OF_NEBULAE,

        /** Drawn before at least one nebula icon, so that fog paints over it. */
        BURIED_UNDER_NEBULAE,

        /** No map on screen, no icon for this entity yet, or the widget could not be read. */
        UNREADABLE
    }
}
