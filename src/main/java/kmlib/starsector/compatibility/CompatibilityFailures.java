package kmlib.starsector.compatibility;

import kmlib.text.KmlibStrings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * The compatibility failures recorded this session: each binding latched on its first, held until
 * a reporter takes them.
 *
 * <p>A binding is a third party and the mod that took it, and it is latched on that pair: a failure
 * recurring every frame is reported once, while two mods over one third party are each reported.
 * The latch holds for the session rather than until the next take, and a record is safe from any
 * thread, the writers being on more than one. Why the record and the report are two steps, and why
 * the pair rather than the third party alone, are set out in this package's README.
 */
public final class CompatibilityFailures {

    /**
     * The session's record: the one every binding records into, and the one the notice drains.
     *
     * <p>One per session rather than per sector, because what it records is a fact about the jars
     * loaded into the process: a binding is resolved once and held for as long as the game runs,
     * and its first record can come from a load step before any sector exists. A sector loaded
     * later finds what was recorded before it waiting.
     */
    public static final CompatibilityFailures SESSION_RECORD = new CompatibilityFailures();

    // Which bindings have been recorded this session - the latch. Membership is what a record is
    // decided on, and the set's add answers whether it was the first atomically, so two threads
    // recording one binding at once cannot both pass.
    private final Set<LatchedBinding> latchedBindings = ConcurrentHashMap.newKeySet();

    // The failures no reporter has taken yet, in the order they were recorded. Separate from the
    // latch because taking empties this and leaves that: a binding stays recorded for the session
    // however many times its failure has been reported.
    private final Queue<CompatibilityFailure> unreportedFailures = new ConcurrentLinkedQueue<>();

    /**
     * Whether anything is waiting to be reported, as the read a per-frame reporter gates on: an
     * empty check on the healthy path, with the take and everything it leads to reached only when
     * this answers yes.
     *
     * @return {@code true} where a recorded failure has not been taken
     */
    public boolean hasUnreported() {

        return !unreportedFailures.isEmpty();
    }

    /**
     * Records the failure of a binding - a third party and the mod that took it - the first time
     * that pair is recorded this session, and ignores every record of it afterwards.
     *
     * @param subjectKey      the identity a third party is latched under, spelled once by whoever
     *                        binds to it; a key rather than a {@link CompatibilitySubject} because
     *                        the subject carries versions the description is what reads
     * @param consumer        the mod that took the binding, whose key completes the latch and whose
     *                        sentence the description puts in the failure's lost-feature slot
     * @param describeFailure builds the failure, invoked only on the record that is kept - which
     *                        is what makes a description that costs something, a reflective probe
     *                        or a version read, affordable on a per-frame path. The latch is taken
     *                        first, so one that throws is invoked once for the session too: the
     *                        throw reaches the caller and the binding records nothing, a lost
     *                        report rather than a repeated one
     */
    public void recordOnce(
            String subjectKey,
            CompatibilityConsumer consumer,
            Supplier<CompatibilityFailure> describeFailure) {

        KmlibStrings.requireText(
            subjectKey,
            "A record latched under no key could not be told from any other subject's.");
        Objects.requireNonNull(
            consumer,
            "A record with no consumer could not say whose feature the failure costs.");
        Objects.requireNonNull(
            describeFailure,
            "A record with nothing to describe the failure would latch a binding and report nothing.");

        // The add is the whole decision: false means another record of this binding - on this
        // thread or another - already passed, and there is nothing further to do or to build.
        if (!latchedBindings.add(new LatchedBinding(subjectKey, consumer.consumerKey()))) {
            return;
        }

        unreportedFailures.add(Objects.requireNonNull(
            describeFailure.get(),
            "A description that answers nothing leaves a recorded binding with nothing to report."));
    }

    /**
     * Takes every failure recorded since the last take, leaving none behind.
     *
     * <p>The bindings taken stay latched: a take is a hand-over to whoever reports, not a reset of
     * the latch, so a binding that fails again after its report is not reported again.
     *
     * <p>Allocates on every call, an empty one included, which is why a per-frame caller reads
     * {@link #hasUnreported()} first and takes only when it answers yes.
     *
     * @return the untaken failures in the order they were recorded; empty where there are none
     */
    public List<CompatibilityFailure> takeUnreported() {

        var takenFailures = new ArrayList<CompatibilityFailure>();

        // Polled one at a time rather than copied and cleared as two steps, so a failure recorded
        // between the two on another thread is never dropped: whatever lands after the last poll
        // waits for the next take.
        for (var failure = unreportedFailures.poll(); failure != null; failure = unreportedFailures.poll()) {
            takenFailures.add(failure);
        }

        return List.copyOf(takenFailures);
    }

    // What a record is latched under: which third party stopped holding, and for which mod. A pair
    // of components rather than the two keys joined into one string, so no separator can be spelled
    // inside a key and collide with a binding nobody recorded.
    private record LatchedBinding(String subjectKey, String consumerKey) {
    }
}
