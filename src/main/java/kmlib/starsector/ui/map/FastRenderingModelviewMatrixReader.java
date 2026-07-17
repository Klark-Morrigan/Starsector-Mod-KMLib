package kmlib.starsector.ui.map;

import kmlib.opengl.FastRendering;

import com.genir.renderer.bridge.context.ContextManager;

/**
 * Reports the modelview Fast Rendering holds on the CPU, the binding of
 * {@link ModelviewMatrixReader} for that renderer. It tracks the modelview in a Java object and
 * multiplies each vertex by it before submitting, leaving GL's own modelview as identity, so GL is
 * not authoritative under it and {@link GlModelviewMatrixReader} would report a matrix describing
 * nothing. Reading its {@code TransformManager} is not a workaround for that: it is the same matrix
 * the vertices are transformed by, which makes it the truth this renderer draws with.
 *
 * <p>That matrix cannot be read where the caller stands, though. Fast Rendering is a deferred
 * renderer: a {@code glTranslatef} on the calling thread only appends a command to a frame buffer,
 * and the {@code TransformManager} it mutates lives on a separate render thread that replays the
 * buffer a step behind. Reading the matrix directly from the calling thread therefore samples
 * whatever unrelated transform the render thread happens to be replaying, torn field-by-field as
 * that thread writes it - a confident wrong point every frame, not an absent one. So the read is
 * submitted as a command of its own through the renderer's executor, which runs it on the render
 * thread at this pass's own position in the stream: the matrix is then exactly the map widget's,
 * and copying it there rather than after the command returns keeps the copy on the one thread that
 * writes it. {@code docs/dev/rendering-environment.md} records the mechanism and its citations.
 *
 * <p>The only class here that names a Fast Rendering type. Loading it on a stock install would
 * throw, since the classes ship in {@code fr.jar} and only a patched install has one, so it must be
 * reached only through {@link FastRendering#isFastRenderingActive} - a reference the JVM resolves
 * lazily, so a branch never taken never loads this.
 *
 * <p>None of what it reads is published API, so it fails safe: any answer it cannot get is reported
 * as no reading at all rather than as a guess, which {@link CampaignMapTransform} turns into a
 * caller that parks rather than one that resolves a wrong point.
 *
 * <p>A single {@link #INSTANCE}, matching {@link GlModelviewMatrixReader}: the binding is a
 * stateless forwarder over a static surface, so one shared value serves every caller.
 */
public enum FastRenderingModelviewMatrixReader implements ModelviewMatrixReader {
    INSTANCE;

    @Override
    public float[] readModelviewMatrix() {
        // A thread the bridge never registered a context for has no matrix to report - not an
        // error, just not an answer, so it reads as an absent one.
        var context = ContextManager.getThreadContext();
        if (context == null) {
            return null;
        }
        // Run the read as a command on the render thread rather than reading the matrix here: only
        // there is the CPU modelview this frame's map transform and stable enough to copy without
        // tearing (see the class note). The copy has to happen inside the command for the same
        // reason - once it returns, the render thread moves on and mutates the matrix again.
        return context.exec.get(renderThreadContext -> {
            var cpuModelView = renderThreadContext.transformManager.getCPUModelView();
            if (cpuModelView == null) {
                return null;
            }
            // Identity here means Fast Rendering pushed the matrix to the GPU instead of tracking
            // it, so this reading is not usable. It needs no check: identity is exactly what
            // CampaignMapTransform already rejects, under either renderer and for the same reason.
            return FastRendering.copyAsColumnMajorFloats(cpuModelView);
        });
    }
}
