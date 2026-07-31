package kmlib.starsector.ui.controls;

import java.util.List;

/**
 * Test-only builders for the {@link ControlSpec.VerticalTable} shapes the tests exercise but no host
 * builds: a label-only table and the single-column icon lists. Production ships only the six-arg
 * {@link ControlSpec.VerticalTable#iconList} (the one shape a host builds) and {@link
 * ControlSpec.VerticalTable#directionTable}; these convenience shapes live here so the tests keep their
 * terse call sites without widening the shipped factory surface.
 */
public final class VerticalTableSpecs {
    private VerticalTableSpecs() {
    }

    /**
     * Builds a label-only vertical table: one stacked option per label, no icons, values, or triangles,
     * in a single column, with a caller-chosen re-pick behaviour.
     *
     * @param labels        the option labels, top to bottom
     * @param selectedIndex the lit option's index, or {@link ControlSpec#NO_SELECTION}
     * @param action        what a click on an option does, keyed by the option index
     * @param reselect      what a click on the lit option does
     * @return the label-only vertical table spec
     */
    public static ControlSpec.VerticalTable buildPlainTable(
            List<String> labels,
            int selectedIndex,
            ControlAction action,
            ReselectBehaviour reselect) {
        return new ControlSpec.VerticalTable(
            labels,
            List.of(),
            List.of(),
            List.of(),
            selectedIndex,
            action,
            reselect,
            ControlSpec.SINGLE_COLUMN,
            false);
    }

    /**
     * Builds a single-column icon list with leading crests and no trailing column.
     *
     * @param labels        the option labels, top to bottom
     * @param iconPaths     the per-option icon paths, aligned to {@code labels} (a null draws none)
     * @param selectedIndex the lit option's index, or {@link ControlSpec#NO_SELECTION}
     * @param action        what a click on an option does, keyed by the option index
     * @return the single-column icon-list spec
     */
    public static ControlSpec.VerticalTable buildIconList(
            List<String> labels,
            List<String> iconPaths,
            int selectedIndex,
            ControlAction action) {
        return ControlSpec.VerticalTable.iconList(
            labels,
            iconPaths,
            List.of(),
            selectedIndex,
            action,
            ControlSpec.SINGLE_COLUMN);
    }

    /**
     * Builds a single-column icon table (crest, name, value) with a right-aligned value per row.
     *
     * @param labels         the option labels, top to bottom
     * @param iconPaths      the per-option icon paths, aligned to {@code labels} (a null draws none)
     * @param trailingLabels the per-option values, aligned to {@code labels} (a null draws none)
     * @param selectedIndex  the lit option's index, or {@link ControlSpec#NO_SELECTION}
     * @param action         what a click on an option does, keyed by the option index
     * @return the single-column icon-table spec
     */
    public static ControlSpec.VerticalTable buildIconList(
            List<String> labels,
            List<String> iconPaths,
            List<String> trailingLabels,
            int selectedIndex,
            ControlAction action) {
        return ControlSpec.VerticalTable.iconList(
            labels,
            iconPaths,
            trailingLabels,
            selectedIndex,
            action,
            ControlSpec.SINGLE_COLUMN);
    }
}
