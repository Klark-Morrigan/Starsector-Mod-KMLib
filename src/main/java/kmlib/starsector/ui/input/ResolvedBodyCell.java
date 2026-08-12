package kmlib.starsector.ui.input;

import kmlib.starsector.ui.controls.Control;

/**
 * One body control and the slot of the strip a point landed on - what a hit-test over a panel's body
 * answers with, neither half being any use alone: the control names what would act and the slot names
 * where on the strip, so a reader handed only one has to walk the strip a second time to recover the
 * other. A press reads the control to reach its action; a hover reads the slot to key the fade it holds
 * the cell at.
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
}
