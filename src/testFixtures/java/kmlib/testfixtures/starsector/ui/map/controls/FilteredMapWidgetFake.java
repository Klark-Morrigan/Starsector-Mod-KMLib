package kmlib.testfixtures.starsector.ui.map.controls;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.ui.SectorMapAPI;

/**
 * A sector map that offers its filter row the way the game's own does, which is the one hop a
 * control appended to that row is reached through. Shipped from KMLib so both KMLib's and consuming
 * mods' tests reach a row the same way.
 *
 * <p>Beside the fixtures that stand for a map a walk merely has to find, because this models a
 * different half of the widget: those answer about where a map is and what hangs under it, and this
 * answers what the map is furnished with. A map built holding no row stands for the shape that
 * carries the accessor and answers nothing through it, which is a state a caller has to survive.
 *
 * <p>The published half of a map is two entity lookups nothing here takes. They throw rather than
 * answering null, so a caller that strayed into one fails here instead of carrying on against a
 * fixture that cannot stand for what it asked.
 */
public final class FilteredMapWidgetFake implements SectorMapAPI {

    private final Object filterRow;

    /**
     * @param filterRow the row this map offers, or null for a map that carries the accessor and
     *                  answers nothing through it
     */
    public FilteredMapWidgetFake(Object filterRow) {
        this.filterRow = filterRow;
    }

    @Override
    public SectorEntityToken getConstellationLabelEntity(Constellation constellation) {
        throw new UnsupportedOperationException("A fixture for a map's filter row holds no entities.");
    }

    public Object getFilter() {
        return filterRow;
    }

    @Override
    public SectorEntityToken getIntelIconEntity(IntelInfoPlugin intel) {
        throw new UnsupportedOperationException("A fixture for a map's filter row holds no entities.");
    }
}
