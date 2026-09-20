package kmlib.starsector.ui.map.transform;

import kmlib.opengl.FastRendering;

import java.util.Objects;

import com.genir.renderer.bridge.context.ContextManager;
import com.genir.renderer.bridge.interfaces.GLCommand;

/**
 * Puts {@link FastRenderingBridgeReading}'s copy command onto Fast Rendering's own render thread,
 * the binding of {@link FastRenderingModelviewMatrixReader.BridgeCopyQueue} for that renderer.
 *
 * <p>The only class here that names a Fast Rendering type. Loading it on a stock install would
 * throw, since the classes ship in {@code fr.jar} and only a patched install has one, so it must be
 * reached only through {@link FastRendering#isFastRenderingActive} - a reference the JVM resolves
 * lazily, so a branch never taken never loads this. Holding every such reference in one small class
 * is also what lets the reader above it be constructed and driven where no bridge exists at all.
 *
 * <p>Both calls it makes are the bridge's, so both can stop holding: the thread's context is looked
 * up through a static this jar was compiled against, and the command is handed to an executor the
 * installed release may declare and refuse. Neither is guarded here. A queue that swallowed its own
 * failure would report "no context" for a bridge that had broken, which reads as an ordinary frame
 * and would leave the reading degrading silently, once a frame, forever; the reader is where the two
 * are told apart.
 */
final class FastRenderingCopyQueue implements FastRenderingModelviewMatrixReader.BridgeCopyQueue {

    // The copy command, held once rather than rebuilt per frame, so the per-frame path enqueues
    // without allocating. A thin adapter: it reads nothing itself, handing the bridge member that
    // answers the matrix over as a reading to be taken inside the copy's own guard.
    private final GLCommand copyModelviewCommand;

    FastRenderingCopyQueue(FastRenderingBridgeReading bridgeReading) {

        Objects.requireNonNull(
            bridgeReading,
            "A queue with no reading to fill would enqueue a command that publishes nothing.");

        copyModelviewCommand = (renderThreadContext, args, argsOffset) ->
            bridgeReading.copyModelviewForNextRead(
                () -> renderThreadContext.transformManager.getCPUModelView());
    }

    @Override
    public boolean enqueueModelviewCopy() {

        // A thread the renderer registered no context for has nothing to enqueue onto. Answered as
        // an ordinary absence rather than thrown: the context is also null before the renderer is
        // up and again after it is torn down, so it is a frame this read has no answer for, not a
        // binding that stopped holding.
        var context = ContextManager.getThreadContext();
        if (context == null) {
            return false;
        }
        // Enqueue the copy rather than waiting for it: a synchronous read stalls the deferred
        // pipeline every frame, which genir's stall detector turns into a fatal error after enough
        // frames.
        context.exec.execute(copyModelviewCommand);
        return true;
    }
}
