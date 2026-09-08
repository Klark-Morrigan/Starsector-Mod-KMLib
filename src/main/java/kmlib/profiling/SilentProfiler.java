package kmlib.profiling;

import kmlib.profiling.snapshot.ProfileOriginTree;

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
    public ProfileScope open(ProfileSection section) {
        // The shared do-nothing scope, so a section opened here allocates
        // nothing and closes to nothing.
        return SilentProfileScope.INSTANCE;
    }

    @Override
    public ProfileScope openRoot(ProfileOrigin origin, ProfileSection section) {
        // The origin is dropped with the span it would have grouped: nothing is
        // accumulating, so there is no tree for it to head.
        return SilentProfileScope.INSTANCE;
    }

    @Override
    public IterationScope openIterations(PhasedSection section) {
        // The same shared scope: a loop's turns are as free to leave unmeasured
        // as the section holding them, and the caller marks them either way.
        return SilentProfileScope.INSTANCE;
    }

    @Override
    public void addCountToOpenScope(ProfileCounter counter, long amount) {
        // Dropped like every other span and amount here, which is what lets a
        // walker count what it traverses unconditionally: with nothing bound the
        // counting is a call and never a tally.
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
    public List<ProfileOriginTree> snapshot() {
        // A shared immutable empty list, so reporting through a silent profiler
        // allocates nothing either.
        return List.of();
    }

    @Override
    public void reset() {
        // Nothing accumulated, so nothing to clear.
    }
}
