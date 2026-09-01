package kmlib.starsector.ui.map.controls;

/**
 * One resolved instance of the row of toggles the game furnishes its map screens from - the strip
 * along the bottom of the {@code M} screen carrying Starscape, Fuel range and the rest, and the
 * shorter band the intel screen's map visor carries. A control appended to that row is appended to
 * one of these.
 *
 * <p>Held rather than fetched afresh at each use, because the widget is what an appended control is
 * attached to: two reads a frame apart can hand back different rows, and a control appended against
 * one of them while the other is on screen is appended to nothing the player can see.
 *
 * <p>The widget stays wrapped rather than being handed out beyond this package. Its class is
 * obfuscated, so nothing outside could name it in a signature anyway - but the wrapper is what says
 * that writing into the row is this package's business, and that a caller holding one is holding an
 * attachment point rather than a widget to reach into.
 *
 * <p>What such a caller can ask is which row it has, and it asks by identity: the row is rebuilt
 * inside the map widget's constructor on every open, so a held one goes stale each time the player
 * closes and reopens the screen. Nothing about the reference itself says so, and a control still
 * attached to the previous row draws nowhere while the new row stands bare.
 */
public final class MapFilterRow {

    // The live widget. Typed as Object because the class is obfuscated and its name churns between
    // game builds, which is also why nothing here reaches into it by name.
    private final Object rowWidget;

    /**
     * Wraps a widget the resolution beside this one has already recognised as a row, which is why
     * this is not reachable from outside the package: a wrapper built over some other widget would
     * be an attachment point to a row that does not exist.
     *
     * @param rowWidget the live row widget
     */
    MapFilterRow(Object rowWidget) {
        this.rowWidget = rowWidget;
    }

    /**
     * Whether this and another handle name the same live row.
     *
     * <p>Identity rather than equality: two handles describe the same row only by wrapping the same
     * object, and what an obfuscated widget's own {@code equals} compares is the game's business.
     *
     * @param otherRow the row to compare against, or null when the caller has none - which answers
     *                 false, since no row is not the same row as this one
     * @return whether both wrap the one widget
     */
    public boolean isSameRowAs(MapFilterRow otherRow) {

        return otherRow != null && otherRow.rowWidget == rowWidget;
    }

    /**
     * The widget itself, for the writes in this package that have to reach into it.
     *
     * @return the live row widget
     */
    Object getRowWidget() {

        return rowWidget;
    }
}
