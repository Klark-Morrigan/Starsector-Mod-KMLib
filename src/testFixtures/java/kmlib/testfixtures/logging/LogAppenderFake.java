package kmlib.testfixtures.logging;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Recording log4j appender that keeps what a class wrote to the game log instead
 * of letting it reach a file, so what a caller logged - and how often, and at
 * what level - can be read back rather than taken on trust.
 *
 * <p>Published as a fixture variant so every KM mod drives the game-log seam through one
 * shared double, in the same way {@code CommandOutputFake} serves the console
 * one. Logging is a static sink no mock can instrument, so a recording appender
 * attached for the length of one call is what stands in for it.
 *
 * <p>Attaching and detaching is {@link #captureLogOf}'s, not each caller's: an
 * appender left on a logger after a case ends goes on collecting whatever the
 * rest of the suite logs, and the next case reads a tally that is not its own.
 *
 * <p>So is the level, for the same reason and a sharper one. A logger's level is
 * process-global and inherited from whatever ancestor happens to carry one, so a
 * capture that did not pin it would report what a class wrote only while nothing
 * else in the JVM - another suite, or a {@code log4j} configuration found on the
 * classpath - had raised it. A guarded line then goes missing from a capture that
 * says nothing about levels, and the case reads as though the code chose not to
 * write it.
 */
public final class LogAppenderFake extends AppenderSkeleton {

    private final List<LoggingEvent> events = new ArrayList<>();

    /**
     * Runs {@code work} with this appender attached to the logger
     * {@code loggingClass} writes through, and hands back what it collected.
     *
     * <p>Everything the class writes is collected, whatever level it wrote at:
     * what a caller is asking is what the code said, and a level is the running
     * game's answer to how much of that a player wants rather than part of the
     * contract. A case about the level itself states it inside {@code work},
     * which then holds for the rest of the capture.
     *
     * @param loggingClass the class whose logger is being listened to
     * @param work         what to run while listening
     * @return the appender, holding the entries that arrived
     */
    public static LogAppenderFake captureLogOf(Class<?> loggingClass, Runnable work) {

        var appenderFake = new LogAppenderFake();
        var logger = Logger.getLogger(loggingClass);

        // Kept, not read off the effective level: what is put back has to be the
        // logger's own setting, and null - inherit from the ancestors - is one of
        // the settings it can have.
        var levelBeforeCapture = logger.getLevel();
        var wasAdditive = logger.getAdditivity();

        logger.setLevel(Level.ALL);

        // Off the root appenders for the length of the capture, so lines planted
        // here do not land in the run's console output beside the test results.
        logger.setAdditivity(false);
        logger.addAppender(appenderFake);
        try {
            work.run();
        } finally {
            logger.removeAppender(appenderFake);
            logger.setAdditivity(wasAdditive);
            logger.setLevel(levelBeforeCapture);
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
