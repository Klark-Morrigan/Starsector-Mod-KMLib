package kmlib.starsector.ui.input;

import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlHoverReport;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.sound.PointerArrivalTarget;

/**
 * One body control and the slot of the strip a point landed on - what a hit-test over a panel's body
 * answers with, neither half being any use alone: the control names what would act and the slot names
 * where on the strip, so a reader handed only one has to walk the strip a second time to recover the
 * other. A press reads the control to reach its action; a hover reads the slot to key the fade it holds
 * the cell at, and the control for what kind of thing the pointer has just reached.
 *
 * <p>The slot is a value because a resolved hit has one by definition. "No cell" is the absence of this
 * record entirely - a null in place of the pair - so a hit that landed on nothing cannot be held at all,
 * and the miss is checked once where the walk ends rather than again on a field.
 *
 * @param control the laid-out body control the point is over
 * @param slot    where in the strip that control sits, and which of its cells is under the point
 */
record ResolvedBodyCell(
    Control control,
    BodyCellSlot slot) {

    /**
     * What kind of thing the player reached in reaching this cell, for a look to answer how loudly. Read
     * off the control while it is in hand, so what a hover carries on is the answer rather than the widget
     * it was taken from.
     *
     * <p>The split is the one the hit-test already makes. A control that resolves a segment is a row of
     * things alike, and a sweep down the strip crosses several of them on its way somewhere; a control hit
     * anywhere on its row has one answer to give, so reaching it is the player having aimed at it. It
     * cannot be read off the cell instead - a whole-row control's single cell and a row's first segment
     * are both index zero.
     *
     * @return the kind of thing this cell is, in the terms a look sets its levels in
     */
    PointerArrivalTarget resolveArrivalTarget() {
        return PanelController.isSegmentedControl(control)
            ? PointerArrivalTarget.LISTED_ITEM
            : PointerArrivalTarget.SINGLE_OPTION_CONTROL;
    }

    /**
     * Where this cell's control reports the hovered cell back to whoever built it, or {@link
     * ControlHoverReport#NONE} for a control that takes no report. Read off the control while the walk
     * still holds it, for the same reason the arrival target above is: what travels on is the answer, so
     * the reading a frame carries never has to hold the widget it was taken from.
     *
     * <p>Only an {@link ControlSpec.Interactive} control ever resolves to a cell, so the chrome arm is
     * unreachable through the walk and is what keeps this a read rather than a cast.
     *
     * @return the control's hover report
     */
    ControlHoverReport resolveHoverReport() {
        return control.spec() instanceof ControlSpec.Interactive interactive
            ? interactive.hoverReport()
            : ControlHoverReport.NONE;
    }
}
