package com.genir.renderer.bridge.context.commands;

import com.genir.renderer.bridge.context.Context;

/**
 * Compile-only mirror of Fast Rendering's {@code GLGetter}, the callback an
 * {@code com.genir.renderer.bridge.context.Executor} runs on the render thread to read state back.
 * KMLib passes one to read the CPU modelview at the map pass's own position in the command stream.
 * Never shipped and never loaded - see {@code com.genir.renderer.bridge.context.ContextManager} in
 * this source set for why these stubs exist at all.
 *
 * @param <V> the value the read yields
 */
public interface GLGetter<V> {

    /**
     * @param context the render context, supplied on the render thread
     * @return the value read on that thread
     */
    V call(Context context);
}
