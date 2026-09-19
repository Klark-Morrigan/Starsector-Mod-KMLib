package kmlib.starsector.ui.input;

import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.specs.InteractiveSpec;
import kmlib.starsector.ui.controls.specs.ReselectBehaviour;

/**
 * Whether a press on an already-resolved cell reaches the control's action, and the firing of it - the
 * whole of a press's narrowing, in one place, downstream of {@link ControlHitResolver}.
 *
 * <p>Apart from the resolver rather than beside it inside one class, because the two answer different
 * questions and the difference is the reason the panel's hovers work: which cell a point is over is a fact
 * about geometry that every reader shares, while whether pressing it would <em>do</em> anything is the
 * press path's alone. Folded together, a hover reading that answer would leave an inert cell dark - a tabs
 * row's lit tab, a plain option pair's lit segment - for exactly as long as it is the one selected.
 *
 * <p>Stateless and owned by no panel, like the resolver above it: a body press reaches it through the
 * panel's own controller, and a tab press through the tab panel's, each having resolved its own cell first.
 */
final class ControlActivation {

    private ControlActivation() {
    }

    /**
     * Fires the action of an already-resolved cell when the control's {@link ReselectBehaviour} says that
     * cell is worth acting on, and reports the cell that fired. A segmented control's lit segment is inert
     * unless its reselect fires on a re-pick - a plain option pair and a tabs row swallow it, a deselectable
     * picker and a re-firing selector do not - and every other cell acts.
     *
     * @param control      the laid-out control the cell belongs to
     * @param resolvedCell the cell a resolver reported under the point, or {@code null} for none
     * @return the cell that fired, or {@code null} when nothing acted
     */
    static Integer activateCellIfActionable(Control control, Integer resolvedCell) {
        if (resolvedCell == ControlHitResolver.NO_CELL_RESOLVED) {
            return ControlHitResolver.NO_CELL_RESOLVED;
        }
        // Only an Interactive spec ever resolves to a cell, so this cannot fail once one came back; it is
        // how the action is reached without a cast.
        if (!(control.spec() instanceof InteractiveSpec interactive)
                || !isActionableCell(interactive, resolvedCell)) {
            return ControlHitResolver.NO_CELL_RESOLVED;
        }
        interactive.action().activateCell(resolvedCell);
        return resolvedCell;
    }

    // Whether a press on an already-resolved cell reaches the control's action. Only a segmented control
    // narrows: its lit segment is inert unless the reselect it carries fires on a re-pick, which is the
    // standard radio rule and what makes re-clicking a vanilla tab strip's active tab do nothing. A
    // single-cell checkbox or toggle has no lit segment to re-pick, so every hit on it acts.
    private static boolean isActionableCell(InteractiveSpec control, int resolvedCell) {
        if (!control.isSegmented()) {
            return true;
        }
        return control.reselectBehaviour().firesOnReselect()
            || resolvedCell != control.selectedIndex();
    }
}
