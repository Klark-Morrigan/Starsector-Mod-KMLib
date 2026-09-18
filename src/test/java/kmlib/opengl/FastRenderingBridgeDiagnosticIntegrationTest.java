package kmlib.opengl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Runs the probe against whatever bridge the runtime actually carries.
 *
 * <p>What that bridge is differs by install, so nothing about its findings is pinned: only that the
 * probe answers rather than throws, which is the one property it has to hold on every install to do
 * its job on the failure path.
 */
final class FastRenderingBridgeDiagnosticIntegrationTest {

    @Nested
    class ProbeInstalledBridge {

        @Test
        void answersWithoutThrowingWhateverTheInstallCarries() {

            assertThatCode(FastRenderingBridgeDiagnostic::probeInstalledBridge)
                .doesNotThrowAnyException();
        }
    }
}
