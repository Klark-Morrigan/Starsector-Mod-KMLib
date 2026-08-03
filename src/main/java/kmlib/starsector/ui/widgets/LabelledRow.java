package kmlib.starsector.ui.widgets;

import kmlib.starsector.ui.text.TextSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A label with a slot to either side of it - the content every stack of icon-label-value rows is built
 * out of, named once so that a measurement or a paint can be written against this rather than against
 * whichever row model happens to carry it. A widget composes it and adds only what is genuinely its
 * own, so the parts they share stay one shape rather than three that cannot be handed to each other.
 *
 * <p>A label is a list of {@link TextSpan} runs read as one sentence: each run starts where the one
 * before it measured out, so a line can pick one stretch of itself out in another colour without that
 * stretch becoming a column. It is required and never empty - a row with no label is not a row, and
 * that floor is what stops the model dissolving into a bag of optional parts with no centre. A
 * one-colour label is the list of one that {@link #createRow} builds, so a caller that never needs a
 * second colour never sees the list.
 *
 * <p>Each flank is a {@link RowSlot}, so what a row leads and trails with is one component either side
 * whatever kind of thing it turns out to be, and a flank it does not fill is a slot like any other.
 * Why that is a sealed set rather than a field per kind is {@code RowSlot}'s own argument.
 *
 * <p>The two rules a row lays its content by are kept apart deliberately. The label's runs <em>flow</em>
 * - they are charged to no column and read as one line. The slots are <em>columns</em> - each reserved
 * at one width across a whole stack, so the labels between them line up. Flattened into a single list
 * of elements, a label's run and a value would become the same kind of thing, and they are not.
 *
 * <p>How wide those columns come out, how far each sits from the label, and where any of it lands are
 * facts about a stack of rows rather than about any one row, so they stay with whatever lays the stack
 * out. A row states what it carries and nothing about where it goes.
 *
 * @param leadingRowSlot  what the row leads with, or {@link RowSlot#EMPTY} when it leads with nothing
 * @param labelTextSpans  the label's runs in reading order, each in the colour it draws in before any
 *                        opacity fade the host applies; never empty
 * @param trailingRowSlot what the row trails with, or {@link RowSlot#EMPTY} when it trails with nothing
 */
public record LabelledRow(
        RowSlot leadingRowSlot,
        List<TextSpan> labelTextSpans,
        RowSlot trailingRowSlot) {

    /**
     * Holds the label to the floor every label is held to ({@link #copyLabelTextSpans}) and rejects a
     * null slot, at construction, where the caller that built the row is still on the stack. An unfilled
     * slot is {@link RowSlot#EMPTY} rather than null, so nothing below has to read a missing slot and an
     * empty one as the same thing - and a null would otherwise surface inside a measurement or a draw
     * call, well past the point that could say which row was meant.
     */
    public LabelledRow {
        Objects.requireNonNull(leadingRowSlot, "leadingRowSlot");
        Objects.requireNonNull(trailingRowSlot, "trailingRowSlot");
        labelTextSpans = copyLabelTextSpans(labelTextSpans);
    }

    /**
     * Builds the plainest row content there is: a one-run label with neither slot filled. What flanks
     * the label and what else the label says are each layered on with a refinement below, so a caller
     * states what its row <em>has</em> and never spells out the absences.
     *
     * @param labelTextSpan the label's one run and the colour it draws in
     * @return the bare content
     */
    public static LabelledRow createRow(TextSpan labelTextSpan) {
        return new LabelledRow(
            RowSlot.EMPTY,
            List.of(labelTextSpan),
            RowSlot.EMPTY);
    }

    /**
     * Returns a copy leading with {@code leadingRowSlot} - the image, tick, or other small element
     * drawn in the column before the label.
     *
     * @param leadingRowSlot what the row leads with
     * @return otherwise-identical content leading with that slot
     */
    public LabelledRow leadsWith(RowSlot leadingRowSlot) {
        return new LabelledRow(leadingRowSlot, labelTextSpans, trailingRowSlot);
    }

    /**
     * Returns a copy whose label runs on into {@code runTextSpan} - the next stretch of the same
     * sentence, picked out in its own colour while what came before it stays as it was. Applied twice,
     * a line reads in three colours at no extra cost to the model.
     *
     * @param runTextSpan the text continuing the label and the colour it draws in
     * @return otherwise-identical content whose label carries that run last
     */
    public LabelledRow continuesWith(TextSpan runTextSpan) {
        return new LabelledRow(
            leadingRowSlot,
            appendLabelTextSpan(labelTextSpans, runTextSpan),
            trailingRowSlot);
    }

    /**
     * Returns a copy trailing with {@code trailingRowSlot} - the value, marker, or other small element
     * drawn in the column past the label.
     *
     * @param trailingRowSlot what the row trails with
     * @return otherwise-identical content trailing with that slot
     */
    public LabelledRow trailsWith(RowSlot trailingRowSlot) {
        return new LabelledRow(leadingRowSlot, labelTextSpans, trailingRowSlot);
    }

    /**
     * The whole label as one line: its runs' text in reading order, joined. For a surface that lays a
     * label in a single draw and charges it a single measurement, where each run's own anchor is never
     * worked out and so the runs read as the one sentence they already are.
     *
     * <p>The colours do not survive the join, since one line drawn once draws in one colour. A surface
     * that picks a run out in its own colour reads the runs themselves rather than this.
     *
     * @return the label's runs joined into one line
     */
    public String resolveLabelText() {
        var labelText = new StringBuilder();
        for (var textSpan : labelTextSpans) {
            labelText.append(textSpan.text());
        }
        return labelText.toString();
    }

    /**
     * Returns {@code labelTextSpans} with {@code runTextSpan} appended - the label's runs read as one
     * sentence, so a run is added to what is already there rather than replacing it, and a second colour
     * never costs a caller the first. Held here beside the floor so that everything carrying a label
     * continues one the same way.
     *
     * @param labelTextSpans the label's runs so far, left as they are
     * @param runTextSpan    the run continuing the label
     * @return the runs with that one last, for the caller's own constructor to hold to the floor
     */
    static List<TextSpan> appendLabelTextSpan(List<TextSpan> labelTextSpans, TextSpan runTextSpan) {
        var continuedTextSpans = new ArrayList<>(labelTextSpans);
        continuedTextSpans.add(runTextSpan);
        return continuedTextSpans;
    }

    /**
     * Copies a label's runs and rejects an empty or null-bearing one, where the caller that built it is
     * still on the stack. The floor a label is held to lives here rather than at each thing that carries
     * one, so a line whose label is all it holds cannot be held to a looser rule than a line with slots
     * around it.
     *
     * <p>A label with no runs at all is not a line, and a null run otherwise surfaces inside a
     * measurement or a draw call, well past the point that could say which line was meant.
     *
     * @param labelTextSpans the label's runs in reading order
     * @return an immutable copy of the runs
     */
    static List<TextSpan> copyLabelTextSpans(List<TextSpan> labelTextSpans) {
        Objects.requireNonNull(labelTextSpans, "labelTextSpans");
        var copiedTextSpans = List.copyOf(labelTextSpans);
        if (copiedTextSpans.isEmpty()) {
            throw new IllegalArgumentException("labelTextSpans must carry at least one run");
        }
        return copiedTextSpans;
    }
}
