package kmlib.starsector.ui.widgets;

import kmlib.starsector.ui.text.TextSpan;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link RowSlotPainter} that records which method each slot handed itself to, and the text slots it
 * was given, instead of drawing anything.
 *
 * <p>Stands in for a drawing surface so the dispatch can be exercised without one - what a kind looks
 * like belongs to the surfaces, while which method a kind reaches belongs to the slots.
 */
final class RecordingRowSlotPainter implements RowSlotPainter {

    private final List<String> paintedKindNames = new ArrayList<>();
    private final List<TextSpan> paintedTextSpans = new ArrayList<>();

    /**
     * @return the kinds handed over, in the order they were painted
     */
    List<String> paintedKindNames() {
        return List.copyOf(paintedKindNames);
    }

    /**
     * @return the runs the text slots carried, in the order they were painted
     */
    List<TextSpan> paintedTextSpans() {
        return List.copyOf(paintedTextSpans);
    }

    @Override
    public void paintEmptySlot() {
        paintedKindNames.add("empty");
    }

    @Override
    public void paintImageSlot(RowSlot.Image imageSlot) {
        paintedKindNames.add("image");
    }

    @Override
    public void paintTextRunsSlot(RowSlot.TextRuns textRunsSlot) {
        paintedKindNames.add("textRuns");
    }

    @Override
    public void paintTextSlot(RowSlot.Text textSlot) {
        paintedKindNames.add("text");
        paintedTextSpans.add(textSlot.textSpan());
    }

    @Override
    public void paintTickSlot(RowSlot.Tick tickSlot) {
        paintedKindNames.add("tick");
    }

    @Override
    public void paintTriangleSlot(RowSlot.Triangle triangleSlot) {
        paintedKindNames.add("triangle");
    }
}
