package kmlib.starsector.ui.controls;

/**
 * Where a control's paint pass gets each of its cells' hover progress from: asked per cell, one fraction
 * per answer saying how far that cell has travelled onto its hovered look. The seam exists so a renderer
 * never reads the cursor - a cell is hovered because whatever owns the panel's live state says so,
 * resolved against the placement the panel was actually drawn at, not because a paint pass hit-tested a
 * mouse position it would have to fetch from the engine mid-draw.
 *
 * <p>A fraction rather than a boolean because a cell eases onto its hovered look and back off it, and
 * because more than one cell of a control can be part-way at once - the one the pointer has left is still
 * winding down while the one it reached is rising. That is why this is asked per cell rather than handed a
 * single hovered index: an index can name only the cell the pointer is on now.
 *
 * <p>Cells are numbered as the hit resolver numbers them - a segment index for a segmented control, {@link
 * ControlSpec#SINGLE_CELL} for a whole-row one - so the cell that lights is the cell a press would land
 * on, both being the same answer.
 */
@FunctionalInterface
public interface ControlHoverSource {

    /** The fraction a cell with no hover on it reads: fully off its hovered look, wearing its settled one. */
    float NOT_HOVERED = 0f;

    /**
     * Builds a source hovering no cell at all - what a control drawn without an animator behind it reports,
     * every cell painting the look its own state names. Named rather than left to each caller's own empty
     * lambda, so a control with no hover running says so in one recognisable way.
     *
     * @return a source answering {@link #NOT_HOVERED} for every cell
     */
    static ControlHoverSource createRestingHoverSource() {
        return cell -> NOT_HOVERED;
    }

    /**
     * How far the cell at {@code cell} has travelled onto its hovered look.
     *
     * @param cell the cell being asked about - a segment index, or {@link ControlSpec#SINGLE_CELL} for a
     *             whole-row control
     * @return its hover fraction, 0 fully off its hovered look and 1 fully on it
     */
    float resolveHoverFractionAt(int cell);
}
