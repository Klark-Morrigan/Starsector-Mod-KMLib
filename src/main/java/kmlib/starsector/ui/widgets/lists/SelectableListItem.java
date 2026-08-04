package kmlib.starsector.ui.widgets.lists;

/**
 * One row of a picker list - the seam between {@link ListPickerControl} and whatever a consuming
 * mod's list actually holds. The picker draws and reports through this and nothing else: an id it
 * hands back when a row is picked, a label it writes on the row, and a crest it draws beside the
 * label. What else an item carries - the numbers it is ranked by, the thing in the game the id
 * resolves to - the picker never opens, since ranking runs through the consumer's own
 * {@link ListSortMode} comparators and resolving is the consumer's business.
 *
 * <p>A consumer declares its own item type (typically a record) and implements this on it, so the
 * comparators keep ranking the type the consumer declared and nothing is copied into a library
 * value on the way into the picker.
 */
public interface SelectableListItem {

    /**
     * @return the item's stable id - the value a pick reports and the picker resolves its lit row
     *         by; must be unique within one list, since a pick is remembered as an id rather than
     *         a position
     */
    String itemId();

    /**
     * @return the item's label for its picker row; null when no name resolves, which the row draws
     *         as unlabelled
     */
    String displayName();

    /**
     * @return the crest sprite path drawn beside the label, or null when the item has no crest,
     *         which the row draws as the label alone
     */
    String crestSpritePath();
}
