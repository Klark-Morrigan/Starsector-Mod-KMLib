package kmlib.starsector.ui.controls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A host's description of one body control: its {@link ControlKind}, the label(s) it carries, an
 * optional trailing label, and the {@code selectedIndex} of its currently lit cell. The layout
 * measures the labels to snap the control to its text and the renderer draws it in that state, while
 * the host that supplies the spec owns what the control means and does. Keeping the description
 * generic - state and all - lets any host compose a control strip without the layout or the renderer
 * learning the control's meaning.
 *
 * <p>The state rides along as data because the host, not the renderer, knows how to read it: a
 * generic renderer shared across hosts cannot map a bare {@code CHECKBOX} back to which setting it
 * reflects. The host reads its live value when it builds the spec (specs are rebuilt each frame), so
 * the carried state is current, and every control reduces to "which of my cells is lit" - a checkbox
 * and a toggle each have one cell (index 0) that is either lit (on) or {@link #NO_SELECTION}, a radio
 * has one lit cell among its segments.
 *
 * <p>The {@code action} is the same idea for input: the host attaches what a click does when it
 * builds the spec, so the framework input listener acts on a click without learning the meaning. The
 * layout and renderer ignore it (they only measure and draw), so a spec built purely to be laid out
 * carries {@link ControlAction#NONE}.
 *
 * <p>{@code alignment} and {@code canDeselect} refine a radio and are inert for the other kinds. The
 * alignment flows a radio's segments horizontally (the default a compact option pair reads best in)
 * or vertically (a stacked view selector). {@code canDeselect} lets a click on the already-lit
 * segment turn the whole control off ({@link #NO_SELECTION}) rather than being inert as a standard
 * always-selected radio's re-pick is.
 *
 * <p>{@code iconPaths} refines a vertical {@link ControlKind#RADIO} into an icon list and is empty
 * for a radio drawn without icons and for every other kind (just as {@code alignment} is inert for a
 * checkbox). It carries one icon texture path per option, in the same order as {@code labels}, with a
 * null entry for an option that draws no icon - so a vertical radio draws an icon beside each named
 * option without the layout or renderer learning what the icon is. A non-empty list is what tells the
 * layout to size rows for icons and the renderer to left-anchor the labels past them.
 *
 * <p>{@code trailingLabels} likewise refines a vertical {@link ControlKind#RADIO} into a table: one
 * right-aligned value per option, in the same order as {@code labels}, drawn flush against each row's
 * right edge (the picker's ranking number, or a sort selector's direction glyph). It is empty for a
 * radio with no per-option values and for every other kind, distinct from {@code trailingLabel} which
 * is a single caption drawn once after the whole control. A blank or absent entry leaves that row's
 * label spanning to the right inset.
 *
 * @param kind          which widget the control is
 * @param labels        the control's own label(s): one for a checkbox or toggle, one per option for
 *                      a radio (in segment order)
 * @param iconPaths     one leading-icon path per option for a vertical {@link ControlKind#RADIO}
 *                      drawn as an icon list (a null entry is an icon-less option); empty for a plain
 *                      radio and every other kind
 * @param trailingLabels one right-aligned per-option value for a vertical {@link ControlKind#RADIO}
 *                      drawn as a table (a blank or absent entry is a value-less option); empty for a
 *                      radio with no per-option values and every other kind
 * @param trailingLabel a label drawn after the control (a radio's caption), or blank for none;
 *                      reserved in the body width so it clears the border though it is not clicked
 * @param selectedIndex the index of the control's lit cell - the active radio segment, or 0 for a
 *                      checked checkbox / a lit toggle - or {@link #NO_SELECTION} when the control
 *                      is off and no cell is lit
 * @param action        what a click on the control does, keyed by which cell was hit; the host
 *                      supplies it, the input listener invokes it, the layout and renderer ignore it
 * @param alignment     the direction a radio's segments flow; ignored by the other kinds
 * @param canDeselect   whether a click on a radio's lit segment turns the control off; ignored by
 *                      the other kinds
 */
public record ControlSpec(ControlKind kind, List<String> labels, List<String> iconPaths,
        List<String> trailingLabels, String trailingLabel, int selectedIndex, ControlAction action,
        RadioAlignment alignment, boolean canDeselect) {
    /** {@code selectedIndex} value meaning the control is off - no cell is lit. */
    public static final int NO_SELECTION = -1;

    // The single cell of a checkbox or toggle: its whole row is one hit target, lit at index 0.
    private static final int SINGLE_CELL = 0;

    /**
     * Rejects the control shapes the layout and renderer cannot draw, so a mis-built spec fails at
     * construction rather than silently dropping state at paint time. The {@code create*} factories
     * only ever emit valid shapes; guarding the canonical constructor holds a caller that builds a
     * spec by hand to those same rules, so the illegal combinations are unrepresentable in practice.
     *
     * <p>The icon list and the per-option value column are read only for a vertical radio, so carrying
     * either on any other shape would go unseen. Deselection clears a radio's lit segment, so it has
     * no meaning on a kind that has no segments to clear.
     */
    public ControlSpec {
        var hasOptionColumns = !iconPaths.isEmpty() || !trailingLabels.isEmpty();
        if (hasOptionColumns
                && (kind != ControlKind.RADIO || alignment != RadioAlignment.VERTICAL)) {
            throw new IllegalArgumentException(
                    "iconPaths and trailingLabels are drawn only on a vertical radio, not on a "
                            + kind + " with " + alignment + " alignment");
        }
        if (canDeselect && kind != ControlKind.RADIO) {
            throw new IllegalArgumentException(
                    "canDeselect refines a radio's lit segment; it has no meaning on a " + kind);
        }
    }

    /**
     * Builds a control with no leading icons - every kind except the icon list, whose {@code
     * iconPaths} is therefore empty. The icon list uses {@link #createIconRadioList}, the one entry
     * that supplies a non-empty icon-path list.
     */
    public ControlSpec(ControlKind kind, List<String> labels, String trailingLabel,
            int selectedIndex, ControlAction action, RadioAlignment alignment, boolean canDeselect) {
        this(kind, labels, List.of(), List.of(), trailingLabel, selectedIndex, action, alignment,
                canDeselect);
    }

    /**
     * Builds a horizontal, always-selected control with the given action - the shape a checkbox,
     * toggle, or a standard option radio (Short/Full) takes. A stacked or deselectable radio uses
     * the full constructor to set its {@link RadioAlignment} and {@code canDeselect}.
     */
    public ControlSpec(ControlKind kind, List<String> labels, String trailingLabel,
            int selectedIndex, ControlAction action) {
        this(kind, labels, trailingLabel, selectedIndex, action, RadioAlignment.HORIZONTAL, false);
    }

    /**
     * Builds a spec with no action, for a layout-only or measurement context (the layout and
     * renderer never invoke the action). A clickable control uses a constructor that supplies its
     * own {@link ControlAction}.
     */
    public ControlSpec(ControlKind kind, List<String> labels, String trailingLabel,
            int selectedIndex) {
        this(kind, labels, trailingLabel, selectedIndex, ControlAction.NONE);
    }

    /**
     * Builds a checkbox: a single-cell {@link ControlKind#CHECKBOX} carrying its label, lit at cell 0
     * when {@code isOn} and {@link #NO_SELECTION} when off, with the given click action. Centralising
     * the on/off-to-{@code selectedIndex} mapping here keeps the "a checkbox is cell 0 lit or nothing"
     * convention in one place, so no host re-derives it at each call site.
     *
     * @param label  the checkbox's trailing label
     * @param isOn   whether the box is ticked
     * @param action what a click on the row does
     * @return the checkbox spec in its current lit state
     */
    public static ControlSpec createCheckbox(String label, boolean isOn, ControlAction action) {
        return new ControlSpec(ControlKind.CHECKBOX, List.of(label), "",
                isOn ? SINGLE_CELL : NO_SELECTION, action);
    }

    /**
     * Builds a caption row: a {@link ControlKind#LABEL} carrying its text as its single label, with
     * no lit cell and no action, since a caption is drawn but never clicked. Composing it here keeps
     * the LABEL shape - text in the first label, {@link #NO_SELECTION}, {@link ControlAction#NONE} -
     * in one place for any host that heads its controls with a caption.
     */
    public static ControlSpec createLabel(String text) {
        return new ControlSpec(ControlKind.LABEL, List.of(text), "", NO_SELECTION);
    }

    /**
     * Builds a divider row: a {@link ControlKind#DIVIDER} carrying no label, no lit cell, and no
     * action, since a rule is drawn but never clicked. The layout spans it to the strip's inner
     * width and the renderer draws the rule; composing it here keeps the "a divider has no label,
     * {@link #NO_SELECTION}, {@link ControlAction#NONE}" shape in one place for any host that parts
     * its sections with a rule.
     */
    public static ControlSpec createDivider() {
        return new ControlSpec(ControlKind.DIVIDER, List.of(), "", NO_SELECTION);
    }

    /**
     * Whether the option at {@code index} draws a leading icon: it does when this control's parallel
     * icon-path list holds a non-null entry there. A shorter icon-path list (or an empty one - a plain
     * radio, or any non-icon kind) leaves the option icon-less, so an out-of-range index reports no
     * icon. One rule for "does option i carry an icon", read by both the layout that sizes the row and
     * the renderer that draws it, so the two cannot disagree on which options show an icon.
     *
     * @param index the option index
     * @return true when the option at that index has a non-null icon path
     */
    public boolean hasIconAt(int index) {
        return index < iconPaths.size() && iconPaths.get(index) != null;
    }

    /**
     * The right-aligned value drawn at option {@code index}'s trailing edge, or an empty string when
     * this control has no value there - a shorter (or empty) trailing-label list, or a null entry.
     * Returning "" rather than null lets the layout and renderer treat "no value" as a zero-width text
     * without a null check at each call site, matching how {@link #hasIconAt} folds a missing icon into
     * a single rule read by both the layout and the renderer.
     *
     * @param index the option index
     * @return the option's trailing value, or "" when it has none
     */
    public String trailingLabelAt(int index) {
        if (index >= trailingLabels.size() || trailingLabels.get(index) == null) {
            return "";
        }
        return trailingLabels.get(index);
    }

    /**
     * Builds a plain vertical {@link ControlKind#RADIO}: one stacked option per label, the lit one at
     * {@code selectedIndex}, with no leading icons and no per-option values. It is the label-only
     * sibling of {@link #createIconRadioList} - the shape a stacked selector takes (a view selector, a
     * sort selector) - so a host need not spell out the empty icon/value lists and the vertical
     * alignment at each call site. {@code canDeselect} refines whether a click on the lit option turns
     * the whole control off: true for a selector that can clear to nothing, false for one that always
     * keeps a segment lit.
     *
     * @param labels        the option labels, top to bottom, in segment order
     * @param selectedIndex the lit option's index, or {@link #NO_SELECTION} when nothing is picked
     * @param action        what a click on an option does, keyed by the option index
     * @param canDeselect   whether a click on the lit option turns the control off
     * @return the vertical radio spec in its current lit state
     */
    public static ControlSpec createVerticalRadio(List<String> labels, int selectedIndex,
            ControlAction action, boolean canDeselect) {
        return new ControlSpec(ControlKind.RADIO, List.copyOf(labels), "", selectedIndex, action,
                RadioAlignment.VERTICAL, canDeselect);
    }

    /**
     * Builds a vertical, deselectable {@link ControlKind#RADIO} drawn as an icon list: one stacked
     * option per label, each drawing the icon at the matching {@code iconPaths} entry (null for an
     * icon-less option). It is a vertical radio like the view selector, distinguished only by carrying
     * icons - the non-empty {@code iconPaths} is what makes the layout size rows for icons and the
     * renderer left-anchor the labels. It is always vertical (an icon list only reads as a column) and
     * always deselectable (a click on the lit option turns the whole list off, the shape a
     * deselectable picker takes - re-picking clears it). The lists run in parallel, so the icon at
     * index {@code i} is drawn on the option labelled {@code labels.get(i)}; a shorter {@code
     * iconPaths} leaves the trailing options icon-less. The icon-path list is copied null-tolerantly
     * (a null entry is a real "no icon" value), so a caller may hand in a mutable list without the
     * spec aliasing it.
     *
     * @param labels        the option labels, top to bottom; must contain no null (an unlabelled
     *                      option passes an empty string)
     * @param iconPaths     the per-option icon paths, aligned to {@code labels}; a null entry draws
     *                      no icon on that option
     * @param selectedIndex the lit option's index, or {@link #NO_SELECTION} when nothing is picked
     * @param action        what a click on an option does, keyed by the option index
     * @return the icon-radio-list spec in its current lit state
     */
    public static ControlSpec createIconRadioList(List<String> labels, List<String> iconPaths,
            int selectedIndex, ControlAction action) {
        return createIconRadioList(labels, iconPaths, List.of(), selectedIndex, action);
    }

    /**
     * Builds the icon-radio list of {@link #createIconRadioList} with a right-aligned value on each
     * option row, so the list reads as a three-column table (crest, name, value). The {@code
     * trailingLabels} run parallel to {@code labels}, so the value at index {@code i} draws on the
     * option labelled {@code labels.get(i)}; a shorter or empty list leaves the trailing options
     * value-less, and a null entry is a real "no value". Both optional lists are copied null-tolerantly
     * so a caller may hand in mutable lists without the spec aliasing them.
     *
     * @param labels         the option labels, top to bottom; must contain no null (an unlabelled
     *                       option passes an empty string)
     * @param iconPaths      the per-option icon paths, aligned to {@code labels}; a null entry draws
     *                       no icon on that option
     * @param trailingLabels the per-option right-aligned values, aligned to {@code labels}; a null or
     *                       absent entry draws no value on that option
     * @param selectedIndex  the lit option's index, or {@link #NO_SELECTION} when nothing is picked
     * @param action         what a click on an option does, keyed by the option index
     * @return the icon-radio-list spec in its current lit state
     */
    public static ControlSpec createIconRadioList(List<String> labels, List<String> iconPaths,
            List<String> trailingLabels, int selectedIndex, ControlAction action) {
        return new ControlSpec(ControlKind.RADIO, List.copyOf(labels),
                Collections.unmodifiableList(new ArrayList<>(iconPaths)),
                Collections.unmodifiableList(new ArrayList<>(trailingLabels)), "", selectedIndex,
                action, RadioAlignment.VERTICAL, true);
    }
}
