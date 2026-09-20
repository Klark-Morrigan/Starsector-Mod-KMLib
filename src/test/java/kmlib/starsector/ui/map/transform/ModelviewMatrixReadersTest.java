package kmlib.starsector.ui.map.transform;

import kmlib.opengl.FastRendering;
import kmlib.opengl.FastRenderingBridgeDiagnostic;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;
import kmlib.testfixtures.starsector.ui.map.transform.ModelviewMatrixReaderFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Covers what the selection does when the bridge binding no longer holds: the reported crash is a
 * {@link LinkageError} out of that binding's class initialisation, taken inside a render pass, and
 * the rule it must hold is that a failed binding costs the reading and never the pass.
 *
 * <p>The binding is driven through the supplier seam because no test JVM reaches the failing branch
 * on its own - the renderer check answers "stock" there, so a suite that could not stage a failure
 * could only ever exercise the path that was never broken.
 */
final class ModelviewMatrixReadersTest {

    // The renderer check's two answers, named so a case reads as being about which renderer is
    // underneath rather than about a bare boolean.
    private static final BooleanSupplier BRIDGE_IN_FORCE = () -> true;

    private static final BooleanSupplier STOCK_RENDERER = () -> false;

    private static final CompatibilityConsumer MAP_OVERLAY = CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER;

    // The two versions a mismatch is stated between, told apart so a case cannot pass on one
    // standing in the other's slot.
    private static final String BOUND_VERSION = "v0.8.8";

    private static final String INSTALLED_VERSION = "v0.9.1";

    // What a report puts where no version could be read. The wording is the report's, so a case
    // about an absent version hands its own in rather than asserting somebody else's.
    private static final String UNKNOWN_VERSION_WORDING = "(version unknown)";

    // The member the probe found broken, in the phrase it names one with.
    private static final FastRenderingBridgeDiagnostic.BrokenMember BROKEN_MEMBER =
        new FastRenderingBridgeDiagnostic.BrokenMember(
            "GLCommand.run",
            "ClassNotFoundException: com.genir.renderer.bridge.interfaces.GLCommand");

    // The error a failed link raises, as the cause a composed failure carries.
    private static final LinkageError BRIDGE_CLASS_GONE_ERROR =
        new NoClassDefFoundError("com/genir/renderer/bridge/interfaces/GLCommand");

    // A link failure of each kind the JVM raises separately: a class that is gone, and a member
    // that is gone or re-signatured. One catch is meant to cover both.
    private static final Supplier<ModelviewMatrixReader> BRIDGE_CLASS_GONE = () -> {
        throw new NoClassDefFoundError("com/genir/renderer/bridge/interfaces/GLCommand");
    };

    private static final Supplier<ModelviewMatrixReader> BRIDGE_MEMBER_GONE = () -> {
        throw new NoSuchMethodError("com.genir.renderer.bridge.context.TransformManager.getCPUModelView()");
    };

    // A record of its own rather than the session's, so a case reads only what it recorded itself.
    private final CompatibilityFailures failures = new CompatibilityFailures();

    // How many times the binding was taken, which is what says whether the choice was held: a
    // second selection that binds again would also probe and record again, on every frame.
    private final AtomicInteger bindCount = new AtomicInteger();

    @Nested
    class ComposeBridgeFailure {

        @Test
        void statesTheRendererBetweenTheVersionsTheProbeRead() {

            var failure = ModelviewMatrixReaders.composeBridgeFailure(
                MAP_OVERLAY,
                BRIDGE_CLASS_GONE_ERROR,
                createDiagnosticBetweenVersions(BOUND_VERSION, INSTALLED_VERSION));

            assertThat(failure.subject().name())
                .isEqualTo(FastRendering.COMPATIBILITY_SUBJECT_NAME);
            assertThat(failure.subject().builtAgainstVersion())
                .isEqualTo(BOUND_VERSION);
            assertThat(failure.subject().installedVersion())
                .isEqualTo(INSTALLED_VERSION);
        }

        @Test
        void carriesEveryBrokenMemberAndTheErrorIntoTheSlotsTheLogReads() {

            var diagnostic = createDiagnosticBetweenVersions(BOUND_VERSION, INSTALLED_VERSION);

            var failure = ModelviewMatrixReaders.composeBridgeFailure(
                MAP_OVERLAY,
                BRIDGE_CLASS_GONE_ERROR,
                diagnostic);

            // The probe's whole phrase, not the one member the JVM tripped on: that is the point of
            // probing at all, and the log line is what a report to the renderer's author is written
            // from.
            assertThat(failure.brokenDetail())
                .isEqualTo(diagnostic.describeBrokenMembers());
            assertThat(failure.cause())
                .isSameAs(BRIDGE_CLASS_GONE_ERROR);
        }

        @Test
        void losesWhatTheConsumerSaysItLoses() {

            var failure = ModelviewMatrixReaders.composeBridgeFailure(
                MAP_OVERLAY,
                BRIDGE_CLASS_GONE_ERROR,
                createDiagnosticBetweenVersions(BOUND_VERSION, INSTALLED_VERSION));

            assertThat(failure.lostFeature())
                .isEqualTo(CompatibilityFailureFixture.LOST_FEATURE);
        }

        @Test
        void statesAnUnreadVersionAsNoneRatherThanAsAReading() {

            // Either version can be absent - a build against the stubs stamps none, and a jar older
            // than the one that introduced the version class reports none - and the wording each
            // slot is rendered with is the report's, not the subject's.
            var failure = ModelviewMatrixReaders.composeBridgeFailure(
                MAP_OVERLAY,
                BRIDGE_CLASS_GONE_ERROR,
                createDiagnosticBetweenVersions(null, null));

            assertThat(failure.subject().hasInstalledVersion())
                .isFalse();
            assertThat(failure.subject().describeInstalledVersion(UNKNOWN_VERSION_WORDING))
                .isEqualTo(UNKNOWN_VERSION_WORDING);
        }
    }

    @Nested
    class SelectReaderForActiveRenderer {

        @Test
        void answersTheGlBindingWhereTheBridgeIsNotInForce() {

            var readers = createReaders(STOCK_RENDERER, BRIDGE_CLASS_GONE);

            assertThat(readers.selectReaderForActiveRenderer(MAP_OVERLAY))
                .isSameAs(GlModelviewMatrixReader.INSTANCE);
            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void answersTheBridgeBindingWhereItHolds() {

            var boundReaderFake = new ModelviewMatrixReaderFake(null);
            var readers = createReaders(BRIDGE_IN_FORCE, () -> boundReaderFake);

            assertThat(readers.selectReaderForActiveRenderer(MAP_OVERLAY))
                .isSameAs(boundReaderFake);
            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void answersNoReadingWhereTheBridgeClassIsGone() {

            var readers = createReaders(BRIDGE_IN_FORCE, BRIDGE_CLASS_GONE);

            assertThat(readers.selectReaderForActiveRenderer(MAP_OVERLAY))
                .isSameAs(UnavailableModelviewMatrixReader.INSTANCE);
        }

        @Test
        void answersNoReadingWhereABridgeMemberIsGone() {

            // The same catch: a member that moved or changed signature fails the binding exactly as
            // a missing class does, and degrading is the same answer to both.
            var readers = createReaders(BRIDGE_IN_FORCE, BRIDGE_MEMBER_GONE);

            assertThat(readers.selectReaderForActiveRenderer(MAP_OVERLAY))
                .isSameAs(UnavailableModelviewMatrixReader.INSTANCE);
        }

        @Test
        void recordsOneFailureLosingWhatTheConsumerSaysItLoses() {

            var readers = createReaders(BRIDGE_IN_FORCE, BRIDGE_CLASS_GONE);

            readers.selectReaderForActiveRenderer(MAP_OVERLAY);

            // The sentence is the consumer's and the subject is the library's: the mod names what
            // stops working, and the library names whose code stopped holding.
            var failure = failures.takeNextUnreported();
            assertThat(failure.lostFeature())
                .isEqualTo(CompatibilityFailureFixture.LOST_FEATURE);
            assertThat(failure.subject().name())
                .isEqualTo(CompatibilityFailureFixture.SUBJECT_NAME);
            assertThat(failures.takeNextUnreported())
                .isNull();
        }

        @Test
        void holdsTheBindingItTookRatherThanBindingAgain() {

            var boundReaderFake = new ModelviewMatrixReaderFake(null);
            var readers = createReaders(BRIDGE_IN_FORCE, () -> countAndBind(boundReaderFake));

            readers.selectReaderForActiveRenderer(MAP_OVERLAY);

            assertThat(readers.selectReaderForActiveRenderer(MAP_OVERLAY))
                .isSameAs(boundReaderFake);
            assertThat(bindCount)
                .hasValue(1);
        }

        @Test
        void holdsTheDegradedReaderRatherThanProbingAgain() {

            // The selection a map pass asks for is asked for per frame, so a failed binding that
            // was not held would re-probe and re-record every frame the map is drawn.
            var readers = createReaders(BRIDGE_IN_FORCE, () -> countAndFail());

            readers.selectReaderForActiveRenderer(MAP_OVERLAY);
            failures.takeNextUnreported();

            assertThat(readers.selectReaderForActiveRenderer(MAP_OVERLAY))
                .isSameAs(UnavailableModelviewMatrixReader.INSTANCE);
            assertThat(bindCount)
                .hasValue(1);
            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void refusesASelectionMadeForNoConsumer() {

            // Refused on the healthy path too, where the consumer is never read: a call that binds
            // cleanly today would otherwise fail at the one moment it must not, inside the render
            // pass that took the first real failure.
            var readers = createReaders(STOCK_RENDERER, BRIDGE_CLASS_GONE);

            assertThatNullPointerException()
                .isThrownBy(() -> readers.selectReaderForActiveRenderer(null));
        }
    }

    // A probe's answer stated by the case rather than read off the machine: the live probe reports
    // whichever jar this install has, which is no basis for an expectation about slots.
    private static FastRenderingBridgeDiagnostic createDiagnosticBetweenVersions(
            String boundVersion,
            String installedVersion) {

        return new FastRenderingBridgeDiagnostic(boundVersion, installedVersion, List.of(BROKEN_MEMBER));
    }

    private ModelviewMatrixReaders createReaders(
            BooleanSupplier isFastRenderingActive,
            Supplier<ModelviewMatrixReader> bindFastRendering) {

        return new ModelviewMatrixReaders(isFastRenderingActive, bindFastRendering, failures);
    }

    private ModelviewMatrixReader countAndBind(ModelviewMatrixReader boundReader) {

        bindCount.incrementAndGet();
        return boundReader;
    }

    private ModelviewMatrixReader countAndFail() {

        bindCount.incrementAndGet();
        throw new NoClassDefFoundError("com/genir/renderer/bridge/interfaces/GLCommand");
    }
}
