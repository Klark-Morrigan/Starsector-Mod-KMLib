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

    /**
     * A group of mutually exclusive option segments, each segment its own hit target. Flows
     * horizontally (an option pair) or vertically (a stacked list) per the spec's {@link
     * RadioAlignment}. A vertical group may carry a leading icon per option (the spec's {@code
     * iconPaths}) - the shape an icon picker takes - which left-anchors its labels past the icon;
     * without icons a vertical group centres its labels like a horizontal one.
     */
    RADIO,

    /** A single button that lights when on; the button is the hit target. */
    TOGGLE,

    /** A text-only caption row, drawn but never clicked - it carries no hit target. */
    LABEL,

    /**
     * A horizontal rule spanning the strip's inner width, drawn but never clicked - it carries no
     * hit target and no label. It parts one run of controls from the next, so a host can head a
     * section with a rule instead of a caption.
     */
    DIVIDER,

    /**
     * A row of vanilla-styled tabs, each snapped to its label-plus-shortcut width, exactly one lit -
     * the panel's navigation, a control like any other rather than special panel chrome. Each tab is
     * its own hit target and carries an optional shortcut hint (the spec's parallel {@code shortcuts});
     * a click fires the select action with the tab's index. It reads as a horizontal single-select
     * strip, but snapped to text and painted in the tab face rather than as equal radio segments.
     */
    TABS
}
