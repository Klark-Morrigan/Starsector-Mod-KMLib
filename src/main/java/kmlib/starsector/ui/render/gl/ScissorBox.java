package kmlib.starsector.ui.render.gl;

/**
 * A scissor clip box in framebuffer pixels, given by its lower-left corner and size - the shape a raw GL
 * scissor takes and {@code GL_SCISSOR_BOX} reports. Immutable; it exists so {@link UiScissor} can
 * compose nested clips as a value with named fields rather than juggling a bare {@code int[]}, and so the
 * intersection is a pure, unit-tested computation off the GL surface.
 */
record ScissorBox(int x, int y, int width, int height) {

    /**
     * Intersects this box with {@code other} into the region common to both. A non-overlap yields a
     * zero-extent box (extents are floored at zero rather than allowed to go negative), so an inner clip
     * wholly outside the box it is composed with draws nothing rather than erroring on a negative extent.
     *
     * @param other the box to narrow this one against
     * @return the overlapping region, with zero-floored extents
     */
    ScissorBox intersectWith(ScissorBox other) {
        var left = Math.max(x, other.x);
        var bottom = Math.max(y, other.y);
        var right = Math.min(x + width, other.x + other.width);
        var top = Math.min(y + height, other.y + other.height);
        return new ScissorBox(
                left,
                bottom,
                Math.max(0, right - left),
                Math.max(0, top - bottom));
    }
}
