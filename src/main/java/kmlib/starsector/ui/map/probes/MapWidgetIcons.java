package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.Global;

import kmlib.logging.SessionWarning;
import kmlib.starsector.ui.coreui.CoreUiTree;

import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Reaches the icon map of whichever map widget is on screen - the insertion-ordered collection the
 * widget draws from, keyed by the entity each icon belongs to.
 *
 * <p>Finds the widget with {@link SubtreeSearch}, descending from the map tab until a component
 * answers the icon accessor. The widget is neither the tab nor the surface picked out of it - the
 * tab holds a scrolling panel, whose content container holds the map - so a fixed path would be a
 * fact about one build's layout. By method name rather than by field type for the same reason: the
 * accessor is public and its name survives obfuscation, while the fields down to it do not.
 *
 * <p>Read-only, and it hands back the widget's own live collection rather than a copy: every caller
 * here reads it within the call and keeps nothing, and copying a map that can hold a nebula per star
 * system would cost more than the reads it serves. A caller that wants to hold the answer holds
 * something derived from it instead.
 *
 * <p>Separate from the probes that use it because more than one now does, and both would otherwise
 * carry their own copy of a walk that is the fragile part - a tree the walk stopped fitting has to
 * be fixed in one place, not found twice.
 */
final class MapWidgetIcons {

    private static final Logger LOG = Global.getLogger(MapWidgetIcons.class);

    // The map widget's own icon accessor. Public and part of its contract, so the name survives
    // obfuscation where the fields leading down to the widget do not.
    private static final String GET_ICONS_METHOD = "getIcons";

    // One latch for one reach. Each caller wording its own consequence would put two near-identical
    // lines in the log for a single tree that stopped being readable, and each would have to be
    // silenced separately.
    private static final SessionWarning WARNING = MapProbeWarnings.createSharedWarning(LOG);

    private MapWidgetIcons() {
    }

    /**
     * The live icon map of the map widget on screen, guarded: no map tab is answered quietly, and a
     * tab that is there with nothing under it answering is reported once.
     *
     * <p>Every caller wants the same thing from a failure - to stop and do nothing - so the guard
     * lives here rather than in each of them. It also means the walk and the reporting of the walk
     * are one thing to keep working, not one plus a copy per caller.
     *
     * @return the widget's live icon map, or null when no map is on screen or the reach failed
     */
    static Map<?, ?> readIconMapOfShownMap() {
        try {
            return readIconMapUnder(ShownMapTab.resolveShownMapTab());

        } catch (Throwable failure) {
            // Swallowed rather than raised: callers ask this from inside a render pass or a
            // campaign frame, and a reach that cannot read the tree must not take either down.
            warnOnce("the map widget's icon map could not be read by reflection", failure);
            return null;
        }
    }

    /**
     * The same read over a tab a caller already holds, so which shapes answer and which are reported
     * can be settled without a live screen to resolve one from.
     *
     * <p>Left unguarded: it is the tab resolution above that decides what a failed walk means, and a
     * second policy here would have to agree with that one forever.
     *
     * @param mapTab the tab to search below, or null when no map screen is up
     * @return the widget's live icon map, or null when there is no tab or nothing under it answers
     */
    static Map<?, ?> readIconMapUnder(Object mapTab) {

        // Null off the map screens, and quietly so: a screen showing no map is the ordinary state
        // rather than a reach that stopped working.
        if (mapTab == null) {
            return null;
        }
        var icons = SubtreeSearch.findFirstUnder(mapTab, MapWidgetIcons::readIconMapOf);

        if (icons == null) {
            warnOnce("no component under the map tab answers " + GET_ICONS_METHOD, null);
        }
        return icons;
    }

    // On this library's own logger, since a reach that stopped fitting the game is the library's
    // news rather than the consuming mod's. The consequence is worded for the reach rather than for
    // any one caller's use of it, both callers losing the same thing.
    private static void warnOnce(String reason, Throwable failure) {
        WARNING.warnOnce(
            "Could not read the map widget's icon order: " + reason
                + ". Nothing that depends on where an icon sits will work this session.",
            failure);
    }

    // What the search above tries on each component: only the map widget defines the accessor, so
    // the first component that answers is the widget and there is nothing else it could find first.
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
}
