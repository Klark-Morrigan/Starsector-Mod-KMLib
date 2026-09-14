package kmlib.profiling;

import java.util.Objects;

/**
 * Holds the one {@link Profiler} the library measures through, so code that
 * runs in the engine's own call paths - where no owner exists to hand a
 * profiler down - still reaches the same instance a readout reports.
 *
 * <p>Silent until a mod binds otherwise: the library must cost nothing to
 * players of mods that never open a readout, and the mod that does is the one
 * that knows when. A deliberate static, and the only one profiling keeps -
 * diagnostics are process-wide and isolate nothing, so a holder here breaks no
 * boundary the rest of the library draws.
 *
 * <p>Not synchronised, like the profilers it holds: binding and measuring both
 * happen on the game thread.
 */
public final class ActiveProfiler {

    private static Profiler boundProfiler = SilentProfiler.INSTANCE;

    private ActiveProfiler() {
    }

    /**
     * Makes {@code profiler} the one every subsequent measurement reaches.
     * Timings the previously bound profiler accumulated stay with it.
     *
     * @param profiler the profiler to measure through from now on
     */
    public static void bindProfiler(Profiler profiler) {
        boundProfiler = Objects.requireNonNull(profiler, "profiler");
    }

    /**
     * @return the bound profiler, or the {@link SilentProfiler} when nothing
     *         has been bound
     */
    public static Profiler resolveProfiler() {
        return boundProfiler;
    }
}
