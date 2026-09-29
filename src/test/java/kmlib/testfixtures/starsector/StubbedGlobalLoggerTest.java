package kmlib.testfixtures.starsector;

import com.fs.starfarer.api.Global;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins that the stand-in answers a class's own logger - the same instance log4j hands out for that
 * class - so a recording appender attached to a production class sees what it wrote, and a static
 * field resolved under the stand-in is never null.
 */
final class StubbedGlobalLoggerTest {

    @Nested
    class OpenGlobalAnsweringLoggers {

        @Test
        void answersEachClassItsOwnLogger() {

            try (var globalMock = StubbedGlobalLogger.openGlobalAnsweringLoggers()) {

                assertThat(Global.getLogger(StubbedGlobalLoggerTest.class))
                    .isSameAs(Logger.getLogger(StubbedGlobalLoggerTest.class));
            }
        }

        @Test
        void yieldsToALoggerTheCallerStubsForOneClass() {
            // What a suite verifying one class's warnings, or silencing one class's logger, relies
            // on: its narrower stub wins for that class while every other class keeps its own.
            var otherLogger = Logger.getLogger("kmlibtest_stubbed_for_one_class");

            try (var globalMock = StubbedGlobalLogger.openGlobalAnsweringLoggers()) {

                globalMock
                    .when(() -> Global.getLogger(StubbedGlobalLoggerTest.class))
                    .thenReturn(otherLogger);

                assertThat(Global.getLogger(StubbedGlobalLoggerTest.class))
                    .isSameAs(otherLogger);
                assertThat(Global.getLogger(StubbedGlobalLogger.class))
                    .isSameAs(Logger.getLogger(StubbedGlobalLogger.class));
            }
        }
    }
}
