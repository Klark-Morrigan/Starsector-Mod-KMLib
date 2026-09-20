package kmlib.starsector.ui.map.transform;

import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Matrix4f;

import java.util.function.Supplier;

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
final class FastRenderingModelviewCopyTest {

    // A map pass's transform as Fast Rendering holds it: a Matrix4f whose fields it reads as
    // m<row><col>, so the pan sits in m03/m13 and a copy that did not transpose would report it in
    // the slots a projection's perspective terms belong in. Not round and not equal on the two
    // axes, so a transpose or an axis swap cannot pass by landing on a matching value.
    private static final float PAN_X = 137.01f;
    private static final float PAN_Y = 41.01f;

    // Where a column-major copy puts that pan, which is where a caller reads it from.
    private static final int COLUMN_MAJOR_PAN_X_SLOT = 12;
    private static final int COLUMN_MAJOR_PAN_Y_SLOT = 13;

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

    private final FastRenderingModelviewCopy modelviewCopy = new FastRenderingModelviewCopy(
        CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER,
        failures);

    @Nested
    class Constructor {

        @Test
        void refusesACopyWithNoConsumer() {

            // A copy that cannot say whose feature it serves could record a failure against nobody,
            // which is the one job it has once the binding breaks.
            assertThatNullPointerException()
                .isThrownBy(() -> new FastRenderingModelviewCopy(null, failures));
        }

        @Test
        void refusesACopyWithNowhereToRecord() {

            assertThatNullPointerException()
                .isThrownBy(() -> new FastRenderingModelviewCopy(
                    CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER,
                    null));
        }
    }

    @Nested
    class CopyModelviewForNextRead {

        @Test
        void publishesTheReadingColumnMajorForTheNextReadToReport() {

            modelviewCopy.copyModelviewForNextRead(() -> createPannedMatrix());

            assertThat(modelviewCopy.reportLatestCopy())
                .isNotNull();
            assertThat(modelviewCopy.reportLatestCopy()[COLUMN_MAJOR_PAN_X_SLOT])
                .isEqualTo(PAN_X);
            assertThat(modelviewCopy.reportLatestCopy()[COLUMN_MAJOR_PAN_Y_SLOT])
                .isEqualTo(PAN_Y);
            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void publishesAnIdentityReadingAsTheReadingItIs() {

            // The renderer pushed that matrix to the GPU instead of tracking it. Identity is a
            // reading rather than a failure, and the transform above reads it as unusable on its
            // own terms - degrading here would report a broken binding over a working one.
            modelviewCopy.copyModelviewForNextRead(() -> createIdentityMatrix());

            assertThat(modelviewCopy.reportLatestCopy())
                .containsExactly(
                    1f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f);
            assertThat(modelviewCopy.isBridgeUnavailable())
                .isFalse();
        }

        @Test
        void publishesNoReadingWhereTheRendererTrackedNoMatrix() {

            modelviewCopy.copyModelviewForNextRead(NO_MATRIX_TRACKED);

            assertThat(modelviewCopy.reportLatestCopy())
                .isNull();
            assertThat(modelviewCopy.isBridgeUnavailable())
                .isFalse();
        }

        @Test
        void keepsAReadingThatRefusedTheCallOffTheRenderThread() {

            // The whole point: the throw stops here. Anywhere else it lands on the game thread a
            // frame later, wrapped, with nothing of ours on the stack to catch it.
            assertThatCode(() -> modelviewCopy.copyModelviewForNextRead(BRIDGE_CALL_REFUSED))
                .doesNotThrowAnyException();
        }

        @Test
        void keepsAReadingWhoseMemberIsGoneOffTheRenderThread() {

            assertThatCode(() -> modelviewCopy.copyModelviewForNextRead(BRIDGE_MEMBER_GONE))
                .doesNotThrowAnyException();
        }

        @Test
        void recordsWhatTheBridgeThrewAgainstTheConsumerThatTookIt() {

            modelviewCopy.copyModelviewForNextRead(BRIDGE_CALL_REFUSED);

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
            modelviewCopy.copyModelviewForNextRead(BRIDGE_CALL_REFUSED);
            modelviewCopy.copyModelviewForNextRead(BRIDGE_CALL_REFUSED);

            failures.takeNextUnreported();
            assertThat(failures.takeNextUnreported())
                .isNull();
        }

        @Test
        void publishesNothingOnceTheBindingHasDegraded() {

            // A command enqueued before the break still runs after it, and a reading it published
            // then would be a matrix from before the break standing where the degraded state says
            // there is none.
            modelviewCopy.copyModelviewForNextRead(BRIDGE_CALL_REFUSED);

            modelviewCopy.copyModelviewForNextRead(() -> createPannedMatrix());

            assertThat(modelviewCopy.reportLatestCopy())
                .isNull();
        }

        @Test
        void dropsTheReadingTakenBeforeTheBindingBroke() {

            // The degraded state is no reading rather than the last one that worked: that matrix
            // describes a pass that ended frames ago, and a caller resolving a cursor against it
            // would resolve the wrong point rather than park.
            modelviewCopy.copyModelviewForNextRead(() -> createPannedMatrix());

            modelviewCopy.copyModelviewForNextRead(BRIDGE_CALL_REFUSED);

            assertThat(modelviewCopy.reportLatestCopy())
                .isNull();
        }
    }

    @Nested
    class IsBridgeUnavailable {

        @Test
        void answersNoWhileTheCopyIsWorking() {

            modelviewCopy.copyModelviewForNextRead(() -> createPannedMatrix());

            assertThat(modelviewCopy.isBridgeUnavailable())
                .isFalse();
        }

        @Test
        void answersYesOnceACopyHasFailed() {

            // What the read path gates on: from the next frame it stays off the bridge entirely
            // rather than enqueueing another command that will fail the same way.
            modelviewCopy.copyModelviewForNextRead(BRIDGE_MEMBER_GONE);

            assertThat(modelviewCopy.isBridgeUnavailable())
                .isTrue();
        }
    }

    @Nested
    class ReportLatestCopy {

        @Test
        void reportsNoReadingBeforeAnyCopyHasRun() {

            // The map's first frame, and its first after a reopen: the command is enqueued but has
            // not run, so there is nothing to report yet.
            assertThat(modelviewCopy.reportLatestCopy())
                .isNull();
        }
    }

    private static Matrix4f createIdentityMatrix() {

        var matrix = new Matrix4f();
        matrix.setIdentity();
        return matrix;
    }

    private static Matrix4f createPannedMatrix() {

        var matrix = createIdentityMatrix();
        matrix.m03 = PAN_X;
        matrix.m13 = PAN_Y;
        return matrix;
    }
}
