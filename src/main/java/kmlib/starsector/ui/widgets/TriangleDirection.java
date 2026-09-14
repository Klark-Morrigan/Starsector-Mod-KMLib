package kmlib.starsector.ui.widgets;

/**
 * Which way a small drawn triangle points - up or down. A substrate-independent direction, carried so
 * a renderer can fill a triangle where a font has no up/down glyph. It names only the shape, not any
 * meaning behind it - the host that supplies it owns what up and down stand for - so the layout and
 * renderer place the marker without knowing that meaning.
 *
 * <p>It sits beside the row content that carries it ({@link RowSlot.Triangle}) rather than with the
 * controls that ask for one, because a direction is a fact about a shape and not about a control: what
 * a spec holds is rows, and a row's slots are what name the shapes they draw.
 */
public enum TriangleDirection {
    /** Points up - the apex sits at the top edge of its box. */
    UP,

    /** Points down - the apex sits at the bottom edge of its box. */
    DOWN,
}
