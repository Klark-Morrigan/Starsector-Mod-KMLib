package kmlib.profiling.snapshot;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what a call's conditions say of themselves: nothing where they were not read or where
 * there is nothing to say, and otherwise the first-call mark and the compilation that ran under
 * the call, in that order.
 */
final class CallWarmthTest {

    private static final long JIT_MILLIS = 412L;
    private static final long NO_JIT_MILLIS = 0L;

    @Nested
    class IsMeasured {

        @Test
        void isMeasuredIsFalseForTheUnmeasuredCall() {

            assertThat(CallWarmth.UNMEASURED.isMeasured())
                .isFalse();
        }

        @Test
        void isMeasuredIsTrueForACallWhoseClockWasReadEvenWhereNothingCompiled() {
            // Zero compilation is a reading - the JVM was warm - and not the same fact as the
            // clock never having been looked at.
            assertThat(new CallWarmth(false, NO_JIT_MILLIS).isMeasured())
                .isTrue();
        }
    }

    @Nested
    class DescribeWarmth {

        @Test
        void describeWarmthSaysNothingForTheUnmeasuredCall() {

            assertThat(CallWarmth.UNMEASURED.describeWarmth())
                .isEmpty();
        }

        @Test
        void describeWarmthSaysNothingForAWarmCallThatWasNotItsRowsFirst() {
            // The plain call, which is nearly every call: a line that said "not first, nothing
            // compiled" on each of them would bury the ones that were.
            assertThat(new CallWarmth(false, NO_JIT_MILLIS).describeWarmth())
                .isEmpty();
        }

        @Test
        void describeWarmthMarksAFirstCallTheJvmWasWarmFor() {
            // What a capture cleared mid-session produces: every row's first call again, on a JVM
            // that compiled nothing under it - first, and not cold.
            assertThat(new CallWarmth(true, NO_JIT_MILLIS).describeWarmth())
                .isEqualTo("first");
        }

        @Test
        void describeWarmthNamesWhatCompiledUnderACallThatWasNotFirst() {

            assertThat(new CallWarmth(false, JIT_MILLIS).describeWarmth())
                .isEqualTo("jitMs=412");
        }

        @Test
        void describeWarmthWritesTheFirstCallMarkBeforeTheCompilation() {
            // The cold call proper: the two facts together, the mark first since it is the one
            // that says why the clock moved.
            assertThat(new CallWarmth(true, JIT_MILLIS).describeWarmth())
                .isEqualTo("first jitMs=412");
        }
    }
}
