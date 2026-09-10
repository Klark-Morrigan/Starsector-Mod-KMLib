package kmlib.logging;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the three things that make a repeated reading worth logging at all: a line is reported when it
 * says something new, the reading behind it is not even taken while nothing is listening, and a
 * reading that says which half of itself is the news is believed.
 *
 * <p>The second is the half a reader would not notice going wrong. A reading of this kind is a walk
 * of a live tree taken from inside a render pass that runs several times a frame, so a describe
 * called ahead of the level check would be the whole cost of the diagnostic paid by every player who
 * never turns it on.
 *
 * <p>The third is what separates a trace that reports a change from one that reports continuously. A
 * keyed reading whose text moves every frame while its key holds still must report once; keyed on
 * its text instead, such a trace reports on every frame and buries the move it was built to catch.
 */
class ChangedLineTraceTest {

    private static final String SUBJECT = "Map icon order";
    private static final String LINE = "slipstream, nebula, mapLayer";
    private static final String CHANGED_LINE = "nebula, mapLayer, slipstream";

    private static final String KEY = "under=[d0 Tab, d1 Panel]";
    private static final String CHANGED_KEY = "under=[d0 Tab]";

    // A logger that is taking DEBUG, which is the state every case here but one is about.
    private static Logger createListeningLoggerMock() {

        var logMock = mock(Logger.class);

        when(logMock.isDebugEnabled())
            .thenReturn(true);

        return logMock;
    }

    @Nested
    class CreateWholeLineTrace {

        @Test
        void createWholeLineTraceRefusesALoggerThatIsNull() {
            // The one way one of these is built wrongly, and it is not a typo: a holder is typically
            // a static field, so a logger declared below it in the same class is still null when it
            // arrives here. Held, that surfaces frames later as a null dereference inside a render
            // pass; refused, it surfaces at load, naming what was built without a logger.
            assertThatThrownBy(() ->
                    ChangedLineTrace.createWholeLineTrace(null, SUBJECT, () -> LINE))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void createWholeLineTraceReportsALineTheFirstTimeItIsSeen() {

            var logMock = createListeningLoggerMock();

            ChangedLineTrace.createWholeLineTrace(logMock, SUBJECT, () -> LINE)
                .traceWhenChanged();

            verify(logMock)
                .debug(SUBJECT + ": " + LINE);
        }

        @Test
        void createWholeLineTraceStaysSilentWhileTheLineIsUnchanged() {
            // The state the map is in nearly always: a still cursor over a map nobody has reopened,
            // read every frame and worth saying once.
            var logMock = createListeningLoggerMock();
            var trace = ChangedLineTrace.createWholeLineTrace(logMock, SUBJECT, () -> LINE);

            trace.traceWhenChanged();
            trace.traceWhenChanged();

            verify(logMock)
                .debug(SUBJECT + ": " + LINE);
        }

        @Test
        void createWholeLineTraceReportsAgainOnceTheLineChanges() {
            // The one moment the trace exists for, so suppressing repeats must not suppress the
            // change they surround.
            var logMock = createListeningLoggerMock();
            var reportedLines = new ArrayList<>(List.of(LINE, CHANGED_LINE));

            var trace = ChangedLineTrace.createWholeLineTrace(
                logMock,
                SUBJECT,
                () -> reportedLines.remove(0));

            trace.traceWhenChanged();
            trace.traceWhenChanged();

            verify(logMock)
                .debug(SUBJECT + ": " + LINE);
            verify(logMock)
                .debug(SUBJECT + ": " + CHANGED_LINE);
        }

        @Test
        void createWholeLineTraceStaysSilentWhenTheReadingHasNoLineToGive() {
            // Every reading behind one of these answers null off the screens it does not apply to,
            // which is an ordinary state rather than something to report.
            var logMock = createListeningLoggerMock();

            ChangedLineTrace.createWholeLineTrace(logMock, SUBJECT, () -> null)
                .traceWhenChanged();

            verify(logMock, never())
                .debug(SUBJECT + ": " + LINE);
        }

        @Test
        void createWholeLineTraceDescribesNothingWhileTheLogIsAboveDebug() {
            // What every player who never turns the trace on pays: nothing. The describe is a walk
            // of the live widget tree, so asking for one and discarding it would be the whole cost
            // of the diagnostic, several times a frame.
            var describeCount = new int[1];
            var logMock = mock(Logger.class);

            ChangedLineTrace.createWholeLineTrace(
                    logMock,
                    SUBJECT,
                    () -> {
                        describeCount[0]++;
                        return LINE;
                    })
                .traceWhenChanged();

            assertThat(describeCount[0])
                .isZero();
        }
    }

    @Nested
    class CreateKeyedLineTrace {

        @Test
        void createKeyedLineTraceRefusesALoggerThatIsNull() {

            assertThatThrownBy(() -> ChangedLineTrace.createKeyedLineTrace(
                    null,
                    SUBJECT,
                    () -> new TracedLine(KEY, LINE)))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void createKeyedLineTraceReportsTheTextRatherThanTheKey() {
            // The key is a decision about what counts as the same news; it is not what a reader is
            // meant to see, and printing it instead would cost the detail the split exists to keep.
            var logMock = createListeningLoggerMock();

            ChangedLineTrace.createKeyedLineTrace(logMock, SUBJECT, () -> new TracedLine(KEY, LINE))
                .traceWhenChanged();

            verify(logMock)
                .debug(SUBJECT + ": " + LINE);
            verify(logMock, never())
                .debug(SUBJECT + ": " + KEY);
        }

        @Test
        void createKeyedLineTraceStaysSilentWhileOnlyTheTextChanges() {
            // The whole point of keying. A reading whose text carries a box that moves under a still
            // cursor would otherwise report every frame, burying the crossings it exists to catch.
            var logMock = createListeningLoggerMock();
            var reportedTexts = new ArrayList<>(List.of(LINE, CHANGED_LINE));

            var trace = ChangedLineTrace.createKeyedLineTrace(
                logMock,
                SUBJECT,
                () -> new TracedLine(KEY, reportedTexts.remove(0)));

            trace.traceWhenChanged();
            trace.traceWhenChanged();

            verify(logMock)
                .debug(SUBJECT + ": " + LINE);
            verify(logMock, never())
                .debug(SUBJECT + ": " + CHANGED_LINE);
        }

        @Test
        void createKeyedLineTraceReportsAgainOnceTheKeyChanges() {

            var logMock = createListeningLoggerMock();
            var reportedLines = new ArrayList<>(List.of(
                new TracedLine(KEY, LINE),
                new TracedLine(CHANGED_KEY, CHANGED_LINE)));

            var trace = ChangedLineTrace.createKeyedLineTrace(
                logMock,
                SUBJECT,
                () -> reportedLines.remove(0));

            trace.traceWhenChanged();
            trace.traceWhenChanged();

            verify(logMock)
                .debug(SUBJECT + ": " + LINE);
            verify(logMock)
                .debug(SUBJECT + ": " + CHANGED_LINE);
        }

        @Test
        void createKeyedLineTraceStaysSilentWhenTheReadingHasNoLineToGive() {

            var logMock = createListeningLoggerMock();

            ChangedLineTrace.createKeyedLineTrace(logMock, SUBJECT, () -> null)
                .traceWhenChanged();

            verify(logMock, never())
                .debug(SUBJECT + ": " + LINE);
        }

        @Test
        void createKeyedLineTraceDescribesNothingWhileTheLogIsAboveDebug() {

            var describeCount = new int[1];
            var logMock = mock(Logger.class);

            ChangedLineTrace.createKeyedLineTrace(
                logMock,
                SUBJECT,
                () -> {
                    describeCount[0]++;
                    return new TracedLine(KEY, LINE);
                }
            ).traceWhenChanged();

            assertThat(describeCount[0])
                .isZero();
        }
    }

    @Nested
    class TracedLineConstructor {

        @Test
        void tracedLineRefusesAKeyThatIsNull() {
            // A null key would compare equal to the state before any line was reported, so the first
            // reading of a session would be swallowed rather than reported.
            assertThatThrownBy(() -> new TracedLine(null, LINE))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void tracedLineRefusesTextThatIsNull() {
            assertThatThrownBy(() -> new TracedLine(KEY, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class CreateWholeLine {

        @Test
        void createWholeLineKeysTheLineOnItsOwnText() {
            // The case where nothing in the reading moves on its own, so any difference is news.
            assertThat(TracedLine.createWholeLine(LINE))
                .isEqualTo(new TracedLine(LINE, LINE));
        }

        @Test
        void createWholeLineRefusesTextThatIsNull() {

            assertThatThrownBy(() -> TracedLine.createWholeLine(null))
                .isInstanceOf(NullPointerException.class);
        }
    }
}
