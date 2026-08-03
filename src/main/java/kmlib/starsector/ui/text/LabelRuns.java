package kmlib.starsector.ui.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A label's runs read as one sentence: the floor every label is held to, how a run is added to one, the
 * single line the runs read as, and where they measure out to when laid side by side. Named once because
 * more than one kind of content carries a label - a row of a stack, a control of a strip - and how runs
 * compose is a fact about runs rather than about whichever content happens to hold them.
 *
 * <p>Runs <em>flow</em>: each starts a word gap past where the one before it measured out, and none of
 * them is charged to a column. That is what parts them from the slots flanking a label, which are columns
 * reserved at one width across a whole stack so the labels between them line up. A surface that laid runs
 * as columns would align the second colour of every line, which is not what picking a stretch of a
 * sentence out means.
 *
 * <p>A label carries at least one run and no null one, checked where the label is built rather than where
 * it is drawn: content with no runs at all is not a line, and a null run otherwise surfaces inside a
 * measurement or a draw call, well past the point that could say which line was meant.
 */
public final class LabelRuns {

    // The gap between two drawn runs of one label, in UI units. A word space rather than a column, so it
    // is charged only where one run actually follows another - and held here rather than per surface,
    // since a strip and a tooltip spacing the same runs differently would read as two different rules.
    private static final float RUN_GAP = 6f;

    private LabelRuns() {
    }

    /**
     * Returns {@code labelTextSpans} with {@code runTextSpan} appended - the runs read as one sentence,
     * so a run is added to what is already there rather than replacing it, and a second colour never
     * costs a caller the first.
     *
     * @param labelTextSpans the label's runs so far, left as they are
     * @param runTextSpan    the run continuing the label
     * @return the runs with that one last, for the caller's own constructor to hold to the floor
     */
    public static List<TextSpan> appendRun(List<TextSpan> labelTextSpans, TextSpan runTextSpan) {
        var continuedTextSpans = new ArrayList<>(labelTextSpans);
        continuedTextSpans.add(runTextSpan);
        return continuedTextSpans;
    }

    /**
     * Copies a label's runs and rejects an empty or null-bearing one, where the caller that built the
     * label is still on the stack. The floor lives here rather than at each thing that carries a label,
     * so a control's own label cannot be held to a looser rule than a row's.
     *
     * @param labelTextSpans the label's runs in reading order
     * @return an immutable copy of the runs
     */
    public static List<TextSpan> copyRuns(List<TextSpan> labelTextSpans) {
        Objects.requireNonNull(labelTextSpans, "labelTextSpans");
        var copiedTextSpans = List.copyOf(labelTextSpans);
        if (copiedTextSpans.isEmpty()) {
            throw new IllegalArgumentException("labelTextSpans must carry at least one run");
        }
        return copiedTextSpans;
    }

    /**
     * Where each of a label's runs sits relative to the label's own left edge, and how wide the runs come
     * to together. One walk, returned as a pair, because a caller re-deriving either half from the other
     * would be re-deciding the gap rule - and a placement disagreeing with the width its host was sized
     * to is exactly the drift a measurement exists to prevent.
     *
     * <p>A run with nothing to draw is charged neither gap nor width and anchors where its predecessor
     * ended, so a caller assembling a run from parts and coming up blank gets the line it would have had
     * without it rather than a gap reserved in front of no glyphs.
     *
     * @param labelTextSpans the label's runs in reading order
     * @param measurer       the width measurement already bound to the face the label draws in
     * @return each run's offset from the label's left edge, and the width the runs occupy together
     */
    public static LabelRunOffsets measureRunOffsets(
            List<TextSpan> labelTextSpans,
            StyledSpanMeasurer measurer) {

        var runOffsetXs = new ArrayList<Float>(labelTextSpans.size());
        var runsWidth = 0f;
        var hasDrawnRun = false;

        for (var labelTextSpan : labelTextSpans) {
            if (!labelTextSpan.hasText()) {
                runOffsetXs.add(runsWidth);
                continue;
            }
            if (hasDrawnRun) {
                runsWidth += RUN_GAP;
            }
            runOffsetXs.add(runsWidth);
            runsWidth += (float) measurer.measureSpanWidth(labelTextSpan);
            hasDrawnRun = true;
        }
        return new LabelRunOffsets(List.copyOf(runOffsetXs), runsWidth);
    }

    /**
     * The whole label as one line: its runs' text in reading order, joined. For a surface that lays a
     * label in a single draw and charges it a single measurement, where each run's own anchor is never
     * worked out and so the runs read as the one sentence they already are.
     *
     * <p>The colours do not survive the join, since one line drawn once draws in one colour. A surface
     * that picks a run out in its own colour reads the runs themselves rather than this.
     *
     * @param labelTextSpans the label's runs in reading order
     * @return the runs joined into one line
     */
    public static String resolveLineText(List<TextSpan> labelTextSpans) {
        var lineText = new StringBuilder();
        for (var textSpan : labelTextSpans) {
            lineText.append(textSpan.text());
        }
        return lineText.toString();
    }

    /**
     * One label's runs measured out from the label's own left edge: where each run starts, and how wide
     * they come to together.
     *
     * @param runOffsetXs each run's offset from the label's left edge, in run order
     * @param runsWidth   the width the runs occupy together, gaps included
     */
    public record LabelRunOffsets(
        List<Float> runOffsetXs,
        float runsWidth) {
    }
}
