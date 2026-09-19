package kmlib.starsector.ui.controls.specs;

import java.util.List;

/**
 * A host's description of one body control: which widget it is and the state that widget draws in.
 * The layout measures its labels to snap it to its text and the renderer paints it in that state,
 * while the host that supplies the spec owns what the control means and does - so any host composes a
 * control strip without the layout or renderer learning the control's meaning.
 *
 * <p>The shape is a sealed hierarchy rather than one flag-laden record: each variant carries only the
 * state its own widget draws, so a component that refines a stacked list ({@code rowGeometry}, {@code
 * columnCount}) simply does not exist on a checkbox or a tabs row. That makes the illegal
 * combinations unrepresentable - there are no runtime guards rejecting, say, an icon list on a
 * divider, because a divider has no such component to set - and lets the layout, renderer, and input
 * listener dispatch by pattern-matching the variant instead of re-branching on a kind tag.
 *
 * <p>State rides along as data because the host, not the renderer, knows how to read it: the host
 * reads its live value when it builds the spec (specs are rebuilt each frame), so the carried state
 * is current. Every clickable control reduces to "which of my cells is lit" - {@link #NO_SELECTION}
 * for none - which {@link InteractiveSpec} carries; the two chrome variants ({@link LabelSpec}, {@link
 * DividerSpec}) are drawn but never clicked and so carry neither a lit cell nor an action.
 */
public sealed interface ControlSpec
    permits InteractiveSpec, LabelSpec, DividerSpec, SideBySideSpec, ScrollingSectionSpec {

    /** {@code selectedIndex} value meaning the control is off - no cell is lit. */
    int NO_SELECTION = -1;

    /** The column count of an ordinary single-column list. */
    int SINGLE_COLUMN = 1;

    /** The single cell of a checkbox or toggle: its whole row is one hit target, lit at index 0. */
    int SINGLE_CELL = 0;

    /** Trailing-caption value meaning the control draws no caption after its row. */
    String NO_TRAILING_CAPTION = "";

    /**
     * The control's own label(s) as the lines they read as: one for a checkbox, toggle, or caption, one
     * per option for a radio, table, or tab (in segment order), and none for a divider. A label authored
     * as several runs is charged one line here, so a control snapped to its text is snapped to what the
     * label says rather than to how many colours it says it in.
     *
     * <p>Only the words survive: a label's image runs have no text form and so appear in none of these
     * lines. A control sized from them alone therefore reserves no room for its images, which is why the
     * label-bearing controls are sized through their runs instead - see {@link VerticalTableSpec#labels()}
     * for the one shape that is not.
     *
     * @return the control's labels, in draw order
     */
    List<String> labels();
}
