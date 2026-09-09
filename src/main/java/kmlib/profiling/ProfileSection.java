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
 * <p>A section may also state its {@link SectionTerms} - how much detail it is
 * worth timing at, what one of its calls is allowed, and how slow a call has to
 * be before it says so in the log. They are the ones the name was first
 * registered with: a section states them once, beside the constant holding it.
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
    private final SectionTerms terms;

    private ProfileSection(String name, SectionTerms terms) {
        this.name = name;
        this.terms = terms;
    }

    /**
     * Resolves the section {@code name} identifies, creating it on
     * {@link SectionTerms#DEFAULT} the first time the name is seen.
     *
     * <p>What nearly every section is: opened a handful of times per frame,
     * timed by any capture that is running, allowed anything, read in the
     * report alone.
     *
     * @param name what the section is called in a report
     * @return the one section carrying that name
     */
    public static ProfileSection registerSection(String name) {
        return registerSection(name, SectionTerms.DEFAULT);
    }

    /**
     * Resolves the section {@code name} identifies, declaring it on
     * {@code terms} the first time the name is seen.
     *
     * @param name  what the section is called in a report
     * @param terms what the section states about itself beyond its name
     * @return the one section carrying that name
     */
    public static ProfileSection registerSection(String name, SectionTerms terms) {
        return SECTIONS_BY_NAME.resolveByName(
            name, resolvedName -> new ProfileSection(resolvedName, terms));
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
        return terms.level();
    }

    /**
     * @return what one call of this section is allowed, or
     *         {@link ProfileBudget#NO_BUDGET} where it stated nothing - which is
     *         most sections, and which a caller tells by reference rather than
     *         by asking the budget
     */
    public ProfileBudget getBudget() {
        return terms.budget();
    }

    /**
     * @return how slow one call of this section has to be before it writes a
     *         line of its own, or {@link CallLogThreshold#NO_LOGGING} where it
     *         stated nothing - which is most sections, whose calls are read in
     *         the report alone
     */
    public CallLogThreshold getCallLogThreshold() {
        return terms.callLogThreshold();
    }

    @Override
    public String toString() {
        return name;
    }
}
