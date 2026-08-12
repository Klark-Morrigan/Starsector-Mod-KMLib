package kmlib.starsector.ui.controls;

/**
 * Where a panel's paint pass gets the hover progress of its body strip from: asked for one control's
 * position in that strip, it answers the {@link ControlHoverSource} that control's own paint reads.
 *
 * <p>Two steps rather than one call taking a strip position and a cell together, because those are two
 * ints a caller can hand over crossed: the walk that draws the strip is the one thing that knows a
 * control's position, so it binds that half once and the widget below it passes only the cell it is
 * painting. Neither end ever holds both numbers loose.
 *
 * <p>Strip positions are numbered as the hit resolver numbers them - a control's place in the drawn body,
 * top to bottom - so the control that lights is the control a press would land on.
 */
@FunctionalInterface
public interface BodyHoverSource {

    /**
     * Builds a source hovering nothing in the body at all - what a panel drawn without an animator behind
     * it reports, every control painting its settled look. Named rather than left to each caller's own empty
     * lambda, so a panel with no hover running says so in one recognisable way.
     *
     * @return a source answering a resting {@link ControlHoverSource} for every strip position
     */
    static BodyHoverSource createRestingHoverSource() {

        // Minted once and answered for every position, a resting source having nothing to tell one control
        // from another - so a strip drawn without an animator does not mint one lambda per row per frame.
        var restingControlHovers = ControlHoverSource.createRestingHoverSource();

        return controlIndex -> restingControlHovers;
    }

    /**
     * The hover progress of the cells of the control standing at {@code controlIndex}.
     *
     * @param controlIndex the control's position in the drawn body strip, top to bottom
     * @return that control's own hover source
     */
    ControlHoverSource resolveControlHoverSourceAt(int controlIndex);
}
