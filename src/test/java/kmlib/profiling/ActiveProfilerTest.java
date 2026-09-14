package kmlib.profiling;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Pins {@link ActiveProfiler}: the silent profiler answers until something is
 * bound, the bound one answers after, and a null binding is refused rather than
 * leaving nothing to measure through.
 */
final class ActiveProfilerTest {

    // The holder is process-wide, so every case leaves it as it found it: silent.
    @AfterEach
    void restoreSilentProfiler() {
        ActiveProfiler.bindProfiler(SilentProfiler.INSTANCE);
    }

    @Nested
    class ResolveProfiler {

        @Test
        void resolvesTheSilentProfilerWhenNothingIsBound() {

            assertThat(ActiveProfiler.resolveProfiler())
                .isSameAs(SilentProfiler.INSTANCE);
        }

        @Test
        void resolvesTheBoundProfilerAfterBinding() {

            var profilerMock = mock(Profiler.class);

            ActiveProfiler.bindProfiler(profilerMock);

            assertThat(ActiveProfiler.resolveProfiler())
                .isSameAs(profilerMock);
        }
    }

    @Nested
    class BindProfiler {

        @Test
        void refusesANullProfilerAndKeepsTheCurrentOne() {

            assertThatThrownBy(() -> ActiveProfiler.bindProfiler(null))
                .isInstanceOf(NullPointerException.class);
            assertThat(ActiveProfiler.resolveProfiler())
                .isSameAs(SilentProfiler.INSTANCE);
        }
    }
}
