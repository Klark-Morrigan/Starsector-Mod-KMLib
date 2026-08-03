package kmlib.starsector.ui.widgets.lists;

/**
 * How many columns a picker list wraps its rows across - one tall stack or two side by side.
 * Declared outright rather than as a per-consumer seam like {@link ListSortMode}, because nothing in
 * a one-or-two column choice is any consumer's own. Each choice owns the save-stable key its pick
 * persists under, the label its selector segment draws, and the column count it feeds the list
 * widget's row geometry, so the selector, the persistence, and the layout all read one source rather
 * than re-deriving the count. Where the key is stored is the consumer's, so nothing here reaches a
 * save.
 *
 * <p>The labels are the literals "1" and "2" rather than string ids the consumer resolves, which is
 * the deliberate exception to {@link ListSortMode}'s hand-over-drawn-text rule: that rule exists so
 * player-visible prose stays translatable, and a digit standing for a count is not prose.
 *
 * <p>{@link #DEFAULT} is the single column, the layout a fresh save and any unrecognised stored
 * key fall back to, so the list always has a live column count even before the player picks one.
 * The values are declared in the order the selector lays its segments out left to right, so the
 * segment a click reports maps straight back to a choice by position.
 */
public enum ListColumns {
    // The stored key happens to spell the count, but it is not derived from it: a key is a frozen
    // save identity and the count is a layout number, so deriving one from the other would let a
    // change to the layout silently rewrite what every existing save resolves through.
    ONE("1", 1),
    TWO("2", 2);

    /** The layout a fresh save and any unrecognised stored key fall back to, so a count always exists. */
    public static final ListColumns DEFAULT = ONE;

    private final String persistenceKey;
    private final int columnCount;

    ListColumns(String persistenceKey, int columnCount) {
        this.persistenceKey = persistenceKey;
        this.columnCount = columnCount;
    }

    /**
     * Resolves a stored column-count key back to its choice, falling back to {@link #DEFAULT} when
     * the key is absent (a save that never picked a count) or names a choice this build no longer
     * offers (a key left by an older or a modded build), so the list always resolves to a live
     * count rather than failing on an unknown key.
     *
     * @param key the persisted column-count key, or null when nothing is stored
     * @return the matching choice, or {@link #DEFAULT} when the key is null or unrecognised
     */
    public static ListColumns fromKeyOrDefault(String key) {
        for (var choice : values()) {
            if (choice.persistenceKey.equals(key)) {
                return choice;
            }
        }
        return DEFAULT;
    }

    /**
     * @return the save-stable key this choice persists under; frozen once shipped, since renaming it
     *         silently resets every save that stored this count back to {@link #DEFAULT}
     */
    public String persistenceKey() {
        return persistenceKey;
    }

    /**
     * The text this choice's selector segment draws - the count itself, which needs no translation.
     * Rendered from {@link #columnCount()} rather than carried beside it, so the digit a segment
     * shows cannot drift from the number of columns picking it lays the list across.
     *
     * @return the drawn label for this choice's selector segment
     */
    public String resolveLabelText() {
        return String.valueOf(columnCount);
    }

    /** @return how many columns the picker list wraps its rows across under this choice */
    public int columnCount() {
        return columnCount;
    }
}
