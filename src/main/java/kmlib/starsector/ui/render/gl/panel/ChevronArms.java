package kmlib.starsector.ui.render.gl.panel;

/**
 * The two arms of the collapse handle's chevron, as the shared endpoint x the arms open toward ({@code
 * endsX}), the apex x they meet at ({@code apexX}), and the three ys the polyline runs through. It is the
 * pure geometry of the rotating glyph split out from the GL draw, so the orientation contract - apex left
 * of the ends when expanded, coincident at the midpoint (a straight vertical line), right of them when
 * docked - is a value a test can pin without a GL context.
 *
 * <p>The chevron is the polyline {@code (endsX, topY) -> (apexX, midY) -> (endsX, bottomY)}: two arms
 * meeting at the apex, their far ends sharing one x so the glyph opens symmetrically about the centre.
 *
 * @param endsX   the x the two arms' far ends share, on the side the chevron opens toward
 * @param apexX   the x the two arms meet at, on the side the chevron points toward
 * @param topY    the top arm end's y
 * @param midY    the apex's y, midway between the ends
 * @param bottomY the bottom arm end's y
 */
public record ChevronArms(
    float endsX,
    float apexX,
    float topY,
    float midY,
    float bottomY) {
}
