package kmlib.starsector.ui.map.controls;

import kmlib.starsector.ui.coreui.CoreUiTree;
import kmlib.starsector.ui.map.probes.EmbeddedMap;
import kmlib.starsector.ui.map.probes.ShownMapTab;

/**
 * Where the map's filter row stands, for each of the two places a map can be on screen: the screen
 * the game is showing a map on, and a map some mod has composited into a panel of its own.
 *
 * <p>Both screens' rows are reached the same way, off the map widget itself, which is what lets one
 * read serve the {@code M} screen's full-width strip and the intel visor's shorter band with no
 * branch on which screen is up. That is a fact about the game rather than a convenience: the map
 * widget builds its own row, so every map on screen has one and no map's row is reachable except
 * through it.
 *
 * <p>No child indexing anywhere in the reach. The map is recognised by the published map interface
 * and the row by an unobfuscated accessor on it, so neither anchor moves when a mod inserts a panel
 * of its own beside either - which is the whole reason a write into somebody else's widget can be
 * attempted at all.
 *
 * <p>Answers null wherever there is nothing to answer with: no map on screen, a map whose shape no
 * longer carries the accessor, or one carrying it and answering nothing. The three are not
 * distinguished, because a caller has the same thing to do about each - stand down, and leave the
 * row untouched.
 */
public final class MapFilterRows {

    // The map widget's own accessor for its filter row. Part of its published shape rather than an
    // obfuscated member, so the name survives a game build the way the map interface does.
    private static final String GET_FILTER_METHOD = "getFilter";

    private MapFilterRows() {
    }

    /**
     * The filter row of the map the player is looking at.
     *
     * @return the row of the {@code M} screen's map tab or of the intel screen's lit map visor, or
     *         null when no screen is showing a map or the map on screen has no row to offer
     * @throws RuntimeException when the reach to the current tab is absent or fails outright, which
     *                          is the map read's own contract and is left standing here so a caller
     *                          applies one policy to a broken reach rather than two
     */
    public static MapFilterRow resolveShownMapFilterRow() {

        return resolveMapFilterRowOf(ShownMapTab.resolveShownMapTab());
    }

    /**
     * The filter row of a map standing somewhere other than the screen the game shows one on.
     *
     * <p>Beside the shown read rather than folded into it, because the two answer about different
     * surfaces at once: a mod's composited map is on screen precisely when the shown read says
     * there is none.
     *
     * @param embeddedMap the map found in somebody else's panel, or null when the caller found none
     * @return its row, or null when there is no such map or it has no row to offer
     */
    public static MapFilterRow resolveEmbeddedMapFilterRow(EmbeddedMap embeddedMap) {

        return embeddedMap == null
            ? null
            : resolveMapFilterRowOf(embeddedMap.widget());
    }

    /**
     * The rule both reads apply, over a map widget read elsewhere.
     *
     * @param mapWidget the map to take the row off, or null when no map was found
     * @return its row, or null when there is no map or it does not answer with one
     */
    static MapFilterRow resolveMapFilterRowOf(Object mapWidget) {

        if (mapWidget == null) {
            return null;
        }

        // The forgiving hop rather than the raising one, which is what turns the absences above
        // into the null below rather than into a throw each caller would have to catch back into
        // one.
        var rowWidget = CoreUiTree.readHopIfOffered(mapWidget, GET_FILTER_METHOD);

        return rowWidget == null
            ? null
            : new MapFilterRow(rowWidget);
    }
}
