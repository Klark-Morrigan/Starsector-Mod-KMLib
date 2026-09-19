package kmlib.testfixtures.starsector.ui.map.probes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A component answering the map widget's {@code getIcons} contract, so a rule about the order the
 * map draws its icons in can be driven without a running game. Published as a fixture variant so both KMLib's
 * and consuming mods' tests build the same shape of widget.
 *
 * <p>Insertion-ordered, because the order is the whole subject: the live widget keeps one icon per
 * entity in the order it seeded them, and every rule read off it is about where one sits relative to
 * the rest. A fixture backed by an unordered map would answer those rules differently on different
 * runs.
 *
 * <p>Also a parent in the core UI's tree, since the widget a walk is looking for is never the root
 * it starts from - it is found somewhere below one, and a fixture that could only be the root would
 * leave the descent to it untested.
 */
public final class MapIconOrderWidgetFake {

    private final List<Object> children;
    private final Map<Object, Object> icons = new LinkedHashMap<>();

    public MapIconOrderWidgetFake(Object... children) {
        this.children = List.of(children);
    }

    /**
     * Seeds the widget's icons in the order given, as the live one is seeded on a map opening.
     *
     * @param iconKeys the entities to hold an icon for, in the order the widget would draw them
     */
    public void seedIconsFor(Object... iconKeys) {
        for (var iconKey : iconKeys) {
            // The icon itself is never read - every rule over this map is about its keys and their
            // order - so the key stands in for its own icon rather than a second fixture being built
            // to be ignored.
            icons.put(iconKey, iconKey);
        }
    }

    public List<Object> getChildrenCopy() {
        return children;
    }

    public Map<Object, Object> getIcons() {
        return icons;
    }
}
