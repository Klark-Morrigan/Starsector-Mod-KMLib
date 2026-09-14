package kmlib.testbootstrap;

import com.fs.starfarer.api.util.Misc;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Pins the guarantee the whole suite rests on: the settings-reading API classes are usable from a
 * test that has arranged nothing.
 *
 * <p>An integration test because there is no unit of it to exercise - the behaviour is the state of
 * the JVM the tests are running in, produced by the platform loading the listener through
 * {@code META-INF/services} before any of them started. Calling the listener directly would prove
 * only that its body runs, not that it was registered, which is the half that actually breaks.
 *
 * <p>It fails loudly when the registration is lost, and that matters more than the assertion looks:
 * without it, dropping the service file would not fail anything here. It would re-arm the ordering
 * hazard, and the suite would go on passing until some unrelated change altered which test ran
 * first, at which point a hundred tests would fail somewhere else entirely with nothing pointing
 * back to the cause.
 */
class VanillaStaticInitialiserIntegrationTest {

    @Nested
    class LauncherSessionOpened {

        @Test
        void launcherSessionOpenedLeavesMiscUsableWithNoStubInPlace() {
            // Deliberately no mockStatic(Global) here - that absence is the condition under test.
            // Reading any member forces the class initialiser, so this passes only where it has
            // already run and succeeded.
            assertThatCode(() -> Misc.random.nextInt(1))
                .doesNotThrowAnyException();
        }
    }
}
