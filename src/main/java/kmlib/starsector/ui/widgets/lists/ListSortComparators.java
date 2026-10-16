package kmlib.starsector.ui.widgets.lists;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * The comparators a numeric sort vocabulary lays a picker list out by, once it has named its
 * metrics: the direction a metric naturally runs in, and the order that ranks items under it. A
 * vocabulary declares which numbers it ranks by and the order ties break down them; how those
 * declarations become a comparator does not differ between vocabularies, so it is stated once here.
 *
 * <p>The shape assembled is fixed: the mode's own key in the requested direction, then the metric
 * chain, then the name, then the item ID. Sharing the tail is the point - two items a player cannot
 * tell apart break the same way under every mode, and no mode can be written that forgets the by-ID
 * key which keeps a fully-level pair from reshuffling between frames. The tail is reachable on its
 * own ({@link #appendNameAndIdTail}) for a mode whose primary key is not a metric, so that promise
 * holds for those too.
 *
 * <p>A mode that ranks by name rather than by a number names no metric (null), which is the one
 * distinction the shape turns on: it leads with the label and lets the whole metric chain follow.
 */
public final class ListSortComparators {

    // Assembles comparators only; never instantiated.
    private ListSortComparators() {
    }

    /**
     * The comparator that orders a list under a mode in {@code direction}. Only the primary key
     * follows the direction; the chain behind it stays canonical, so two items level on the primary
     * break the same way whichever direction shows.
     *
     * @param <T>           the list item type ranked
     * @param primaryMetric the number this mode promotes to the primary key, or null for a mode that
     *                      ranks by name
     * @param metricChain   the vocabulary's numbers in the order ties break down them, each read
     *                      high-to-low
     * @param direction     the way the primary key runs - the mode's default, or the flipped
     *                      opposite
     * @return the item comparator for that mode in the requested direction
     */
    public static <T extends SelectableListItem> Comparator<T> assembleComparator(
            ToIntFunction<T> primaryMetric,
            List<ToIntFunction<T>> metricChain,
            SortDirection direction) {

        Comparator<T> order = resolvePrimaryOrder(primaryMetric, direction);

        // The chain is appended entire, the promoted metric included: a pair that reached the chain is
        // level on the primary key, so reading that same number again lands on the same answer and the
        // next key decides. Leaving its slot in is what keeps the chain one list rather than a list
        // plus a rule about which of its entries is being skipped.
        for (var metric : metricChain) {
            order = order.thenComparing(compareByMetricDescending(metric));
        }

        // The tail follows for a by-name mode too, on the same reasoning the chain does: a pair that
        // reached it is level on the primary key, so a name mode's leading key answers identically
        // when the tail reads it again and the by-ID key decides.
        return appendNameAndIdTail(order);
    }

    /**
     * The tail every ranking here ends in, appended to an order the caller assembled itself: the
     * item's name, A-to-Z and case-insensitively, then its ID.
     *
     * <p>The entry point exists for a mode whose primary key is not a metric, which
     * {@link #assembleComparator} has no way to accept. Such a mode still has to break a level pair
     * the way every other mode does, and rolling the tail by hand at each such mode is the
     * duplication this class exists to close.
     *
     * <p>The by-ID key behind the name is what gives a total order, so two items level on every
     * visible key keep a fixed position rather than reshuffling as a per-frame sort re-runs. A null
     * name sorts as empty, so an unlabelled item sorts with the blanks rather than throwing.
     *
     * @param <T>   the list item type ranked
     * @param order the caller's own keys, in the direction the player asked for - the tail behind
     *              them stays canonical, so a level pair breaks the same way whichever direction
     *              shows
     * @return that order with the name-and-ID tail behind it
     */
    public static <T extends SelectableListItem> Comparator<T> appendNameAndIdTail(Comparator<T> order) {

        return order
            .thenComparing(compareByNameAscending())
            .thenComparing(SelectableListItem::itemId);
    }

    /**
     * The direction a mode ranks in until the player flips it, which follows from what it ranks by:
     * a number runs high-to-low so the bigger item leads, and a name runs A-to-Z.
     *
     * @param <T>    the list item type ranked
     * @param metric the number the mode ranks by, or null for a mode that ranks by name
     * @return that mode's natural sort direction
     */
    public static <T extends SelectableListItem> SortDirection resolveDefaultDirection(
            ToIntFunction<T> metric) {

        return metric == null
            ? SortDirection.ASCENDING
            : SortDirection.DESCENDING;
    }

    // One metric as a high-to-low comparator, so the bigger item ranks first - the direction every
    // number in the chain is read in, whichever way the primary key was asked to run.
    private static <T extends SelectableListItem> Comparator<T> compareByMetricDescending(
            ToIntFunction<T> metric) {

        return Comparator.comparingInt(metric).reversed();
    }

    private static <T extends SelectableListItem> Comparator<T> compareByNameAscending() {

        return Comparator.comparing(
            ListSortComparators::resolveDisplayNameOrEmpty,
            String.CASE_INSENSITIVE_ORDER);
    }

    private static String resolveDisplayNameOrEmpty(SelectableListItem item) {

        return item.displayName() == null
            ? ""
            : item.displayName();
    }

    // The mode's own key in the requested direction: its natural order - a number high-to-low, a
    // name A-to-Z - reversed when the requested direction is the opposite of that.
    private static <T extends SelectableListItem> Comparator<T> resolvePrimaryOrder(
            ToIntFunction<T> primaryMetric,
            SortDirection direction) {

        Comparator<T> defaultOrder = primaryMetric == null
            ? compareByNameAscending()
            : compareByMetricDescending(primaryMetric);

        return direction == resolveDefaultDirection(primaryMetric)
            ? defaultOrder
            : defaultOrder.reversed();
    }
}
