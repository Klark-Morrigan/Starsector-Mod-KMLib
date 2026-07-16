package kmlib.starsector.ui.controls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A host's description of one body control: which widget it is and the state that widget draws in.
 * The layout measures its labels to snap it to its text and the renderer paints it in that state,
 * while the host that supplies the spec owns what the control means and does - so any host composes a
 * control strip without the layout or renderer learning the control's meaning.
 *
 * <p>The shape is a sealed hierarchy rather than one flag-laden record: each variant carries only the
 * state its own widget draws, so a field that refines a stacked list ({@code iconPaths}, {@code
 * columnCount}) simply does not exist on a checkbox or a tabs row. That makes the illegal
 * combinations unrepresentable - there are no runtime guards rejecting, say, an icon list on a
 * divider, because a divider has no such component to set - and lets the layout, renderer, and input
 * listener dispatch by pattern-matching the variant instead of re-branching on a kind tag.
 *
 * <p>State rides along as data because the host, not the renderer, knows how to read it: the host
 * reads its live value when it builds the spec (specs are rebuilt each frame), so the carried state
 * is current. Every clickable control reduces to "which of my cells is lit" - {@link #NO_SELECTION}
 * for none - which {@link Interactive} carries; the two chrome variants ({@link Label}, {@link
 * Divider}) are drawn but never clicked and so carry neither a lit cell nor an action.
 */
public sealed interface ControlSpec {
    /** {@code selectedIndex} value meaning the control is off - no cell is lit. */
    int NO_SELECTION = -1;

    /** The column count of an ordinary single-column list. */
    int SINGLE_COLUMN = 1;

    /** The single cell of a checkbox or toggle: its whole row is one hit target, lit at index 0. */
    int SINGLE_CELL = 0;

    /**
     * The control's own label(s): one for a checkbox, toggle, or caption, one per option for a radio
     * or tab (in segment order), and none for a divider. The layout measures these to snap the control
     * to its text.
     *
     * @return the control's labels, in draw order
     */
    List<String> labels();

    /**
     * A clickable, stateful control - a checkbox, toggle, radio, table, or tabs row - as opposed to
     * the {@link Label} and {@link Divider} chrome that is drawn but never clicked. It carries the lit
     * cell the renderer draws and the action the input listener fires; the host attaches both when it
     * builds the spec, so the framework acts on a click without learning what the control means.
     */
    sealed interface Interactive extends ControlSpec {
        /**
         * The index of the control's lit cell - the active radio segment, or {@link #SINGLE_CELL} for
         * a checked checkbox or a lit toggle - or {@link #NO_SELECTION} when nothing is lit.
         *
         * @return the lit cell's index, or {@link #NO_SELECTION}
         */
        int selectedIndex();

        /**
         * What a click on the control does, keyed by which cell was hit; the host supplies it and the
         * input listener invokes it, while the layout and renderer ignore it.
         *
         * @return the click action
         */
        ControlAction action();

        /**
         * Whether the control is lit - any cell selected. A single-cell checkbox or toggle reads on
         * from this; a radio uses it to tell a lit list from a fully-cleared one.
         *
         * @return true when a cell is lit
         */
        default boolean isLit() {
            return selectedIndex() != NO_SELECTION;
        }
    }

    /**
     * A tick box with a trailing label: a single-cell control lit at {@link #SINGLE_CELL} when on and
     * {@link #NO_SELECTION} when off, its whole row one hit target. The renderer draws the tick box at
     * the row's left and the label beside it.
     *
     * @param label         the checkbox's trailing label
     * @param selectedIndex {@link #SINGLE_CELL} when ticked, {@link #NO_SELECTION} when off
     * @param action        what a click on the row does
     */
    record Checkbox(String label, int selectedIndex, ControlAction action) implements Interactive {
        /**
         * Builds a checkbox in its current lit state, mapping on/off to the single-cell {@code
         * selectedIndex} in one place so no host re-derives the "cell 0 lit or nothing" convention.
         *
         * @param label  the checkbox's trailing label
         * @param isOn   whether the box is ticked
         * @param action what a click on the row does
         * @return the checkbox spec
         */
        public static Checkbox lit(String label, boolean isOn, ControlAction action) {
            return new Checkbox(label, isOn ? SINGLE_CELL : NO_SELECTION, action);
        }

        @Override
        public List<String> labels() {
            return List.of(label);
        }
    }

    /**
     * A single framed button that washes when on: a single-cell control lit at {@link #SINGLE_CELL}
     * when on and {@link #NO_SELECTION} when off, the button itself the hit target. It reads the same
     * on/off state as a {@link Checkbox} but draws as a lit push-button with its label centred inside,
     * rather than a tick box with an adjacent label.
     *
     * @param label         the button's centred label
     * @param selectedIndex {@link #SINGLE_CELL} when on, {@link #NO_SELECTION} when off
     * @param action        what a click on the button does
     */
    record Toggle(String label, int selectedIndex, ControlAction action) implements Interactive {
        /**
         * Builds a toggle in its current lit state, mapping on/off to the single-cell {@code
         * selectedIndex} as {@link Checkbox#lit} does for a tick box.
         *
         * @param label  the button's centred label
         * @param isOn   whether the button is lit
         * @param action what a click on the button does
         * @return the toggle spec
         */
        public static Toggle lit(String label, boolean isOn, ControlAction action) {
            return new Toggle(label, isOn ? SINGLE_CELL : NO_SELECTION, action);
        }

        @Override
        public List<String> labels() {
            return List.of(label);
        }
    }

    /**
     * A text-only caption row, drawn but never clicked - it heads a run of controls with a title. It
     * is not {@link Interactive}: a caption carries no lit cell and no action.
     *
     * @param text the caption text
     */
    record Label(String text) implements ControlSpec {
        @Override
        public List<String> labels() {
            return List.of(text);
        }
    }

    /**
     * A horizontal rule spanning the strip's inner width, drawn but never clicked - it parts one run
     * of controls from the next. It is not {@link Interactive} and carries no label; the layout spans
     * it to the content width and the renderer draws the rule.
     */
    record Divider() implements ControlSpec {
        @Override
        public List<String> labels() {
            return List.of();
        }
    }

    /**
     * A row of mutually exclusive option segments laid side by side. Its {@link SegmentSizing} picks how
     * the segments size: {@link SegmentSizing#UNIFORM} gives every segment the widest label's width (even
     * cells, the default an option pair reads as), {@link SegmentSizing#SNAPPED} gives each its own
     * label's width (a ragged row that would waste space as even cells). Its {@link ReselectBehaviour}
     * sets what a re-pick of the lit segment does: an option pair is {@link ReselectBehaviour#INERT}
     * (always one lit, the standard row), while a {@link ReselectBehaviour#DESELECT} row clears to
     * nothing on a re-pick - so a horizontal radio can read as the on/off selector a {@link
     * VerticalTable} did, only laid across one row.
     *
     * @param labels        the option labels, left to right, in segment order
     * @param selectedIndex the lit option's index, or {@link #NO_SELECTION} when nothing is picked
     * @param action        what a click on an option does, keyed by the option index
     * @param trailingLabel a caption drawn after the row, or blank for none
     * @param segmentSizing how the segments size (uniform cells, or each snapped to its own label)
     * @param reselect      what a re-pick of the lit segment does (inert for an option pair, deselect for
     *                      a clearable selector)
     */
    record HorizontalRadio(List<String> labels, int selectedIndex, ControlAction action,
            String trailingLabel, SegmentSizing segmentSizing, ReselectBehaviour reselect)
            implements Interactive {
        /**
         * Builds an even-cell horizontal radio - the standard option pair, sized {@link
         * SegmentSizing#UNIFORM}.
         *
         * @param labels        the option labels, left to right, in segment order
         * @param trailingLabel a caption drawn after the row, or blank for none
         * @param selectedIndex the lit option's index, or {@link #NO_SELECTION}
         * @param action        what a click on an option does, keyed by the option index
         * @return the uniform horizontal radio spec
         */
        public static HorizontalRadio uniform(
                List<String> labels,
                String trailingLabel,
                int selectedIndex,
                ControlAction action) {
            return new HorizontalRadio(
                    List.copyOf(labels),
                    selectedIndex,
                    action,
                    trailingLabel,
                    SegmentSizing.UNIFORM,
                    ReselectBehaviour.INERT);
        }

        /**
         * Builds a horizontal radio whose segments each snap to their own label's width ({@link
         * SegmentSizing#SNAPPED}), for a ragged option row that would waste space as even cells.
         *
         * @param labels        the option labels, left to right, in segment order
         * @param trailingLabel a caption drawn after the row, or blank for none
         * @param selectedIndex the lit option's index, or {@link #NO_SELECTION}
         * @param action        what a click on an option does, keyed by the option index
         * @return the snapped horizontal radio spec
         */
        public static HorizontalRadio snapped(
                List<String> labels,
                String trailingLabel,
                int selectedIndex,
                ControlAction action) {
            return new HorizontalRadio(
                    List.copyOf(labels),
                    selectedIndex,
                    action,
                    trailingLabel,
                    SegmentSizing.SNAPPED,
                    ReselectBehaviour.INERT);
        }

        /**
         * Builds an even-cell horizontal radio that clears to nothing when its lit segment is re-picked
         * ({@link ReselectBehaviour#DESELECT}) - the shape a horizontal on/off selector takes, where a
         * re-click of the active option turns it off. It carries no trailing caption and sizes {@link
         * SegmentSizing#UNIFORM}, so a short option row reads as even cells.
         *
         * @param labels        the option labels, left to right, in segment order
         * @param selectedIndex the lit option's index, or {@link #NO_SELECTION} when nothing is picked
         * @param action        what a click on an option does, keyed by the option index
         * @return the deselectable horizontal radio spec
         */
        public static HorizontalRadio deselectable(
                List<String> labels,
                int selectedIndex,
                ControlAction action) {
            return new HorizontalRadio(
                    List.copyOf(labels),
                    selectedIndex,
                    action,
                    "",
                    SegmentSizing.UNIFORM,
                    ReselectBehaviour.DESELECT);
        }
    }

    /**
     * A vertical stack of option rows, exactly one lit, each row optionally carrying a leading icon
     * and a right-aligned trailing value - so the list reads as a table (crest, name, value). It is
     * the general stacked selector: a label-only list, an icon picker, and a direction list (a triangle
     * per row) are all this variant with different columns filled.
     *
     * <p>The {@code iconPaths}, {@code trailingLabels}, and {@code trailingDirections} run parallel to
     * {@code labels}: the entry at index {@code i} draws on the option labelled {@code labels.get(i)}. A
     * shorter or empty list, or a null entry, leaves that option icon-less or value-less. The trailing
     * slot holds a text value or a direction triangle, not both - a row with a triangle
     * ({@code trailingDirections} entry) carries no trailing text. {@code reselect} refines what a click
     * on the lit option does; {@code columnCount} spreads the options across columns (filling each top to
     * bottom before the next); {@code scrolls} marks this as the capped strip's one flex region - the
     * single source the capped layout, the clipping renderer, and the scrolling input listener all read
     * so the three agree which control scrolls.
     *
     * @param labels             the option labels, top to bottom, in segment order
     * @param iconPaths          one leading-icon path per option (a null entry is an icon-less option);
     *                           empty for a list drawn without icons
     * @param trailingLabels     one right-aligned value per option (a null or absent entry is a
     *                           value-less option); empty for a list with no trailing column
     * @param trailingDirections one trailing direction triangle per option (a null or absent entry is a
     *                           triangle-less option); empty for a list with no direction column, so a
     *                           text-valued table leaves this empty and a direction table leaves {@code
     *                           trailingLabels} empty
     * @param selectedIndex      the lit option's index, or {@link #NO_SELECTION} when nothing is picked
     * @param action             what a click on an option does, keyed by the option index
     * @param reselect           what a click on the lit option does (deselect, re-fire, or inert)
     * @param columnCount        how many columns to spread the options across ({@link #SINGLE_COLUMN}
     *                           for one column)
     * @param scrolls            whether this control is the capped strip's scrolling flex region
     */
    record VerticalTable(
            List<String> labels,
            List<String> iconPaths,
            List<String> trailingLabels,
            List<TriangleDirection> trailingDirections,
            int selectedIndex,
            ControlAction action,
            ReselectBehaviour reselect,
            int columnCount,
            boolean scrolls) implements Interactive {
        /**
         * Copies the option lists defensively - null-tolerantly, since a null entry is a real "no icon",
         * "no value", or "no triangle" - and rejects a column count the layout cannot lay out, so a
         * mis-built table fails at construction rather than at paint time.
         */
        public VerticalTable {
            labels = List.copyOf(labels);
            iconPaths = Collections.unmodifiableList(new ArrayList<>(iconPaths));
            trailingLabels = Collections.unmodifiableList(new ArrayList<>(trailingLabels));
            trailingDirections = Collections.unmodifiableList(new ArrayList<>(trailingDirections));
            if (columnCount < SINGLE_COLUMN) {
                throw new IllegalArgumentException("columnCount must be at least " + SINGLE_COLUMN
                        + ", was " + columnCount);
            }
        }

        /**
         * Builds a deselectable icon list (crest, name, value) spread across {@code columnCount} columns:
         * a vertical table where re-picking the lit option clears the list. The {@code iconPaths} and
         * {@code trailingLabels} run parallel to {@code labels} - a null or absent entry leaves that
         * option icon-less or value-less, and an all-null (but present) icon column reads as a table with
         * no crests. A table whose trailing column is direction triangles rather than text uses {@link
         * #directionTable} instead.
         *
         * @param labels         the option labels, top to bottom
         * @param iconPaths      the per-option icon paths, aligned to {@code labels} (a null draws none)
         * @param trailingLabels the per-option values, aligned to {@code labels} (a null draws none)
         * @param selectedIndex  the lit option's index, or {@link #NO_SELECTION}
         * @param action         what a click on an option does, keyed by the option index
         * @param columnCount    how many columns to spread the options across
         * @return the icon-table spec
         */
        public static VerticalTable iconList(
                List<String> labels,
                List<String> iconPaths,
                List<String> trailingLabels,
                int selectedIndex,
                ControlAction action,
                int columnCount) {
            return new VerticalTable(
                    labels,
                    iconPaths,
                    trailingLabels,
                    List.of(),
                    selectedIndex,
                    action,
                    ReselectBehaviour.DESELECT,
                    columnCount,
                    false);
        }

        /**
         * Builds a direction table: the {@link #iconList} shape - an all-null icon column (the
         * three-column geometry, no crests) - but with a direction triangle per row in place of a
         * trailing text value, for a trailing column that marks a direction a font has no up/down glyph
         * for. The {@code trailingDirections} run parallel to {@code labels}: the entry at index {@code i}
         * draws its triangle on option {@code i}, a null entry drawing none. {@code reselect} refines what
         * a click on the lit option does, so a re-firing selector can flip the direction on a re-pick.
         *
         * @param labels             the option labels, top to bottom
         * @param trailingDirections the per-option direction triangles, aligned to {@code labels} (a null
         *                           draws none)
         * @param selectedIndex      the lit option's index, or {@link #NO_SELECTION}
         * @param action             what a click on an option does, keyed by the option index
         * @param reselect           what a click on the lit option does (deselect, re-fire, or inert)
         * @return the direction-table spec, in a single column
         */
        public static VerticalTable directionTable(
                List<String> labels,
                List<TriangleDirection> trailingDirections,
                int selectedIndex,
                ControlAction action,
                ReselectBehaviour reselect) {
            return new VerticalTable(
                    labels,
                    Collections.<String>nCopies(labels.size(), null),
                    List.of(),
                    trailingDirections,
                    selectedIndex,
                    action,
                    reselect,
                    SINGLE_COLUMN,
                    false);
        }

        /**
         * Whether the option at {@code index} draws a leading icon - a non-null entry in the parallel
         * icon-path list. A shorter or empty list, or an out-of-range index, reports no icon. One rule
         * read by both the layout that sizes the row and the renderer that draws it, so the two cannot
         * disagree on which options show an icon.
         *
         * @param index the option index
         * @return true when the option at that index has a non-null icon path
         */
        public boolean hasIconAt(int index) {
            return index < iconPaths.size() && iconPaths.get(index) != null;
        }

        /**
         * The right-aligned value drawn at option {@code index}'s trailing edge, or "" when this option
         * has none - a shorter or empty list, or a null entry. Returning "" rather than null lets the
         * layout and renderer treat "no value" as a zero-width text without a null check at each site.
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
         * The direction triangle drawn in option {@code index}'s trailing slot, or null when this option
         * has none - a shorter or empty list, or a null entry. A non-null direction draws a triangle in
         * place of a text value, so the layout and renderer read this to tell a triangle row from a text
         * row without a null check duplicated at each site.
         *
         * @param index the option index
         * @return the option's trailing direction triangle, or null when it has none
         */
        public TriangleDirection directionAt(int index) {
            if (index >= trailingDirections.size()) {
                return null;
            }
            return trailingDirections.get(index);
        }

        /**
         * Returns a copy of this table marked as the capped strip's scrolling flex region, so a host
         * builds its list through the ordinary factory and then opts it into scrolling without a
         * scroll-specific factory. Only a stacked list scrolls, so this method exists on this variant
         * alone - no other control can be marked scrolling.
         *
         * @return an otherwise-identical table with {@link #scrolls()} set
         */
        public VerticalTable asScrolling() {
            return new VerticalTable(
                    labels,
                    iconPaths,
                    trailingLabels,
                    trailingDirections,
                    selectedIndex,
                    action,
                    reselect,
                    columnCount,
                    true);
        }
    }

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
    record Tabs(
            List<String> labels,
            List<String> shortcuts,
            int selectedIndex,
            ControlAction action)
            implements Interactive {
        /** Copies the label and shortcut lists defensively; a null shortcut entry is a real "no hint". */
        public Tabs {
            labels = List.copyOf(labels);
            shortcuts = Collections.unmodifiableList(new ArrayList<>(shortcuts));
        }

        /**
         * The shortcut hint drawn at tab {@code index}, or "" when the tab has none - a shorter or empty
         * list, or a null entry. Returning "" rather than null lets the layout and renderer treat "no
         * hint" as a zero-width text without a null check at each site, as {@link
         * VerticalTable#trailingLabelAt} does for a missing value.
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
    }

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
     * <p>It is not {@link Interactive} and carries no labels of its own - each child carries its own -
     * so it is measured through its two columns rather than a label. Its children are ordinary controls,
     * so a column holds no further group; nesting is neither needed nor laid out.
     *
     * @param leftColumn  the controls filling the left column, top to bottom
     * @param rightColumn the controls filling the right column, top to bottom
     */
    record SideBySide(List<ControlSpec> leftColumn, List<ControlSpec> rightColumn)
            implements ControlSpec {
        /** Copies both columns defensively, so a later edit to a caller's list cannot mutate the spec. */
        public SideBySide {
            leftColumn = List.copyOf(leftColumn);
            rightColumn = List.copyOf(rightColumn);
        }

        @Override
        public List<String> labels() {
            return List.of();
        }
    }
}
