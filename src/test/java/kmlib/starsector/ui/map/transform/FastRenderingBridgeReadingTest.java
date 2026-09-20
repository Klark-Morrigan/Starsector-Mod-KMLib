package kmlib.starsector.ui.map.transform;

import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.testfixtures.opengl.RendererModelviewMatrices;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Matrix4f;

import java.util.function.Supplier;

import static kmlib.testfixtures.opengl.RendererModelviewMatrices.createMapPassAsFastRenderingHoldsIt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Covers the rule the copy exists to hold: nothing escapes onto the renderer's own thread.
 *
 * <p>A command that throws where Fast Rendering runs it is not caught where it was enqueued - the
 * renderer re-throws it wrapped on the game thread at the next frame swap, outside every KM stack
 * frame - so a body that is not total kills the game over a hover highlight. That is why the
 * reading is taken through a value here rather than off a context: a live renderer cannot be asked
 * to fail on demand, and a context cannot be built outside the game at all.
 */
final class FastRenderingBridgeReadingTest {

    // The two shapes a bridge that stopped holding fails in where it is called: a member that moved
    // since this jar was compiled, and an entry point the installed release declares but refuses.
    private static final Supplier<Matrix4f> BRIDGE_MEMBER_GONE = () -> {
        throw new NoSuchMethodError("com.genir.renderer.bridge.context.TransformManager.getCPUModelView()");
    };

    private static final Supplier<Matrix4f> BRIDGE_CALL_REFUSED = () -> {
        throw new UnsupportedOperationException("getCPUModelView()");
    };

    // The renderer answering that it is tracking no matrix - not a failure, just no reading.
    private static final Supplier<Matrix4f> NO_MATRIX_TRACKED = () -> null;

    // A record of its own rather than the session's, so a case reads only what it recorded itself.
    private final CompatibilityFailures failures = new CompatibilityFailures();

    private final FastRenderingBridgeReading bridgeReading = new FastRenderingBridgeReading(
        CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER,
        failures);

    @Nested
    class Constructor {

        @Test
        void refusesACopyWithNoConsumer() {

            // A copy that cannot say whose feature it serves could record a failure against nobody,
            // which is the one job it has once the binding breaks.
            assertThatNullPointerException()
                .isThrownBy(() -> new FastRenderingBridgeReading(null, failures));
        }

        @Test
        void refusesACopyWithNowhereToRecord() {

            assertThatNullPointerException()
                .isThrownBy(() -> new FastRenderingBridgeReading(
                    CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER,
                    null));
        }
    }

    @Nested
    class CopyModelviewForNextRead {

        @Test
        void publishesTheReadingColumnMajorForTheNextReadToReport() {

            bridgeReading.copyModelviewForNextRead(() -> createMapPassAsFastRenderingHoldsIt());

            assertThat(bridgeReading.reportLatestCopy())
                .isNotNull();
            assertThat(bridgeReading.reportLatestCopy()[RendererModelviewMatrices.COLUMN_MAJOR_PAN_X_SLOT])
                .isEqualTo(RendererModelviewMatrices.MAP_PASS_PAN_X);
            assertThat(bridgeReading.reportLatestCopy()[RendererModelviewMatrices.COLUMN_MAJOR_PAN_Y_SLOT])
                .isEqualTo(RendererModelviewMatrices.MAP_PASS_PAN_Y);
            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void publishesAnIdentityReadingAsTheReadingItIs() {

            // The renderer pushed that matrix to the GPU instead of tracking it. Identity is a
            // reading rather than a failure, and the transform above reads it as unusable on its
            // own terms - degrading here would report a broken binding over a working one.
            bridgeReading.copyModelviewForNextRead(() -> createIdentityMatrix());

            assertThat(bridgeReading.reportLatestCopy())
                .containsExactly(
                    1f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f);
            assertThat(bridgeReading.isBridgeUnavailable())
                .isFalse();
        }

        @Test
        void publishesNoReadingWhereTheRendererTrackedNoMatrix() {

            bridgeReading.copyModelviewForNextRead(NO_MATRIX_TRACKED);

            assertThat(bridgeReading.reportLatestCopy())
                .isNull();
            assertThat(bridgeReading.isBridgeUnavailable())
                .isFalse();
        }

        @Test
        void keepsAReadingThatRefusedTheCallOffTheRenderThread() {

            // The whole point: the throw stops here. Anywhere else it lands on the game thread a
            // frame later, wrapped, with nothing of ours on the stack to catch it.
            assertThatCode(() -> bridgeReading.copyModelviewForNextRead(BRIDGE_CALL_REFUSED))
                .doesNotThrowAnyException();
        }

        @Test
        void keepsAReadingWhoseMemberIsGoneOffTheRenderThread() {

            assertThatCode(() -> bridgeReading.copyModelviewForNextRead(BRIDGE_MEMBER_GONE))
                .doesNotThrowAnyException();
        }

        @Test
        void recordsWhatTheBridgeThrewAgainstTheConsumerThatTookIt() {

            bridgeReading.copyModelviewForNextRead(BRIDGE_CALL_REFUSED);

            var failure = failures.takeNextUnreported();
            assertThat(failure.subject().name())
                .isEqualTo(CompatibilityFailureFixture.SUBJECT_NAME);
            assertThat(failure.lostFeature())
                .isEqualTo(CompatibilityFailureFixture.LOST_FEATURE);
        }

        @Test
        void recordsOneFailureHoweverManyFramesTheCopyFails() {

            // The copy runs per frame, so a failure that recorded per frame would file a report for
            // every frame the map stays open.
            bridgeReading.copyModelviewForNextRead(BRIDGE_CALL_REFUSED);
            bridgeReading.copyModelviewForNextRead(BRIDGE_CALL_REFUSED);

            failures.takeNextUnreported();
            assertThat(failures.takeNextUnreported())
                .isNull();
        }

        @Test
        void publishesNothingOnceTheBindingHasDegraded() {

            // A command enqueued before the break still runs after it, and a reading it published
            // then would be a matrix from before the break standing where the degraded state says
            // there is none.
            bridgeReading.copyModelviewForNextRead(BRIDGE_CALL_REFUSED);

            bridgeReading.copyModelviewForNextRead(() -> createMapPassAsFastRenderingHoldsIt());

            assertThat(bridgeReading.reportLatestCopy())
                .isNull();
        }

        @Test
        void dropsTheReadingTakenBeforeTheBindingBroke() {

            // The degraded state is no reading rather than the last one that worked: that matrix
            // describes a pass that ended frames ago, and a caller resolving a cursor against it
            // would resolve the wrong point rather than park.
            bridgeReading.copyModelviewForNextRead(() -> createMapPassAsFastRenderingHoldsIt());

            bridgeReading.copyModelviewForNextRead(BRIDGE_CALL_REFUSED);

            assertThat(bridgeReading.reportLatestCopy())
                .isNull();
        }
    }

    @Nested
    class IsBridgeUnavailable {

        @Test
        void answersNoWhileTheCopyIsWorking() {

            bridgeReading.copyModelviewForNextRead(() -> createMapPassAsFastRenderingHoldsIt());

            assertThat(bridgeReading.isBridgeUnavailable())
                .isFalse();
        }

        @Test
        void answersYesOnceACopyHasFailed() {

            // What the read path gates on: from the next frame it stays off the bridge entirely
            // rather than enqueueing another command that will fail the same way.
            bridgeReading.copyModelviewForNextRead(BRIDGE_MEMBER_GONE);

            assertThat(bridgeReading.isBridgeUnavailable())
                .isTrue();
        }
    }

    @Nested
    class ReportLatestCopy {

        @Test
        void reportsNoReadingBeforeAnyCopyHasRun() {

            // The map's first frame, and its first after a reopen: the command is enqueued but has
            // not run, so there is nothing to report yet.
            assertThat(bridgeReading.reportLatestCopy())
                .isNull();
        }
    }

    private static Matrix4f createIdentityMatrix() {

        var matrix = new Matrix4f();
        matrix.setIdentity();
        return matrix;
    }
}
