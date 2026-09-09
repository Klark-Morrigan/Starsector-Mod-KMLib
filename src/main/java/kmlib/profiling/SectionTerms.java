package kmlib.profiling;

import kmlib.profiling.budget.ProfileBudget;

/**
 * Everything a section may state about itself beyond its name: how much detail
 * a capture has to be keeping to time it, what one of its calls is allowed, and
 * how slow a call has to be before it says so in the log.
 *
 * <p>One value rather than one registration overload per term, because the
 * terms combine: a section under a budget may also be worth a line, and a
 * per-item section may be both. Stated by starting from {@link #DEFAULT} and
 * replacing the terms that differ, so a section names only what it changes and
 * the defaults live in one place.
 *
 * @param level            how much detail a capture has to be keeping to time
 *                         the section at all
 * @param budget           what one call of the section is allowed
 * @param callLogThreshold how slow one call has to be to write a line as it
 *                         closes
 */
public record SectionTerms(
    ProfileLevel level,
    ProfileBudget budget,
    CallLogThreshold callLogThreshold) {

    /**
     * What a section states when it states nothing: timed by any capture,
     * allowed anything, and read in the report alone.
     */
    public static final SectionTerms DEFAULT = new SectionTerms(
        ProfileLevel.COARSE, ProfileBudget.NO_BUDGET, CallLogThreshold.NO_LOGGING);

    /**
     * @param level how much detail a capture has to be keeping to time the
     *              section, in place of the current term
     * @return these terms with the level replaced
     */
    public SectionTerms withLevel(ProfileLevel level) {
        return new SectionTerms(level, budget, callLogThreshold);
    }

    /**
     * @param budget what one call of the section is allowed, in place of the
     *               current term
     * @return these terms with the budget replaced
     */
    public SectionTerms withBudget(ProfileBudget budget) {
        return new SectionTerms(level, budget, callLogThreshold);
    }

    /**
     * @param callLogThreshold how slow one call has to be to write a line, in
     *                         place of the current term
     * @return these terms with the threshold replaced
     */
    public SectionTerms withCallLogThreshold(CallLogThreshold callLogThreshold) {
        return new SectionTerms(level, budget, callLogThreshold);
    }
}
