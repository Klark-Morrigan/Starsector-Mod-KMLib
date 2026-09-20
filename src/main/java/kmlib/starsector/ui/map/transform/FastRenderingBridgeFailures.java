package kmlib.starsector.ui.map.transform;

import kmlib.opengl.FastRendering;
import kmlib.opengl.FastRenderingBridgeDiagnostic;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.CompatibilityFailure;
import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.starsector.compatibility.CompatibilitySubject;

/**
 * How a Fast Rendering binding that stopped holding is described and recorded, for the two places
 * here that take one: {@link ModelviewMatrixReaders}, where the binding is resolved, and
 * {@link FastRenderingModelviewCopy}, where it is called a frame at a time.
 *
 * <p>Shared rather than spelled at each of them because the two fill the same slots from the same
 * sources, and a report a player reads once must not say two different things about one renderer
 * depending on which side of the binding noticed. The identity itself is not spelled here either:
 * {@link FastRendering#COMPATIBILITY_SUBJECT_KEY} and {@link FastRendering#COMPATIBILITY_SUBJECT_NAME}
 * hold it, so a record and a report cannot drift apart into two subjects.
 *
 * <p>Names no Fast Rendering type, so it loads wherever the record does - including from the
 * degraded side of a binding that could not load at all.
 */
final class FastRenderingBridgeFailures {

    private FastRenderingBridgeFailures() {
    }

    /**
     * Records that the bridge stopped holding for this consumer, under the renderer's own key.
     *
     * <p>The probe runs inside the description rather than before it, so the reflective pass is
     * paid on the record that is kept and not on one the latch ignores - which is what makes this
     * affordable to call from a path that runs per frame.
     *
     * @param failureRecord   where the session's failures are collected
     * @param consumer        the mod that took the binding, whose key completes the latch and whose
     *                        sentence names what the failure costs
     * @param bindingFailure  what was thrown: a {@link LinkageError} where the binding never linked,
     *                        or whatever the bridge threw where it linked and then failed
     */
    static void recordBridgeFailure(
            CompatibilityFailures failureRecord,
            CompatibilityConsumer consumer,
            Throwable bindingFailure) {

        failureRecord.recordOnce(
            FastRendering.COMPATIBILITY_SUBJECT_KEY,
            consumer,
            () -> composeBridgeFailure(
                consumer,
                bindingFailure,
                FastRenderingBridgeDiagnostic.probeInstalledBridge()));
    }

    // What the player and the log are told, as the slots of one failure: the renderer and the two
    // versions the mismatch is stated between, every mirrored member the probe found broken rather
    // than the single one the JVM gave up on, the consumer's own sentence, and the caught error as
    // the cause a log line carries a trace from.
    //
    // Takes the probe's answer rather than probing itself, so what fills which slot is stated
    // against a diagnostic a suite composes - the probe reads whichever jar the machine has, which
    // is no basis for an expectation.
    static CompatibilityFailure composeBridgeFailure(
            CompatibilityConsumer consumer,
            Throwable bindingFailure,
            FastRenderingBridgeDiagnostic diagnostic) {

        return new CompatibilityFailure(
            new CompatibilitySubject(
                FastRendering.COMPATIBILITY_SUBJECT_NAME,
                diagnostic.boundVersion(),
                diagnostic.installedVersion()),
            consumer.lostFeature(),
            diagnostic.describeBrokenMembers(),
            bindingFailure);
    }
}
