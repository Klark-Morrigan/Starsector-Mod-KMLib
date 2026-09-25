package kmlib.persistence;

import java.util.Arrays;

/**
 * Maps a stored key back to the {@link PersistedChoice} it names. One place for the match loop every
 * such option set would otherwise repeat: scan the options for the one whose key matches, and fall
 * back when none does - nothing stored yet, or a key left behind by an option that no longer exists,
 * whether dropped by a later build or written by another mod.
 *
 * <p>Falling back rather than failing is the point: a stored choice is the player's preference, and
 * one that cannot be read is best read as never having been made.
 */
public final class PersistedChoices {

    private PersistedChoices() {
    }

    /**
     * Finds the option whose key equals {@code key}, among an enum's constants.
     *
     * @param options  the options to match against, e.g. from an enum's {@code values()}
     * @param key      the stored key, or null when nothing is stored
     * @param fallback the option returned when none matches
     * @param <T>      the option type
     * @return the matching option, or {@code fallback} when the key is null or matches none
     */
    public static <T extends PersistedChoice> T fromKey(T[] options, String key, T fallback) {
        return fromKey(Arrays.asList(options), key, fallback);
    }

    /**
     * Finds the option whose key equals {@code key}, among options a caller assembles rather than
     * declares - a vocabulary each consumer states for itself.
     *
     * <p>The first match wins, so options sharing a key resolve to whichever the caller listed
     * first.
     *
     * @param options  the options to match against, in the order ties are settled by
     * @param key      the stored key, or null when nothing is stored
     * @param fallback the option returned when none matches
     * @param <T>      the option type
     * @return the matching option, or {@code fallback} when the key is null or matches none
     */
    public static <T extends PersistedChoice> T fromKey(
            Iterable<? extends T> options,
            String key,
            T fallback) {

        if (key == null) {
            return fallback;
        }
        for (var option : options) {
            if (key.equals(option.persistenceKey())) {
                return option;
            }
        }
        return fallback;
    }
}
