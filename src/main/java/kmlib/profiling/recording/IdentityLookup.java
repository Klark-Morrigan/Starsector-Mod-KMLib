package kmlib.profiling.recording;

import java.util.List;
import java.util.function.Function;

/**
 * Finds the entry a registered value owns in one of the short lists this package
 * keeps, and opens one where there is none yet.
 *
 * <p>One home for the rule those lists share. Everything they are filed under -
 * an origin, a section, a counter - is a registered value, so a match is a
 * reference comparison, and the list is short enough that scanning it beats
 * hashing a name on a path that runs inside the very loops being counted.
 *
 * <p>It takes how to read the key rather than making the entries share a type,
 * because they are four different shapes - a group of roots, a row, what one
 * open call has tallied, what a row has accumulated over its calls - and the
 * only thing they have in common is that each is filed under one identity.
 */
final class IdentityLookup {

    private IdentityLookup() {
    }

    /**
     * @param values   the entries to search, at most one per key
     * @param readKey  what a given entry is filed under
     * @param key      the identity being looked up
     * @param <T>      the entry type
     * @param <K>      the registered value entries are filed under
     * @return the entry filed under {@code key}, or {@code null} where there is
     *         none - which is "nothing has been filed here", a fact every caller
     *         acts on rather than an error
     */
    static <T, K> T findByKey(List<T> values, Function<T, K> readKey, K key) {

        // Indexed rather than for-each, so no iterator is allocated on a path
        // that runs on every open, every add and every close.
        for (var index = 0; index < values.size(); index++) {
            var value = values.get(index);
            if (readKey.apply(value) == key) {
                return value;
            }
        }
        return null;
    }

    /**
     * Finds the entry {@code key} owns, appending one the first time that key is
     * seen in {@code values}.
     *
     * <p>Appended rather than made up front, so a list holds only what was
     * actually reached and stays in the order it was first reached in - which is
     * the order a report reads its rows in.
     *
     * @param values      the entries to search and extend, in first-seen order
     * @param readKey     what a given entry is filed under
     * @param key         the identity being resolved
     * @param createEntry makes the entry a key stands for, called only where the
     *                    list has none yet
     * @param <T>         the entry type
     * @param <K>         the registered value entries are filed under
     * @return the entry the list keeps for that key
     */
    static <T, K> T resolveByKey(
            List<T> values,
            Function<T, K> readKey,
            K key,
            Function<K, T> createEntry) {

        var found = findByKey(values, readKey, key);

        if (found != null) {
            return found;
        }
        var opened = createEntry.apply(key);

        values.add(opened);
        return opened;
    }
}
