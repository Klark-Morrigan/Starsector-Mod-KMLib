package com.genir.renderer.bridge.context;

/**
 * Compile-only mirror of Fast Rendering's {@code Context}. Declares only {@code transformManager},
 * the one member KMLib reads. Never shipped and never loaded - see
 * {@code com.genir.renderer.bridge.context.ContextManager} in this source set for why these stubs
 * exist at all.
 */
public class Context {

    /** The context's transform state. Public and final on the real class. */
    public final TransformManager transformManager = null;
}
