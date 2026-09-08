package kmlib.profiling.recording;

import kmlib.profiling.ProfileSection;
import kmlib.text.KmlibStrings;
import kmlib.time.Timings;

import java.util.List;

/**
 * The one line a call writes as it closes: what ran, how long it took, what it
 * counted, and what the caller named it.
 *
 * <p>Composed here rather than at the sites that used to write such lines by
 * hand, so every one of them reads alike and the duration in the log is the very
 * span the row was accumulated from. What varies between sites is what they
 * counted and what they named the call, both of which the scope is already
 * carrying.
 *
 * <p>The counts are the call's inclusive amounts - what it counted itself plus
 * what anything it opened counted - which is the quantity its duration covered
 * and so the one the line has to state.
 */
final class ClosedCallLine {

    // Marks the line as one the profiler wrote, so a reader following a rebuild
    // through the log can tell a measured pass from the prose around it.
    private static final String LINE_PREFIX = "Profiled ";

    private static final String DURATION_LABEL = " took=";
    private static final String COUNT_SEPARATOR = "=";

    private ClosedCallLine() {
        // Composes only; never instantiated.
    }

    /**
     * @param section      what ran
     * @param elapsedNanos how long the call took
     * @param counts       what it counted, in the order it first counted them
     * @param tag          what the caller named this one call, blank where it
     *                     named nothing
     * @return the line to write
     */
    static String describeClosedCall(
            ProfileSection section,
            long elapsedNanos,
            List<ScopeCount> counts,
            String tag) {

        var line = new StringBuilder(LINE_PREFIX)
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
        // Quoted and last, a tag being free text whose end has to be tellable
        // from a reading that might follow it.
        if (KmlibStrings.hasText(tag)) {
            line.append(" \"").append(tag).append('"');
        }
        return line.toString();
    }
}
