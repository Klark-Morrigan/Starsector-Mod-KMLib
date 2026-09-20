package kmlib.starsector.ui.map.transform;

import java.util.Objects;

/**
 * Reports the modelview Fast Rendering holds on the CPU, the binding of
 * {@link ModelviewMatrixReader} for that renderer. It tracks the modelview in a Java object and
 * multiplies each vertex by it before submitting, leaving GL's own modelview as identity, so GL is
 * not authoritative under it and {@link GlModelviewMatrixReader} would report a matrix describing
 * nothing. Reading its {@code TransformManager} is not a workaround for that: it is the same matrix
 * the vertices are transformed by, which makes it the truth this renderer draws with.
 *
 * <p>That matrix cannot be read where the caller stands, and cannot be read synchronously either.
 * Fast Rendering is a deferred renderer: a {@code glTranslatef} on the calling thread only appends
 * a command to a frame buffer, and the {@code TransformManager} it mutates lives on a separate
 * render thread that replays the buffer a step behind. Reading the matrix inline from the calling
 * thread samples an unrelated in-flight transform, torn field-by-field. Forcing the read to
 * complete synchronously would read the right matrix, but it stalls the pipeline every frame, and
 * genir's stall detector kills the game once a caller stalls on enough frames in a row.
 *
 * <p>So the read is deferred instead. Each call enqueues a fire-and-forget command - which does not
 * stall - that copies the matrix on the render thread at this pass's own position in the stream,
 * where it is the map widget's transform and stable, and stores it in
 * {@link FastRenderingModelviewCopy}. The call returns the copy a prior frame's command produced.
 * The result is a frame or two old (the render thread runs a frame behind, and a given frame's copy
 * is only guaranteed complete a frame later), which is invisible for a still map - the cursor moves
 * but the transform does not - and trails by a frame or two of pan velocity while panning.
 * {@code docs/dev/rendering-environment.md} records the mechanism and its citations.
 *
 * <p>Enqueuing is what {@link BridgeCopyQueue} does, and every Fast Rendering type this reader
 * depends on sits behind it, in {@link FastRenderingCopyQueue}. That is what makes the guards below
 * assertable: a bridge cannot be asked to fail on demand, and on a stock install it cannot be loaded
 * to be asked at all.
 *
 * <p>None of what it reads is published API, so it fails safe: any answer it cannot get is reported
 * as no reading at all rather than as a guess, which {@link CampaignMapTransform} turns into a
 * caller that parks rather than one that resolves a wrong point. Before the first frame's command
 * has run - the map's first frame, and its first after a reopen - the stored copy is null or a
 * frame stale, which parks or self-corrects on the next frame. Where the bridge stops holding
 * outright, every read is no reading from then on and the mod that took the binding is told once.
 *
 * <p>One reader per consumer, built by {@link ModelviewMatrixReaders} where the binding is taken,
 * rather than a shared singleton. A reader that could not say who it serves could not record a
 * failure against anyone, and what a broken binding costs is the taking mod's to state; that one
 * map is on screen at a time is a fact about the map, not about how many mods draw over it.
 */
public final class FastRenderingModelviewMatrixReader implements ModelviewMatrixReader {

    // The copy this reader publishes across the render/game thread boundary, and the latch that
    // says the bridge stopped holding. Its own value because a throw on the render thread can only
    // be contained inside the command, which is the one place this class has nothing else to do -
    // and because both sides of the binding then latch and record through one thing.
    private final FastRenderingModelviewCopy modelviewCopy;

    // What hands the copy command to the renderer, as the only route from here into the bridge.
    private final BridgeCopyQueue copyQueue;

    FastRenderingModelviewMatrixReader(FastRenderingModelviewCopy modelviewCopy, BridgeCopyQueue copyQueue) {

        this.modelviewCopy = Objects.requireNonNull(
            modelviewCopy,
            "A reader with no copy would have nowhere to read a deferred matrix back from.");
        this.copyQueue = Objects.requireNonNull(
            copyQueue,
            "A reader with no queue could not reach the render thread the matrix is copied on.");
    }

    @Override
    public float[] readModelviewMatrix() {
        // Checked before anything reaches the bridge: once the binding has failed on either thread,
        // it is gone for the session, and asking again would only queue another frame's worth of
        // the same failure.
        if (modelviewCopy.isBridgeUnavailable()) {
            return null;
        }
        try {
            // Nothing to report for a frame the renderer had no context for. Not a failure, so it
            // does not degrade the binding: the context is absent before the renderer is up and
            // again after it is torn down, and both are frames the map simply does not hover on.
            if (!copyQueue.enqueueModelviewCopy()) {
                return null;
            }
        } catch (LinkageError | RuntimeException enqueueFailure) {
            // The bridge failing on the game thread, where the reading is asked for. Distinct from
            // the copy's own guard, which covers the same binding failing on the render thread a
            // frame later, and not covered by the guard around the binding itself: a release that
            // declares an entry point and refuses it links cleanly and throws only here. Losing the
            // reading costs a hover highlight; letting it out of a render pass costs the game.
            modelviewCopy.degradeOnBridgeFailure(enqueueFailure);
            return null;
        }
        // Return the previous frame's copy: the command just enqueued has not run yet.
        return modelviewCopy.reportLatestCopy();
    }

    /**
     * What carries a modelview copy to the renderer's own thread, as the seam the bridge's
     * game-thread failures are staged through.
     *
     * <p>Answers whether the copy was enqueued rather than handing back the renderer's context, so
     * that no Fast Rendering type appears in a signature the reader names - which is what keeps the
     * reader loadable, and its guards drivable, where the bridge is not.
     */
    @FunctionalInterface
    interface BridgeCopyQueue {

        /**
         * @return {@code true} where the copy was enqueued, {@code false} where the calling thread
         *         has no render context to enqueue onto - an absent answer for this frame rather
         *         than a binding that stopped holding
         * @throws LinkageError      where a bridge member named by the binding is gone or
         *                           re-signatured
         * @throws RuntimeException  where the installed release declares the entry point and
         *                           refuses the call, which is how a bridge gap surfaces from
         *                           {@code v0.8.9}
         */
        boolean enqueueModelviewCopy();
    }
}
