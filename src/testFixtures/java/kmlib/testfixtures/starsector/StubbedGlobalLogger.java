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
 * load order rather than by the suite, so the stub is owed regardless of what is under test. The
 * Starsector conventions' restricted-call gate holds it: {@code mockStatic(Global.class)} is made
 * here and nowhere else.
 *
 * <p>Answered the way the real call answers - the class's own log4j logger - rather than with a mock
 * or one logger named for this fixture. A recording appender attached to a production class's
 * logger then sees what that class wrote, which is how a suite reads a class's log - through
 * {@code LogAppenderFake} - rather than through a mock logger that only reaches the class if it
 * happened to be first loaded under this suite's stand-in.
 *
 * <p>Final class with a private constructor: fixture of static wiring, no instances.
 */
public final class StubbedGlobalLogger {

    private StubbedGlobalLogger() {
        // fixture of static wiring, no instances.
    }

    /**
     * Opens a stand-in for {@code Global} that answers the logger. A suite that needs the sector or
     * the settings as well stubs them on the stand-in this returns.
     *
     * <p>An arrangement holding several seams in a {@code StaticSeams} hands it this one to hold,
     * rather than opening {@code Global} there bare.
     *
     * @return the open stand-in, which the caller closes - as a try-with-resources, or from the
     *         teardown matching the setup it was made in
     */
    public static MockedStatic<Global> openGlobalAnsweringLoggers() {

        var globalMock = mockStatic(Global.class);

        globalMock
            .when(() -> Global.getLogger(any(Class.class)))
            .thenAnswer(call -> Logger.getLogger(call.<Class<?>>getArgument(0)));
        return globalMock;
    }
}
