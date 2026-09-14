package kmlib.opengl;

import kmlib.math.geometry.Rectangle;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Reading a four-int pixel rectangle back out of GL.
 *
 * <p>GL reports more than one rectangle about the frame - which pixels a pass maps into, which it
 * may write to - identically and meaning different things. Only the read is shared here; what each
 * rectangle <em>is</em> stays with the named type that asks for it.
 *
 * <p>The buffer size is the mechanic worth stating once: LWJGL sizes its {@code glGetInteger} check
 * against the largest result any {@code pname} can return rather than against the one being asked
 * for, so the buffer must hold sixteen ints however few the rectangle fills. A four-int buffer is
 * rejected outright, which is a crash rather than a short read.
 */
final class GlPixelRectangle {

    private static final int PIXEL_RECTANGLE_INT_COUNT = 4;

    // The order GL reports a rectangle in, for the step out of the bare int array it arrives as.
    private static final int ORIGIN_X_SLOT = 0;
    private static final int ORIGIN_Y_SLOT = 1;
    private static final int WIDTH_SLOT = 2;
    private static final int HEIGHT_SLOT = 3;

    // Sized for LWJGL's check rather than for the four ints a rectangle fills; only those four are
    // copied back out.
    private static final int GL_GET_INTEGER_MIN_BUFFER_INTS = 16;

    private GlPixelRectangle() {
    }

    /**
     * @param rectanglePname the {@code glGetInteger} name of the rectangle wanted
     * @return its four ints, freshly copied so the caller owns them rather than aliasing a scratch
     *         buffer
     */
    static int[] readFrom(int rectanglePname) {

        var rectangleBuffer = BufferUtils.createIntBuffer(GL_GET_INTEGER_MIN_BUFFER_INTS);
        GL11.glGetInteger(rectanglePname, rectangleBuffer);

        var rectangle = new int[PIXEL_RECTANGLE_INT_COUNT];
        rectangleBuffer.get(rectangle);
        return rectangle;
    }

    /**
     * The same rectangle as the value type the rest of the library measures with, for every caller
     * that wants to compare or word it rather than hand it back to GL.
     *
     * @param rectangle the four ints, as {@link #readFrom} reports them
     * @return the same box as a {@link Rectangle}, or null when it is not four ints at all
     */
    static Rectangle toRectangle(int[] rectangle) {

        if (rectangle == null || rectangle.length != PIXEL_RECTANGLE_INT_COUNT) {
            return null;
        }
        return new Rectangle(
            rectangle[ORIGIN_X_SLOT],
            rectangle[ORIGIN_Y_SLOT],
            rectangle[WIDTH_SLOT],
            rectangle[HEIGHT_SLOT]);
    }
}
