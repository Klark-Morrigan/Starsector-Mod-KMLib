package kmlib.starsector.ui.map.probes;

import kmlib.starsector.ui.coreui.CoreUiTree;

import java.util.Map;

/**
 * Reaches the icon map of whichever map widget is on screen - the insertion-ordered collection the
 * widget draws from, keyed by the entity each icon belongs to.
 *
 * <p>Finds the widget by descending from the map tab until a component answers the icon accessor.
 * The widget is neither the tab nor the surface picked out of it - the tab holds a scrolling panel,
 * whose content container holds the map - so a fixed path would be a fact about one build's layout.
 * By method name rather than by field type for the same reason: the accessor is public and its name
 * survives obfuscation, while the fields down to it do not.
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

    // The map widget's own icon accessor. Public and part of its contract, so the name survives
    // obfuscation where the fields leading down to the widget do not.
    private static final String GET_ICONS_METHOD = "getIcons";

    private MapWidgetIcons() {
    }

    /**
     * Takes the tab rather than resolving it, so a caller that has to tell "no map is on screen"
     * from "the map is there and the reach into it failed" can, those being an ordinary state and a
     * reportable one. Resolving it here would collapse the two into one null.
     *
     * @param mapTab the map tab on screen, from {@link ShownMapTab}
     * @return the widget's live icon map, or null when nothing under the tab answers the accessor
     */
    static Map<?, ?> readIconMapUnder(Object mapTab) {
        return resolveIconMapUnder(mapTab, 0);
    }

    // Depth-first from the tab, first component that answers the accessor wins. Only the map widget
    // defines it, so there is nothing else the walk could find first.
    private static Map<?, ?> resolveIconMapUnder(Object component, int depth) {
        if (component == null || depth > ProbeLimits.MAX_SEARCH_DEPTH) {
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
}
