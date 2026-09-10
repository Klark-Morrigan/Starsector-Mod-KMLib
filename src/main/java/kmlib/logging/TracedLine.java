package kmlib.logging;

import java.util.Objects;

/**
 * A diagnostic line, paired with the reading that decides whether it is news.
 *
 * <p>Splitting the two is what keeps a report-it-when-it-changes trace from collapsing back into a
 * per-frame one. A line rich enough to diagnose from carries values that move on their own - a
 * widget's box while a list scrolls beneath the cursor, a panel's opacity part-way through a fade -
 * and a trace comparing whole lines counts every one of those as a change. The structural move it
 * was built to catch then arrives buried under thousands of near-copies of itself, which is the
 * failure the trace existed to prevent.
 *
 * <p>The key holds only what the line is about, so it stands still while nothing it answers for has
 * moved. The text carries the whole reading, exact values and all, as it stood at the moment the key
 * moved. Nothing is dropped from the log by keying a line this way - what changes is how often the
 * line is written, not what it says when it is.
 *
 * <p>A key is therefore a deliberate choice about what counts as the same news, not a cheaper hash
 * of the text: two readings sharing a key are held to be the same reading, and a fact left out of
 * the key is a fact whose movement alone will never be reported.
 *
 * @param changeKey what the line is about, compared against the last line reported to decide whether
 *                  to report this one
 * @param text      the line as it should be read, carrying the detail the key leaves out
 */
public record TracedLine(
    String changeKey,
    String text) {

    public TracedLine {

        Objects.requireNonNull(
            changeKey,
            "A traced line with no key could not be told apart from the one before it.");
        Objects.requireNonNull(
            text,
            "A traced line with no text would report nothing.");
    }
}
