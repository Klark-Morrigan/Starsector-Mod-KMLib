package kmlib.starsector.ui.map.icons;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/**
 * What the decision has read, kept for the two lines that report it: the latest readings, which a
 * stand-down report needs to say what was seen on the way to it, and the readings of the latest lift
 * alone, which the line reporting the put-back carries.
 *
 * <p>The two are kept apart because neither serves the other's line. A map left up records a clear
 * reading on every advance, which soon pushes a lift out of the latest readings; and a wait longer
 * than their capacity would push out the removal the lift started with.
 */
final class ReseatReadingLog {

    // How many of the latest readings are kept for the stand-down report. A lift is at least two
    // advances - out, then back - so this holds every advance of the attempts the bound allows when
    // each drop shows at once, and the tail of them when the waits run longer, which is the end a
    // report wants either way.
    static final int RECENT_READINGS_CAPACITY = 12;

    // The latest readings, oldest first. The ordinary frame, with no map up, records nothing.
    private final Deque<ReseatReading> recentReadings = new ArrayDeque<>();

    // The latest lift's readings, from its removal to the advance that ended its wait. Bounded by the
    // wait bound, being restarted by every removal.
    private final List<ReseatReading> latestLiftReadings = new ArrayList<>();

    /**
     * The latest lift's readings, oldest first, as one bracketed list for a log line - empty brackets
     * before any lift.
     *
     * <p>A wait between the removal and the put-back is the widget still showing the icon on an
     * advance after the removal, which happens only when several advances share a rendered frame:
     * the campaign's speed-up. So the list says whether a lift ran under it, and that it waited.
     *
     * @return each reading of the lift as its advance, what was found and what was ordered
     */
    String describeLatestLiftReadings() {
        return joinReadings(latestLiftReadings);
    }

    /**
     * The latest readings, oldest first, as one bracketed list for a log line - empty brackets while
     * nothing has been read.
     *
     * @return at most {@link #RECENT_READINGS_CAPACITY} readings, each as its advance, what was found
     *         and what was ordered
     */
    String describeRecentReadings() {
        return joinReadings(recentReadings);
    }

    /**
     * Keeps a reading taken while a lift is under way, in the lift's record as well as among the latest.
     *
     * @param reading a reading of the lift after its removal
     */
    void recordLiftReading(ReseatReading reading) {

        recordReading(reading);
        latestLiftReadings.add(reading);
    }

    /**
     * Keeps a reading among the latest, dropping the oldest once the capacity is reached.
     *
     * @param reading what one advance found and ordered
     */
    void recordReading(ReseatReading reading) {

        recentReadings.addLast(reading);

        if (recentReadings.size() > RECENT_READINGS_CAPACITY) {
            recentReadings.removeFirst();
        }
    }

    /**
     * Starts the record of a new lift with the reading that removed the entity, forgetting the last.
     *
     * @param removal the reading that ordered the removal
     */
    void recordLiftStart(ReseatReading removal) {

        latestLiftReadings.clear();
        recordLiftReading(removal);
    }

    // One list for a log line, shared so the two descriptions word a reading alike.
    private static String joinReadings(Collection<ReseatReading> readings) {

        return readings
            .stream()
            .map(ReseatReading::describe)
            .collect(Collectors.joining(", ", "[", "]"));
    }
}
