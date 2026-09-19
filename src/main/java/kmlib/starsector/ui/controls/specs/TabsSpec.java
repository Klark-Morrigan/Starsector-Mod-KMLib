package kmlib.starsector.ui.controls.specs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A row of vanilla-styled tabs, exactly one lit - the panel's navigation, a control like any other
 * rather than special panel chrome. Each tab snaps to its own label-plus-shortcut width and is its
 * own hit target; a click fires the action with the tab's index. The {@code shortcuts} run parallel
 * to {@code labels}, so a shorter or empty list (or a null entry) leaves the trailing tabs hint-less.
 *
 * @param labels        the tab labels, left to right (an unlabelled tab passes an empty string)
 * @param shortcuts     the per-tab shortcut hints, aligned to {@code labels} (a null entry is a tab
 *                      with no hint); empty for a strip drawn without hints
 * @param selectedIndex the lit tab's index, or {@link #NO_SELECTION} when none is lit
 * @param action        what a click on a tab does, keyed by the tab index
 */
public record TabsSpec(
    List<String> labels,
    List<String> shortcuts,
    int selectedIndex,
    ControlAction action) implements InteractiveSpec {

    /** Copies the label and shortcut lists defensively; a null shortcut entry is a real "no hint". */
    public TabsSpec {
        labels = List.copyOf(labels);
        shortcuts = Collections.unmodifiableList(new ArrayList<>(shortcuts));
    }

    /**
     * The shortcut hint drawn at tab {@code index}, or "" when the tab has none - a shorter or empty
     * list, or a null entry. Returning "" rather than null lets the layout and renderer treat "no
     * hint" as a zero-width text without a null check at each site.
     *
     * @param index the tab index
     * @return the tab's shortcut hint, or "" when it has none
     */
    public String shortcutAt(int index) {
        if (index >= shortcuts.size() || shortcuts.get(index) == null) {
            return "";
        }
        return shortcuts.get(index);
    }

    @Override
    public boolean isSegmented() {
        return true;
    }

    @Override
    public ReselectBehaviour reselectBehaviour() {
        return ReselectBehaviour.INERT;
    }
}
