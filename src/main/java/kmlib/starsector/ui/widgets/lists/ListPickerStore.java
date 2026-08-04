package kmlib.starsector.ui.widgets.lists;

/**
 * Where a {@link ListPickerControl}'s three player choices are kept - the spotlighted item, the
 * sort, and the column count. The picker composes three widgets that each report a pick rather
 * than persisting one, and a consumer wiring them separately would thread three callbacks through
 * every layer between its own store and the picker; declaring them together means a consumer hands
 * over one value that names all three of its slots.
 *
 * <p>Write-only, and deliberately so: the picker never reads a choice back through this, since the
 * live values arrive as its own parameters. That keeps this package free of any read path into a
 * consumer's save - the division the whole family rests on, that the library owns the model and
 * the consuming mod owns where the answer is kept.
 */
public interface ListPickerStore {

    /** Clears the spotlight, so the list draws with no row lit and nothing filtered. */
    void clearItemPick();

    /**
     * Persists a picked column count.
     *
     * @param columns the choice the columns selector reported
     */
    void storeColumnsPick(ListColumns columns);

    /**
     * Persists a picked item as the spotlighted one.
     *
     * @param itemId the id of the item whose row was picked, never null - a pick that spotlights
     *               nothing arrives through {@link #clearItemPick()} instead, so a consumer never
     *               has to read a null as an instruction
     */
    void storeItemPick(String itemId);

    /**
     * Persists a picked sort, both its mode and its direction.
     *
     * @param sort the sort the sort selector reported; typed on the wildcard because a store keeps
     *             the pair's two keys and never ranks anything with it
     */
    void storeSortPick(ListSort<?> sort);
}
