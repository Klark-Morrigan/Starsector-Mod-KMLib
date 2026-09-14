package kmlib.collections;

import java.util.function.Function;

/**
 * Generic, Starsector-agnostic collection helpers shared across the KMLib jar.
 * Lives in its own package (not {@code kmlib.starsector.*}) because the helpers
 * here are pure collection utilities - they have no dependency on Starsector's
 * API surface and are reused by any KMLib code, Starsector-related or not.
 *
 * <p>Sibling concept to {@link kmlib.text.KmlibStrings}: both keep KMLib from
 * pulling in a util module (Apache Commons, Guava) just for a handful of small,
 * widely shared primitives.
 */
public final class KmlibCollections {

    private KmlibCollections() {
    }

    /**
     * Joins {@code items} into a single string, rendering each with
     * {@code render} and separating them with {@code delimiter}.
     *
     * <p>Unlike {@link String#join(CharSequence, Iterable)}, the elements need
     * not already be strings: the renderer maps each one, so a collection of
     * domain objects can be joined by a chosen field in a single pass without an
     * intermediate string list. Lives here so the several "list these for the
     * player" messages across KMLib share one implementation rather than each
     * re-hand-rolling the iterate-and-separate loop.
     *
     * @param items     the items to join; may be empty, never null
     * @param delimiter the separator placed between rendered items
     * @param render    maps each item to its string form
     * @param <T>       the item type
     * @return the rendered items joined by the delimiter, or "" when empty
     */
    public static <T> String join(Iterable<T> items, String delimiter,
            Function<? super T, String> render) {
        var joined = new StringBuilder();
        // A first-item flag rather than length() > 0: the latter would skip the
        // delimiter after an item that renders to the empty string.
        var isFirst = true;
        for (var item : items) {
            if (!isFirst) {
                joined.append(delimiter);
            }
            joined.append(render.apply(item));
            isFirst = false;
        }
        return joined.toString();
    }
}
