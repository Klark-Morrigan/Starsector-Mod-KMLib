package kmlib.starsector.ui.map.transform;

/**
 * Reports no modelview at all, on every read. The binding of {@link ModelviewMatrixReader} for a
 * renderer whose matrix cannot be reached: not "read it from GL" and not "read it from the bridge",
 * but "there is nothing to read". It exists so that degrading still hands a caller a reader rather
 * than a null one, keeping the contract callers already hold - {@link CampaignMapTransform} reads an
 * absent matrix as "park rather than guess", and this reader is that answer made permanent.
 *
 * <p>It is deliberately not a fallback onto {@link GlModelviewMatrixReader}. Under a renderer that
 * tracks the modelview on the CPU, GL's own copy is identity, so reading it back trades a crash for
 * a matrix that describes nothing: a wrong answer rather than no answer. The degraded state is "no
 * reading", never a guessed one.
 *
 * <p>A single {@link #INSTANCE}, as {@link GlModelviewMatrixReader} is: it holds no state, so one
 * shared value serves every caller. Fast Rendering's binding is not one, holding a copy taken off
 * the render thread and the consumer its failures are recorded against.
 */
public enum UnavailableModelviewMatrixReader implements ModelviewMatrixReader {
    INSTANCE;

    /**
     * @return {@code null} every time, the reading a binding cannot serve
     */
    @Override
    public float[] readModelviewMatrix() {
        return null;
    }
}
