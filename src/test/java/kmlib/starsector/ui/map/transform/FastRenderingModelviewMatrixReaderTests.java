package kmlib.starsector.ui.map.transform;

import kmlib.starsector.compatibility.CompatibilityFailure;
import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;
import kmlib.testfixtures.starsector.ui.map.transform.ModelviewMatrixReaderFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Covers the guard around the modelview read under Fast Rendering: a release that serves the read
 * passes it through, and one that refuses it mid-render costs the reading for the session and files
 * one report, rather than throwing out of the render pass.
 *
 * <p>Driven through the read seam because the bridge cannot be asked to refuse on demand. The two
 * staged refusals are the two shapes the renderer's own releases have produced: a method the bridge
 * did not declare through {@code v0.8.8}, and one its facade declares and refuses from {@code v0.8.9}.
 */
final class FastRenderingModelviewMatrixReaderTests {

    private static final float[] SERVED_MATRIX = {
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        137f, 41f, 0f, 1f};

    // A record of its own rather than the session's, so a case reads only what it recorded itself.
    private final CompatibilityFailures failures = new CompatibilityFailures();

    // How many times the bridge was reached for, which is what says a refused reader stays off it:
    // a read runs per frame, so one that kept asking would throw for every frame the map is open.
    private final AtomicInteger readCount = new AtomicInteger();

    @Nested
    class Constructor {

        @Test
        void refusesAReaderWithNoConsumer() {

            assertThatNullPointerException()
                .isThrownBy(() -> new FastRenderingModelviewMatrixReader(
                    null,
                    failures,
                    countAndServe()));
        }

        @Test
        void refusesAReaderWithNoRecord() {

            assertThatNullPointerException()
                .isThrownBy(() -> new FastRenderingModelviewMatrixReader(
                    CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER,
                    null,
                    countAndServe()));
        }

        @Test
        void refusesAReaderWithNoReadToGuard() {

            assertThatNullPointerException()
                .isThrownBy(() -> new FastRenderingModelviewMatrixReader(
                    CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER,
                    failures,
                    null));
        }
    }

    @Nested
    class ReadModelviewMatrix {

        @Test
        void answersTheMatrixTheReadServes() {

            var reader = createReader(countAndServe());

            assertThat(reader.readModelviewMatrix())
                .containsExactly(SERVED_MATRIX);
            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void answersNoReadingWhereTheBridgeRefusesTheCall() {

            // The facade's refusal, from v0.8.9: the method links and throws when called, inside the
            // render pass that asked - exactly where a throw must not reach.
            var reader = createReader(countAndRefuse());

            assertThatCode(reader::readModelviewMatrix)
                .doesNotThrowAnyException();
            assertThat(reader.readModelviewMatrix())
                .isNull();
        }

        @Test
        void answersNoReadingWhereTheBridgeLacksTheMethod() {

            // The same catch for the earlier shape: through v0.8.8 the method was not declared, so
            // the call fails to link rather than throwing.
            var reader = createReader(countAndFailToLink());

            assertThatCode(reader::readModelviewMatrix)
                .doesNotThrowAnyException();
            assertThat(reader.readModelviewMatrix())
                .isNull();
        }

        @Test
        void recordsWhatTheBridgeThrewAgainstTheConsumerThatTookIt() {

            var reader = createReader(countAndRefuse());

            reader.readModelviewMatrix();

            // The sentence is the consumer's and the subject is the library's: the mod names what
            // stops working, and the library names whose code refused.
            var failure = CompatibilityFailureFixture.takeNextBindingFailure(failures);
            assertThat(failure.subject().name())
                .isEqualTo("Fast Rendering");
            assertThat(failure.consumer().lostFeature())
                .isEqualTo(CompatibilityFailureFixture.LOST_FEATURE);
            assertThat(failure.cause())
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void recordsOneFailureHoweverOftenTheMapAsks() {

            var reader = createReader(countAndRefuse());

            reader.readModelviewMatrix();
            reader.readModelviewMatrix();
            reader.readModelviewMatrix();

            assertThat(failures.takeNextUnreported())
                .isNotNull();
            assertThat(failures.takeNextUnreported())
                .isNull();
        }

        @Test
        void staysOffTheBridgeOnceAReadWasRefused() {

            var reader = createReader(countAndRefuse());

            reader.readModelviewMatrix();
            reader.readModelviewMatrix();

            assertThat(readCount)
                .hasValue(1);
        }
    }

    @Nested
    class ComposeReadFailure {

        @Test
        void targetsTheFirstReleaseThatServesTheRead() {

            // The release a player on an older one is told to update to.
            var failure = composeFailureWithInstalledVersion("v0.9.0");

            assertThat(failure.subject().builtAgainstVersion())
                .isEqualTo("v0.9.1rc1");
        }

        @Test
        void carriesTheInstalledReleaseItWasHanded() {

            var failure = composeFailureWithInstalledVersion("v0.9.0");

            assertThat(failure.subject().installedVersion())
                .isEqualTo("v0.9.0");
        }

        @Test
        void namesTheRefusedReadAndWhereItFailed() {

            var failure = composeFailureWithInstalledVersion("v0.9.0");

            assertThat(failure.breakage().brokenDetail())
                .isEqualTo("GL11.glGetFloat(GL_MODELVIEW_MATRIX, FloatBuffer)");
            assertThat(failure.breakage().failureSite())
                .isEqualTo("reading the modelview back through the bridge");
        }
    }

    private static CompatibilityFailure composeFailureWithInstalledVersion(String installedVersion) {

        return FastRenderingModelviewMatrixReader.composeReadFailure(
            CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER,
            new UnsupportedOperationException("GL11.glGetFloat(int, FloatBuffer)"),
            installedVersion);
    }

    private FastRenderingModelviewMatrixReader createReader(ModelviewMatrixReader glReader) {

        return new FastRenderingModelviewMatrixReader(
            CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER,
            failures,
            glReader);
    }

    // The read a serving release answers, and the two ways an older one does not. Each counts the
    // call first, so a case can say whether the bridge was reached at all.
    private ModelviewMatrixReader countAndServe() {

        var servedReaderFake = new ModelviewMatrixReaderFake(SERVED_MATRIX);
        return () -> {
            readCount.incrementAndGet();
            return servedReaderFake.readModelviewMatrix();
        };
    }

    private ModelviewMatrixReader countAndRefuse() {

        return () -> {
            readCount.incrementAndGet();
            throw new UnsupportedOperationException(
                "UnsupportedOperationException: GL11.glGetFloat(int, FloatBuffer)");
        };
    }

    private ModelviewMatrixReader countAndFailToLink() {

        return () -> {
            readCount.incrementAndGet();
            throw new NoSuchMethodError(
                "com.genir.renderer.bridge.commands.GL11.glGetFloat(ILjava/nio/FloatBuffer;)V");
        };
    }
}
