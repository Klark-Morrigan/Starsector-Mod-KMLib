package kmlib.starsector.ui.input;

import kmlib.starsector.ui.controls.ControlHoverReport;
import kmlib.starsector.ui.sound.PointerArrivalTarget;

/**
 * The body cell a pointer is on: where it sits in the strip, what kind of thing reaching it is, and where
 * the control standing there wants that told. A frame's reading of the body, and the whole of what the
 * panel's parts need to answer that reading - the fade is held against the slot, the arrival sounds at the
 * level its kind names, and the host that built the control hears which of its cells the pointer reached.
 *
 * <p>The three travel as one value because a hovered cell has all of them or is not there at all. Held
 * apart on the reading, a slot with no kind beside it - or a kind with no slot - would compile, and the
 * missing half would only surface as a moment answered at a level nobody chose or not answered at all.
 *
 * <p>The kind and the report are both settled where the hit-test had the control in hand rather than
 * carried on to whoever spends them, which is what keeps the widget out of the fade's key: {@link
 * BodyCellSlot} stays the place on the strip and learns nothing about what stands there. A reader handed
 * the control instead would have to ask it what kind of thing it is and where it reports, and each answer
 * would then be given in as many places as there are readers.
 *
 * @param slot          where on the strip the cell sits, and which cell of its control it is
 * @param arrivalTarget what kind of thing the player reached in reaching it, for the look to answer how
 *                      loudly
 * @param hoverReport   where that control reports the cell under the pointer back to its host, {@link
 *                      ControlHoverReport#NONE} for a control that takes no report
 */
public record HoveredBodyCell(
    BodyCellSlot slot,
    PointerArrivalTarget arrivalTarget,
    ControlHoverReport hoverReport) {
}
