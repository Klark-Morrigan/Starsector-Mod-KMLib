package com.genir.renderer.bridge.context.executor;

import com.genir.renderer.bridge.interfaces.GLCommand;

/**
 * Compile-only mirror of Fast Rendering's {@code Executor}, the type {@code Context.exec} is
 * declared as from v0.9.0. Declares only {@code execute}, the one member KMLib calls: it enqueues a
 * {@link GLCommand} to run on the render thread at the caller's position in the command stream, and
 * returns without waiting - so, unlike the executor's synchronous readbacks, it never stalls the
 * deferred pipeline. Never shipped and never loaded - see
 * {@code com.genir.renderer.bridge.context.ContextManager} in this source set for why these stubs
 * exist at all.
 *
 * <p>An interface in this package because that is the field's declared type, and a field links by
 * its type: through v0.9.0rc2 it was a class in {@code bridge.context}, and a jar compiled against
 * either shape fails to link {@code Context.exec} on the other.
 */
public interface Executor {

    /**
     * @param command the work to replay on the render thread
     */
    void execute(GLCommand command);
}
