package kmlib.starsector.ui.input;

/**
 * Where a cell sits in a panel's body strip: which control down the body, and which cell across that
 * control. It travels as one value because neither number places a cell on its own - a cell index means
 * nothing without the control it indexes into, and two loose ints in a parameter list are two ints that
 * can be passed crossed over.
 *
 * <p>It is what a body hover is held against, so the answer to "what identifies a body control across a
 * rebuild" is: the place, not the widget standing in it. A host rebuilds its control specs every frame
 * from live values, so nothing in a body strip carries an identity that outlives the rebuild while a
 * slot does - but availability is not the argument. The case the eye actually watches is the pointer
 * standing still while the strip changes under it, when a layer switches or a filter clears. Keyed by
 * slot, the lift under the cursor runs on unbroken, which is what the player sees happening: something
 * is lit under the pointer and stays lit. Keyed by a widget identity, the fade would restart from rest
 * and the control under the cursor would dip dark for a traverse for no reason the player can see.
 *
 * <p>What it costs is the mirror of that, and it is deliberate: a fade winding down on a slot the
 * pointer has left is inherited by whatever the rebuild puts in that slot, so a control can show a brief
 * lift it never earned. It is bounded by one traverse, it lands away from the cursor, and it takes a
 * strip change inside that window to happen at all - a smaller price than a dip on the one control the
 * player is looking at.
 *
 * @param controlIndex the control's position in the drawn body strip, top to bottom
 * @param cell         the cell of that control - a segment index, or {@link
 *                     kmlib.starsector.ui.controls.ControlSpec#SINGLE_CELL} for a whole-row control
 */
public record BodyCellSlot(
    int controlIndex,
    int cell) {
}
