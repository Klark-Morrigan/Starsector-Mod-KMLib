package kmlib.starsector.ui.layout;

/**
 * The room a panel's body is laid into: the top-left its content starts at, and how far down that
 * content may run before its scrolling control has to give up the difference. A body has no stated
 * width - it is as wide as the strip measures - so this is not a rectangle.
 *
 * <p>The three travel as one value because two of them are coordinates of the same anchor and the third
 * is measured down from it, which left them as three same-typed arguments side by side in the signature
 * that took them, with nothing to catch a caller transposing a pair. Built only by {@link
 * PanelLayout.ContentOrigin#limitBodyTo}, out of components that same value already holds separately, so
 * the coordinates are never loose in the passing.
 *
 * @param contentX    the body's left edge (the content inset), in UI coordinates
 * @param contentTopY the body's top edge, in UI coordinates (below a header, or the content top)
 * @param maxHeight   the most the body may stand before its scrolling control caps
 */
public record BodyRoom(
    float contentX,
    float contentTopY,
    float maxHeight) {
}
