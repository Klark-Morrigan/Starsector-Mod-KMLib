package kmlib.testfixtures.starsector.ui.map.probes;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.ui.SectorMapAPI;

import java.util.List;

/**
 * A sector map that is also a parent in the core UI's tree, which is the shape a rule about embedded
 * maps has to be driven against: a map widget composed into somebody's panel, with content of its own
 * hanging below it. Shipped from KMLib so both KMLib's and consuming mods' tests build the same shape
 * of tree.
 *
 * <p>Not a component, deliberately. What a walk recognises a map by is the map interface, and what
 * makes one measurable on screen is a separate question its holder answers - so a fixture that were
 * both would let a rule depending on the second pass while testing only the first.
 *
 * <p>The published half of a map is two entity lookups no tree walk takes. They throw rather than
 * answering null, so a caller that strayed into one fails here instead of carrying on against a
 * fixture that cannot stand for what it asked.
 */
public final class SectorMapWidgetFake implements SectorMapAPI {
    private final List<Object> children;

    public SectorMapWidgetFake(Object... children) {
        this.children = List.of(children);
    }

    @Override
    public SectorEntityToken getConstellationLabelEntity(Constellation constellation) {
        throw new UnsupportedOperationException("A fixture for tree walks holds no entities.");
    }

    public List<Object> getChildrenCopy() {
        return children;
    }

    @Override
    public SectorEntityToken getIntelIconEntity(IntelInfoPlugin intel) {
        throw new UnsupportedOperationException("A fixture for tree walks holds no entities.");
    }
}
