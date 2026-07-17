package com.genir.renderer.bridge.context;

import org.lwjgl.util.vector.Matrix4f;

/**
 * Compile-only mirror of Fast Rendering's {@code TransformManager}. Declares only
 * {@code getCPUModelView}, the one member KMLib reads. Never shipped and never loaded - see
 * {@code com.genir.renderer.bridge.context.ContextManager} in this source set for why these
 * stubs exist at all.
 */
public class TransformManager {

    /**
     * @return the live modelview Fast Rendering multiplies vertices by. Its {@link Matrix4f} fields
     *         are read as {@code m<row><col>}, transposed from LWJGL's own convention
     */
    public Matrix4f getCPUModelView() {
        throw new UnsupportedOperationException("Compile-only stub of Fast Rendering's bridge");
    }
}
