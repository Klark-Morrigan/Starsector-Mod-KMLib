package kmlib.starsector.ui.layout;

/**
 * Four edge insets in pixels - {@code top}, {@code right}, {@code bottom}, {@code left}, in CSS clockwise
 * order - bundling a layout's edge spacing so it takes one argument rather than four parallel scalars. A
 * generic, immutable inset any UI layout can anchor by; a consumer that spaces only some edges (a panel
 * that grows rightward and so keeps no right margin) simply passes 0 for the unused ones.
 *
 * @param top    pixels of inset at the top edge
 * @param right  pixels of inset at the right edge
 * @param bottom pixels of inset at the bottom edge
 * @param left   pixels of inset at the left edge
 */
public record Padding(
    int top,
    int right,
    int bottom,
    int left) {
}
