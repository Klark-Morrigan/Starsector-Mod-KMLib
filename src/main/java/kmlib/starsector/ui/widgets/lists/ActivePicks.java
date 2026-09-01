package kmlib.starsector.ui.widgets.lists;

import java.util.Objects;

/**
 * What a picker is currently showing: which item is spotlighted, how the list is ranked, and how
 * many columns it is laid across. The three are the consuming mod's live read of its own store,
 * handed back to the picker so the block draws the state the player last left it in.
 *
 * <p>They travel together because they are one reading. A picker's body is built afresh every frame
 * from all three, and a caller pairing a fresh one with a value it held from an earlier frame would
 * light a row in an order that has since changed - so the read is taken once, as a whole, and
 * carried as a whole.
 *
 * <p>Bundled also because they are exactly what {@link ListPickerStore} writes: an item pick, a sort
 * pick and a columns pick out, the same three in. The two sides being one shape is what lets a
 * consumer's binder be read down its middle - what it stores under each key, and what it reads back
 * for each - rather than as six unrelated hops.
 *
 * <p>{@code selectedItemId} is the one that may be absent, a null standing for the cleared
 * spotlight {@link ListPickerStore#clearItemPick()} reports. A sort and a column count are always
 * in force: a list is always ordered somehow and always laid across some number of columns.
 *
 * @param <T>            the consuming mod's item type, which the bundled sort ranks
 * @param selectedItemId the spotlighted item's id, or null when nothing is spotlighted
 * @param sort           how the list is ranked - the metric, its direction, and the vocabulary both
 *                       were chosen from
 * @param columns        how many columns the list wraps its rows across
 */
public record ActivePicks<T>(
    String selectedItemId,
    ListSort<T> sort,
    ListColumns columns) {

    /**
     * Rejects a missing sort or column count, so a half-built reading fails where it is assembled
     * rather than inside the frame that first tries to rank or lay out a list with it. The
     * spotlighted id is unchecked, a null there being the state where nothing is spotlighted.
     */
    public ActivePicks {
        Objects.requireNonNull(sort, "sort");
        Objects.requireNonNull(columns, "columns");
    }
}
