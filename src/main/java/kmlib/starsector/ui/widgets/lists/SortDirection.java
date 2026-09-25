package kmlib.starsector.ui.widgets.lists;

import kmlib.persistence.PersistedChoice;

/**
 * The direction a picker list's active sort runs in - ascending or descending on the chosen metric.
 * It rides alongside a sort mode: a mode fixes which key ranks the rows, this fixes which way that key
 * runs. Split out from the mode because the two persist and change independently - picking a new mode
 * resets the direction to that mode's default, while re-picking the lit mode flips only the direction.
 *
 * <p>Each direction owns the save-stable key it persists under; a sort selector maps it to the up/down
 * triangle it draws in a row's trailing slot, since the body font renders no up/down glyph. Where that
 * key is stored is the consumer's, so nothing here reaches a save.
 * {@link #opposite()} is the flip a re-pick applies.
 */
public enum SortDirection implements PersistedChoice {
    ASCENDING("asc"),
    DESCENDING("desc");

    private final String persistenceKey;

    SortDirection(String persistenceKey) {
        this.persistenceKey = persistenceKey;
    }

    /**
     * @return the save-stable key this direction persists under; frozen once shipped, since renaming
     *         it silently resets every save that stored this direction to its mode's default
     */
    @Override
    public String persistenceKey() {
        return persistenceKey;
    }

    /** @return the other direction - the flip a re-pick of the lit sort mode applies */
    public SortDirection opposite() {
        return this == ASCENDING ? DESCENDING : ASCENDING;
    }
}
