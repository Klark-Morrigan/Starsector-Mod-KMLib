package kmlib.starsector.ui.map.transform;

import kmlib.opengl.FastRendering;
import kmlib.opengl.FastRenderingBridgeDiagnostic;
import kmlib.starsector.compatibility.CompatibilityBreakage;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.CompatibilityFailure;
import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.starsector.compatibility.CompatibilitySubject;
import kmlib.starsector.ui.compatibility.ScreenCompatibilityNotices;

/**
 * How a Fast Rendering binding that stopped holding is described and recorded, for the two places
 * here that file one: {@link ModelviewMatrixReaders}, where the binding is resolved, and
 * {@link FastRenderingBridgeReading}, which stands for both sides of calling it - its own command on
 * the render thread, and {@link FastRenderingModelviewMatrixReader}'s enqueue on the game thread.
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

    // Where a binding to this renderer can stop holding, as the phrase each guard files under. Three
    // rather than one because the same broken jar reaches the three in three different ways, and
    // which one caught it is what says whether the member is gone, whether the installed release
    // declares it and refuses the call, or whether it fails only where the renderer itself runs it.
    // Spelled here rather than at each guard, so the phrase a log is searched for is one phrase.

    /** As the binding is taken and the class naming the bridge initialises. */
    static final String WHILE_RESOLVING_BINDING = "resolving the binding";

    /** As the game thread calls it, which is where a declared-but-unimplemented entry point throws. */
    static final String WHILE_CALLING_FROM_GAME_THREAD = "calling the bridge from the game thread";

    /** As the renderer replays the command the game thread enqueued, on its own thread. */
    static final String WHILE_RUNNING_ON_RENDER_THREAD =
        "running the copy command on the renderer's own thread";

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
     * @param failureSite     which of the three guards caught it, one of the phrases above
     * @param bindingFailure  what was thrown: a {@link LinkageError} where the binding never linked,
     *                        or whatever the bridge threw where it linked and then failed
     */
    static void recordBridgeFailure(
            CompatibilityFailures failureRecord,
            CompatibilityConsumer consumer,
            String failureSite,
            Throwable bindingFailure) {

        // Composed against the consumer the record hands back rather than the one given: the two
        // differ only where the record found the consumer's key reused, and the report is filed
        // under whichever key the record settled on.
        failureRecord.recordOnce(
            FastRendering.COMPATIBILITY_SUBJECT_KEY,
            consumer,
            recordedAs -> composeBridgeFailure(
                recordedAs,
                failureSite,
                bindingFailure,
                FastRenderingBridgeDiagnostic.probeInstalledBridge()));

        // Told on the screen it was found on, where that is safe. A binding to this renderer breaks
        // during a map pass, and the script that shows the campaign's dialog is not advanced while
        // a core screen is up - so left to that reporter alone, a failure found on the map is shown
        // only once the player has left the screen it was about. A raise that finds no screen
        // leaves the record untouched and the dialog gets it as before.
        if (isGameThreadSite(failureSite)) {
            ScreenCompatibilityNotices.showPendingFailureOnScreen(failureRecord);
        }
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
            String failureSite,
            Throwable bindingFailure,
            FastRenderingBridgeDiagnostic diagnostic) {

        return new CompatibilityFailure(
            new CompatibilitySubject(
                FastRendering.COMPATIBILITY_SUBJECT_NAME,
                diagnostic.boundVersion(),
                diagnostic.installedVersion()),
            consumer,
            new CompatibilityBreakage(failureSite, diagnostic.describeBrokenMembers()),
            bindingFailure);
    }

    // Whether the guard that caught a failure was one the game's own thread runs, which is what
    // decides whether anything may be put on screen from it. Named rather than negated from the
    // render-thread constant, so a guard added beside these is off the screen path until it says
    // otherwise - the safe way round for a question whose wrong answer touches the widget tree
    // from a thread the game does not expect.
    private static boolean isGameThreadSite(String failureSite) {

        return WHILE_RESOLVING_BINDING.equals(failureSite)
            || WHILE_CALLING_FROM_GAME_THREAD.equals(failureSite);
    }
}
