package kmlib.starsector.ui.controls;

/**
 * What the pointer is doing to a panel's body strip, carried as one value: how far onto its hovered look
 * each cell stands, and how far through its press lift each one is. They travel together because both are
 * resolved by one owner against the one placement being drawn, off one read of the cursor, so a consumer
 * that could hand over one without the other could hand over two readings of different frames.
 *
 * <p>Each half is still asked per strip position and then per cell, so the pairing this bundles does not
 * loosen the two ints a slot is made of; what is one value here is the reading, not the treatment.
 */
public record BodyInteractionSources(
    BodyHoverSource bodyHovers,
    BodyPressSource bodyPresses) {

    /**
     * A body with nothing happening to it: no cell hovered and none pressed, so every control paints the
     * settled look its spec names. What a consumer drawing a panel without an animator behind it passes.
     */
    public static final BodyInteractionSources RESTING = new BodyInteractionSources(
        BodyHoverSource.createRestingHoverSource(),
        BodyPressSource.createRestingPressSource());

    /**
     * Both channels of the control standing at {@code controlIndex}, bound into the pair its own paint pass
     * reads. Here rather than at the strip walk that wants it, so the two halves are picked out of the body
     * at one place - a walk asking each half for itself is a walk that could ask them for different
     * positions, which would light one control and lift another.
     *
     * @param controlIndex the control's position in the drawn body strip, top to bottom
     * @return that control's own hover and press channels
     */
    public ControlInteractionSources resolveControlInteractionSourcesAt(int controlIndex) {
        return new ControlInteractionSources(
            bodyHovers.resolveControlHoverSourceAt(controlIndex),
            bodyPresses.resolveControlPressSourceAt(controlIndex));
    }
}
