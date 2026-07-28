package com.genir.renderer.bridge.context;

import com.genir.renderer.bridge.interfaces.GLCommand;

/**
 * Compile-only mirror of Fast Rendering's {@code Executor}. Declares only {@code execute}, the one
 * member KMLib calls: it enqueues a {@link GLCommand} to run on the render thread at the caller's
 * position in the command stream, and returns without waiting - so, unlike the executor's
 * synchronous readbacks, it never stalls the deferred pipeline. Never shipped and never loaded -
 * see {@code com.genir.renderer.bridge.context.ContextManager} in this source set for why these
 * stubs exist at all.
 */
public class Executor {

    /**
     * @param command the work to replay on the render thread
     */
    public void execute(GLCommand command) {
        throw new UnsupportedOperationException("Compile-only stub of Fast Rendering's bridge");
    }
}
