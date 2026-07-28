package kmlib.starsector.ui.map;

import kmlib.opengl.FastRendering;

import java.util.concurrent.atomic.AtomicReference;

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
 * where it is the map widget's transform and stable, and stores it. The call returns the copy a
 * prior frame's command produced. The result is a frame or two old (the render thread runs a frame
 * behind, and a given frame's copy is only guaranteed complete a frame later), which is invisible
 * for a still map - the cursor moves but the transform does not - and trails by a frame or two of
 * pan velocity while panning. {@code docs/dev/rendering-environment.md} records the mechanism and
 * its citations.
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
 * frame stale, which parks or self-corrects on the next frame.
 *
 * <p>A single {@link #INSTANCE}, matching {@link GlModelviewMatrixReader}: one map is on screen at
 * a time, so one shared holder serves it. The copy is published across the render/game thread
 * boundary through an {@link AtomicReference}, which also makes the cross-thread read tear-free.
 */
public enum FastRenderingModelviewMatrixReader implements ModelviewMatrixReader {
    INSTANCE;

    // The latest modelview copied off the render thread, or null before the first copy has run.
    // Written by the enqueued command on the render thread, read on the game thread; the atomic
    // reference is what safely publishes each whole float[] across that boundary.
    private final AtomicReference<float[]> latestModelview = new AtomicReference<>();

    // The copy, held once rather than rebuilt per frame. It captures only this singleton's holder,
    // so one instance serves every frame and the per-frame path enqueues without allocating. It
    // runs on the render thread at the enqueuing pass's stream position, where the modelview is the
    // map's and stable, so the copy (transpose included) is done there before anything mutates it.
    // Identity - Fast Rendering pushed the matrix to the GPU rather than tracking it - is copied
    // through as-is; CampaignMapTransform already reads identity as unusable.
    private final GLCommand copyModelviewCommand = (renderThreadContext, args, argsOffset) -> {
        var cpuModelView = renderThreadContext.transformManager.getCPUModelView();
        latestModelview.set(cpuModelView == null
                ? null
                : FastRendering.copyAsColumnMajorFloats(cpuModelView));
    };

    @Override
    public float[] readModelviewMatrix() {
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
        return latestModelview.get();
    }
}
