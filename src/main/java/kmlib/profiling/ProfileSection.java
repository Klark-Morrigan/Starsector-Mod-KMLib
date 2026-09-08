package kmlib.profiling;

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

    // Declared before the reserved section below, since that constant is
    // registered through it and static fields initialise in the order they are
    // written.
    private static final NameRegistry<ProfileSection> SECTIONS_BY_NAME = new NameRegistry<>();

    /**
     * Where a count added with no scope open lands, under
     * {@link ProfileOrigin#UNSCOPED}.
     *
     * <p>Each such count is one call of this row, timed at nothing: no scope
     * bracketed it, so there is no duration to report and only the amount is
     * worth having. Kept rather than dropped, since a traversal from a path
     * nobody profiled is the first thing a reader hunting stray work looks for.
     */
    public static final ProfileSection UNSCOPED_COUNTS = registerSection("counts");

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
        return SECTIONS_BY_NAME.resolveByName(name, ProfileSection::new);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
