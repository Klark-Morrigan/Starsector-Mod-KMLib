package kmlib.starsector.ui.controls;

/**
 * The kind of a body control - which raw-GL widget lays it out and paints it. A control strip is a
 * generic column of these: a consumer supplies whichever controls it needs, and the layout snaps and
 * stacks them by kind without knowing what any one control means. What a control does on a click
 * stays with the consumer that supplied it, so one host renders another host's controls as readily
 * as its own.
 */
public enum ControlKind {
    /** A tick box with a trailing label; the whole row is the hit target. */
    CHECKBOX,

    /** A row of mutually exclusive option segments, each segment its own hit target. */
    RADIO,

    /** A single button that lights when on; the button is the hit target. */
    TOGGLE,

    /** A text-only caption row, drawn but never clicked - it carries no hit target. */
    LABEL
}
