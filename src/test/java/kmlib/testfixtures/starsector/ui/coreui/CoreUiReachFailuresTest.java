package kmlib.testfixtures.starsector.ui.coreui;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins that each failure is the type a boundary has to catch, since a fixture throwing something
 * easier would let a boundary that catches too little pass the suites written against it.
 */
final class CoreUiReachFailuresTest {

    @Nested
    class ThrowWrappedGameFailure {

        @Test
        void throwsTheCheckedWrapperAroundTheGamesOwnFailure() {

            assertThatThrownBy(CoreUiReachFailures::throwWrappedGameFailure)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class ThrowUnlinkedMember {

        @Test
        void throwsALinkageError() {

            assertThatThrownBy(CoreUiReachFailures::throwUnlinkedMember)
                .isInstanceOf(NoSuchMethodError.class);
        }
    }
}
