package kmlib.starsector.ui.input;

import kmlib.starsector.ui.controls.Control;

/**
 * One body control and the cell of it a point landed on - what a hit-test over a panel's body strip
 * answers with, neither half being any use alone: the control names what would act and the cell names
 * where on it, so a reader handed only one has to walk the strip a second time to recover the other.
 *
 * <p>The cell is a primitive because a resolved hit has one by definition. "No cell" is the absence of
 * this value entirely - a null in place of the pair - so a hit that landed on nothing cannot be held at
 * all, and the miss is checked once where the walk ends rather than again on the field.
 *
 * @param control the laid-out body control the point is over
 * @param cell    the cell of that control under the point
 */
record ResolvedBodyCell(
    Control control,
    int cell) {
}
