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
 * The compatibility failures recorded this session: each subject latched on its first, held until
 * a reporter takes them.
 *
 * <p>A binding breaks where it is used, which is a render pass or a load step - neither a place a
 * player can be told from - and it breaks on every frame that reaches it afterwards. So a record is
 * not a report: it is kept here, once per subject, for something running on a frame that can show a
 * dialog to take and show. Once per subject is what stops a per-frame failure filing a report per
 * frame, the same warn-once shape rendering code already holds, and it holds for the session rather
 * than until the next take: the second frame's failure is the first one again, not news.
 *
 * <p>The failure is described through a supplier rather than passed built because describing one
 * costs something - a reflective walk over the members a binding mirrors, a version read off the
 * third party - and that cost is only worth paying on the record that is kept. The latch is taken
 * before the supplier runs, so a supplier that throws is invoked once for the session too: its
 * failure reaches the caller and its subject records nothing, which is a lost report rather than a
 * repeated one.
 *
 * <p>Safe from any thread, because the writers are not on one. A deferred renderer runs a binding's
 * command on its own render thread while the game thread resolves and calls the same binding, and
 * the two can fail on the same subject in the same frame; one of them wins the latch, the other's
 * supplier never runs.
 */
public final class CompatibilityFailures {

    // Which subjects have been recorded this session - the latch. Membership is what a record is
    // decided on, and the set's add answers whether it was the first atomically, so two threads
    // recording one subject at once cannot both pass.
    private final Set<String> recordedSubjectKeys = ConcurrentHashMap.newKeySet();

    // The failures no reporter has taken yet, in the order they were recorded. Separate from the
    // latch because taking empties this and leaves that: a subject stays recorded for the session
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
     * Records the failure of a subject, the first time that subject is recorded this session, and
     * ignores every record of it afterwards.
     *
     * @param subjectKey      the identity a subject is latched under, spelled once by whoever
     *                        binds to it; a key rather than a {@link CompatibilitySubject} because
     *                        the subject carries versions the description is what reads
     * @param describeFailure builds the failure, invoked only on the record that is kept
     */
    public void recordOnce(String subjectKey, Supplier<CompatibilityFailure> describeFailure) {

        KmlibStrings.requireText(
            subjectKey,
            "A record latched under no key could not be told from any other subject's.");
        Objects.requireNonNull(describeFailure, "describeFailure");

        // The add is the whole decision: false means another record of this subject - on this
        // thread or another - already passed, and there is nothing further to do or to build.
        if (!recordedSubjectKeys.add(subjectKey)) {
            return;
        }

        unreportedFailures.add(Objects.requireNonNull(
            describeFailure.get(),
            "A description that answers nothing leaves a recorded subject with nothing to report."));
    }

    /**
     * Takes every failure recorded since the last take, leaving none behind.
     *
     * <p>The subjects taken stay recorded: a take is a hand-over to whoever reports, not a reset of
     * the latch, so a subject that fails again after its report is not reported again.
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
}
