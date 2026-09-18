package kmlib.opengl;

import kmlib.opengl.FastRenderingBridgeDiagnostic.BrokenMember;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what the probe reports for a bridge in each state it can be in - wholly absent, wholly as
 * mirrored, and one member off - and that the version read fails on its own rather than with the
 * members.
 *
 * <p>Every case answers the bridge's names with shapes written here, so the reading pinned is of
 * the shape the case describes and not of whichever release the runtime happens to carry.
 */
final class FastRenderingBridgeDiagnosticTest {

    private static final String RESIGNATURED_MEMBER = "ContextManager.getThreadContext";

    @Nested
    class ProbeBridge {

        @Test
        void reportsEveryMemberWhereNothingLoads() {

            var diagnostic = FastRenderingBridgeDiagnostic.probeBridge(
                MirroredBridgeShapes.lookupAmong(Map.of()));

            // In mirror order, so the log reads in the order the adapter's own binding resolves.
            assertThat(diagnostic.brokenMembers())
                .extracting(BrokenMember::member)
                .containsExactly(
                    "ContextManager.getThreadContext",
                    "Context.transformManager",
                    "Context.exec",
                    "Executor.execute(GLCommand)",
                    "GLCommand.run",
                    "TransformManager.getCPUModelView");
        }

        @Test
        void namesTheMissingClassAsEachReasonWhereNothingLoads() {

            var diagnostic = FastRenderingBridgeDiagnostic.probeBridge(
                MirroredBridgeShapes.lookupAmong(Map.of()));

            assertThat(diagnostic.brokenMembers())
                .extracting(BrokenMember::reason)
                .allSatisfy(reason -> assertThat(reason).startsWith("ClassNotFoundException: com.genir.renderer."));
        }

        @Test
        void readsNoVersionWhereNothingLoads() {

            var diagnostic = FastRenderingBridgeDiagnostic.probeBridge(
                MirroredBridgeShapes.lookupAmong(Map.of()));

            assertThat(diagnostic.installedVersion())
                .isNull();
        }

        @Test
        void reportsNoMemberWhereEverySignatureMatchesTheMirrors() {

            var diagnostic = FastRenderingBridgeDiagnostic.probeBridge(
                MirroredBridgeShapes.lookupAmong(MirroredBridgeShapes.mapMatchingShapesByName()));

            assertThat(diagnostic.brokenMembers())
                .isEmpty();
        }

        @Test
        void readsTheVersionWhereEverySignatureMatchesTheMirrors() {

            var diagnostic = FastRenderingBridgeDiagnostic.probeBridge(
                MirroredBridgeShapes.lookupAmong(MirroredBridgeShapes.mapMatchingShapesByName()));

            assertThat(diagnostic.installedVersion())
                .isEqualTo(MirroredBridgeShapes.REPORTED_VERSION);
        }

        @Test
        void reportsOnlyTheMemberWhoseSignatureChanged() {

            var shapesByName = MirroredBridgeShapes.mapMatchingShapesByName();
            shapesByName.put(
                MirroredBridgeShapes.CONTEXT_MANAGER_NAME,
                MirroredBridgeShapes.ContextManagerResignaturedShape.class);

            var diagnostic = FastRenderingBridgeDiagnostic.probeBridge(
                MirroredBridgeShapes.lookupAmong(shapesByName));

            // The same name with a different return type is not the member the adapter binds to,
            // and the five around it still hold.
            assertThat(diagnostic.brokenMembers())
                .extracting(BrokenMember::member)
                .containsExactly(RESIGNATURED_MEMBER);
        }

        @Test
        void namesTheLookupFailureAsTheReasonWhereASignatureChanged() {

            var shapesByName = MirroredBridgeShapes.mapMatchingShapesByName();
            shapesByName.put(
                MirroredBridgeShapes.CONTEXT_MANAGER_NAME,
                MirroredBridgeShapes.ContextManagerResignaturedShape.class);

            var diagnostic = FastRenderingBridgeDiagnostic.probeBridge(
                MirroredBridgeShapes.lookupAmong(shapesByName));

            assertThat(diagnostic.brokenMembers())
                .extracting(BrokenMember::reason)
                .singleElement()
                .asString()
                .startsWith("NoSuchMethodException");
        }

        @Test
        void reportsOnlyTheMemberWhoseClassLoadsWithoutIt() {

            var shapesByName = MirroredBridgeShapes.mapMatchingShapesByName();
            shapesByName.put(
                MirroredBridgeShapes.CONTEXT_MANAGER_NAME,
                MirroredBridgeShapes.ContextManagerWithoutAccessorShape.class);

            var diagnostic = FastRenderingBridgeDiagnostic.probeBridge(
                MirroredBridgeShapes.lookupAmong(shapesByName));

            assertThat(diagnostic.brokenMembers())
                .extracting(BrokenMember::member)
                .containsExactly(RESIGNATURED_MEMBER);
        }

        @Test
        void readsNoVersionWhereOnlyTheVersionClassIsAbsent() {

            var shapesByName = MirroredBridgeShapes.mapMatchingShapesByName();
            shapesByName.remove(MirroredBridgeShapes.VERSION_NAME);

            var diagnostic = FastRenderingBridgeDiagnostic.probeBridge(
                MirroredBridgeShapes.lookupAmong(shapesByName));

            // A jar older than v0.8.2: the bridge holds and the version is simply not there to read.
            assertThat(diagnostic.installedVersion())
                .isNull();
            assertThat(diagnostic.brokenMembers())
                .isEmpty();
        }

        @Test
        void readsNoVersionWhereTheVersionReadThrows() {

            var shapesByName = MirroredBridgeShapes.mapMatchingShapesByName();
            shapesByName.put(MirroredBridgeShapes.VERSION_NAME, MirroredBridgeShapes.VersionThrowingShape.class);

            var diagnostic = FastRenderingBridgeDiagnostic.probeBridge(
                MirroredBridgeShapes.lookupAmong(shapesByName));

            // The version is guarded apart from the members: a read that throws costs the version
            // and leaves the members' findings intact.
            assertThat(diagnostic.installedVersion())
                .isNull();
            assertThat(diagnostic.brokenMembers())
                .isEmpty();
        }

        @Test
        void carriesTheVersionTheBuildStamped() {

            var diagnostic = FastRenderingBridgeDiagnostic.probeBridge(
                MirroredBridgeShapes.lookupAmong(MirroredBridgeShapes.mapMatchingShapesByName()));

            // Whatever the build stamped - a version on the patched leg, none on the stub leg - is
            // what the diagnostic carries; the stamp's own rules are pinned beside it.
            assertThat(diagnostic.boundVersion())
                .isEqualTo(FastRenderingBinding.readBoundVersion());
        }
    }

    @Nested
    class DescribeBrokenMembers {

        @Test
        void statesThatEveryMemberHoldsWhereNoneIsBroken() {

            var diagnostic = new FastRenderingBridgeDiagnostic(null, null, List.of());

            assertThat(diagnostic.describeBrokenMembers())
                .isEqualTo("no mirrored bridge member is missing or re-signatured");
        }

        @Test
        void namesTheMemberWithItsReasonInParentheses() {

            var diagnostic = new FastRenderingBridgeDiagnostic(
                null,
                null,
                List.of(new BrokenMember("Context.exec", "NoSuchFieldException: exec")));

            assertThat(diagnostic.describeBrokenMembers())
                .isEqualTo("Context.exec (NoSuchFieldException: exec)");
        }

        @Test
        void joinsSeveralMembersWithSemicolons() {

            var diagnostic = new FastRenderingBridgeDiagnostic(
                null,
                null,
                List.of(
                    new BrokenMember("Context.exec", "NoSuchFieldException: exec"),
                    new BrokenMember("GLCommand.run", "ClassNotFoundException: GLCommand")));

            assertThat(diagnostic.describeBrokenMembers())
                .isEqualTo("Context.exec (NoSuchFieldException: exec)"
                    + "; GLCommand.run (ClassNotFoundException: GLCommand)");
        }
    }

    @Nested
    class HasBrokenMembers {

        @Test
        void isFalseWhereNoneIsBroken() {

            assertThat(new FastRenderingBridgeDiagnostic(null, null, List.of()).hasBrokenMembers())
                .isFalse();
        }

        @Test
        void isTrueWhereOneIsBroken() {

            var diagnostic = new FastRenderingBridgeDiagnostic(
                null,
                null,
                List.of(new BrokenMember("Context.exec", "NoSuchFieldException: exec")));

            assertThat(diagnostic.hasBrokenMembers())
                .isTrue();
        }
    }
}
