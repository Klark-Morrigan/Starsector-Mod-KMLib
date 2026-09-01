package kmlib.starsector.ui.input;

import kmlib.starsector.ui.controls.ControlHoverReport;

import java.util.Objects;

/**
 * Tells a body control's host which of its cells the pointer is on, turning the reading a panel takes every
 * frame into a report per change. A host answering a hover wants the moment the answer became this cell,
 * not the same cell restated sixty times a second - and, above all, wants to hear the pointer leave, which
 * is the one thing a stream of readings never says out loud.
 *
 * <p>Keyed by the {@link BodyCellSlot}, the terms the reading arrives in and the terms the fades beside it
 * are held in. That is what makes a list scrolling under a still pointer re-report: the rows move, so the
 * row index under the cursor changes, so the slot changes. It is also the cost of keying by the place - a
 * strip rebuilt with different items in the same slots reports nothing, the pointer having stayed where it
 * was, so a host holding an item id keeps one that its own rebuild may have moved.
 *
 * <p>The channel a reading went out on is kept with it, so the leave reaches the same host that was told
 * about the arrival. It has to be kept rather than re-read: a host rebuilds its specs every frame, so by
 * the time the pointer leaves a control that control's spec is gone - and where the pointer leaves the
 * panel entirely there is no spec to read at all.
 *
 * <p>Crossing from one control to another reports twice - the cell to the control reached, and the leave to
 * the control left - while crossing between cells of one control reports once, the new cell superseding the
 * old on the same channel. A leave sent there too would be a host told it holds nothing an instant before
 * being told what it holds, which reads as a flicker in whatever it drives.
 */
final class BodyHoverReporter {

    // Which slot the host was last told about, or null for none. The reading rather than the report, so a
    // frame that resolves the same slot again is silent whichever spec is standing in it.
    private BodyCellSlot reportedSlot;

    // The channel that reading went out on, resting at NONE so a leave with nothing to clear is a call that
    // does nothing rather than a null check at each site.
    private ControlHoverReport reportedHoverReport = ControlHoverReport.NONE;

    /**
     * Reports this frame's reading where it differs from the last one, for the per-frame pass that resolved
     * it to call once it has the placement's answer. A reading equal to the last is dropped, which is what
     * makes this a report per change rather than per frame.
     *
     * @param hoveredCell the body cell the pointer is on this frame, or null when it is on none
     */
    void reportHoverChangeTo(HoveredBodyCell hoveredCell) {

        var hoveredSlot = hoveredCell == null
            ? null
            : hoveredCell.slot();

        if (Objects.equals(hoveredSlot, reportedSlot)) {
            return;
        }
        reportLeaveOfControlLeftBehind(hoveredSlot);

        reportedSlot = hoveredSlot;
        reportedHoverReport = hoveredCell == null
            ? ControlHoverReport.NONE
            : hoveredCell.hoverReport();

        if (hoveredCell != null) {
            reportedHoverReport.reportHoveredCell(hoveredSlot.cell());
        }
    }

    /**
     * Reports the leave and forgets what was reported, for a panel that stops showing. A host left holding
     * the last cell it was told about would go on answering a hover over a strip that is no longer drawn,
     * and would never hear otherwise - the pointer leaves nothing when the panel leaves under it.
     *
     * <p>Nothing is reported where nothing was, so a panel stood down twice tells its host once.
     */
    void reportHoverCleared() {
        reportLeaveToHostLastTold();
        reportedHoverReport = ControlHoverReport.NONE;
        reportedSlot = null;
    }

    // Tells the control the pointer has just left that it holds the pointer no longer - unless the new
    // reading is on that same control, where the report that follows supersedes this one on the very
    // channel it would have gone out on.
    private void reportLeaveOfControlLeftBehind(BodyCellSlot hoveredSlot) {
        if (isOnControlAlreadyReported(hoveredSlot)) {
            return;
        }
        reportLeaveToHostLastTold();
    }

    // The leave itself, going out on the channel the last reading did. Both the crossing above and a panel
    // standing down send it, and one place to send it from is one place the resting NONE swallows it - a
    // host that was never told anything hears nothing either way.
    private void reportLeaveToHostLastTold() {
        reportedHoverReport.reportHoveredCell(ControlHoverReport.NO_CELL_HOVERED);
    }

    // Whether this frame's reading is on the control the last report went to. Compared by the control's
    // place in the strip rather than by the channel it reported through, the channel being a fresh lambda
    // off every frame's rebuilt spec and so never equal to the one held from the frame before.
    private boolean isOnControlAlreadyReported(BodyCellSlot hoveredSlot) {
        return reportedSlot != null
            && hoveredSlot != null
            && reportedSlot.controlIndex() == hoveredSlot.controlIndex();
    }
}
