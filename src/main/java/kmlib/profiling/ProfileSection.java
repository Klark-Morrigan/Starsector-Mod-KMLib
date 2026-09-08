package kmlib.profiling;

import kmlib.profiling.budget.ProfileBudget;

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
 *
 * <p>A section may also state what one of its calls is allowed, which is what
 * turns its rows into findings rather than numbers, how much detail it is worth
 * timing at, and how slow a call has to be before it says so in the log. All
 * three are the ones the name was first registered with - a section states them
 * once, beside the constant holding it.
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
    private final ProfileLevel level;
    private final ProfileBudget budget;
    private final CallLogThreshold callLogThreshold;

    private ProfileSection(
            String name,
            ProfileLevel level,
            ProfileBudget budget,
            CallLogThreshold callLogThreshold) {

        this.name = name;
        this.level = level;
        this.budget = budget;
        this.callLogThreshold = callLogThreshold;
    }

    /**
     * Resolves the section {@code name} identifies, creating it the first time
     * the name is seen.
     *
     * <p>Timed by any capture that is running at all: a section stating no level
     * is one opened a handful of times per frame, which is what a reader looking
     * for where a frame went reads first.
     *
     * @param name what the section is called in a report
     * @return the one section carrying that name
     */
    public static ProfileSection registerSection(String name) {
        return resolveSection(
            name, ProfileLevel.COARSE, ProfileBudget.NO_BUDGET, CallLogThreshold.NO_LOGGING);
    }

    /**
     * Resolves the section {@code name} identifies, declaring it at
     * {@code level} the first time the name is seen.
     *
     * <p>What a section on a per-item path declares, so that a capture taken to
     * read whole frames skips it for a comparison rather than timing it once per
     * item.
     *
     * @param name  what the section is called in a report
     * @param level how much detail a capture has to be keeping to time it
     * @return the one section carrying that name
     */
    public static ProfileSection registerSection(String name, ProfileLevel level) {
        return resolveSection(name, level, ProfileBudget.NO_BUDGET, CallLogThreshold.NO_LOGGING);
    }

    /**
     * Resolves the section {@code name} identifies, declaring it with
     * {@code budget} the first time the name is seen.
     *
     * @param name   what the section is called in a report
     * @param budget what one call of it is allowed
     * @return the one section carrying that name
     */
    public static ProfileSection registerSection(String name, ProfileBudget budget) {
        return resolveSection(name, ProfileLevel.COARSE, budget, CallLogThreshold.NO_LOGGING);
    }

    /**
     * Resolves the section {@code name} identifies, declaring it with
     * {@code callLogThreshold} the first time the name is seen.
     *
     * <p>What a step of a rebuild states, so the line it used to write by hand -
     * a clock read either side of the block and the counts printed beside the
     * duration - is written from the scope it opens anyway.
     *
     * @param name             what the section is called in a report
     * @param callLogThreshold how slow one call has to be to say so in the log
     * @return the one section carrying that name
     */
    public static ProfileSection registerSection(
            String name,
            CallLogThreshold callLogThreshold) {

        return resolveSection(
            name, ProfileLevel.COARSE, ProfileBudget.NO_BUDGET, callLogThreshold);
    }

    public String getName() {
        return name;
    }

    /**
     * @return how much detail a capture has to be keeping for this section's
     *         spans to be timed at all; {@link ProfileLevel#COARSE} where it
     *         stated nothing
     */
    public ProfileLevel getLevel() {
        return level;
    }

    /**
     * @return what one call of this section is allowed, or
     *         {@link ProfileBudget#NO_BUDGET} where it stated nothing - which is
     *         most sections, and which a caller tells by reference rather than
     *         by asking the budget
     */
    public ProfileBudget getBudget() {
        return budget;
    }

    /**
     * @return how slow one call of this section has to be before it writes a
     *         line of its own, or {@link CallLogThreshold#NO_LOGGING} where it
     *         stated nothing - which is most sections, whose calls are read in
     *         the report alone
     */
    public CallLogThreshold getCallLogThreshold() {
        return callLogThreshold;
    }

    @Override
    public String toString() {
        return name;
    }

    // The one place a name becomes a section, so that what a caller left unsaid
    // is defaulted once rather than at each way in - and so that first
    // registration winning is one rule rather than three alike.
    private static ProfileSection resolveSection(
            String name,
            ProfileLevel level,
            ProfileBudget budget,
            CallLogThreshold callLogThreshold) {

        return SECTIONS_BY_NAME.resolveByName(
            name,
            resolvedName -> new ProfileSection(resolvedName, level, budget, callLogThreshold));
    }
}
