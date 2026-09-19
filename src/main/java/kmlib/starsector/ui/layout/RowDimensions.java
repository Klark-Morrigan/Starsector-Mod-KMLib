package kmlib.starsector.ui.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * How tall and how wide each row of a stack stands, in stack order top to bottom.
 *
 * <p>One value rather than two lists handed over in turn, because they are one reading of one run
 * of rows. Parted, they can arrive from different readings - two lists of different lengths, or the
 * heights of a run the widths were never measured from - and a stacker handed either has no way to
 * notice. Held together, the two are checked against each other once, where they are stated, so a
 * run that could not describe a stack is never built.
 *
 * @param rowHeights each row's height, top to bottom
 * @param rowWidths  each row's width, in the same order
 */
public record RowDimensions(
    List<Float> rowHeights,
    List<Float> rowWidths) {

    /** The dimensions of a run holding no rows, named so the empty case is met by name. */
    public static final RowDimensions EMPTY = new RowDimensions(List.of(), List.of());

    public RowDimensions {

        Objects.requireNonNull(rowHeights, "A run with no heights could not say how tall its rows stand.");
        Objects.requireNonNull(rowWidths, "A run with no widths could not say how wide its rows stand.");

        if (rowHeights.size() != rowWidths.size()) {
            throw new IllegalArgumentException(
                "A run states " + rowHeights.size() + " heights against " + rowWidths.size()
                    + " widths, so at least one row has no full size.");
        }
        rowHeights = List.copyOf(rowHeights);
        rowWidths = List.copyOf(rowWidths);
    }

    /**
     * @return how many rows the run holds
     */
    public int countRows() {

        return rowWidths.size();
    }

    /**
     * A run whose rows all stand the same height, taking their widths one each.
     *
     * @param rowHeight the height every row shares
     * @param rowWidths each row's width, top to bottom
     * @return the run, one row per width
     */
    public static RowDimensions createUniform(float rowHeight, List<Float> rowWidths) {

        Objects.requireNonNull(rowWidths, "A run with no widths could not say how wide its rows stand.");

        var rowHeights = new ArrayList<Float>(rowWidths.size());
        for (var index = 0; index < rowWidths.size(); index++) {
            rowHeights.add(rowHeight);
        }
        return new RowDimensions(rowHeights, rowWidths);
    }
}
