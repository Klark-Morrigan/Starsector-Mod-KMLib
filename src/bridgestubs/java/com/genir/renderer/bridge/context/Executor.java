package com.genir.renderer.bridge.context;

import com.genir.renderer.bridge.context.commands.GLGetter;

/**
 * Compile-only mirror of Fast Rendering's {@code Executor}. Declares only {@code get}, the one
 * member KMLib calls: it submits a {@link GLGetter} to the render thread, runs it there at the
 * caller's position in the command stream, and blocks for the result. Never shipped and never
 * loaded - see {@code com.genir.renderer.bridge.context.ContextManager} in this source set for why
 * these stubs exist at all.
 */
public class Executor {

    /**
     * @param task the read to run on the render thread
     * @param <T>  the value the read yields
     * @return the value the task returned
     */
    public <T> T get(GLGetter<T> task) {
        throw new UnsupportedOperationException("Compile-only stub of Fast Rendering's bridge");
    }
}
