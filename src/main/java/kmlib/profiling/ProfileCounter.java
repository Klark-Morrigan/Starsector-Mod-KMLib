package kmlib.profiling;

/**
 * A counted quantity's identity: the systems a call visited, the markets it
 * read, the cells it built, the walks it performed.
 *
 * <p>A duration on its own cannot be judged - "40ms" says nothing until it is
 * "40ms over 2100 entities" - so the number it is read against is accumulated
 * beside it rather than printed in a log line the profiler never sees.
 *
 * <p>Registered by name and held in a {@code static final}, so adding to one on
 * a per-frame path compares references rather than hashing a name.
 */
public final class ProfileCounter {

    private static final NameRegistry<ProfileCounter> COUNTERS_BY_NAME = new NameRegistry<>();

    private final String name;

    private ProfileCounter(String name) {
        this.name = name;
    }

    /**
     * Resolves the counter {@code name} identifies, creating it the first time
     * the name is seen.
     *
     * @param name what the counter is called in a report
     * @return the one counter carrying that name
     */
    public static ProfileCounter registerCounter(String name) {
        return COUNTERS_BY_NAME.resolveByName(name, ProfileCounter::new);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
