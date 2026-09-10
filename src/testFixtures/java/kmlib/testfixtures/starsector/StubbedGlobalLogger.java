package kmlib.testfixtures.starsector;

import com.fs.starfarer.api.Global;

import org.apache.log4j.Logger;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

/**
 * A stand-in for {@code Global} that still hands out loggers, for a suite that has to mock the
 * game's static entry point for some other reason.
 *
 * <p>This is not optional wherever {@code Global} is mocked. A class whose static {@code LOG} field
 * is first resolved while the stand-in is open keeps whatever it was handed for the rest of the
 * JVM; left as the mock's unstubbed null, every later suite that logs through that class faults on
 * a line it never wrote - a failure that reads as belonging to whichever suite happened to run
 * after, and that comes and goes with the order they run in. Which classes those are is decided by
 * load order rather than by the suite, so the stub is owed regardless of what is under test.
 *
 * <p>Answered the way the real call answers - the class's own log4j logger - rather than with a mock
 * or one logger named for this fixture. A recording appender attached to a production class's
 * logger then sees what that class wrote, and a suite that needs a mock logger to verify against
 * stubs its own in place of this.
 *
 * <p>Final class with a private constructor: fixture of static wiring, no instances.
 */
public final class StubbedGlobalLogger {

    private StubbedGlobalLogger() {
        // fixture of static wiring, no instances.
    }

    /**
     * Opens a stand-in for {@code Global} that answers only the logger, for a suite that needs
     * nothing else of it.
     *
     * @return the open stand-in, which the caller closes - as a try-with-resources, or from the
     *         teardown matching the setup it was made in
     */
    public static MockedStatic<Global> openGlobalAnsweringLoggers() {

        var globalMock = mockStatic(Global.class);

        answerLoggersOn(globalMock);
        return globalMock;
    }

    /**
     * Makes an already open stand-in answer the logger, for a suite that opened one to stub the
     * sector or the settings.
     *
     * @param globalMock the open stand-in the caller owns and closes
     */
    public static void answerLoggersOn(MockedStatic<Global> globalMock) {

        globalMock
            .when(() -> Global.getLogger(any(Class.class)))
            .thenAnswer(call -> Logger.getLogger(call.<Class<?>>getArgument(0)));
    }
}
