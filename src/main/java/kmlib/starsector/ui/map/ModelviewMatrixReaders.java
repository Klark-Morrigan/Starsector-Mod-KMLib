package kmlib.starsector.ui.map;

import com.fs.starfarer.api.Global;

import kmlib.opengl.FastRendering;

import org.apache.log4j.Logger;

/**
 * Picks the {@link ModelviewMatrixReader} binding the running renderer needs, so a caller that just
 * wants the map's transform does not have to know that the answer lives somewhere different under
 * Fast Rendering than it does under stock LWJGL. Concentrating the choice here is also what keeps
 * {@link FastRenderingModelviewMatrixReader} unreachable on an install that cannot load it.
 */
public final class ModelviewMatrixReaders {
    private static final Logger LOG = Global.getLogger(ModelviewMatrixReaders.class);

    // The chosen binding, held because the renderer cannot change while the game runs, so the
    // choice is made once rather than re-derived on every frame that reads the map's transform.
    private static ModelviewMatrixReader activeReader;

    private ModelviewMatrixReaders() {
    }

    /**
     * Resolves the binding for the renderer in force, choosing on first call and reporting the same
     * one thereafter.
     *
     * @return the reader to hand {@link CampaignMapTransform#captureFromMapPass}
     */
    public static synchronized ModelviewMatrixReader selectForActiveRenderer() {
        if (activeReader == null) {
            activeReader = resolveReaderForActiveRenderer();
            // Logged once, at INFO: which renderer is underneath decides where a matrix is read
            // from, so it is the first thing worth knowing about a hover that resolves the wrong
            // cell - and it is not otherwise visible from a log.
            LOG.info("Modelview matrix source resolved; reader="
                    + activeReader.getClass().getSimpleName());
        }
        return activeReader;
    }

    // Naming the Fast Rendering binding only inside the taken branch is what keeps it off a stock
    // install: the JVM resolves that reference when the branch runs, so a stock game never loads a
    // class whose own dependencies it does not have.
    private static ModelviewMatrixReader resolveReaderForActiveRenderer() {
        if (FastRendering.isFastRenderingActive()) {
            return FastRenderingModelviewMatrixReader.INSTANCE;
        }
        return GlModelviewMatrixReader.INSTANCE;
    }
}
