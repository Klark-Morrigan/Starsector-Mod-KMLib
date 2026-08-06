package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Describes the order the map widget will draw its terrain icons in, so an overlay that rides on a
 * terrain can be told where in the starfield it lands rather than left to infer it.
 *
 * <p>The widget keeps one icon per entity in an insertion-ordered map and walks the terrain-tagged
 * ones in that order, so where an overlay's own icon was seeded decides what is painted over it -
 * and the map's own starfield fog is seeded last, after every real entity. No published call
 * answers where anything sits in that order, and the map is reseeded each time one is opened, so
 * the position is neither derivable nor stable enough to be assumed. Reading it is what makes a
 * layering claim checkable in play instead of inferred from a decompile, and what says - one line,
 * years later - that a game build has started seeding its icons differently.
 *
 * <p>Reports the plugin class beside the reported type because the two can disagree. A terrain
 * reports the type the engine gates its drawing on, which is not always the type it was registered
 * as, so the type alone cannot say whose icon a row is; the plugin names the code that draws it and
 * always identifies it.
 *
 * <p>Describes the whole order where {@link MapIconLayeringProbe} answers one entity's place in it.
 * The two read the same collection through {@link MapWidgetIcons} and differ in what they are for: a
 * line to read when the picture is wrong, and an answer to act on while it still can be.
 *
 * <p>Read-only throughout. It walks the live icon map without touching it, and hands back a
 * description rather than the map, since the collection it read is the widget's own and outlives
 * the call.
 *
 * <p>Describes rather than logs, like {@link MapTabWidgetTrace}: a line written here would sit
 * outside every mod's logger subtree and answer to no mod's verbosity, and the caller is the only
 * one that knows how often the answer is worth repeating.
 */
public final class MapIconOrderTrace {

    private static final Logger LOG = Global.getLogger(MapIconOrderTrace.class);

    // What a terrain-tagged icon reports when its entity is not a terrain after all, or holds no
    // plugin. Neither is expected; both are described rather than dropped, since an unexpected icon
    // still occupies a slot and moving every position after it is exactly what would mislead.
    private static final String UNTYPED_TERRAIN = "untyped";
    private static final String NO_PLUGIN = "none";

    // Says once per session that this stopped working, since a caller handed null cannot tell a
    // screen with no map from a reach that broke.
    private static final SessionWarning WARNING = new SessionWarning(LOG);

    private MapIconOrderTrace() {
    }

    /**
     * The terrain icons the map widget on screen is holding, in the order it will draw them: how
     * many there are, then each one's position, reported type and plugin.
     *
     * <p>Costs a walk down the widget tree and a pass over the icon map, so a caller in a render
     * pass should ask only while it intends to report the answer.
     *
     * @return a one-line description for a log, or null when no map is on screen or the reach into
     *         the widget tree failed - neither of which the caller can act on differently
     */
    public static String describeTerrainIconOrder() {
        try {
            // Null off the map screens, and quietly so: a screen showing no map is the ordinary
            // state rather than a reach that stopped working.
            var mapTab = ShownMapTab.resolveShownMapTab();
            if (mapTab == null) {
                return null;
            }
            var icons = MapWidgetIcons.readIconMapUnder(mapTab);
            if (icons == null) {
                warnOnce("no component under the map tab answers its icon accessor", null);
                return null;
            }
            return describeTerrainIcons(readTerrainIcons(icons));
        } catch (Throwable failure) {
            // Swallowed rather than raised: this is a diagnostic, and one that cannot read the tree
            // must not take down the render pass its caller is in the middle of.
            warnOnce("the map widget's icon map could not be read by reflection", failure);
            return null;
        }
    }

    /**
     * Builds the line from readings taken elsewhere.
     *
     * <p>Names the total before naming any icon, so a description held to the cap is visibly a
     * head of the order rather than the whole of it.
     *
     * @param terrainIcons the terrain icons in the order the widget holds them
     * @return a one-line description, naming at most the shared cap of them
     */
    static String describeTerrainIcons(List<TerrainIconReading> terrainIcons) {
        return "terrainIcons=" + terrainIcons.size()
            + " order=" + ProbeDescriptions.describeUpToCap(
                terrainIcons, MapIconOrderTrace::describeTerrainIcon);
    }

    /**
     * Every terrain-tagged icon in a widget's icon map, in the order the map holds them.
     *
     * <p>Positions count the non-terrain icons too. Those hold slots between the terrain ones, so a
     * position that skipped them would be an index into this list rather than into the widget's
     * map - and the whole use of the reading is comparing one map open's positions against
     * another's.
     *
     * <p>Reads the map without touching it, and keeps nothing that outlives the call: the map is
     * the widget's own, and it rebuilds it whenever a map is opened.
     *
     * @param icons the widget's icon map, keyed by the entity each icon draws
     * @return one reading per terrain-tagged key, in the map's own order
     */
    static List<TerrainIconReading> readTerrainIcons(Map<?, ?> icons) {
        var terrainIcons = new ArrayList<TerrainIconReading>();
        var position = 0;
        for (var iconKey : icons.keySet()) {
            if (iconKey instanceof SectorEntityToken entity && entity.hasTag(Tags.TERRAIN)) {
                terrainIcons.add(readTerrainIcon(position, entity));
            }
            position++;
        }
        return terrainIcons;
    }

    private static String describeTerrainIcon(TerrainIconReading icon) {
        return "[" + icon.position() + "] " + icon.terrainType() + " " + icon.pluginTypeName();
    }

    // The tag is what the widget itself sorts on, so an entity carrying it is reported whether or
    // not it turns out to be a terrain - the tag going one way and the type the other is a state
    // worth seeing rather than one to filter out.
    private static TerrainIconReading readTerrainIcon(int position, SectorEntityToken entity) {
        if (!(entity instanceof CampaignTerrainAPI terrain)) {
            return new TerrainIconReading(position, UNTYPED_TERRAIN, NO_PLUGIN);
        }
        var plugin = terrain.getPlugin();
        return new TerrainIconReading(
            position,
            terrain.getType(),
            plugin == null ? NO_PLUGIN : plugin.getClass().getSimpleName());
    }

    // On this library's own logger, since a reach that stopped fitting the game is the library's
    // news rather than the consuming mod's. Both failures word the same consequence, so both go
    // through one warning and the first of them silences the rest.
    private static void warnOnce(String reason, Throwable failure) {
        WARNING.warnOnce(
            "Could not read the map widget's icon order: " + reason
                + ". Nothing will be described about map layering this session.",
            failure);
    }
}
