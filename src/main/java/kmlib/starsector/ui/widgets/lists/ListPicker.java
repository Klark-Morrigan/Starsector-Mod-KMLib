package kmlib.starsector.ui.widgets.lists;

import java.util.List;

/**
 * What a picker offers: the selectable items and the vocabulary that ranks them. The two are
 * bundled because one consumer's items ranked by another's vocabulary is nonsense - carried apart,
 * every builder in the chain takes both and trusts its caller that the pair matches, whereas the
 * shared type parameter makes a mismatch a compile error. Same discipline as {@link ListSortModes}
 * bundling the modes with the mode a stored key falls back to, one level up.
 *
 * <p>It is parameterised on the item rather than on whatever numbers the item carries, so a
 * consumer whose lists rank by different metrics still hands one type over: each list arrives with
 * the only vocabulary that can read it, and the plumbing between the consumer's list and this
 * package stays free of any type naming what is being ranked.
 *
 * @param <T>       the consumer's own item type, drawn through {@link SelectableListItem} and
 *                  ranked by the bundled vocabulary's comparators
 * @param items     the selectable items; order here is immaterial since the sort reorders them for
 *                  display
 * @param sortModes the vocabulary that ranks {@code items} - the set a stored sort key resolves
 *                  against, and the mode it falls back to
 */
public record ListPicker<T extends SelectableListItem>(
    List<T> items,
    ListSortModes<T> sortModes) {

    /**
     * The offers-nothing case as a value rather than a null, so a consumer with no list to
     * spotlight answers with a picker like any other and no caller above it tests for absence.
     * A picker with no items draws no controls at all, which is what makes the empty case a
     * complete answer rather than a placeholder.
     *
     * <p>Its vocabulary is empty and its fallback mode null. That is sound only because an empty
     * picker is never ranked: a caller reads the item list first, finds it empty, and stops before
     * any stored sort is resolved. A caller that resolved the sort first would resolve against a
     * vocabulary with nothing to fall back to.
     *
     * @param <T> the item type the caller's empty picker stands in for; unconstrained, since an
     *            empty picker holds nothing of that type
     * @return a picker offering no items and no vocabulary
     */
    public static <T extends SelectableListItem> ListPicker<T> empty() {
        return new ListPicker<T>(
            List.of(),
            new ListSortModes<T>(
                List.of(),
                null));
    }
}
