package kmlib.text;

/**
 * One column of a {@link TextTable}: how narrow it may get, and which side its
 * cells are padded on.
 *
 * <p>A floor rather than a width, because the width is what the widest cell
 * turns out to need. What the floor is for is a table read against the one
 * before it: a column that closed up around a run of small numbers would put
 * every column after it somewhere else, and two captures whose columns sit in
 * different places cannot be compared by eye.
 *
 * <p>Which side the padding goes is what tells a name from a number: text reads
 * from the left and is padded after it, while digits of the same magnitude only
 * line up when the padding goes before them.
 */
public final class TextTableColumn {

    private final int floorWidth;
    private final boolean isPaddedOnTheLeft;

    private TextTableColumn(int floorWidth, boolean isPaddedOnTheLeft) {
        this.floorWidth = floorWidth;
        this.isPaddedOnTheLeft = isPaddedOnTheLeft;
    }

    /**
     * A column whose cells start at its left edge - what a name, a label or any
     * other text reads as.
     *
     * @param floorWidth how narrow the column may get, whatever its cells hold
     * @return the column so described
     */
    public static TextTableColumn alignCellsLeft(int floorWidth) {
        return new TextTableColumn(floorWidth, false);
    }

    /**
     * A column whose cells end at its right edge - what a number reads as, since
     * that is what puts one figure's digits over the next one's.
     *
     * @param floorWidth how narrow the column may get, whatever its cells hold
     * @return the column so described
     */
    public static TextTableColumn alignCellsRight(int floorWidth) {
        return new TextTableColumn(floorWidth, true);
    }

    int getFloorWidth() {
        return floorWidth;
    }

    boolean isPaddedOnTheLeft() {
        return isPaddedOnTheLeft;
    }
}
