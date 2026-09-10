package kmlib.testfixtures.starsector;

import com.fs.starfarer.api.Global;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

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
    }

    @Nested
    class AnswerLoggersOn {

        @Test
        void makesACallerOpenedStandInAnswerTheSameWay() {

            try (var globalMock = mockStatic(Global.class)) {

                StubbedGlobalLogger.answerLoggersOn(globalMock);

                assertThat(Global.getLogger(StubbedGlobalLoggerTest.class))
                    .isSameAs(Logger.getLogger(StubbedGlobalLoggerTest.class));
            }
        }
    }
}
