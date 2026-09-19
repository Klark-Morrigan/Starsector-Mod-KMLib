package kmlib.starsector.ui.controls;

import kmlib.starsector.ui.controls.specs.ControlSpec;

/**
 * Where a control's paint pass gets each of its cells' press progress from: asked per cell, one fraction
 * per answer saying how far through its press lift that cell currently stands. The seam exists so a
 * renderer holds no timing - a press is an event, and how long ago it landed belongs with whatever saw
 * it, not with a pass that runs once a frame and remembers nothing between them.
 *
 * <p>A second channel beside {@link ControlHoverSource} rather than a reading composed into it, the two
 * answering different questions about one cell: where the pointer is standing, and what it just did
 * there.
 *
 * <p>Cells are numbered as the hit resolver numbers them - a segment index for a segmented control,
 * {@link ControlSpec#SINGLE_CELL} for a whole-row one - so the cell that lifts is the cell the press
 * landed on, both being the same answer.
 */
@FunctionalInterface
public interface ControlPressSource {

    /** The fraction a cell with no press running on it reads: no lift at all, so it wears what it had. */
    float NOT_PRESSED = 0f;

    /**
     * Builds a source lifting no cell at all - what a control drawn without an animator behind it reports,
     * every cell painting whatever its own state and hover already give it. Named rather than left to each
     * caller's own empty lambda, so a control with no press running says so in one recognisable way.
     *
     * @return a source answering {@link #NOT_PRESSED} for every cell
     */
    static ControlPressSource createRestingPressSource() {
        return cell -> NOT_PRESSED;
    }

    /**
     * How far through its press lift the cell at {@code cell} currently stands.
     *
     * @param cell the cell being asked about - a segment index, or {@link ControlSpec#SINGLE_CELL} for a
     *             whole-row control
     * @return its press fraction, 0 with nothing running on it and 1 at the lift's peak
     */
    float resolvePressFractionAt(int cell);
}
