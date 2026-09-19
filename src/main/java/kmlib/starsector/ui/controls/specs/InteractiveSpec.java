package kmlib.starsector.ui.controls.specs;

/**
 * A clickable, stateful control - a checkbox, toggle, radio, table, or tabs row - as opposed to
 * the {@link LabelSpec} and {@link DividerSpec} chrome that is drawn but never clicked. It carries the lit
 * cell the renderer draws and the action the input listener fires; the host attaches both when it
 * builds the spec, so the framework acts on a click without learning what the control means.
 */
public sealed interface InteractiveSpec
    extends ControlSpec
    permits CheckboxSpec, ToggleSpec, RadioSpec, VerticalTableSpec, TabsSpec {

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
     * Whether the control's cells are separately hit targets rather than the whole row being one.
     *
     * <p>Answered by the control rather than worked out about it from outside, because it is one
     * rule with two readers: the hit-test that resolves a cell, and the narrowing that decides
     * whether pressing one acts. Stated on either side, a variant would be hit as a row of
     * segments and pressed as a whole row, or the other way about.
     *
     * <p>Carried by every variant rather than defaulted, unlike the two answers below it. A variant
     * that inherited the wrong answer here would not be cautious, it would be wrong: a whole-row
     * target resolves every press to the first cell, so a segmented control that forgot to say so
     * fires its first option whichever one was pressed.
     *
     * @return whether the cells are separate hit targets
     */
    boolean isSegmented();

    /**
     * What a re-pick of the already-lit cell does.
     *
     * <p>Defaulted, because inheriting it costs nothing that matters: a control that meant to clear
     * on a re-pick and never said so simply does not clear, which is the conservative half of the
     * choice rather than a wrong reading of the press. A set carrying its own rule answers with it.
     *
     * @return the re-pick behaviour, {@link ReselectBehaviour#INERT} for a control holding none
     */
    default ReselectBehaviour reselectBehaviour() {
        return ReselectBehaviour.INERT;
    }

    /**
     * Whether the control is lit - any cell selected. A single-cell checkbox or toggle reads on
     * from this; a radio uses it to tell a lit list from a fully-cleared one.
     *
     * @return true when a cell is lit
     */
    default boolean isLit() {
        return selectedIndex() != NO_SELECTION;
    }

    /**
     * Where the control reports which of its cells the pointer is on, for a host that answers a hover
     * as well as a click. The input listener calls it as the hovered cell changes; the layout and the
     * renderer ignore it, exactly as they ignore the action beside it.
     *
     * <p>Defaulted rather than carried by every variant, because a report is only worth wiring where a
     * control's cells stand for things the host holds: a stacked list's rows name items it can act on,
     * while a checkbox's single cell says no more than its own lit state already does. A variant whose
     * host comes to want one carries a component overriding this, and nothing that drives the report
     * changes - the frame reads it here and never branches on a variant.
     *
     * @return where the hovered cell is reported, {@link ControlHoverReport#NONE} for a control that
     *         reports none
     */
    default ControlHoverReport hoverReport() {
        return ControlHoverReport.NONE;
    }
}
