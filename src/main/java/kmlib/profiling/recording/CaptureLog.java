package kmlib.profiling.recording;

import kmlib.profiling.ProfileSection;
import kmlib.profiling.snapshot.BudgetBreach;
import kmlib.text.KmlibStrings;
import kmlib.time.Timings;

import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What a capture says in the game log as it happens, beside what it keeps for
 * the report: a call that broke what its section allows, a scope closed out of
 * order, and a call of a section that states a threshold it ran over.
 *
 * <p>Apart from the accumulation because these are three sentences and the
 * bookkeeping of which have been said, and none of that is a fact about a row.
 * The findings are said once per section per capture: the pass that broke a
 * bound breaks it on every frame it runs, and the tenth line says nothing the
 * first did not - while a different section is a different bug and still gets
 * said. A capture that is cleared forgets what it said, since the next one has
 * to stand on its own.
 *
 * <p>The logger is asked of log4j directly rather than of the game, which is
 * the same logger under the same name - the game's own helper is that call and
 * nothing more. A package that knows nothing about Starsector then stays that
 * way, and the name still sits under {@code kmlib}, so the library's own level
 * control governs it like everything else. Every line is written under this
 * class's name rather than the caller's, so a reader raising the level to
 * follow one pass raises it on profiling rather than on whichever package
 * happened to open the scope.
 */
final class CaptureLog {

    private static final Logger LOG = Logger.getLogger(CaptureLog.class);

    // Marks a closed call's line as one the profiler wrote, so a reader following
    // a rebuild through the log can tell a measured pass from the prose around it.
    private static final String CLOSED_CALL_PREFIX = "Profiled ";

    private static final String DURATION_LABEL = " took=";
    private static final String COUNT_SEPARATOR = "=";

    private final Set<ProfileSection> sectionsReportedOverBudget = new HashSet<>();
    private final Set<ProfileSection> sectionsReportedOutOfOrder = new HashSet<>();

    /**
     * Says that {@code section} went over budget, the first time it does in
     * this capture.
     *
     * @param section the section whose call broke its bound
     * @param breach  what it broke, {@link BudgetBreach#NO_BREACH} for nothing
     * @param tag     what the caller named the breaching call, blank where it
     *                named nothing
     */
    void reportBreachOnce(ProfileSection section, BudgetBreach breach, String tag) {

        if (!breach.hasBreached() || !sectionsReportedOverBudget.add(section)) {
            return;
        }
        LOG.warn("Profiling section '" + section.getName() + "' went over budget: "
            + breach.describeBreach() + describeCall(", on call ", tag)
            + ". Said once; the row carries the latest breach of it.");
    }

    /**
     * Says that {@code section} was still open when a scope outside it closed,
     * the first time it is in this capture.
     *
     * @param section the section of the scope that was closed for its caller
     */
    void reportOutOfOrderCloseOnce(ProfileSection section) {

        if (!sectionsReportedOutOfOrder.add(section)) {
            return;
        }
        LOG.warn("Profiling scope '" + section.getName() + "' was still open when a scope"
            + " outside it closed, so it was closed too. Read its rows as calls that had not"
            + " finished.");
    }

    /**
     * Writes the line a call of {@code section} owes as it closes, where the
     * section states a threshold this call ran over: what ran, how long it
     * took, what it counted, and what the caller named it.
     *
     * <p>The counts are the call's inclusive amounts - what it counted itself
     * plus what anything it opened counted - which is the quantity its duration
     * covered and so the one the line has to state.
     *
     * @param section      what ran
     * @param elapsedNanos how long the call took
     * @param counts       what it counted, in the order it first counted them
     * @param tag          what the caller named this one call, blank where it
     *                     named nothing
     */
    void reportClosedCall(
            ProfileSection section,
            long elapsedNanos,
            List<ScopeCount> counts,
            String tag) {

        // The threshold first: nearly every section states none, and that answer
        // is a comparison where asking log4j is a lookup.
        if (!section.getCallLogThreshold().shouldLogCall(elapsedNanos) || !LOG.isDebugEnabled()) {
            return;
        }
        LOG.debug(describeClosedCall(section, elapsedNanos, counts, tag));
    }

    /**
     * Forgets what has been said, for a capture that is being cleared: what was
     * reported against the old capture is no guide to the next.
     */
    void forgetWhatWasSaid() {
        sectionsReportedOverBudget.clear();
        sectionsReportedOutOfOrder.clear();
    }

    // The closed call's line, composed here rather than at the sites that used to
    // write such lines by hand so every one of them reads alike.
    private static String describeClosedCall(
            ProfileSection section,
            long elapsedNanos,
            List<ScopeCount> counts,
            String tag) {

        var line = new StringBuilder(CLOSED_CALL_PREFIX)
            .append('\'')
            .append(section.getName())
            .append('\'')
            .append(DURATION_LABEL)
            .append(Timings.formatMillis(elapsedNanos));

        for (var count : counts) {
            line.append(' ')
                .append(count.getCounter().getName())
                .append(COUNT_SEPARATOR)
                .append(count.getTotalAmount());
        }
        return line.append(describeCall(" ", tag)).toString();
    }

    // What the caller named the call, where it named anything, after whatever
    // introduces it. Quoted and last, a tag being free text whose end has to be
    // tellable from whatever might follow it.
    private static String describeCall(String lead, String tag) {
        return KmlibStrings.hasText(tag) ? lead + '"' + tag + '"' : "";
    }
}
