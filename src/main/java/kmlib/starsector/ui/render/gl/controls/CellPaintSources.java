package kmlib.starsector.ui.render.gl.controls;

/**
 * The finished per-cell paint a widget draws a control's cells from: the wash the pointer laid on a cell,
 * and the light a press lit it with. One value rather than a parameter each, so the widget passes below
 * take what a cell is dressed in as one thing and a channel added later reaches them without every
 * signature on the way growing another argument.
 *
 * <p>They stay separate fields inside it because they are drawn in order and not blended: the wash is the
 * cell's surface under the pointer, and the light is added over it. A press lands on a cell the pointer is
 * already holding fully washed, so a treatment that composed the two into one paint would show nothing on
 * the presses a player actually makes.
 *
 * @param hoverWashes the wash each cell takes under the pointer
 * @param pressLights the light each cell takes for the press it answered
 */
public record CellPaintSources(
    CellHoverWashSource hoverWashes,
    CellPressLightSource pressLights) {
}
