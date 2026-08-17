package kmlib.opengl;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * The pixel rectangle GL is currently drawing into.
 *
 * <p>Held apart from the callers that want it because more than one does, and because the read has
 * a quirk worth stating once: LWJGL sizes its {@code glGetInteger} check against the largest result
 * any {@code pname} can return rather than against the one being asked for, so the buffer must hold
 * sixteen ints however few the viewport fills. A viewport-sized buffer is rejected outright, which
 * is a crash rather than a short read - a mistake each caller would otherwise be free to make on
 * its own.
 *
 * <p>Worth reading rather than assuming, and that is the other reason it is published: the viewport
 * is whatever the pass in force left bound. A panel that narrows it for its own drawing and does
 * not restore it leaves every later pass measuring against that rectangle, so anything mapping
 * pixels through it is mapping through someone else's frame.
 */
public final class GlViewport {

    private static final int VIEWPORT_INT_COUNT = 4;

    // The order GL reports a viewport in. Named because the four are read back as a bare int array,
    // where a bracketed index says nothing about which of the four it reaches.
    private static final int ORIGIN_X_SLOT = 0;
    private static final int ORIGIN_Y_SLOT = 1;
    private static final int WIDTH_SLOT = 2;
    private static final int HEIGHT_SLOT = 3;

    // Sized for LWJGL's check rather than for the four ints a viewport fills; only those four are
    // copied back out.
    private static final int GL_GET_INTEGER_MIN_BUFFER_INTS = 16;

    private GlViewport() {
    }

    /**
     * The bound viewport as {@code {x, y, width, height}} in pixels.
     *
     * <p>Must be called with a GL context current, which for this library's callers means from
     * inside a render pass.
     *
     * @return the four ints, freshly copied so the caller owns them rather than aliasing a scratch
     *         buffer
     */
    public static int[] readViewport() {

        var viewportBuffer = BufferUtils.createIntBuffer(GL_GET_INTEGER_MIN_BUFFER_INTS);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuffer);

        var viewport = new int[VIEWPORT_INT_COUNT];
        viewportBuffer.get(viewport);
        return viewport;
    }

    /**
     * Words a viewport for a diagnostic line.
     *
     * @param viewport the four ints, as {@link #readViewport} reports them
     * @return its origin and size, or a note that it is not a viewport at all
     */
    public static String describeViewport(int[] viewport) {

        if (viewport == null || viewport.length != VIEWPORT_INT_COUNT) {
            return "unreadable";
        }
        return "[x=" + viewport[ORIGIN_X_SLOT]
            + " y=" + viewport[ORIGIN_Y_SLOT]
            + " w=" + viewport[WIDTH_SLOT]
            + " h=" + viewport[HEIGHT_SLOT]
            + "]";
    }
}
