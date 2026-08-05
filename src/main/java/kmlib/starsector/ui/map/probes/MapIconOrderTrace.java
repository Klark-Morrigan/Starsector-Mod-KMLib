package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import kmlib.starsector.ui.coreui.CoreUiTree;

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
 * <p>Finds the widget by descending from the map tab until a component answers the icon accessor.
 * The widget is neither the tab nor the surface picked out of it - the tab holds a scrolling panel,
 * whose content container holds the map - so a fixed path would be a fact about one build's layout.
 * By method name rather than by field type for the same reason: the accessor is public and its name
 * survives obfuscation, while the fields down to it do not.
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

    // The map widget's own icon accessor. Public and part of its contract, so the name survives
    // obfuscation where the fields leading down to the widget do not.
    private static final String GET_ICONS_METHOD = "getIcons";

    // How deep to descend from the map tab before giving up. The widget sits a few panels down; a
    // bound keeps a pathological tree from a runaway walk.
    private static final int MAX_SEARCH_DEPTH = 12;

    // Cap on the icons named in one description, so a sector full of nebulae stays readable. The
    // count is reported alongside, so a description that named fewer says so rather than reading
    // as the whole map.
    private static final int MAX_TRACE_ICONS = 24;

    // What a terrain-tagged icon reports when its entity is not a terrain after all, or holds no
    // plugin. Neither is expected; both are described rather than dropped, since an unexpected icon
    // still occupies a slot and moving every position after it is exactly what would mislead.
    private static final String UNTYPED_TERRAIN = "untyped";
    private static final String NO_PLUGIN = "none";

    // One warning per session, so a build where the reach breaks says so once rather than per call.
    private static boolean hasWarnedThisSession;

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
            var icons = resolveIconMapUnder(mapTab, 0);
            if (icons == null) {
                warnOnce("no component under the map tab answers " + GET_ICONS_METHOD, null);
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
     * @return a one-line description, naming at most the first {@code MAX_TRACE_ICONS} of them
     */
    static String describeTerrainIcons(List<TerrainIconReading> terrainIcons) {
        var describedIcons = new ArrayList<String>();
        for (var icon : terrainIcons) {
            if (describedIcons.size() >= MAX_TRACE_ICONS) {
                break;
            }
            describedIcons.add("[" + icon.position() + "] "
                + icon.terrainType() + " " + icon.pluginTypeName());
        }
        return "terrainIcons=" + terrainIcons.size() + " order=" + describedIcons;
    }

    // Every terrain-tagged icon, keyed by where it sits among all of them. The position counts
    // non-terrain icons too: they hold slots between the terrain ones, and a position that skipped
    // them would not be the one a later reading could be compared against.
    private static List<TerrainIconReading> readTerrainIcons(Map<?, ?> icons) {
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

    // Depth-first from the tab, first component that answers the accessor wins. Only the map widget
    // defines it, so there is nothing else the walk could find first.
    private static Map<?, ?> resolveIconMapUnder(Object component, int depth) {
        if (component == null || depth > MAX_SEARCH_DEPTH) {
            return null;
        }
        var icons = readIconMapOf(component);
        if (icons != null) {
            return icons;
        }
        for (var child : CoreUiTree.readChildrenOf(component)) {
            var childIcons = resolveIconMapUnder(child, depth + 1);
            if (childIcons != null) {
                return childIcons;
            }
        }
        return null;
    }

    private static Map<?, ?> readIconMapOf(Object component) {
        try {
            return CoreUiTree.invokeNoArg(component, GET_ICONS_METHOD) instanceof Map<?, ?> icons
                ? icons
                : null;
        } catch (Throwable notTheMapWidget) {
            // Every component but one is expected to fail this, so an absent accessor is how the
            // walk moves on rather than something to report.
            return null;
        }
    }

    // WARN rather than DEBUG, and on this library's own logger: a reach that stopped fitting the
    // game is the library's news, and it has to survive the default log level to be seen at all.
    private static void warnOnce(String reason, Throwable failure) {
        if (hasWarnedThisSession) {
            return;
        }
        hasWarnedThisSession = true;
        var message = "Could not read the map widget's icon order: " + reason
            + ". Nothing will be described about map layering this session.";
        if (failure == null) {
            LOG.warn(message);
        } else {
            LOG.warn(message, failure);
        }
    }
}
