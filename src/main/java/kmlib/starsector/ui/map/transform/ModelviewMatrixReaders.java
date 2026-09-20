package kmlib.starsector.ui.map.transform;

import com.fs.starfarer.api.Global;

import kmlib.opengl.FastRendering;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.CompatibilityFailures;

import org.apache.log4j.Logger;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Picks the {@link ModelviewMatrixReader} binding the running renderer needs, so a caller that just
 * wants the map's transform does not have to know that the answer lives somewhere different under
 * Fast Rendering than it does under stock LWJGL. Concentrating the choice here is also what keeps
 * {@link FastRenderingModelviewMatrixReader} unreachable on an install that cannot load it.
 *
 * <p>Fast Rendering's bridge is not published API and has been relocated between releases without
 * notice, so binding to it can fail at the moment the class initialises - a {@link LinkageError}
 * naming a class or a member that moved, raised from inside whichever render pass reached it first.
 * That kills the pass rather than the feature, and names KM classes in a trace the player then
 * blames KM for. So the binding is taken under guard: a link failure costs the reading alone,
 * degrades onto {@link UnavailableModelviewMatrixReader}, and is recorded for the player to be told
 * about once, naming the renderer and its version rather than the mod that was reading it.
 *
 * <p>The sentence naming what that costs comes from the caller rather than from here. This class
 * knows the third party, both versions, the member that moved and what was thrown, and nothing at
 * all about what is drawn over the reading - see {@link CompatibilityConsumer}. What the report is
 * composed of is {@link FastRenderingBridgeFailures}, shared with the reader, which meets the same
 * bridge failing where it is called rather than where it is linked.
 */
public final class ModelviewMatrixReaders {
    private static final Logger LOG = Global.getLogger(ModelviewMatrixReaders.class);

    // The selection the game runs on, wired to the live renderer check, the real bridge binding and
    // the session's record. Held as a value so the same logic can be driven in a test against a
    // binding that fails - which no test JVM's renderer would otherwise reach at all.
    private static final ModelviewMatrixReaders SESSION_SELECTION = new ModelviewMatrixReaders(
        FastRendering::isFastRenderingActive,
        ModelviewMatrixReaders::bindFastRenderingReader,
        CompatibilityFailures.SESSION_RECORD);

    // Whether the bridge is underneath, and what binds to it. The binding is taken through a value
    // rather than by a direct reference so its failure can be staged; in production it is the
    // method below, whose declared answer is the port, so nothing outside that method's body names
    // a bridge-bound type.
    private final BooleanSupplier isFastRenderingActive;
    private final BridgeReaderBinding bindFastRendering;

    // Where a failed binding is recorded, taken rather than reached for so a test records into one
    // of its own instead of into the session's.
    private final CompatibilityFailures failureRecord;

    // The chosen binding, held because the renderer cannot change while the game runs, so the
    // choice is made once rather than re-derived on every frame that reads the map's transform.
    // Holding it is also what stops a failed binding probing and recording on every frame.
    private ModelviewMatrixReader activeReader;

    ModelviewMatrixReaders(
            BooleanSupplier isFastRenderingActive,
            BridgeReaderBinding bindFastRendering,
            CompatibilityFailures failureRecord) {

        this.isFastRenderingActive = Objects.requireNonNull(
            isFastRenderingActive,
            "A selection with nothing to answer which renderer is in force could choose no binding.");
        this.bindFastRendering = Objects.requireNonNull(
            bindFastRendering,
            "A selection with no bridge binding could not serve the renderer it exists for.");
        this.failureRecord = Objects.requireNonNull(
            failureRecord,
            "A selection with nowhere to record would degrade silently and tell no player why.");
    }

    /**
     * Resolves the binding for the renderer in force, choosing on first call and reporting the same
     * one thereafter.
     *
     * @param consumer the mod taking the reading, as the key a failed binding is recorded under and
     *                 the sentence naming what it loses where the binding does not hold. Read only
     *                 on that path: a binding that holds never looks at it
     * @return the reader to hand {@link CampaignMapTransform#captureFromMapPass}, which is
     *         {@link UnavailableModelviewMatrixReader} where the renderer's matrix cannot be reached
     */
    public static ModelviewMatrixReader selectForActiveRenderer(CompatibilityConsumer consumer) {

        return SESSION_SELECTION.selectReaderForActiveRenderer(consumer);
    }

    // The selection itself, on an instance, so a suite can drive it with a binding that fails and a
    // record of its own. Synchronised for the same reason the static held: the first caller decides
    // for every later one, and map passes are not guaranteed to be the only thread asking.
    synchronized ModelviewMatrixReader selectReaderForActiveRenderer(CompatibilityConsumer consumer) {

        // Checked on every call rather than only where it is read: a consumer missing from a call
        // that binds cleanly would otherwise surface as a failure to record the first real failure,
        // inside a render pass, which is the one moment this feature exists to keep quiet.
        Objects.requireNonNull(
            consumer,
            "A selection made for no consumer could not say whose feature a failed binding costs.");

        if (activeReader == null) {
            activeReader = resolveReaderForActiveRenderer(consumer);
            // Logged once, at INFO: which renderer is underneath decides where a matrix is read
            // from, so it is the first thing worth knowing about a hover that resolves the wrong
            // cell - and it is not otherwise visible from a log.
            LOG.info("Modelview matrix source resolved; reader="
                + activeReader.getClass().getSimpleName());
        }
        return activeReader;
    }

    // The production binding, behind a method whose declared answer is the port rather than the
    // bridge-bound class: the JVM resolves the reference in the body when the body runs, so a stock
    // install never loads a class whose own dependencies it does not have.
    //
    // The reader is built here rather than shared, because it records its own call-time failures
    // and can only do that against the consumer this selection was asked for.
    private static ModelviewMatrixReader bindFastRenderingReader(
            CompatibilityConsumer consumer,
            CompatibilityFailures failureRecord) {

        return new FastRenderingModelviewMatrixReader(consumer, failureRecord);
    }

    // Which binding the running renderer needs, and the guard the bridge one is taken under. The
    // stock branch cannot fail this way, so only the bridge branch is wrapped.
    private ModelviewMatrixReader resolveReaderForActiveRenderer(CompatibilityConsumer consumer) {

        if (!isFastRenderingActive.getAsBoolean()) {
            return GlModelviewMatrixReader.INSTANCE;
        }
        try {
            return bindFastRendering.bindReaderFor(consumer, failureRecord);

        } catch (LinkageError bindingFailure) {
            // One catch for every way a binding stops holding at link time - a class that is gone,
            // a member that is gone, a signature that changed - because the JVM raises all three
            // the same way and the answer to each is the same: lose the reading, not the pass.
            FastRenderingBridgeFailures.recordBridgeFailure(failureRecord, consumer, bindingFailure);
            return UnavailableModelviewMatrixReader.INSTANCE;
        }
    }

    /**
     * What takes the bridge binding for one consumer, as the seam its failure is staged through.
     *
     * <p>Two arguments rather than none, because the reader it answers with records its own
     * call-time failures: a binding that could not say who it serves or where to file what it
     * caught could only degrade silently.
     */
    @FunctionalInterface
    interface BridgeReaderBinding {

        /**
         * @param consumer      the mod the reader is built for
         * @param failureRecord where the reader files a binding that stops holding once it is bound
         * @return the bridge-bound reader
         * @throws LinkageError where the binding no longer holds at link time, which is the failure
         *                      the selection above degrades on
         */
        ModelviewMatrixReader bindReaderFor(
            CompatibilityConsumer consumer,
            CompatibilityFailures failureRecord);
    }
}
