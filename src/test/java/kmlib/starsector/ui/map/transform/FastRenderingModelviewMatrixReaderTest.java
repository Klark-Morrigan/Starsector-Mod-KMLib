package kmlib.starsector.ui.map.transform;

import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Matrix4f;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Covers the bridge failing on the game thread, where the reading is asked for: an entry point that
 * links and then refuses the call, which walks straight past the guard around the binding because
 * nothing failed to link.
 *
 * <p>Driven through the queue seam because the bridge cannot be asked to fail on demand, and on the
 * install a test JVM has it cannot be loaded to be asked at all. The two staged failures are the
 * two shapes the renderer's own releases have produced - a member that moved since this jar was
 * compiled, and an entry point {@code v0.8.9}'s facade declares and refuses - and one catch covers
 * the context lookup and the enqueue alike, both being calls into the same bridge from this thread.
 */
final class FastRenderingModelviewMatrixReaderTest {

    // A record of its own rather than the session's, so a case reads only what it recorded itself.
    private final CompatibilityFailures failures = new CompatibilityFailures();

    private final FastRenderingBridgeReading bridgeReading = new FastRenderingBridgeReading(
        CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER,
        failures);

    // How many times the bridge was reached for, which is what says a degraded reader stays off it:
    // a read runs per frame, so one that kept calling would fail for every frame the map is open.
    private final AtomicInteger enqueueCount = new AtomicInteger();

    @Nested
    class Constructor {

        @Test
        void refusesAReaderWithNoCopy() {

            assertThatNullPointerException()
                .isThrownBy(() -> new FastRenderingModelviewMatrixReader(null, countAndEnqueue()));
        }

        @Test
        void refusesAReaderWithNoQueue() {

            // A reader that could not reach the render thread would enqueue nothing and report the
            // absent copy of a binding that is in fact working.
            assertThatNullPointerException()
                .isThrownBy(() -> new FastRenderingModelviewMatrixReader(bridgeReading, null));
        }
    }

    @Nested
    class ReadModelviewMatrix {

        @Test
        void answersTheCopyAnEarlierFramesCommandLeftBehind() {

            // The command enqueued by this read runs a frame later, so what the read reports is the
            // copy an earlier one already published.
            bridgeReading.copyModelviewForNextRead(() -> createIdentityMatrix());
            var reader = createReader(countAndEnqueue());

            assertThat(reader.readModelviewMatrix())
                .isSameAs(bridgeReading.reportLatestCopy());
            assertThat(enqueueCount)
                .hasValue(1);
            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void answersNoReadingForAFrameTheRendererHadNoContextFor() {

            // Absent rather than broken: the renderer answers no context before it is up and again
            // after it is torn down, which is a frame with no reading rather than a failed binding.
            bridgeReading.copyModelviewForNextRead(() -> createIdentityMatrix());
            var reader = createReader(countAndReportNoContext());

            assertThat(reader.readModelviewMatrix())
                .isNull();
            assertThat(bridgeReading.isBridgeUnavailable())
                .isFalse();
            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void degradesWhereTheBridgeRefusesTheCall() {

            // The failure this step exists for. It links, so the guard around the binding never
            // sees it, and it is thrown where the reading is asked for - inside a render pass,
            // which is exactly where a throw must not reach.
            var reader = createReader(countAndRefuseTheCall());

            assertThatCode(reader::readModelviewMatrix)
                .doesNotThrowAnyException();
            assertThat(bridgeReading.isBridgeUnavailable())
                .isTrue();
        }

        @Test
        void degradesWhereABridgeMemberIsGone() {

            // The same catch: a member that moved or changed signature fails the call exactly as a
            // refused one does, and losing the reading is the same answer to both.
            var reader = createReader(countAndFailToFindTheMember());

            assertThatCode(reader::readModelviewMatrix)
                .doesNotThrowAnyException();
            assertThat(bridgeReading.isBridgeUnavailable())
                .isTrue();
        }

        @Test
        void recordsWhatTheBridgeThrewAgainstTheConsumerThatTookIt() {

            var reader = createReader(countAndRefuseTheCall());

            reader.readModelviewMatrix();

            // The sentence is the consumer's and the subject is the library's: the mod names what
            // stops working, and the library names whose code stopped holding.
            var failure = failures.takeNextUnreported();
            assertThat(failure.subject().name())
                .isEqualTo(CompatibilityFailureFixture.SUBJECT_NAME);
            assertThat(failure.consumer().lostFeature())
                .isEqualTo(CompatibilityFailureFixture.LOST_FEATURE);

            // Filed as this thread's, not the renderer's: both degrade through the one latch, so
            // the site is what keeps the log from reporting a game-thread refusal as a command
            // that failed where the renderer ran it.
            assertThat(failure.breakage().failureSite())
                .isEqualTo(FastRenderingBridgeFailures.WHILE_CALLING_FROM_GAME_THREAD);
        }

        @Test
        void staysOffTheBridgeOnceACallHasFailed() {

            var reader = createReader(countAndRefuseTheCall());

            reader.readModelviewMatrix();

            assertThat(reader.readModelviewMatrix())
                .isNull();
            assertThat(enqueueCount)
                .hasValue(1);
        }

        @Test
        void answersNoReadingWhereTheCopyDegradedOnTheRenderThread() {

            // The other side of the same binding: a command that failed where the renderer ran it
            // latched the copy, and this thread is off the bridge from the next frame without
            // having met the failure itself.
            bridgeReading.copyModelviewForNextRead(() -> {
                throw new UnsupportedOperationException("TransformManager.getCPUModelView()");
            });
            var reader = createReader(countAndEnqueue());

            assertThat(reader.readModelviewMatrix())
                .isNull();
            assertThat(enqueueCount)
                .hasValue(0);
        }
    }

    private static Matrix4f createIdentityMatrix() {

        var matrix = new Matrix4f();
        matrix.setIdentity();
        return matrix;
    }

    private FastRenderingModelviewMatrixReader createReader(
            FastRenderingModelviewMatrixReader.BridgeCopyQueue copyQueue) {

        return new FastRenderingModelviewMatrixReader(bridgeReading, copyQueue);
    }

    // The queue the renderer would be if it were working, and the three ways it is not. Each counts
    // the call first, so a case can say whether the bridge was reached at all.
    private FastRenderingModelviewMatrixReader.BridgeCopyQueue countAndEnqueue() {

        return () -> {
            enqueueCount.incrementAndGet();
            return true;
        };
    }

    private FastRenderingModelviewMatrixReader.BridgeCopyQueue countAndFailToFindTheMember() {

        return () -> {
            enqueueCount.incrementAndGet();
            throw new NoSuchMethodError("com.genir.renderer.bridge.context.ContextManager.getThreadContext()");
        };
    }

    private FastRenderingModelviewMatrixReader.BridgeCopyQueue countAndReportNoContext() {

        return () -> {
            enqueueCount.incrementAndGet();
            return false;
        };
    }

    private FastRenderingModelviewMatrixReader.BridgeCopyQueue countAndRefuseTheCall() {

        return () -> {
            enqueueCount.incrementAndGet();
            throw new UnsupportedOperationException("com.genir.renderer.bridge.context.Executor.execute()");
        };
    }
}
