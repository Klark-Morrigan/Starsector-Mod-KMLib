package kmlib.starsector.ui.widgets.tabs;

/**
 * The three seams a tab row's paint is read through, carried together: the surface each tab settles on,
 * the momentary lift over it, and the light laid across the finished result. A chrome needs all three to
 * draw a row and none of them alone, so they travel as one value rather than as three arguments threaded
 * through every layer between a panel's live state and its chrome.
 *
 * <p>They stay separate fields inside it because they resolve in an order and reach the screen at
 * different moments: the look settles, the lift moves that surface, and the light is added over whatever
 * was drawn. Folded into one value, a click over a half-faded hover would have no way to say which was
 * applied to which - and light mixed into a surface would arrive diluted by however much of that surface
 * is actually painted.
 *
 * @param looks  where each tab's settled look comes from
 * @param washes where each tab's momentary lift comes from
 * @param lights where the light laid over each finished tab comes from
 */
public record TabPaintSources(
    TabLookSource looks,
    TabWashSource washes,
    TabLightSource lights) {
}
