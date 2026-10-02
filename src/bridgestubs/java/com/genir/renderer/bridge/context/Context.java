package com.genir.renderer.bridge.context;

import com.genir.renderer.bridge.context.executor.Executor;

/**
 * Compile-only mirror of Fast Rendering's {@code Context}. Declares only the members KMLib reads -
 * {@code transformManager} for the modelview and {@code exec} to run that read on the render thread.
 * Never shipped and never loaded - see {@code com.genir.renderer.bridge.context.ContextManager} in
 * this source set for why these stubs exist at all.
 */
public class Context {

    /** The context's transform state. Public and final on the real class. */
    public final TransformManager transformManager = null;

    /**
     * The context's render-thread command executor. Public and final on the real class, and declared
     * as the {@link Executor} interface from v0.9.0 - the type is part of what links.
     */
    public final Executor exec = null;
}
