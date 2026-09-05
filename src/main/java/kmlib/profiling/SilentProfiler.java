package kmlib.profiling;

import java.util.List;
import java.util.function.Supplier;

/**
 * The {@link Profiler} that keeps nothing, so code that measures through the
 * seam costs a virtual call and no more when nobody is reading a readout.
 *
 * <p>Work handed to {@link #measure} still runs - the seam decides whether a
 * span is kept, never whether it happens - and the clock is never read, since
 * a duration nothing will report is not worth the read. One shared instance,
 * because it holds no state and a second one would say nothing the first
 * does not.
 */
public final class SilentProfiler implements Profiler {

    public static final SilentProfiler INSTANCE = new SilentProfiler();

    private SilentProfiler() {
    }

    @Override
    public void measure(String section, Runnable work) {
        work.run();
    }

    @Override
    public <T> T measure(String section, Supplier<T> work) {
        return work.get();
    }

    @Override
    public void record(String section, long elapsedNanos) {
        // Dropped on purpose: nothing is accumulating.
    }

    @Override
    public List<SectionTiming> snapshot() {
        // A shared immutable empty list, so reporting through a silent profiler
        // allocates nothing either.
        return List.of();
    }

    @Override
    public void reset() {
        // Nothing accumulated, so nothing to clear.
    }
}
