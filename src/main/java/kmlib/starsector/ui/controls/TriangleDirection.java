package kmlib.starsector.ui.controls;

/**
 * Which way a small drawn triangle points - up or down. A substrate-independent direction, carried so
 * a renderer can fill a triangle where a font has no up/down glyph. It names only the shape, not any
 * meaning behind it - the host that supplies it owns what up and down stand for - so the layout and
 * renderer place the marker without knowing that meaning.
 */
public enum TriangleDirection {
    /** Points up - the apex sits at the top edge of its box. */
    UP,

    /** Points down - the apex sits at the bottom edge of its box. */
    DOWN,
}
