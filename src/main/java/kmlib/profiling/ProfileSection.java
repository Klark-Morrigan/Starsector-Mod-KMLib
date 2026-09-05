package kmlib.profiling;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A profiled section's identity: the name its rows are reported under,
 * resolved once and held in a {@code static final} rather than spelled at
 * every call.
 *
 * <p>Identity is what a scope is opened with, so the profiler finds the row a
 * call belongs to by comparing references against the few children its open
 * scope already has, instead of hashing a name on a path that runs every
 * frame.
 *
 * <p>One instance per name, always: a block opened by name through
 * {@link Profiler#measure} and one opened through {@link Profiler#open} then
 * land on one row rather than on two rows spelled alike.
 */
public final class ProfileSection {

    // Registration runs from static initialisers, and which thread runs one is
    // whichever first touched the class holding the constant - the single place
    // in profiling that cannot assume the game thread, hence a concurrent table.
    private static final Map<String, ProfileSection> SECTIONS_BY_NAME = new ConcurrentHashMap<>();

    private final String name;

    private ProfileSection(String name) {
        this.name = name;
    }

    /**
     * Resolves the section {@code name} identifies, creating it the first time
     * the name is seen.
     *
     * @param name what the section is called in a report
     * @return the one section carrying that name
     */
    public static ProfileSection registerSection(String name) {
        return SECTIONS_BY_NAME.computeIfAbsent(
            Objects.requireNonNull(name, "name"),
            ProfileSection::new);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
