package kmlib.starsector.ui.controls.specs;

/**
 * A set of mutually exclusive option cells, exactly one lit - laid across a row or stacked into a
 * column. A reader that acts on any radio names this rather than the two variants: the hit-test that
 * splits one into cells and the activation that reads its re-pick rule both do, so neither branches on
 * how the cells are arranged.
 *
 * <p>Two variants rather than one record carrying a direction, on the same rule the hierarchy above
 * follows: the state each draws differs. Cells laid across a row size to their labels and may carry a
 * caption past the last one; stacked cells are one column wide, so neither component exists to be set
 * wrongly on them.
 */
public sealed interface RadioSpec
    extends InteractiveSpec
    permits HorizontalRadioSpec, VerticalRadioSpec {

    /**
     * What a re-pick of the lit cell does - inert for a set that always holds one once picked,
     * deselect for a clearable one.
     *
     * @return the re-pick behaviour
     */
    ReselectBehaviour reselect();

    /**
     * A radio's cells are separately hit, whichever way the set is arranged.
     *
     * @return {@code true}
     */
    @Override
    default boolean isSegmented() {
        return true;
    }

    /**
     * Read off this interface rather than off each alignment, so a stacked set answers a re-pick
     * exactly as a laid-across one does - the behaviour is the control's, not its arrangement's.
     *
     * @return the re-pick behaviour the set carries
     */
    @Override
    default ReselectBehaviour reselectBehaviour() {
        return reselect();
    }
}
