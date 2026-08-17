package kmlib.opengl;

import kmlib.math.geometry.Rectangles;

import org.lwjgl.opengl.GL11;

/**
 * The pixel rectangle GL maps normalised coordinates into - what a projection's {@code -1..1} is
 * stretched across.
 *
 * <p>Worth reading rather than assuming, which is why it is published: the viewport is whatever the
 * pass in force left bound. Anything mapping a screen pixel through it - unprojecting a cursor, for
 * instance - is measuring against that rectangle and not against the screen, so a pass that
 * narrowed it and did not restore it changes the answer for every pass after it.
 *
 * <p>Distinct from {@link GlScissor}, which is about where a pass may <em>write</em> rather than
 * how its coordinates are stretched. The two are frequently the same rectangle and mean different
 * things, so they are named apart even though both read as four ints.
 */
public final class GlViewport {

    private GlViewport() {
    }

    /**
     * The bound viewport as {@code {x, y, width, height}} in pixels.
     *
     * <p>Must be called with a GL context current, which for this library's callers means from
     * inside a render pass.
     *
     * @return its four ints
     */
    public static int[] readViewport() {
        return GlPixelRectangle.readFrom(GL11.GL_VIEWPORT);
    }

    /**
     * Words a viewport for a diagnostic line.
     *
     * @param viewport the four ints, as {@link #readViewport} reports them
     * @return its origin and size, or a note that it is not a viewport at all
     */
    public static String describeViewport(int[] viewport) {
        return Rectangles.describe(GlPixelRectangle.toRectangle(viewport));
    }
}
