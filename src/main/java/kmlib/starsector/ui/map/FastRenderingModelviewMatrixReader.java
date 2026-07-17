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
        var cpuModelView = context.transformManager.getCPUModelView();
        if (cpuModelView == null) {
            return null;
        }
        // Identity here means Fast Rendering pushed the matrix to the GPU instead of tracking it,
        // so this reading is not usable. It needs no check: identity is exactly what
        // CampaignMapTransform already rejects, under either renderer and for the same reason.
        return FastRendering.copyAsColumnMajorFloats(cpuModelView);
    }
}
