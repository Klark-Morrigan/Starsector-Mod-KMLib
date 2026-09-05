package kmlib.profiling;

/**
 * The scope that records nothing, handed out wherever a section is opened but
 * nothing is listening.
 *
 * <p>One shared instance, because it holds no state: opening a section on a
 * silent profiler then allocates nothing at all, which is what lets library
 * code sit inside a scope on a per-frame path and cost nothing when no readout
 * is bound.
 */
final class SilentProfileScope implements ProfileScope {

    static final SilentProfileScope INSTANCE = new SilentProfileScope();

    private SilentProfileScope() {
    }

    @Override
    public void close() {
        // Nothing was timed, so there is nothing to attribute.
    }
}
