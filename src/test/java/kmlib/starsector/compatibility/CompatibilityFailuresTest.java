package kmlib.starsector.compatibility;

import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture.createFailure;
import static kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture.createFailureBrokenAt;
import static kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture.createFailureLosing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Covers the two rules the registry holds - one record per binding for the session, and a take that
 * hands over without unlatching - and that the first rule survives two threads recording at once.
 *
 * <p>A binding is a third party and the mod that took it, so the cases worth the most are the two
 * that pull those apart: two mods over one third party each record, and one mod recording the same
 * third party twice records once. A latch on the third party alone passes every other case here.
 */
final class CompatibilityFailuresTest {

    private static final String FAST_RENDERING = CompatibilityFailureFixture.FAST_RENDERING_SUBJECT_KEY;

    private static final String NEXERELIN = CompatibilityFailureFixture.NEXERELIN_SUBJECT_KEY;

    private static final CompatibilityConsumer MAP_OVERLAY = CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER;

    private static final CompatibilityConsumer COLONY_PANEL = CompatibilityFailureFixture.COLONY_PANEL_CONSUMER;

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

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, () -> createFailure());

            assertThat(failures.hasUnreported())
                .isTrue();
        }

        @Test
        void isFalseAgainOnceTheFailureWasTaken() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, () -> createFailure());
            failures.takeUnreported();

            assertThat(failures.hasUnreported())
                .isFalse();
        }
    }

    @Nested
    class RecordOnce {

        @Test
        void keepsTheFirstFailureForABindingAndIgnoresTheSecond() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, () -> createFailureBrokenAt("first"));
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, () -> createFailureBrokenAt("second"));

            assertThat(failures.takeUnreported())
                .extracting(CompatibilityFailure::brokenDetail)
                .containsExactly("first");
        }

        @Test
        void doesNotInvokeTheDescriberOnTheIgnoredRecord() {

            var describeCount = new AtomicInteger();

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, () -> countAndCreateFailure(describeCount));
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, () -> countAndCreateFailure(describeCount));

            assertThat(describeCount)
                .hasValue(1);
        }

        @Test
        void recordsASecondConsumerOfOneSubjectIndependently() {

            // What one broken third party costs is a different thing to each mod bound to it, said
            // in that mod's own words - so a latch keeping only the first would leave the second
            // mod's players told what another mod lost.
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, () -> createFailureLosing(MAP_OVERLAY.lostFeature()));
            failures.recordOnce(FAST_RENDERING, COLONY_PANEL, () -> createFailureLosing(COLONY_PANEL.lostFeature()));

            assertThat(failures.takeUnreported())
                .extracting(CompatibilityFailure::lostFeature)
                .containsExactly(
                    CompatibilityFailureFixture.LOST_FEATURE,
                    CompatibilityFailureFixture.COLONY_PANEL_LOST_FEATURE);
        }

        @Test
        void recordsASecondSubjectIndependently() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, () -> createFailureBrokenAt("first"));
            failures.recordOnce(NEXERELIN, MAP_OVERLAY, () -> createFailureBrokenAt("second"));

            assertThat(failures.takeUnreported())
                .extracting(CompatibilityFailure::brokenDetail)
                .containsExactly("first", "second");
        }

        @Test
        void keepsTheBindingLatchedAfterItsFailureWasTaken() {

            // Once per session, not once per take: the frame after a report fails the same way, and
            // that is the failure already reported rather than a new one.
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, () -> createFailureBrokenAt("first"));
            failures.takeUnreported();
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, () -> createFailureBrokenAt("second"));

            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void invokesAThrowingDescriberOnceForTheSession() {

            var describeCount = new AtomicInteger();

            assertThatIllegalStateException()
                .isThrownBy(() -> failures.recordOnce(
                    FAST_RENDERING,
                    MAP_OVERLAY,
                    () -> countAndThrow(describeCount)));
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, () -> countAndThrow(describeCount));

            // The latch was taken before the describer ran, so the second record is the ignored
            // path and the binding reports nothing rather than throwing on every frame.
            assertThat(describeCount)
                .hasValue(1);
            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void storesOneFailureWhereTwoThreadsRecordTheSameBindingAtOnce() throws InterruptedException {

            var describeCount = new AtomicInteger();
            var startGate = new CountDownLatch(1);
            Runnable recordFromThread = () -> {
                awaitStart(startGate);
                failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, () -> countAndCreateFailure(describeCount));
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

            failures.recordOnce(NEXERELIN, MAP_OVERLAY, () -> createFailureBrokenAt("first"));
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, () -> createFailureBrokenAt("second"));

            assertThat(failures.takeUnreported())
                .extracting(CompatibilityFailure::brokenDetail)
                .containsExactly("first", "second");
        }

        @Test
        void leavesNothingBehindOnceTaken() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, () -> createFailure());
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
        return createFailure();
    }

    private static CompatibilityFailure countAndThrow(AtomicInteger describeCount) {

        describeCount.incrementAndGet();
        throw new IllegalStateException("A describer that could not describe");
    }
}
