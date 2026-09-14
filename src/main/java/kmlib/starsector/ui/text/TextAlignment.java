package kmlib.starsector.ui.text;

/**
 * Where a run of text sits relative to the point it is drawn at: one of the nine combinations of a
 * vertical edge (top, middle, bottom) and a horizontal one (left, middle, right). The name says
 * which corner or edge of the text box lands on the draw point, so {@code TOP_LEFT} hangs the text
 * down and to the right of it while {@code CENTER} straddles it.
 *
 * <p>It names no drawing surface on purpose. Both surfaces text reaches the screen through already
 * have a nine-position vocabulary of their own - LazyLib's {@code LazyFont.TextAnchor} for raw GL,
 * vanilla's {@code com.fs.starfarer.api.ui.Alignment} for the game's widgets - so neither is a
 * neutral home for the concept: a style carrying one spelling could only travel to that one
 * surface, and reaching the other would mean translating out of a vocabulary it has no part in.
 *
 * <p>Naming LazyLib's spelling in particular would cost more than a translation. KMLib holds LazyLib
 * at arm's length - only the classes that actually draw cached text name it, so an install without
 * LazyLib never loads them - while a look value is carried by every styled control, whichever
 * surface paints it. Its alignment type therefore has to be one that loads with LazyLib absent.
 */
public enum TextAlignment {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT
}
