package kmlib.profiling;

import java.util.List;
import java.util.function.Function;

/**
 * Finds the entry a counter owns in one of the short per-counter lists this
 * package keeps.
 *
 * <p>One home for the rule those lists share: a counter is a registered value,
 * so a match is a reference comparison, and the list is short enough that
 * scanning it beats hashing a name on a path that runs inside the very loops
 * being counted.
 *
 * <p>It takes how to read the key rather than making the entries share a type,
 * because they are three different shapes - what one call has tallied, what a
 * row has accumulated over its calls, and the immutable value a reader is
 * handed - and the only thing they have in common is the counter each is filed
 * under.
 */
final class CounterLookup {

    private CounterLookup() {
    }

    /**
     * @param values      the entries to search, at most one per counter
     * @param readCounter what a given entry is filed under
     * @param counter     the counter being looked up
     * @param <T>         the entry type
     * @return the entry filed under {@code counter}, or {@code null} where
     *         there is none - which is "nothing has counted this here", a fact
     *         every caller acts on rather than an error
     */
    static <T> T findByCounter(
            List<T> values,
            Function<T, ProfileCounter> readCounter,
            ProfileCounter counter) {

        // Indexed rather than for-each, so no iterator is allocated on a path
        // that runs on every add and every close.
        for (var index = 0; index < values.size(); index++) {
            var value = values.get(index);
            if (readCounter.apply(value) == counter) {
                return value;
            }
        }
        return null;
    }
}
