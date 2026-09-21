package kmlib.starsector.ui.map.transform;

import kmlib.opengl.FastRendering;
import kmlib.opengl.FastRenderingBridgeDiagnostic;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers which slot of a report each part of a failed Fast Rendering binding lands in, for both
 * sides that record one: the selection that could not link the binding, and the reader whose
 * enqueued command met it broken at call time. Both compose through here, so a player told about
 * one renderer is told the same thing about it whichever side noticed.
 */
final class FastRenderingBridgeFailuresTest {

    // The two versions a mismatch is stated between, told apart so a case cannot pass on one
    // standing in the other's slot.
    private static final String BOUND_VERSION = "v0.8.8";

    private static final String INSTALLED_VERSION = "v0.9.1";

    // What a report puts where no version could be read. The wording is the report's, so a case
    // about an absent version hands its own in rather than asserting somebody else's.
    private static final String UNKNOWN_VERSION_WORDING = "(version unknown)";

    // The two mods over one binding, each losing something the other does not.
    private static final CompatibilityConsumer MAP_OVERLAY = CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER;

    private static final CompatibilityConsumer COLONY_PANEL = CompatibilityFailureFixture.COLONY_PANEL_CONSUMER;

    // The error a failed link raises, as the cause a composed failure carries.
    private static final LinkageError BRIDGE_CLASS_GONE_ERROR =
        new NoClassDefFoundError("com/genir/renderer/bridge/interfaces/GLCommand");

    // What the bridge throws where it linked and then refused the call - the shape a release that
    // declares an entry point it does not implement fails in.
    private static final RuntimeException BRIDGE_CALL_REFUSED =
        new UnsupportedOperationException("glGetFloat(int, FloatBuffer)");

    // A record of its own rather than the session's, so a case reads only what it recorded itself.
    private final CompatibilityFailures failures = new CompatibilityFailures();

    @Nested
    class ComposeBridgeFailure {

        @Test
        void statesTheRendererBetweenTheVersionsTheProbeRead() {

            var failure = FastRenderingBridgeFailures.composeBridgeFailure(
                MAP_OVERLAY,
                FastRenderingBridgeFailures.WHILE_RESOLVING_BINDING,
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

            var failure = FastRenderingBridgeFailures.composeBridgeFailure(
                MAP_OVERLAY,
                FastRenderingBridgeFailures.WHILE_RESOLVING_BINDING,
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
        void carriesWhatTheBridgeThrewWhereItLinkedAndThenRefusedTheCall() {

            // The call-time surface: nothing failed to link, so the cause is an ordinary exception
            // rather than an error, and the same slot takes it.
            var failure = FastRenderingBridgeFailures.composeBridgeFailure(
                MAP_OVERLAY,
                FastRenderingBridgeFailures.WHILE_CALLING_FROM_GAME_THREAD,
                BRIDGE_CALL_REFUSED,
                createDiagnosticBetweenVersions(BOUND_VERSION, INSTALLED_VERSION));

            assertThat(failure.cause())
                .isSameAs(BRIDGE_CALL_REFUSED);
        }

        @Test
        void namesWhichGuardCaughtTheBinding() {

            // The slot that tells one release apart from another: a member that moved and an entry
            // point declared and then refused both reach the log as a broken binding, and where the
            // guard met it is the only reading that says which.
            var failure = FastRenderingBridgeFailures.composeBridgeFailure(
                MAP_OVERLAY,
                FastRenderingBridgeFailures.WHILE_RUNNING_ON_RENDER_THREAD,
                BRIDGE_CALL_REFUSED,
                createDiagnosticBetweenVersions(BOUND_VERSION, INSTALLED_VERSION));

            assertThat(failure.failureSite())
                .isEqualTo(FastRenderingBridgeFailures.WHILE_RUNNING_ON_RENDER_THREAD);
        }

        @Test
        void losesWhatTheConsumerSaysItLoses() {

            var failure = FastRenderingBridgeFailures.composeBridgeFailure(
                MAP_OVERLAY,
                FastRenderingBridgeFailures.WHILE_RESOLVING_BINDING,
                BRIDGE_CLASS_GONE_ERROR,
                createDiagnosticBetweenVersions(BOUND_VERSION, INSTALLED_VERSION));

            assertThat(failure.lostFeature())
                .isEqualTo(CompatibilityFailureFixture.LOST_FEATURE);
        }

        @Test
        void statesAnUnreadVersionAsNoneRatherThanAsAReading() {

            // Either version can be absent - a build against the mirrors stamps none, and a jar
            // older than the one that introduced the version class reports none - and the wording
            // each slot is rendered with is the report's, not the subject's.
            var failure = FastRenderingBridgeFailures.composeBridgeFailure(
                MAP_OVERLAY,
                FastRenderingBridgeFailures.WHILE_RESOLVING_BINDING,
                BRIDGE_CLASS_GONE_ERROR,
                createDiagnosticBetweenVersions(null, null));

            assertThat(failure.subject().hasInstalledVersion())
                .isFalse();
            assertThat(failure.subject().describeInstalledVersion(UNKNOWN_VERSION_WORDING))
                .isEqualTo(UNKNOWN_VERSION_WORDING);
        }
    }

    @Nested
    class RecordBridgeFailure {

        @Test
        void recordsOneFailureNamingTheRendererAndWhatTheConsumerLoses() {

            // The probe inside reads whichever jar this machine has, so what is asserted is the
            // slots the recorder fills itself rather than anything it read off an install.
            FastRenderingBridgeFailures.recordBridgeFailure(
                failures,
                MAP_OVERLAY,
                FastRenderingBridgeFailures.WHILE_CALLING_FROM_GAME_THREAD,
                BRIDGE_CALL_REFUSED);

            var failure = failures.takeNextUnreported();
            assertThat(failure.subject().name())
                .isEqualTo(FastRendering.COMPATIBILITY_SUBJECT_NAME);
            assertThat(failure.lostFeature())
                .isEqualTo(CompatibilityFailureFixture.LOST_FEATURE);
            assertThat(failure.cause())
                .isSameAs(BRIDGE_CALL_REFUSED);
        }

        @Test
        void recordsEachConsumerOverTheRendererUnderTheKeyTheyShare() {

            // One renderer, two mods: latched on the pair, so both are reported, and a second
            // record from either is the one the latch drops.
            FastRenderingBridgeFailures.recordBridgeFailure(
                failures,
                MAP_OVERLAY,
                FastRenderingBridgeFailures.WHILE_CALLING_FROM_GAME_THREAD,
                BRIDGE_CALL_REFUSED);
            FastRenderingBridgeFailures.recordBridgeFailure(
                failures,
                COLONY_PANEL,
                FastRenderingBridgeFailures.WHILE_CALLING_FROM_GAME_THREAD,
                BRIDGE_CALL_REFUSED);
            FastRenderingBridgeFailures.recordBridgeFailure(
                failures,
                MAP_OVERLAY,
                FastRenderingBridgeFailures.WHILE_RESOLVING_BINDING,
                BRIDGE_CLASS_GONE_ERROR);

            assertThat(failures.takeNextUnreported().lostFeature())
                .isEqualTo(CompatibilityFailureFixture.LOST_FEATURE);
            assertThat(failures.takeNextUnreported().lostFeature())
                .isEqualTo(CompatibilityFailureFixture.COLONY_PANEL_LOST_FEATURE);
            assertThat(failures.takeNextUnreported())
                .isNull();
        }
    }

    // A probe's answer stated by the case rather than read off the machine: the live probe reports
    // whichever jar this install has, which is no basis for an expectation about slots.
    private static FastRenderingBridgeDiagnostic createDiagnosticBetweenVersions(
            String boundVersion,
            String installedVersion) {

        return new FastRenderingBridgeDiagnostic(boundVersion, installedVersion, List.of(CompatibilityFailureFixture.BROKEN_MEMBER));
    }
}
