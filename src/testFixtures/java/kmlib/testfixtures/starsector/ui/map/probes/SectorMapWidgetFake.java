package kmlib.testfixtures.starsector.ui.map.probes;

import kmlib.testfixtures.starsector.ui.map.BaseSectorMapFake;

import java.util.List;

/**
 * A sector map that is also a parent in the core UI's tree, which is the shape a rule about embedded
 * maps has to be driven against: a map widget composed into somebody's panel, with content of its own
 * hanging below it. Published as a fixture variant so both KMLib's and consuming mods' tests build the same shape
 * of tree.
 *
 * <p>Not a component, deliberately. What a walk recognises a map by is the map interface, and what
 * makes one measurable on screen is a separate question its holder answers - so a fixture that were
 * both would let a rule depending on the second pass while testing only the first.
 */
public final class SectorMapWidgetFake extends BaseSectorMapFake {
    private final List<Object> children;

    public SectorMapWidgetFake(Object... children) {
        this.children = List.of(children);
    }

    public List<Object> getChildrenCopy() {
        return children;
    }
}
