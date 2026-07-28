package com.genir.renderer.bridge.interfaces;

import com.genir.renderer.bridge.context.Context;

/**
 * Compile-only mirror of Fast Rendering's {@code GLCommand}, a unit of work an
 * {@code com.genir.renderer.bridge.context.Executor} replays on the render thread. KMLib enqueues
 * one to copy the CPU modelview at the map pass's own position in the command stream, without
 * stalling the pipeline. Never shipped and never loaded - see
 * {@code com.genir.renderer.bridge.context.ContextManager} in this source set for why these stubs
 * exist at all.
 */
public interface GLCommand {

    /**
     * @param context    the render context, supplied on the render thread
     * @param args       the command's packed float arguments; unused by KMLib's copy command
     * @param argsOffset where this command's arguments start in {@code args}
     */
    void run(Context context, float[] args, int argsOffset);
}
