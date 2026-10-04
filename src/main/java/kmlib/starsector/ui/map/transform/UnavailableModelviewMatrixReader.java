package kmlib.starsector.ui.map.transform;

/**
 * Reports no modelview at all, on every read. The binding of {@link ModelviewMatrixReader} for a
 * caller with no matrix to read: not "read it from GL", but "there is nothing to read". It exists so
 * that such a caller still holds a reader rather than a null one, keeping the contract callers
 * already hold - {@link CampaignMapTransform} reads an absent matrix as "park rather than guess", and
 * this reader is that answer made permanent.
 *
 * <p>A single {@link #INSTANCE}, as {@link GlModelviewMatrixReader} is: it holds no state, so one
 * shared value serves every caller. Fast Rendering's binding is not one, holding the consumer its
 * failure is recorded against.
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
