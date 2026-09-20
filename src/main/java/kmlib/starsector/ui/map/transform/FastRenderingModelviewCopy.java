package kmlib.starsector.ui.map.transform;

import kmlib.opengl.FastRendering;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.CompatibilityFailures;

import org.lwjgl.util.vector.Matrix4f;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * The modelview {@link FastRenderingModelviewMatrixReader} copies off the render thread, and what
 * that reader reports once copying it stops working.
 *
 * <p>Apart from the reader because the two answer to different threads and to different failures.
 * The reader binds to Fast Rendering's bridge and is called from the game thread; the copy is taken
 * on the renderer's own thread, inside the command the reader enqueues, and is the one place a
 * throw from that side can be contained at all. A command that throws does not fail where it was
 * enqueued: the renderer captures it and re-throws it wrapped on the game thread at the next frame
 * swap, outside any KM stack frame, so a guard around the enqueue would never see it and the game
 * dies over a hover highlight. {@code docs/dev/rendering-environment.md} records that mechanism.
 *
 * <p>So the copy is total by construction rather than by being short enough to look safe. Anything
 * the read or the copy throws costs the reading and is recorded once, against the consumer that
 * took the binding.
 *
 * <p>Names no Fast Rendering type: what it takes is a reading, not a context. That is what lets the
 * failure path be stated against a read that throws, which is the one thing a live renderer will
 * not do on demand.
 */
final class FastRenderingModelviewCopy {

    // The latest modelview copied off the render thread, or null before the first copy has run and
    // once the binding has stopped holding. Written on the render thread, read on the game thread;
    // the atomic reference is what safely publishes each whole float[] across that boundary.
    private final AtomicReference<float[]> latestCopy = new AtomicReference<>();

    // Who took the binding and where its failure is filed. Held rather than reached for, so the
    // sentence a player reads is the taking mod's and a suite records into a record of its own.
    private final CompatibilityConsumer consumer;
    private final CompatibilityFailures failureRecord;

    // Whether the bridge has stopped holding, latched on the first failure and never cleared: the
    // renderer does not change while the game runs, so a binding that broke once is broken for the
    // session. Volatile because it is set on the render thread and read on the game thread, and
    // reading it is what keeps the broken path off the bridge from the next frame on.
    private volatile boolean isBridgeUnavailable;

    FastRenderingModelviewCopy(CompatibilityConsumer consumer, CompatibilityFailures failureRecord) {

        this.consumer = Objects.requireNonNull(
            consumer,
            "A copy with no consumer could not say whose feature a failed binding costs.");
        this.failureRecord = Objects.requireNonNull(
            failureRecord,
            "A copy with nowhere to record would degrade silently and tell no player why.");
    }

    /**
     * Takes the reading the render thread offers and publishes it for the next read on the game
     * thread, or degrades where taking it does not work.
     *
     * <p>Runs on the render thread, at the enqueuing pass's own position in the command stream,
     * where the modelview is the map widget's and stable - so the copy, transpose included, is done
     * there before anything mutates it. Identity is published as-is rather than read as a failure:
     * the renderer pushed that matrix to the GPU instead of tracking it, and
     * {@link CampaignMapTransform} already reads identity as unusable.
     *
     * @param readModelview the renderer's matrix, as a reading taken inside the guard rather than
     *                      before it - the bridge member that answers it is as able to throw as the
     *                      copy is, and a reading taken outside would escape onto the render thread
     */
    void copyModelviewForNextRead(Supplier<Matrix4f> readModelview) {

        // A command enqueued before the break is still replayed after it. Once the binding is gone
        // it is gone for the session, so that straggler does no work: a copy published after the
        // degrade would put a matrix from before the break back where a reading is reported from.
        if (isBridgeUnavailable) {
            return;
        }
        try {
            var cpuModelView = readModelview.get();
            latestCopy.set(cpuModelView == null
                ? null
                : FastRendering.copyAsColumnMajorFloats(cpuModelView));

        } catch (LinkageError | RuntimeException copyFailure) {
            // Every way the bridge stops holding where it is called: a member that moved since this
            // jar was compiled, and an entry point the installed release declares but does not
            // implement. A fault in the JVM itself is not caught, being the one thing not worth
            // trading for a degraded overlay.
            degradeOnBridgeFailure(copyFailure);
        }
    }

    /**
     * @return {@code true} once a copy has failed, the answer the read path stays off the bridge on
     */
    boolean isBridgeUnavailable() {

        return isBridgeUnavailable;
    }

    /**
     * @return the copy the last command to run left behind, or {@code null} where none has run yet
     *         or the binding has degraded
     */
    float[] reportLatestCopy() {

        return latestCopy.get();
    }

    // Latched before the record is filed, so the next frame is already off the bridge whatever the
    // record does with what it was handed. The copy taken before the break is dropped rather than
    // kept: it describes a pass that has been over for frames, and the degraded state is no reading
    // at all rather than a stale one.
    private void degradeOnBridgeFailure(Throwable copyFailure) {

        isBridgeUnavailable = true;
        latestCopy.set(null);
        FastRenderingBridgeFailures.recordBridgeFailure(failureRecord, consumer, copyFailure);
    }
}
