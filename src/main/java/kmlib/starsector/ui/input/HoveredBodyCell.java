package kmlib.starsector.ui.input;

import kmlib.starsector.ui.sound.PointerArrivalTarget;

/**
 * The body cell a pointer is on: where it sits in the strip, and what kind of thing reaching it is. A
 * frame's reading of the body, and the whole of what the panel's parts need to answer that reading - the
 * fade is held against the slot, and the arrival sounds at the level its kind names.
 *
 * <p>The two travel as one value because a hovered cell has both or is not there at all. Held apart on
 * the reading, a slot with no kind beside it - or a kind with no slot - would compile, and the missing
 * half would only surface as a moment answered at a level nobody chose or not answered at all.
 *
 * <p>The kind is settled where the hit-test had the control in hand rather than carried on to whoever
 * sounds it, which is what keeps the widget out of the fade's key: {@link BodyCellSlot} stays the place
 * on the strip and learns nothing about what stands there. A reader handed the control instead would have
 * to ask what kind of thing it is, and the answer would then be given in as many places as there are
 * readers.
 *
 * @param slot          where on the strip the cell sits, and which cell of its control it is
 * @param arrivalTarget what kind of thing the player reached in reaching it, for the look to answer how
 *                      loudly
 */
public record HoveredBodyCell(
    BodyCellSlot slot,
    PointerArrivalTarget arrivalTarget) {
}
