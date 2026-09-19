package kmlib.starsector.ui.controls.specs;

/**
 * Where a control tells the host that built it which of its cells the pointer is on - the hover's
 * counterpart to {@link ControlAction}. A framework input listener resolves the hovered cell against the
 * placement being drawn and calls {@link #reportHoveredCell} as that answer changes, without learning what
 * the report means: one host lights what picking that row would show, another takes no report at all.
 *
 * <p>Reported on change rather than every frame, and with {@link #NO_CELL_HOVERED} as the pointer leaves,
 * so a host holds what it was last told rather than re-deciding it per frame - and is always told when to
 * let go of it.
 *
 * <p>It is not what a paint pass reads. {@link ControlHoverSource} is that: a fraction per cell saying how
 * far the cell has travelled onto its hovered look, resolved by whatever owns the panel's live state and
 * drawn by the widget. This one runs the other way - out to whoever supplied the control - and carries
 * which cell rather than how far.
 *
 * <p>Cells are numbered as the hit resolver numbers them - a segment index for a segmented control, {@link
 * ControlSpec#SINGLE_CELL} for a whole-row one - so the cell reported is the cell a press would land on,
 * both being the same answer.
 */
@FunctionalInterface
public interface ControlHoverReport {

    /** What is reported as the pointer leaves: it is on no cell of the control. */
    Integer NO_CELL_HOVERED = null;

    /** A report nobody is listening to, for a control whose host wants none - every reading dropped. */
    ControlHoverReport NONE = hoveredCell -> {
    };

    /**
     * Takes which cell of the control the pointer has come onto, or that it has left the control.
     *
     * @param hoveredCell the hovered cell - a segment index, or {@link ControlSpec#SINGLE_CELL} for a
     *                    whole-row control - or {@link #NO_CELL_HOVERED} when the pointer is on none of it
     */
    void reportHoveredCell(Integer hoveredCell);
}
