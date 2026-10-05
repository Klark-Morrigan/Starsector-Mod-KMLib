package kmlib.starsector.ui.map.transform;

import com.fs.starfarer.api.Global;

import kmlib.opengl.FastRendering;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.CompatibilityFailures;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Picks the {@link ModelviewMatrixReader} binding the running renderer needs, so a caller that just
 * wants the map's transform does not have to know that the read can be refused under Fast Rendering
 * and never is under stock LWJGL.
 *
 * <p>Both bindings read the same GL entry point. Under Fast Rendering the read is taken under
 * {@link FastRenderingModelviewMatrixReader}'s guard, because releases before
 * {@value FastRendering#FIRST_MODELVIEW_READ_RELEASE} refuse it mid-render.
 *
 * <p>The sentence naming what a refused read costs comes from the caller rather than from here. This
 * class knows the third party, both versions and what was thrown, and nothing at all about what is
 * drawn over the reading - see {@link CompatibilityConsumer}.
 */
public final class ModelviewMatrixReaders {
    private static final Logger LOG = Global.getLogger(ModelviewMatrixReaders.class);

    // The selection the game runs on, wired to the live renderer check and the session's record.
    // Held as a value so a suite drives the same logic over a renderer answer of its own.
    private static final ModelviewMatrixReaders SESSION_SELECTION = new ModelviewMatrixReaders(
        FastRendering::isFastRenderingActive,
        CompatibilityFailures.SESSION_RECORD);

    private final BooleanSupplier isFastRenderingActive;

    // Where a refused read is recorded, taken rather than reached for so a suite records into one of
    // its own instead of into the session's.
    private final CompatibilityFailures failureRecord;

    // The binding each consumer was given, held because the renderer cannot change while the game
    // runs, so a choice is made once rather than re-derived on every frame that reads the map's
    // transform.
    //
    // Per consumer rather than one for all of them, because a guarded reader carries the consumer
    // its failure is recorded against: one held for the session would be the first caller's, so a
    // second mod over the same refused read would be told nothing, or told what the first one lost.
    private final Map<String, ModelviewMatrixReader> readersByConsumerKey = new HashMap<>();

    ModelviewMatrixReaders(BooleanSupplier isFastRenderingActive, CompatibilityFailures failureRecord) {

        this.isFastRenderingActive = Objects.requireNonNull(
            isFastRenderingActive,
            "A selection with nothing to answer which renderer is in force could choose no binding.");
        this.failureRecord = Objects.requireNonNull(
            failureRecord,
            "A selection with nowhere to record would degrade silently and tell no player why.");
    }

    /**
     * Resolves the binding for the renderer in force, choosing on a consumer's first call and
     * reporting that consumer the same one thereafter.
     *
     * <p>A consumer that has not asked before gets a binding of its own, so that a renderer which
     * refuses the read is reported to each mod reading the map rather than only to whichever asked
     * first.
     *
     * @param consumer the mod taking the reading, as the key a refused read is recorded under and the
     *                 sentence naming what it loses where the read is refused. Read only on that path:
     *                 a read that is served never looks at it
     * @return the reader to hand {@link CampaignMapTransform#captureFromMapPass}
     */
    public static ModelviewMatrixReader selectForActiveRenderer(CompatibilityConsumer consumer) {

        return SESSION_SELECTION.selectReaderForActiveRenderer(consumer);
    }

    // The selection itself, on an instance, so a suite can drive it with a renderer answer and a
    // record of its own. Synchronised because a consumer's first call decides for its every later
    // one, and map passes are not guaranteed to be the only thread asking.
    synchronized ModelviewMatrixReader selectReaderForActiveRenderer(CompatibilityConsumer consumer) {

        // Checked on every call rather than only where it is read: a consumer missing from a call
        // that reads cleanly would otherwise surface as a failure to record the first real failure,
        // inside a render pass, which is the one moment this feature exists to keep quiet.
        Objects.requireNonNull(
            consumer,
            "A selection made for no consumer could not say whose feature a refused read costs.");

        var heldReader = readersByConsumerKey.get(consumer.consumerKey());
        if (heldReader != null) {
            return heldReader;
        }
        var resolvedReader = resolveReaderForActiveRenderer(consumer);
        readersByConsumerKey.put(consumer.consumerKey(), resolvedReader);

        // Logged once per consumer, at INFO: which renderer is underneath decides whether the read
        // can be refused, so it is the first thing worth knowing about a hover that resolves nothing
        // - and it is not otherwise visible from a log. Named by consumer, because two mods resolving
        // apart is what the line would otherwise read as one mod resolving twice.
        LOG.info("Modelview matrix source resolved; consumer=" + consumer.consumerKey()
            + "; reader=" + resolvedReader.getClass().getSimpleName());

        return resolvedReader;
    }

    // The stock read is never refused, so it is shared; the guarded one records against the consumer
    // it was built for, so it is not.
    private ModelviewMatrixReader resolveReaderForActiveRenderer(CompatibilityConsumer consumer) {

        if (!isFastRenderingActive.getAsBoolean()) {
            return GlModelviewMatrixReader.INSTANCE;
        }
        return new FastRenderingModelviewMatrixReader(consumer, failureRecord, GlModelviewMatrixReader.INSTANCE);
    }
}
