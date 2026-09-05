package kmlib.profiling;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * The {@link Profiler} that keeps what it is handed, accumulating a tree of
 * sections in memory until it is read or cleared.
 *
 * <p>It holds the stack of scopes currently open, which is what makes a row a
 * path rather than a name: a section opened while another is open becomes that
 * one's child, and its time is part of the parent's total as well as its own.
 * A section opened with nothing open is a root.
 *
 * <p>The clock is injected (defaulting to {@link System#nanoTime()}) so the
 * accumulation reads whatever time source its caller names rather than the
 * system clock.
 *
 * <p>The logger is asked of log4j directly rather than of the game, which is
 * the same logger under the same name - the game's own helper is that call and
 * nothing more. A package that knows nothing about Starsector then stays that
 * way, and the name still sits under {@code kmlib}, so the library's own level
 * control governs it like everything else.
 */
public final class RecordingProfiler implements Profiler {

    private static final Logger LOG = Logger.getLogger(RecordingProfiler.class);

    private final List<ProfileNodeAccumulator> rootNodes = new ArrayList<>();
    private final List<RecordingProfileScope> openScopes = new ArrayList<>();
    private final Set<ProfileSection> sectionsReportedOutOfOrder = new HashSet<>();

    private final LongSupplier clockNanos;

    /**
     * Creates a profiler timing against the system nanosecond clock.
     */
    public RecordingProfiler() {
        this(System::nanoTime);
    }

    /**
     * Creates a profiler timing against {@code clockNanos}, for a caller that
     * supplies its own time source rather than reading the system clock.
     *
     * @param clockNanos source of the current time in nanoseconds
     */
    public RecordingProfiler(LongSupplier clockNanos) {
        this.clockNanos = clockNanos;
    }

    @Override
    public ProfileScope open(ProfileSection section) {
        var scope = new RecordingProfileScope(this, resolveNode(section), clockNanos.getAsLong());
        openScopes.add(scope);
        return scope;
    }

    @Override
    public void measure(String section, Runnable work) {
        var scope = open(ProfileSection.registerSection(section));
        try {
            work.run();
        } finally {
            // Closed in finally so a throwing block still contributes its
            // (partial) span rather than vanishing from the tree - and so the
            // stack unwinds with it, leaving the next call to land where it
            // belongs.
            scope.close();
        }
    }

    @Override
    public <T> T measure(String section, Supplier<T> work) {
        var scope = open(ProfileSection.registerSection(section));
        try {
            return work.get();
        } finally {
            scope.close();
        }
    }

    @Override
    public void record(String section, long elapsedNanos) {
        // A span the caller timed itself still belongs under whatever is open,
        // so it lands on the same node an open()/close() pair would have. It
        // counted nothing and named nothing: there was no scope to do either on.
        resolveNode(ProfileSection.registerSection(section))
            .addSpan(elapsedNanos, List.of(), WorstCall.NO_TAG);
    }

    @Override
    public List<ProfileNode> snapshot() {
        var roots = new ArrayList<ProfileNode>(rootNodes.size());
        for (var rootNode : rootNodes) {
            roots.add(rootNode.buildNode());
        }
        return roots;
    }

    @Override
    public void reset() {
        rootNodes.clear();
        // The open scopes go with the tree they were pointing into. A scope
        // closing after this finds itself off the stack and records nothing,
        // which is the right answer: its node no longer exists.
        openScopes.clear();
        sectionsReportedOutOfOrder.clear();
    }

    /**
     * Ends {@code scope}, and with it anything opened inside it that was never
     * closed.
     *
     * @param scope the scope being closed, ignored when it is not open
     */
    void closeScope(RecordingProfileScope scope) {
        var closingIndex = openScopes.lastIndexOf(scope);
        if (closingIndex < 0) {
            // Already closed, or opened before a reset: there is no span left to
            // attribute, and recording a second one would count the call twice.
            return;
        }

        var endNanos = clockNanos.getAsLong();
        // A scope closed out of order is a bug in the caller, not a state this
        // may sit in: leaving a younger scope open would make the next
        // unrelated section a child of a call that has already returned, and
        // every row under it a lie. So the younger ones end here too.
        for (var youngerIndex = openScopes.size() - 1; youngerIndex > closingIndex; youngerIndex--) {
            var abandoned = openScopes.remove(youngerIndex);
            endScope(abandoned, endNanos);
            reportOutOfOrderClose(abandoned.getSection());
        }
        openScopes.remove(closingIndex);
        endScope(scope, endNanos);
    }

    // Records the span and hands what the scope counted to whatever it was open
    // inside, which is what makes a row's counts inclusive the way its time is.
    // Taken off the stack first, so the scope now on top is the one the ended
    // scope ran inside - including down an unwind, where the scopes above the
    // one being closed end into it before it ends into its own parent.
    private void endScope(RecordingProfileScope scope, long endNanos) {

        scope.recordSpan(endNanos);

        if (!openScopes.isEmpty()) {
            openScopes.get(openScopes.size() - 1).receiveChildCounts(scope);
        }
    }

    // The node a section opened right now belongs to: a child of whatever is
    // open, or a root when nothing is.
    private ProfileNodeAccumulator resolveNode(ProfileSection section) {
        return openScopes.isEmpty()
            ? ProfileNodeAccumulator.resolveNodeIn(rootNodes, section)
            : openScopes.get(openScopes.size() - 1).resolveChildNode(section);
    }

    // Once per section, because the sites this happens on run every frame and
    // the second line says nothing the first did not - while a different
    // section left open is a different bug and still gets said.
    private void reportOutOfOrderClose(ProfileSection section) {
        if (!sectionsReportedOutOfOrder.add(section)) {
            return;
        }
        LOG.warn("Profiling scope '" + section.getName() + "' was still open when a scope"
            + " outside it closed, so it was closed too. Read its rows as calls that had not"
            + " finished.");
    }
}
