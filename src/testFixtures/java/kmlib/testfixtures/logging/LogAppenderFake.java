package kmlib.testfixtures.logging;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Recording log4j appender that keeps what a class wrote to the game log instead
 * of letting it reach a file, so what a caller logged - and how often, and at
 * what level - can be read back rather than taken on trust.
 *
 * <p>Shipped from KMLib so every KM mod drives the game-log seam through one
 * shared double, in the same way {@code CommandOutputFake} serves the console
 * one. Logging is a static sink no mock can instrument, so a recording appender
 * attached for the length of one call is what stands in for it.
 *
 * <p>Attaching and detaching is {@link #captureLogOf}'s, not each caller's: an
 * appender left on a logger after a case ends goes on collecting whatever the
 * rest of the suite logs, and the next case reads a tally that is not its own.
 */
public final class LogAppenderFake extends AppenderSkeleton {

    private final List<LoggingEvent> events = new ArrayList<>();

    /**
     * Runs {@code work} with this appender attached to the logger
     * {@code loggingClass} writes through, and hands back what it collected.
     *
     * @param loggingClass the class whose logger is being listened to
     * @param work         what to run while listening
     * @return the appender, holding the entries that arrived
     */
    public static LogAppenderFake captureLogOf(Class<?> loggingClass, Runnable work) {

        var appenderFake = new LogAppenderFake();
        var logger = Logger.getLogger(loggingClass);

        logger.addAppender(appenderFake);
        try {
            work.run();
        } finally {
            logger.removeAppender(appenderFake);
        }
        return appenderFake;
    }

    @Override
    public void close() {
        // Nothing is held open; what was collected stays readable after the
        // appender is detached.
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    /**
     * @return the entries that arrived, in order, for what was said as well as
     *         at which level
     */
    public List<LoggingEvent> getEvents() {
        return events;
    }

    /**
     * @return what was said, in order - the reading for a caller counting
     *         messages rather than inspecting them
     */
    public List<String> getMessages() {

        var messages = new ArrayList<String>(events.size());

        for (var event : events) {
            messages.add(String.valueOf(event.getMessage()));
        }
        return messages;
    }

    @Override
    protected void append(LoggingEvent event) {
        events.add(event);
    }
}
