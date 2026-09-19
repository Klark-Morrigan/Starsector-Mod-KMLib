package kmlib.starsector.ui.controls.specs;

import java.util.List;

/**
 * A column of mutually exclusive option cells stacked top to bottom, exactly one lit. The shape a set
 * of more than two or three options reads as: the same options laid across one row letter too narrow
 * to tell apart once the row is snapped to a body's width.
 *
 * <p>Distinct from {@link VerticalTableSpec}, which stacks rows holding their own parts - a crest, a
 * label, a trailing value. This is the plain stack: one label per cell and nothing else, which is
 * what an option list that happens to be long wants.
 *
 * @param labels        the option labels, top to bottom, in cell order
 * @param selectedIndex the lit option's index, or {@link #NO_SELECTION} when nothing is picked
 * @param action        what a click on an option does, keyed by the option index
 * @param reselect      what a re-pick of the lit cell does (inert for a set that always holds one,
 *                      deselect for a clearable one)
 */
public record VerticalRadioSpec(
    List<String> labels,
    int selectedIndex,
    ControlAction action,
    ReselectBehaviour reselect) implements RadioSpec {

    /** Copies the label list defensively, so a later edit to a caller's list cannot mutate the spec. */
    public VerticalRadioSpec {
        labels = List.copyOf(labels);
    }

    /**
     * Builds the plain stacked option column a host reaches for by default: a re-pick of the lit cell
     * inert, so the column holds one option once one is picked.
     *
     * @param labels        the option labels, top to bottom, in cell order
     * @param selectedIndex the lit option's index, or {@link #NO_SELECTION} when nothing is picked
     * @param action        what a click on an option does, keyed by the option index
     * @return the plain stacked radio spec
     */
    public static VerticalRadioSpec of(
            List<String> labels,
            int selectedIndex,
            ControlAction action) {

        return new VerticalRadioSpec(
            labels,
            selectedIndex,
            action,
            ReselectBehaviour.INERT);
    }

    /**
     * Returns a copy of this column handling a re-pick of its lit cell the given way - {@link
     * ReselectBehaviour#DESELECT} for a clearable selector, where a re-click of the active option
     * clears the column rather than leaving it lit.
     *
     * @param reselect what a re-pick of the lit cell does
     * @return an otherwise-identical column handling a re-pick that way
     */
    public VerticalRadioSpec handlesReselect(ReselectBehaviour reselect) {

        return new VerticalRadioSpec(
            labels,
            selectedIndex,
            action,
            reselect);
    }
}
