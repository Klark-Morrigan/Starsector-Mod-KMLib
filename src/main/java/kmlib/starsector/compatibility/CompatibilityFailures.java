package kmlib.starsector.compatibility;

import kmlib.text.KmlibStrings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

/**
 * The compatibility failures recorded this session: each binding latched on its first, held until
 * a reporter takes them.
 *
 * <p>A binding is a third party and the mod that took it, and it is latched on that pair: a failure
 * recurring every frame is reported once, while two mods over one third party are each reported.
 * Under one pair the consumer's sentence tells a second feature from the same one again - a mod
 * that filed two features under one key has both reported, the second under a numbered key, while
 * the same feature recording again is dropped. The latch holds for the session rather than until
 * the next take, and a record is safe from any thread, the writers being on more than one. Why the
 * record and the report are two steps, why the pair rather than the third party alone, and why a
 * reused key is numbered at the record rather than refused, are set out in this package's README.
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

    // What the latch is taken under. This record's own lock rather than the record itself, which
    // is a public singleton anything could synchronise on and stall every writer behind.
    private final Object latchLock = new Object();

    // The latch: under each binding recorded this session - a third party and a consumer's key -
    // the sentences recorded there, in record order. Whether a sentence is present is what a record
    // is decided on, and its position is what numbers a reused key. Read and written under the lock
    // only, because those are one decision: two threads recording one binding at once must not
    // both pass with one sentence, nor both take one position with two.
    private final Map<LatchedBinding, List<String>> latchedSentencesByBinding = new HashMap<>();

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
     * that pair is recorded this session with that consumer's sentence, and ignores every record
     * of it afterwards.
     *
     * <p>A different sentence under a pair already recorded is a second feature the mod filed
     * under one feature key, and is kept: the describer is handed that consumer under a numbered
     * key, so both reports arrive. The sentence is what tells that from the same feature recording
     * again, being what the caller already passed - so the ignored path still builds nothing.
     *
     * @param subjectKey      the identity a third party is latched under, spelled once by whoever
     *                        binds to it; a key rather than a {@link CompatibilitySubject} because
     *                        the subject carries versions the description is what reads
     * @param consumer        the mod that took the binding, whose key completes the latch and whose
     *                        sentence tells a second feature under that key from the same one again
     * @param describeFailure builds the failure from the consumer the record filed it under,
     *                        invoked only on the record that is kept - which is what makes a
     *                        reflective probe or a version read affordable on a per-frame path. The
     *                        latch is taken first, so a describer that throws is invoked once for
     *                        the session too: the binding records nothing rather than throwing on
     *                        every frame
     */
    public void recordOnce(
            String subjectKey,
            CompatibilityConsumer consumer,
            Function<CompatibilityConsumer, CompatibilityFailure> describeFailure) {

        KmlibStrings.requireText(
            subjectKey,
            "A record latched under no key could not be told from any other subject's.");
        Objects.requireNonNull(
            consumer,
            "A record with no consumer could not say whose feature the failure costs.");
        Objects.requireNonNull(
            describeFailure,
            "A record with nothing to describe the failure would latch a binding and report nothing.");

        var recordedAs = takeLatch(subjectKey, consumer);

        // Null is the whole decision: another record of this binding with this sentence - on this
        // thread or another - already passed, and there is nothing further to do or to build.
        if (recordedAs == null) {
            return;
        }

        unreportedFailures.add(Objects.requireNonNull(
            describeFailure.apply(recordedAs),
            "A description that answers nothing leaves a recorded binding with nothing to report."));
    }

    /**
     * Takes the oldest failure not yet reported, leaving the rest where they are.
     *
     * <p>One at a time because a reporter can only show one at a time: the game drops a message
     * dialog asked for behind another, so a reporter that took them all would be holding a second
     * queue of what it could not yet show. Taken in record order, so the first binding to break is
     * the first a player is told about.
     *
     * <p>The binding taken stays latched: a take is a hand-over to whoever reports, not a reset of
     * the latch, so a binding that fails again after its report is not reported again.
     *
     * @return the oldest untaken failure, or {@code null} where none is waiting
     */
    public CompatibilityFailure takeNextUnreported() {

        return unreportedFailures.poll();
    }

    // Takes the latch for one record, as one step under the lock: whether this sentence was
    // recorded under the binding before, and where it was not, which position it takes there.
    // Answers the consumer the failure is composed against - the caller's own for the first
    // sentence under its key, the caller's under a numbered key for each sentence after it - or
    // null where the sentence was already latched.
    private CompatibilityConsumer takeLatch(String subjectKey, CompatibilityConsumer consumer) {

        var binding = new LatchedBinding(subjectKey, consumer.consumerKey());

        synchronized (latchLock) {
            var sentences = latchedSentencesByBinding.computeIfAbsent(binding, key -> new ArrayList<>());

            if (sentences.contains(consumer.lostFeature())) {
                return null;
            }

            sentences.add(consumer.lostFeature());

            // Which position keeps the bare key is the consumer's rule, not this one's.
            return consumer.resolveConsumerAtPosition(sentences.size());
        }
    }

    // What a record is latched under: which third party stopped holding, and for which mod. A pair
    // of components rather than the two keys joined into one string, so no separator can be spelled
    // inside a key and collide with a binding nobody recorded.
    private record LatchedBinding(String subjectKey, String consumerKey) {
    }
}
