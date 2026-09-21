package kmlib.starsector.ui.map.transform;

import kmlib.opengl.FastRendering;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.CompatibilityFailures;

import org.lwjgl.util.vector.Matrix4f;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * What Fast Rendering's bridge last answered, and whether it still answers at all: the modelview
 * copied off the render thread, the latch that says the binding stopped holding, and the one report
 * filed when it does.
 *
 * <p>The three belong together because they are one question asked from two threads.
 * {@link FastRenderingModelviewMatrixReader} asks it on the game thread, where the reading is wanted
 * and where the enqueue can fail; the copy runs on the renderer's own thread, inside the command
 * that reader enqueues, where the read can fail differently. Either side finding the bridge broken
 * has to stop the other from touching it, so there is one latch and not two, and the side that
 * latches first is the side that files the report.
 *
 * <p>That the copy is taken here at all, rather than in the reader, is the render thread's doing. A
 * command that throws does not fail where it was enqueued: the renderer captures it and re-throws it
 * wrapped on the game thread at the next frame swap, outside any KM stack frame, so a guard around
 * the enqueue would never see it and the game dies over a hover highlight. The only place it can be
 * contained is inside the command body, so the copy is total by construction rather than by being
 * short enough to look safe. {@code docs/dev/rendering-environment.md} records that mechanism.
 *
 * <p>Names no Fast Rendering type: what it takes is a reading, not a context. That is what lets the
 * failure path be stated against a read that throws, which is the one thing a live renderer will
 * not do on demand.
 */
final class FastRenderingBridgeReading {

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

    FastRenderingBridgeReading(CompatibilityConsumer consumer, CompatibilityFailures failureRecord) {

        this.consumer = Objects.requireNonNull(
            consumer,
            "A reading with no consumer could not say whose feature a failed binding costs.");
        this.failureRecord = Objects.requireNonNull(
            failureRecord,
            "A reading with nowhere to record would degrade silently and tell no player why.");
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
            degradeOnBridgeFailure(
                FastRenderingBridgeFailures.WHILE_RUNNING_ON_RENDER_THREAD,
                copyFailure);
        }
    }

    /**
     * @return {@code true} once the bridge has failed on either thread, the answer both sides stay
     *         off it on
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

    /**
     * Latches the binding unavailable and files one report for it, the answer both threads give a
     * bridge that stopped holding.
     *
     * <p>Latched before the record is filed, so the next frame is already off the bridge whatever
     * the record does with what it was handed. The copy taken before the break is dropped rather
     * than kept: it describes a pass that has been over for frames, and the degraded state is no
     * reading at all rather than a stale one.
     *
     * <p>Shared with {@link FastRenderingModelviewMatrixReader}, which meets the same binding
     * failing on the game thread where the reading is asked for. Degrading there through this
     * rather than through a second latch is what makes a binding that broke on either thread broken
     * on both, and keeps one broken renderer to one report.
     *
     * @param failureSite which side met it, which the latch deliberately does not distinguish but
     *                    the report does: the two fail on different threads for different reasons,
     *                    and a log that named only the latch would describe neither
     * @param copyFailure what the bridge threw, carried into the report as its cause
     */
    void degradeOnBridgeFailure(String failureSite, Throwable copyFailure) {

        isBridgeUnavailable = true;
        latestCopy.set(null);
        FastRenderingBridgeFailures.recordBridgeFailure(
            failureRecord,
            consumer,
            failureSite,
            copyFailure);
    }
}
