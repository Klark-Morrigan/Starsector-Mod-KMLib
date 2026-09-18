package kmlib.starsector.compatibility;

import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Covers the two rules the registry holds - one record per subject for the session, and a take that
 * hands over without unlatching - and that the first rule survives two threads recording at once.
 */
final class CompatibilityFailuresTest {

    private static final String FAST_RENDERING = CompatibilityFailureFixture.FAST_RENDERING_SUBJECT_KEY;

    private static final String NEXERELIN = CompatibilityFailureFixture.NEXERELIN_SUBJECT_KEY;

    private final CompatibilityFailures failures = new CompatibilityFailures();

    @Nested
    class HasUnreported {

        @Test
        void isFalseWhereNothingWasRecorded() {

            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void isTrueOnceAFailureWasRecorded() {

            failures.recordOnce(FAST_RENDERING, CompatibilityFailureFixture::createFailure);

            assertThat(failures.hasUnreported())
                .isTrue();
        }

        @Test
        void isFalseAgainOnceTheFailureWasTaken() {

            failures.recordOnce(FAST_RENDERING, CompatibilityFailureFixture::createFailure);
            failures.takeUnreported();

            assertThat(failures.hasUnreported())
                .isFalse();
        }
    }

    @Nested
    class RecordOnce {

        @Test
        void keepsTheFirstFailureForASubjectAndIgnoresTheSecond() {

            failures.recordOnce(FAST_RENDERING, () -> CompatibilityFailureFixture.createFailureBrokenAt("first"));
            failures.recordOnce(FAST_RENDERING, () -> CompatibilityFailureFixture.createFailureBrokenAt("second"));

            assertThat(failures.takeUnreported())
                .extracting(CompatibilityFailure::brokenDetail)
                .containsExactly("first");
        }

        @Test
        void doesNotInvokeTheDescriberOnTheIgnoredRecord() {

            var describeCount = new AtomicInteger();

            failures.recordOnce(FAST_RENDERING, () -> countAndCreateFailure(describeCount));
            failures.recordOnce(FAST_RENDERING, () -> countAndCreateFailure(describeCount));

            assertThat(describeCount)
                .hasValue(1);
        }

        @Test
        void recordsASecondSubjectIndependently() {

            failures.recordOnce(FAST_RENDERING, () -> CompatibilityFailureFixture.createFailureBrokenAt("first"));
            failures.recordOnce(NEXERELIN, () -> CompatibilityFailureFixture.createFailureBrokenAt("second"));

            assertThat(failures.takeUnreported())
                .extracting(CompatibilityFailure::brokenDetail)
                .containsExactly("first", "second");
        }

        @Test
        void keepsTheSubjectLatchedAfterItsFailureWasTaken() {

            // Once per session, not once per take: the frame after a report fails the same way, and
            // that is the failure already reported rather than a new one.
            failures.recordOnce(FAST_RENDERING, () -> CompatibilityFailureFixture.createFailureBrokenAt("first"));
            failures.takeUnreported();
            failures.recordOnce(FAST_RENDERING, () -> CompatibilityFailureFixture.createFailureBrokenAt("second"));

            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void invokesAThrowingDescriberOnceForTheSession() {

            var describeCount = new AtomicInteger();

            assertThatIllegalStateException()
                .isThrownBy(() -> failures.recordOnce(FAST_RENDERING, () -> countAndThrow(describeCount)));
            failures.recordOnce(FAST_RENDERING, () -> countAndThrow(describeCount));

            // The latch was taken before the describer ran, so the second record is the ignored
            // path and the subject reports nothing rather than throwing on every frame.
            assertThat(describeCount)
                .hasValue(1);
            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void storesOneFailureWhereTwoThreadsRecordTheSameSubjectAtOnce() throws InterruptedException {

            var describeCount = new AtomicInteger();
            var startGate = new CountDownLatch(1);
            Runnable recordFromThread = () -> {
                awaitStart(startGate);
                failures.recordOnce(FAST_RENDERING, () -> countAndCreateFailure(describeCount));
            };
            var renderThread = new Thread(recordFromThread);
            var gameThread = new Thread(recordFromThread);
            renderThread.start();
            gameThread.start();

            // Both threads are parked on the gate, so releasing it is what makes the two records
            // race rather than run one after the other.
            startGate.countDown();
            renderThread.join();
            gameThread.join();

            assertThat(describeCount)
                .hasValue(1);
            assertThat(failures.takeUnreported())
                .hasSize(1);
        }
    }

    @Nested
    class TakeUnreported {

        @Test
        void answersNothingWhereNothingWasRecorded() {

            assertThat(failures.takeUnreported())
                .isEmpty();
        }

        @Test
        void answersTheRecordedFailuresInRecordOrder() {

            failures.recordOnce(NEXERELIN, () -> CompatibilityFailureFixture.createFailureBrokenAt("first"));
            failures.recordOnce(FAST_RENDERING, () -> CompatibilityFailureFixture.createFailureBrokenAt("second"));

            assertThat(failures.takeUnreported())
                .extracting(CompatibilityFailure::brokenDetail)
                .containsExactly("first", "second");
        }

        @Test
        void leavesNothingBehindOnceTaken() {

            failures.recordOnce(FAST_RENDERING, CompatibilityFailureFixture::createFailure);
            failures.takeUnreported();

            assertThat(failures.takeUnreported())
                .isEmpty();
        }
    }

    // Parks the calling thread until the gate opens; an interruption while parked is reported as a
    // broken arrangement rather than swallowed into a record that never raced.
    private static void awaitStart(CountDownLatch startGate) {

        try {
            startGate.await();

        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted before the recording threads were released", interruption);
        }
    }

    private static CompatibilityFailure countAndCreateFailure(AtomicInteger describeCount) {

        describeCount.incrementAndGet();
        return CompatibilityFailureFixture.createFailure();
    }

    private static CompatibilityFailure countAndThrow(AtomicInteger describeCount) {

        describeCount.incrementAndGet();
        throw new IllegalStateException("A describer that could not describe");
    }
}
