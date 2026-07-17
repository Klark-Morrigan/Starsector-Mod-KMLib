package com.genir.renderer.bridge.context;

/**
 * Compile-only mirror of Fast Rendering's {@code ContextManager}, and the entry point of the three
 * stubs in this source set.
 *
 * <p>These exist so KMLib compiles identically on a stock install and a Fast-Rendering-patched one.
 * The real classes ship in {@code starsector-core/fr.jar}, which only a patched install has, so a
 * build that named them directly could not run anywhere else - and CI would then need a patched
 * game just to compile a class it cannot execute. Stubs keep the build a property of the source
 * rather than of the machine.
 *
 * <p>They are never in the jar (this source set is compile-only) and never loaded: at runtime the
 * real {@code fr.jar} classes are the only ones present, and a stock install never reaches the code
 * that names them. A method body here would therefore be dead weight, which is why they throw.
 *
 * <p>The build binds the real {@code fr.jar} instead of these whenever the install has one, so an
 * ordinary build on a patched machine type-checks against genir's actual bytes and any drift in
 * these signatures surfaces as a compile error - see {@code build.gradle}. Mirror only what KMLib
 * reads: every member added here is one more thing that can silently diverge.
 *
 * <p>{@code docs/dev/rendering-environment.md} records the real shape and its citations.
 */
public class ContextManager {

    /** @return the calling thread's render context, or {@code null} on an unregistered thread */
    public static Context getThreadContext() {
        throw new UnsupportedOperationException("Compile-only stub of Fast Rendering's bridge");
    }
}
