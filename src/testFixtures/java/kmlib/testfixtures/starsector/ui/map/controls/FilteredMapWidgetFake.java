package kmlib.testfixtures.starsector.ui.map.controls;

import kmlib.testfixtures.starsector.ui.map.BaseSectorMapFake;

/**
 * A sector map that offers its filter row the way the game's own does, which is the one hop a
 * control appended to that row is reached through. Shipped from KMLib so both KMLib's and consuming
 * mods' tests reach a row the same way.
 *
 * <p>Beside the fixtures that stand for a map a walk merely has to find, because this models a
 * different half of the widget: those answer about where a map is and what hangs under it, and this
 * answers what the map is furnished with. A map built holding no row stands for the shape that
 * carries the accessor and answers nothing through it, which is a state a caller has to survive.
 */
public final class FilteredMapWidgetFake extends BaseSectorMapFake {

    private final Object filterRow;

    /**
     * @param filterRow the row this map offers, or null for a map that carries the accessor and
     *                  answers nothing through it
     */
    public FilteredMapWidgetFake(Object filterRow) {
        this.filterRow = filterRow;
    }

    public Object getFilter() {
        return filterRow;
    }
}
