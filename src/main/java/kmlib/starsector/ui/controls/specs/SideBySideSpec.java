package kmlib.starsector.ui.controls.specs;

import java.util.List;

/**
 * Two columns of controls laid side by side in one strip row - a left column and a right column,
 * each its own top-to-bottom run of controls. It exists because the strip otherwise stacks one
 * full-width row per control; this pairs two runs across one row so a selector can sit beside a
 * related block rather than above it. The layout sizes the group to its columns (as wide as the
 * left column, a gap, then the right, and as tall as the taller column) and places each column in
 * its own half, then flattens the group to its children's laid-out controls - so it is not drawn or
 * clicked as a unit and the renderer and input listener only ever see the ordinary controls inside
 * it, never this container.
 *
 * <p>It is not {@link InteractiveSpec} and carries no labels of its own - each child carries its own -
 * so it is measured through its two columns rather than a label. Its children are ordinary controls,
 * so a column holds no further group; nesting is neither needed nor laid out.
 *
 * @param leftColumn  the controls filling the left column, top to bottom
 * @param rightColumn the controls filling the right column, top to bottom
 */
public record SideBySideSpec(
    List<ControlSpec> leftColumn,
    List<ControlSpec> rightColumn) implements ControlSpec {

    /** Copies both columns defensively, so a later edit to a caller's list cannot mutate the spec. */
    public SideBySideSpec {
        leftColumn = List.copyOf(leftColumn);
        rightColumn = List.copyOf(rightColumn);
    }

    @Override
    public List<String> labels() {
        return List.of();
    }
}
