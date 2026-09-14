package kmlib.profiling.recording;

import kmlib.profiling.IterationScope;
import kmlib.profiling.PhasedSection;
import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileLevel;
import kmlib.profiling.ProfileOrigin;
import kmlib.profiling.ProfileScope;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.Profiler;
import kmlib.profiling.SilentProfiler;
import kmlib.profiling.snapshot.CallWarmth;
import kmlib.profiling.snapshot.ProfileOriginTree;
import kmlib.profiling.snapshot.WorstCall;

import java.util.ArrayList;
import java.util.List;
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
 * <p>Roots are grouped by the origin they were opened under, so one profiler
 * spanning two games keeps two trees rather than one that averages them. A root
 * nobody named an origin for is the reserved
 * {@link ProfileOrigin#UNSCOPED} one's.
 *
 * <p>The clock is injected (defaulting to {@link System#nanoTime()}) so the
 * accumulation reads whatever time source its caller names rather than the
 * system clock.
 *
 * <p>How much it keeps is a level (defaulting to {@link ProfileLevel#FINE}, a
 * profiler asked for with no level keeping everything it is handed). A section
 * registered finer than that is opened on the {@link SilentProfiler} instead, so
 * the section costs a comparison and no clock read - which is what lets a
 * per-item section exist at all without a reader of whole frames paying for it.
 *
 * <p>Some of what it accumulates is also said as it happens, through the
 * {@link CaptureLog}: a call that broke what its section allows, a scope closed
 * out of order, and a call of a section that states a threshold it ran over.
 */
public final class RecordingProfiler implements Profiler {

    // A count that arrived with no scope open has a size but no span, and a
    // duration nobody measured must not appear in a column read as measured.
    private static final long UNTIMED_CALL_NANOS = 0L;

    private final List<ProfileOriginAccumulator> originGroups = new ArrayList<>();
    private final List<RecordingProfileScope> openScopes = new ArrayList<>();
    private final CaptureLog captureLog = new CaptureLog();

    private final ProfileLevel recordedLevel;
    private final LongSupplier clockNanos;

    // The JVM's compilation clock, read either side of a call whose section
    // states a log threshold so its line can say whether the JVM was compiling
    // under it. Injected like the time source, for the same reason: a test
    // scripts it, and the accumulation reads whatever its caller names.
    private final LongSupplier compilationMillis;

    /**
     * Creates a profiler keeping every level of detail, timing against the
     * system nanosecond clock.
     */
    public RecordingProfiler() {
        this(ProfileLevel.FINE, System::nanoTime);
    }

    /**
     * Creates a profiler keeping every level of detail, timing against
     * {@code clockNanos}, for a caller that supplies its own time source rather
     * than reading the system clock.
     *
     * @param clockNanos source of the current time in nanoseconds
     */
    public RecordingProfiler(LongSupplier clockNanos) {
        this(ProfileLevel.FINE, clockNanos);
    }

    /**
     * Creates a profiler keeping detail down to {@code recordedLevel}, timing
     * against the system nanosecond clock.
     *
     * @param recordedLevel the finest detail this capture keeps
     */
    public RecordingProfiler(ProfileLevel recordedLevel) {
        this(recordedLevel, System::nanoTime);
    }

    /**
     * Creates a profiler keeping detail down to {@code recordedLevel}, timing
     * against {@code clockNanos} and reading the JVM's own compilation clock.
     *
     * @param recordedLevel the finest detail this capture keeps
     * @param clockNanos    source of the current time in nanoseconds
     */
    public RecordingProfiler(ProfileLevel recordedLevel, LongSupplier clockNanos) {
        this(recordedLevel, clockNanos, JitCompilationClock::readTotalCompilationMillis);
    }

    /**
     * Creates a profiler keeping detail down to {@code recordedLevel}, timing
     * against {@code clockNanos} and reading what the JVM has compiled off
     * {@code compilationMillis}.
     *
     * @param recordedLevel     the finest detail this capture keeps
     * @param clockNanos        source of the current time in nanoseconds
     * @param compilationMillis source of how long the JVM has spent compiling so
     *                          far, in milliseconds
     */
    public RecordingProfiler(
            ProfileLevel recordedLevel,
            LongSupplier clockNanos,
            LongSupplier compilationMillis) {

        this.recordedLevel = recordedLevel;
        this.clockNanos = clockNanos;
        this.compilationMillis = compilationMillis;
    }

    @Override
    public ProfileScope open(ProfileSection section) {

        if (!canRecordSection(section)) {
            return SilentProfiler.INSTANCE.open(section);
        }
        // No loop under this scope, so there is no per-turn state to carry: what
        // a plain section costs to open is the scope itself.
        return openNestedScope(section, null);
    }

    @Override
    public ProfileScope openRoot(ProfileOrigin origin, ProfileSection section) {

        if (!canRecordSection(section)) {
            return SilentProfiler.INSTANCE.openRoot(origin, section);
        }
        // No parent scope, whatever is open: the node hangs off the origin's
        // group, and nothing above it answers for what runs inside it.
        return pushScope(resolveRootNode(origin, section), null, null);
    }

    @Override
    public IterationScope openIterations(PhasedSection section) {

        var loopSection = section.getSection();

        if (!canRecordSection(loopSection)) {
            return SilentProfiler.INSTANCE.openIterations(section);
        }
        // The turns are the finest thing a loop offers - a clock read per step
        // per item - so a capture reading whole frames still gets the loop's own
        // span and pays nothing per turn.
        return openNestedScope(
            loopSection,
            canRecordIterations() ? new ScopeIterations(section) : null);
    }

    @Override
    public void addCountToOpenScope(ProfileCounter counter, long amount) {

        var openScope = resolveInnermostOpenScope();

        if (openScope == null) {
            recordUnscopedCount(counter, amount);
            return;
        }
        openScope.addCount(counter, amount);
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
        var recordedSection = ProfileSection.registerSection(section);

        if (!canRecordSection(recordedSection)) {
            return;
        }
        var breach = resolveNode(recordedSection)
            .addSpan(elapsedNanos, List.of(), WorstCall.NO_TAG, CallWarmth.UNMEASURED);

        captureLog.reportBreachOnce(recordedSection, breach, WorstCall.NO_TAG);
    }

    @Override
    public ProfileLevel getRecordedLevel() {
        return recordedLevel;
    }

    @Override
    public List<ProfileOriginTree> snapshot() {
        var trees = new ArrayList<ProfileOriginTree>(originGroups.size());
        for (var originGroup : originGroups) {
            trees.add(originGroup.buildTree());
        }
        return trees;
    }

    @Override
    public void reset() {
        originGroups.clear();
        // The open scopes go with the tree they were pointing into. A scope
        // closing after this finds itself off the stack and records nothing,
        // which is the right answer: its node no longer exists.
        openScopes.clear();
        // Said again after a clear, since the capture a finding was reported
        // against is gone and the next one has to stand on its own.
        captureLog.forgetWhatWasSaid();
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
            captureLog.reportOutOfOrderCloseOnce(abandoned.getSection());
        }
        openScopes.remove(closingIndex);
        endScope(scope, endNanos);
    }

    /**
     * @return the clock this profiler times against, for a scope timing the
     *         turns of a loop between its own two ends - every span of one
     *         capture is then read off one time source
     */
    long readClockNanos() {
        return clockNanos.getAsLong();
    }

    // Whether this capture is keeping the detail the section states it is worth
    // timing at. Asked before the clock is read, so a section the capture is not
    // reading costs this comparison and nothing else.
    private boolean canRecordSection(ProfileSection section) {
        return recordedLevel.canAdmitLevel(section.getLevel());
    }

    // Whether the turns inside a loop's span are measured. Apart from the
    // section's own level because a loop's span is worth keeping at the level
    // its caller runs it at, while a clock read per step per item is not.
    private boolean canRecordIterations() {
        return recordedLevel.canAdmitLevel(ProfileLevel.FINE);
    }

    // Both opens that nest, since what differs between them is only the per-turn
    // state the scope carries: the section lands under whatever is already open,
    // and that scope is the one its counts will roll into.
    private RecordingProfileScope openNestedScope(
            ProfileSection section,
            ScopeIterations iterations) {

        return pushScope(resolveNode(section), iterations, resolveInnermostOpenScope());
    }

    // Times from now and puts the scope on the stack.
    private RecordingProfileScope pushScope(
            ProfileNodeAccumulator node,
            ScopeIterations iterations,
            RecordingProfileScope parentScope) {

        var scope = new RecordingProfileScope(
            this,
            node,
            clockNanos.getAsLong(),
            readCompilationMillisFor(node.getSection()),
            iterations,
            parentScope);

        openScopes.add(scope);
        return scope;
    }

    // Where the JVM's compilation clock stands as a call opens, for a section
    // whose calls are events - the ones that state a log threshold - and the
    // not-measured mark for the rest: the bean is a native read, and the paths
    // that open a scope thirty thousand times a second have nothing to gain from
    // it.
    private long readCompilationMillisFor(ProfileSection section) {

        return section.getCallLogThreshold().canLogAnyCall()
            ? compilationMillis.getAsLong()
            : CallWarmth.NOT_MEASURED_MILLIS;
    }

    // Records the span and rolls what the scope counted into the scope it names
    // as its parent, which is what makes a row's counts inclusive the way its
    // time is. Named rather than read off the top of the stack, so an unwind -
    // where several scopes end at one close - charges each one's counts to the
    // row it actually ran inside.
    private void endScope(RecordingProfileScope scope, long endNanos) {

        var elapsedNanos = endNanos - scope.getStartNanos();
        var section = scope.getSection();
        var warmth = scope.resolveWarmth(compilationMillis);

        captureLog.reportBreachOnce(
            section, scope.recordSpan(elapsedNanos, warmth), scope.getTag());

        captureLog.reportClosedCall(
            section, elapsedNanos, warmth, scope.getCounts(), scope.getTag());
        scope.handCountsToParentScope();
    }

    // One call of the reserved row per count that arrives with nothing open,
    // there being no scope to gather several into one call.
    private void recordUnscopedCount(ProfileCounter counter, long amount) {

        var callCounts = new ArrayList<ScopeCount>(1);

        ScopeCount.resolveCountIn(callCounts, counter).addSelfAmount(amount);

        var breach = resolveRootNode(ProfileOrigin.UNSCOPED, ProfileSection.UNSCOPED_COUNTS)
            .addSpan(UNTIMED_CALL_NANOS, callCounts, WorstCall.NO_TAG, CallWarmth.UNMEASURED);

        captureLog.reportBreachOnce(ProfileSection.UNSCOPED_COUNTS, breach, WorstCall.NO_TAG);
    }

    // The node a section opened right now belongs to: a child of whatever is
    // open, or a root of the reserved origin when nothing is - which is where a
    // walk from a path that named no origin is seen rather than lost.
    private ProfileNodeAccumulator resolveNode(ProfileSection section) {

        var parentScope = resolveInnermostOpenScope();

        return parentScope == null
            ? resolveRootNode(ProfileOrigin.UNSCOPED, section)
            : parentScope.resolveChildNode(section);
    }

    // What a section opened now would be opened inside, or null when nothing is
    // open and it would be a root.
    private RecordingProfileScope resolveInnermostOpenScope() {
        return openScopes.isEmpty() ? null : openScopes.get(openScopes.size() - 1);
    }

    private ProfileNodeAccumulator resolveRootNode(ProfileOrigin origin, ProfileSection section) {
        return ProfileOriginAccumulator
            .resolveOriginIn(originGroups, origin)
            .resolveRootNode(section);
    }
}
