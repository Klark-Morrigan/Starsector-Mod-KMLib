package kmlib.starsector.ui.map.transform;

import kmlib.opengl.FastRendering;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.CompatibilityFailures;

import com.genir.renderer.bridge.context.ContextManager;
import com.genir.renderer.bridge.interfaces.GLCommand;

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
 * <p>The only class here that names a Fast Rendering type. Loading it on a stock install would
 * throw, since the classes ship in {@code fr.jar} and only a patched install has one, so it must be
 * reached only through {@link FastRendering#isFastRenderingActive} - a reference the JVM resolves
 * lazily, so a branch never taken never loads this.
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
    // be contained inside the command, which is the one place this class has nothing else to do.
    private final FastRenderingModelviewCopy modelviewCopy;

    // The copy command, held once rather than rebuilt per frame, so the per-frame path enqueues
    // without allocating. A thin adapter: it reads nothing itself, handing the bridge member that
    // answers the matrix over as a reading to be taken inside the copy's own guard.
    private final GLCommand copyModelviewCommand;

    FastRenderingModelviewMatrixReader(CompatibilityConsumer consumer, CompatibilityFailures failureRecord) {

        modelviewCopy = new FastRenderingModelviewCopy(consumer, failureRecord);
        copyModelviewCommand = (renderThreadContext, args, argsOffset) ->
            modelviewCopy.copyModelviewForNextRead(
                () -> renderThreadContext.transformManager.getCPUModelView());
    }

    @Override
    public float[] readModelviewMatrix() {
        // Checked before anything reaches the bridge: once a command has failed on the render
        // thread, the binding is gone for the session, and asking again would only queue another
        // frame's worth of the same failure.
        if (modelviewCopy.isBridgeUnavailable()) {
            return null;
        }
        // A thread the bridge never registered a context for has no matrix to report - not an
        // error, just not an answer, so it reads as an absent one.
        var context = ContextManager.getThreadContext();
        if (context == null) {
            return null;
        }
        // Enqueue the copy rather than waiting for it: a synchronous read stalls the deferred
        // pipeline every frame, which genir's stall detector turns into a fatal error after enough
        // frames.
        context.exec.execute(copyModelviewCommand);
        // Return the previous frame's copy: the command just enqueued has not run yet.
        return modelviewCopy.reportLatestCopy();
    }
}
