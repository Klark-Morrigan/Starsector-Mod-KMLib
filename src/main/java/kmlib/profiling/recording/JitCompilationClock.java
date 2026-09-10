package kmlib.profiling.recording;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;

/**
 * The JVM's own count of how long it has spent compiling, read as a clock: the
 * source a {@link RecordingProfiler} defaults to for what happened under a call.
 *
 * <p>A class rather than a method reference at the one call site, because the
 * bean may be absent or may not support the reading, and a profiler must not
 * throw on a JVM that happens to lack it: such a JVM reads as one that never
 * compiles, which leaves every call's conditions unmeasured rather than wrong.
 * Resolved once, the bean being a process-wide singleton and its lookup not a
 * per-call cost.
 */
final class JitCompilationClock {

    private static final long NEVER_COMPILES_MILLIS = 0L;

    private static final CompilationMXBean COMPILATION = resolveMonitoredBean();

    private JitCompilationClock() {
    }

    /**
     * @return how long the JVM has spent compiling so far, in milliseconds, or
     *         zero forever on a JVM that cannot say
     */
    static long readTotalCompilationMillis() {
        return COMPILATION == null ? NEVER_COMPILES_MILLIS : COMPILATION.getTotalCompilationTime();
    }

    private static CompilationMXBean resolveMonitoredBean() {

        var bean = ManagementFactory.getCompilationMXBean();

        return bean != null && bean.isCompilationTimeMonitoringSupported() ? bean : null;
    }
}
