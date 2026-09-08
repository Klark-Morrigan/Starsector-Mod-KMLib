package kmlib.profiling;

/**
 * Which game a capture's roots came from: the label a reader matches back to
 * the save the numbers were taken in.
 *
 * <p>Diagnostics isolate nothing - one profiler, one tree - but they do have to
 * attribute. Two games' roots are two groups, because a row averaging a walk of
 * one sector with a walk of another describes neither, and a reader who cannot
 * say which game a maximum came from cannot go back and reproduce it.
 *
 * <p>What a label is made of is the caller's to say: profiling knows nothing of
 * sectors or saves, and takes whatever describes one well enough to be
 * recognised.
 *
 * <p>One instance per label, like a section, so a root opened every frame finds
 * its group by comparing references rather than by hashing a string.
 */
public final class ProfileOrigin {

    // Declared before the reserved origin below, since that constant is
    // registered through it and static fields initialise in the order they are
    // written.
    private static final NameRegistry<ProfileOrigin> ORIGINS_BY_LABEL = new NameRegistry<>();

    /**
     * Where a section opened with no root open lands.
     *
     * <p>Kept rather than dropped: a traversal from a path nobody profiled is
     * the first thing a reader wants to see, and a number that quietly went
     * missing is the one thing a capture must not have.
     */
    public static final ProfileOrigin UNSCOPED = registerOrigin("unscoped");

    private final String label;

    private ProfileOrigin(String label) {
        this.label = label;
    }

    /**
     * Resolves the origin {@code label} identifies, creating it the first time
     * the label is seen.
     *
     * @param label what the game these roots came from is called in a report
     * @return the one origin carrying that label
     */
    public static ProfileOrigin registerOrigin(String label) {
        return ORIGINS_BY_LABEL.resolveByName(label, ProfileOrigin::new);
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
