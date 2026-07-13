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
 * <p>{@code alignment} and {@code reselect} refine a radio and are inert for the other kinds. The
 * alignment flows a radio's segments horizontally (the default a compact option pair reads best in)
 * or vertically (a stacked view selector). {@code reselect} says what a click on the already-lit
 * segment does - stay inert, turn the control off, or fire the action again on that option - via
 * {@link ReselectBehaviour}, so a picker that clears and a selector that flips a sub-state can share
 * one radio widget.
 *
 * <p>{@code trailingScale} is the size the per-option trailing values are measured and drawn at,
 * relative to the strip's body font size: 1.0 for a value that reads at the body size (the picker's
 * ranking numbers), less for a compact trailing column (a sort selector's direction letters, kept
 * smaller than the option names). Held here so the layout reserves and the renderer draws the
 * trailing column at the same size, keeping what is measured and what is painted in step. It refines
 * a radio with a trailing column and is 1.0 (inert) for every control without one.
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
 * <p>{@code columnCount} refines a vertical {@link ControlKind#RADIO} into a multi-column list: its
 * options fill each column top to bottom before the next, so a long list reads across two columns
 * rather than one tall stack. It is {@link #SINGLE_COLUMN} for the ordinary one-column list and for
 * every other kind - only a vertical radio may carry a wider count, since only a stacked list has rows
 * to spread across columns.
 *
 * <p>{@code scrolls} marks this control as the one flex region of a capped control strip: when a host
 * caps the strip's height (a bottom-padding limit), the controls before this one pin to the top and
 * the controls after it pin to the bottom, while this one takes the room left between and scrolls its
 * own rows when they overrun it. It is the single source both the capped layout, the renderer (which
 * clips it to its viewport), and the input listener (which scrolls it and clamps its clicks) read, so
 * the three agree on which control scrolls without a positional guess. A stacked list is the only
 * shape that scrolls, so it refines a vertical {@link ControlKind#RADIO} only and is {@code false} for
 * every other control; a strip carrying none is never capped and keeps its natural height.
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
 * @param reselect      what a click on a radio's lit segment does (inert, deselect, or re-fire);
 *                      ignored by the other kinds
 * @param trailingScale the size the per-option trailing values draw at, relative to the body font
 *                      size; 1.0 for a control with no trailing column or one drawn at body size
 * @param columnCount   how many columns a vertical radio spreads its options across; {@link
 *                      #SINGLE_COLUMN} for a one-column list and every other kind
 * @param scrolls       whether this control is the capped strip's scrolling flex region; {@code
 *                      false} for every control that pins at its natural height
 */
public record ControlSpec(ControlKind kind, List<String> labels, List<String> iconPaths,
        List<String> trailingLabels, String trailingLabel, int selectedIndex, ControlAction action,
        RadioAlignment alignment, ReselectBehaviour reselect, double trailingScale, int columnCount,
        boolean scrolls) {
    /** {@code selectedIndex} value meaning the control is off - no cell is lit. */
    public static final int NO_SELECTION = -1;

    /** The trailing-value size a control uses when its trailing column reads at the body font size. */
    public static final double BODY_TRAILING_SCALE = 1d;

    /** The column count of an ordinary single-column list and of every non-list control. */
    public static final int SINGLE_COLUMN = 1;

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
     * no meaning on a kind that has no segments to clear. A column count past one spreads a stacked
     * list's options, so it too refines only a vertical radio.
     */
    public ControlSpec {
        var hasOptionColumns = !iconPaths.isEmpty() || !trailingLabels.isEmpty();
        if (hasOptionColumns
                && (kind != ControlKind.RADIO || alignment != RadioAlignment.VERTICAL)) {
            throw new IllegalArgumentException(
                    "iconPaths and trailingLabels are drawn only on a vertical radio, not on a "
                            + kind + " with " + alignment + " alignment");
        }
        if (reselect != ReselectBehaviour.INERT && kind != ControlKind.RADIO) {
            throw new IllegalArgumentException(
                    "a non-inert reselect behaviour refines a radio's lit segment; it has no "
                            + "meaning on a " + kind);
        }
        if (trailingScale <= 0d) {
            throw new IllegalArgumentException(
                    "trailingScale is a size multiplier and must be positive, was " + trailingScale);
        }
        if (columnCount < SINGLE_COLUMN) {
            throw new IllegalArgumentException(
                    "columnCount is a column count and must be at least " + SINGLE_COLUMN + ", was "
                            + columnCount);
        }
        if (columnCount != SINGLE_COLUMN
                && (kind != ControlKind.RADIO || alignment != RadioAlignment.VERTICAL)) {
            throw new IllegalArgumentException(
                    "a multi-column list spreads a vertical radio's options, not a " + kind + " with "
                            + alignment + " alignment");
        }
        if (scrolls && (kind != ControlKind.RADIO || alignment != RadioAlignment.VERTICAL)) {
            throw new IllegalArgumentException(
                    "only a stacked list scrolls, so the scroll flag refines a vertical radio, not a "
                            + kind + " with " + alignment + " alignment");
        }
    }

    /**
     * Builds a control with no leading icons and no per-option trailing column - every kind except
     * the icon list, whose {@code iconPaths} is therefore empty and whose trailing column is drawn at
     * the body size. The icon list uses {@link #createIconRadioList}, the one entry that supplies a
     * non-empty icon-path list.
     */
    public ControlSpec(ControlKind kind, List<String> labels, String trailingLabel,
            int selectedIndex, ControlAction action, RadioAlignment alignment,
            ReselectBehaviour reselect) {
        this(kind, labels, List.of(), List.of(), trailingLabel, selectedIndex, action, alignment,
                reselect, BODY_TRAILING_SCALE, SINGLE_COLUMN, false);
    }

    /**
     * Builds a horizontal, always-selected control with the given action - the shape a checkbox,
     * toggle, or a standard option radio (Short/Full) takes. A stacked, deselectable, or re-firing
     * radio uses the full constructor to set its {@link RadioAlignment} and {@link ReselectBehaviour}.
     */
    public ControlSpec(ControlKind kind, List<String> labels, String trailingLabel,
            int selectedIndex, ControlAction action) {
        this(kind, labels, trailingLabel, selectedIndex, action, RadioAlignment.HORIZONTAL,
                ReselectBehaviour.INERT);
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
     * sibling of {@link #createVerticalRadioTable} - the shape a stacked selector takes (a view
     * selector) - so a host need not spell out the empty icon/value lists and the vertical alignment at
     * each call site. {@code reselect} refines what a click on the lit option does: {@link
     * ReselectBehaviour#DESELECT} for a selector that clears to nothing, {@link ReselectBehaviour#INERT}
     * for one that always keeps a segment lit.
     *
     * @param labels        the option labels, top to bottom, in segment order
     * @param selectedIndex the lit option's index, or {@link #NO_SELECTION} when nothing is picked
     * @param action        what a click on an option does, keyed by the option index
     * @param reselect      what a click on the lit option does
     * @return the vertical radio spec in its current lit state
     */
    public static ControlSpec createVerticalRadio(List<String> labels, int selectedIndex,
            ControlAction action, ReselectBehaviour reselect) {
        return new ControlSpec(ControlKind.RADIO, List.copyOf(labels), "", selectedIndex, action,
                RadioAlignment.VERTICAL, reselect);
    }

    /**
     * Builds a vertical, deselectable {@link ControlKind#RADIO} drawn as an icon list: one stacked
     * option per label, each drawing the icon at the matching {@code iconPaths} entry (null for an
     * icon-less option). It is a vertical radio like the view selector, distinguished only by carrying
     * icons - the non-empty {@code iconPaths} is what makes the layout size rows for icons and the
     * renderer left-anchor the labels. It is always vertical (an icon list only reads as a column) and
     * deselectable (a click on the lit option turns the whole list off, the shape a deselectable
     * picker takes - re-picking clears it); a selector needing another re-pick behaviour uses {@link
     * #createVerticalRadioTable}. The lists run in parallel, so the icon at
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
        return createIconRadioList(labels, iconPaths, trailingLabels, selectedIndex, action,
                SINGLE_COLUMN);
    }

    /**
     * Builds the icon-radio list of {@link #createIconRadioList(List, List, List, int, ControlAction)}
     * spread across {@code columnCount} columns: its options fill each column top to bottom before the
     * next, so a long list reads across two columns rather than one tall stack. Every option keeps its
     * three-column row geometry (crest, name, value); the count only decides how the rows wrap. A count
     * of {@link #SINGLE_COLUMN} is the ordinary one-column list.
     *
     * @param labels         the option labels, top to bottom; must contain no null (an unlabelled
     *                       option passes an empty string)
     * @param iconPaths      the per-option icon paths, aligned to {@code labels}; a null entry draws
     *                       no icon on that option
     * @param trailingLabels the per-option right-aligned values, aligned to {@code labels}; a null or
     *                       absent entry draws no value on that option
     * @param selectedIndex  the lit option's index, or {@link #NO_SELECTION} when nothing is picked
     * @param action         what a click on an option does, keyed by the option index
     * @param columnCount    how many columns to spread the options across
     * @return the icon-radio-list spec in its current lit state
     */
    public static ControlSpec createIconRadioList(List<String> labels, List<String> iconPaths,
            List<String> trailingLabels, int selectedIndex, ControlAction action, int columnCount) {
        return createVerticalRadioTable(labels, iconPaths, trailingLabels, selectedIndex, action,
                ReselectBehaviour.DESELECT, BODY_TRAILING_SCALE, columnCount);
    }

    /**
     * Builds the general vertical {@link ControlKind#RADIO} table both a picker and a sort selector
     * take: one stacked option per label, an optional leading icon and an optional trailing value per
     * row, with a caller-chosen reselect behaviour and trailing size. It generalises {@link
     * #createIconRadioList} - which is this with a deselectable re-pick and a body-size trailing
     * column - so a selector that instead re-fires on the lit row and draws a compact trailing column
     * (a sort selector's direction letters) shares the same three-column geometry. A row opts out of
     * an icon with a null {@code iconPaths} entry, so an all-null (but present) icon column reads as a
     * table with no crests, the same shape an alliance-only picker takes. Both optional lists are
     * copied null-tolerantly so a caller may hand in mutable lists without the spec aliasing them.
     *
     * @param labels         the option labels, top to bottom; must contain no null (an unlabelled
     *                       option passes an empty string)
     * @param iconPaths      the per-option icon paths, aligned to {@code labels}; a null entry draws
     *                       no icon on that option
     * @param trailingLabels the per-option right-aligned values, aligned to {@code labels}; a null or
     *                       absent entry draws no value on that option
     * @param selectedIndex  the lit option's index, or {@link #NO_SELECTION} when nothing is picked
     * @param action         what a click on an option does, keyed by the option index
     * @param reselect       what a click on the lit option does (deselect, re-fire, or inert)
     * @param trailingScale  the trailing values' size relative to the body font size ({@link
     *                       #BODY_TRAILING_SCALE} for a body-size column, less for a compact one)
     * @return the vertical radio table spec in its current lit state, laid out in a single column
     */
    public static ControlSpec createVerticalRadioTable(List<String> labels, List<String> iconPaths,
            List<String> trailingLabels, int selectedIndex, ControlAction action,
            ReselectBehaviour reselect, double trailingScale) {
        return createVerticalRadioTable(labels, iconPaths, trailingLabels, selectedIndex, action,
                reselect, trailingScale, SINGLE_COLUMN);
    }

    /**
     * Builds the vertical radio table of {@link #createVerticalRadioTable(List, List, List, int,
     * ControlAction, ReselectBehaviour, double)} spread across {@code columnCount} columns: its options
     * fill each column top to bottom before the next. A count of {@link #SINGLE_COLUMN} is the ordinary
     * one-column table; a wider count only decides how the rows wrap, leaving each row's geometry
     * unchanged.
     *
     * @param labels         the option labels, top to bottom; must contain no null (an unlabelled
     *                       option passes an empty string)
     * @param iconPaths      the per-option icon paths, aligned to {@code labels}; a null entry draws
     *                       no icon on that option
     * @param trailingLabels the per-option right-aligned values, aligned to {@code labels}; a null or
     *                       absent entry draws no value on that option
     * @param selectedIndex  the lit option's index, or {@link #NO_SELECTION} when nothing is picked
     * @param action         what a click on an option does, keyed by the option index
     * @param reselect       what a click on the lit option does (deselect, re-fire, or inert)
     * @param trailingScale  the trailing values' size relative to the body font size ({@link
     *                       #BODY_TRAILING_SCALE} for a body-size column, less for a compact one)
     * @param columnCount    how many columns to spread the options across
     * @return the vertical radio table spec in its current lit state
     */
    public static ControlSpec createVerticalRadioTable(List<String> labels, List<String> iconPaths,
            List<String> trailingLabels, int selectedIndex, ControlAction action,
            ReselectBehaviour reselect, double trailingScale, int columnCount) {
        return new ControlSpec(ControlKind.RADIO, List.copyOf(labels),
                Collections.unmodifiableList(new ArrayList<>(iconPaths)),
                Collections.unmodifiableList(new ArrayList<>(trailingLabels)), "", selectedIndex,
                action, RadioAlignment.VERTICAL, reselect, trailingScale, columnCount, false);
    }

    /**
     * Returns a copy of this control marked as the capped strip's scrolling flex region, so a host
     * builds its list through the ordinary {@code create*} factory and then opts that one control into
     * scrolling without a scroll-specific factory per list shape. Only a stacked list scrolls, so the
     * canonical constructor's guard rejects the copy unless this control is a vertical radio.
     *
     * @return an otherwise-identical spec with {@link #scrolls()} set
     */
    public ControlSpec buildScrollableCopy() {
        return new ControlSpec(kind, labels, iconPaths, trailingLabels, trailingLabel, selectedIndex,
                action, alignment, reselect, trailingScale, columnCount, true);
    }
}
