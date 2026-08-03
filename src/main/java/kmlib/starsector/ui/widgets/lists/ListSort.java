package kmlib.starsector.ui.widgets.lists;

import java.util.Comparator;

/**
 * The sort a picker list is ranked by: the metric and the direction it runs in, paired because the
 * two are always chosen, stored, and read together. Bundling them keeps a picker's builder from
 * carrying the mode and the direction as two loose parameters, and gives the "what does the stored
 * pair mean" resolution one home rather than a copy at each reader. Which modes exist is the
 * consuming mod's declaration ({@link ListSortMode}); this record only pairs and resolves.
 *
 * <p>The stored keys arrive as parameters rather than being read here, because where a mod keeps
 * them - which sector-memory key, which settings field - is the mod's own concern and the one part
 * of a sort that cannot be shared. The consumer reads its own store and hands the two raw keys over;
 * everything about what they mean lives here.
 *
 * @param <T>       the list item type the sort's comparator ranks
 * @param mode      the metric the list is ranked by
 * @param direction the direction that metric runs in
 */
public record ListSort<T>(
    ListSortMode<T> mode,
    SortDirection direction) {

    /**
     * The stored sort resolved against the caller's own vocabulary: the stored mode key matched among
     * the vocabulary's modes (or its default when nothing is stored or the key names a mode the
     * caller no longer offers), then the stored direction resolved against that mode's own default,
     * so a save with no stored direction reads the mode's natural order.
     *
     * @param <T>                 the list item type the modes rank
     * @param storedModeKey       the mode key the caller's store holds, or null when none is stored
     * @param storedDirectionKey  the direction key the caller's store holds, or null when none is
     *                            stored
     * @param sortModes           the caller's sort vocabulary - the set a stored key resolves
     *                            against, and the mode it falls back to
     * @return the stored sort
     */
    public static <T> ListSort<T> resolveStored(
            String storedModeKey,
            String storedDirectionKey,
            ListSortModes<T> sortModes) {

        var mode = resolveModeOrDefault(storedModeKey, sortModes);
        var direction = SortDirection.fromKeyOrDefault(storedDirectionKey, mode.defaultDirection());

        return new ListSort<>(mode, direction);
    }

    /**
     * The comparator that ranks the list under this sort - the mode's comparator run in this
     * direction.
     *
     * @return the item comparator for this sort
     */
    public Comparator<T> comparator() {
        return mode.comparator(direction);
    }

    // The stored mode key matched back to one of the caller's modes, falling back to the caller's
    // default when nothing is stored (a fresh save) or the key names a mode the caller no longer
    // offers (a key left by an older or a modded build), so a picker always resolves to a live
    // mode rather than failing on an unknown key.
    private static <T> ListSortMode<T> resolveModeOrDefault(
            String storedModeKey,
            ListSortModes<T> sortModes) {

        for (var candidate : sortModes.modes()) {
            if (candidate.persistenceKey().equals(storedModeKey)) {
                return candidate;
            }
        }
        return sortModes.defaultMode();
    }
}
