package kmlib.starsector.ui.map.transform;

import com.fs.starfarer.api.Global;

import kmlib.opengl.FastRendering;
import kmlib.opengl.FastRenderingBridgeDiagnostic;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.CompatibilityFailure;
import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.starsector.compatibility.CompatibilitySubject;

import org.apache.log4j.Logger;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

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
 * all about what is drawn over the reading - see {@link CompatibilityConsumer}.
 */
public final class ModelviewMatrixReaders {
    private static final Logger LOG = Global.getLogger(ModelviewMatrixReaders.class);

    // The third party a failed bridge binding is recorded against: the key the record latches
    // under, and the name the player is shown. Spelled here because this is the only class that
    // binds to that renderer through a branch of its own.
    private static final String FAST_RENDERING_SUBJECT_KEY = "fast-rendering";
    private static final String FAST_RENDERING_SUBJECT_NAME = "Fast Rendering";

    // The selection the game runs on, wired to the live renderer check, the real bridge binding and
    // the session's record. Held as a value so the same logic can be driven in a test against a
    // binding that fails - which no test JVM's renderer would otherwise reach at all.
    private static final ModelviewMatrixReaders SESSION_SELECTION = new ModelviewMatrixReaders(
        FastRendering::isFastRenderingActive,
        ModelviewMatrixReaders::bindFastRenderingReader,
        CompatibilityFailures.SESSION_RECORD);

    // Whether the bridge is underneath, and what binds to it. The binding is a supplier rather than
    // a direct reference so its failure can be staged; in production it is the method below, whose
    // declared answer is the port, so nothing outside that method's body names a bridge-bound type.
    private final BooleanSupplier isFastRenderingActive;
    private final Supplier<ModelviewMatrixReader> bindFastRendering;

    // Where a failed binding is recorded, taken rather than reached for so a test records into one
    // of its own instead of into the session's.
    private final CompatibilityFailures failureRecord;

    // The chosen binding, held because the renderer cannot change while the game runs, so the
    // choice is made once rather than re-derived on every frame that reads the map's transform.
    // Holding it is also what stops a failed binding probing and recording on every frame.
    private ModelviewMatrixReader activeReader;

    ModelviewMatrixReaders(
            BooleanSupplier isFastRenderingActive,
            Supplier<ModelviewMatrixReader> bindFastRendering,
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
    private static ModelviewMatrixReader bindFastRenderingReader() {

        return FastRenderingModelviewMatrixReader.INSTANCE;
    }

    // What the player and the log are told, built only where a record is kept. The probe names
    // every mirrored member that does not hold, rather than the single one the JVM gave up on, and
    // reads both versions; the caught error rides along as the cause for the log's stack trace.
    private static CompatibilityFailure describeBridgeFailure(
            CompatibilityConsumer consumer,
            LinkageError bindingFailure) {

        var diagnostic = FastRenderingBridgeDiagnostic.probeInstalledBridge();

        return new CompatibilityFailure(
            new CompatibilitySubject(
                FAST_RENDERING_SUBJECT_NAME,
                diagnostic.boundVersion(),
                diagnostic.installedVersion()),
            consumer.lostFeature(),
            diagnostic.describeBrokenMembers(),
            bindingFailure);
    }

    // Naming the Fast Rendering binding only inside the taken branch is what keeps it off a stock
    // install: the JVM resolves that reference when the branch runs, so a stock game never loads a
    // class whose own dependencies it does not have.
    private ModelviewMatrixReader resolveReaderForActiveRenderer(CompatibilityConsumer consumer) {

        if (!isFastRenderingActive.getAsBoolean()) {
            return GlModelviewMatrixReader.INSTANCE;
        }
        try {
            return bindFastRendering.get();

        } catch (LinkageError bindingFailure) {
            // One catch for every way a binding stops holding at link time - a class that is gone,
            // a member that is gone, a signature that changed - because the JVM raises all three
            // the same way and the answer to each is the same: lose the reading, not the pass.
            failureRecord.recordOnce(
                FAST_RENDERING_SUBJECT_KEY,
                consumer,
                () -> describeBridgeFailure(consumer, bindingFailure));
            return UnavailableModelviewMatrixReader.INSTANCE;
        }
    }
}
