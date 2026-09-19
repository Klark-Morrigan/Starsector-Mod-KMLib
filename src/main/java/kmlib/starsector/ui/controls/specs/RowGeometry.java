package kmlib.starsector.ui.controls.specs;

/**
 * How a stacked list lays each of its rows out - as a table of columns, or as one uniform cell per row.
 * The two are different widgets wearing the same stack: a table reads left to right (what the row leads
 * with, its name, its value) and lines those parts up down the whole stack, while a uniform list reads
 * as a column of equal cells with each name centred in its own.
 *
 * <p>Stated by whoever builds the list rather than inferred from what the rows happen to hold, because
 * the two answers part company exactly where it matters: a table every one of whose rows leads with
 * nothing is still a table - its names stay left-anchored where a crested row's name would start, so a
 * list that happens to hold no images this frame does not re-centre itself and jump. Inferring the
 * layout from the content makes that a property of the data a host cannot see, let alone hold steady.
 *
 * <p>It says nothing about how many columns the stack wraps its rows across - that is a separate
 * decision about how the stack folds, and either geometry can be laid in one column or several.
 */
public enum RowGeometry {

    /**
     * Three columns: the leading slot at the row's left inset, the label left-anchored past it, and the
     * trailing slot flush against the row's right inset. Every row lays its parts at the same offsets,
     * so a stack reads as a table whose names and values line up whether or not each row fills each of
     * its slots.
     */
    COLUMNS,

    /**
     * One cell per row, every cell as wide as the widest label and the label centred in it - the option
     * row of a horizontal radio, stacked. For a list of choices that reads as a set of buttons rather
     * than as a table of entries; the flanking slots have no column to sit in here.
     */
    UNIFORM_SEGMENTS
}
