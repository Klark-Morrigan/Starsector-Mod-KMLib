package kmlib.opengl;

import kmlib.color.Colors;

import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * Sets the current OpenGL draw colour from an AWT {@link Color}.
 *
 * <p>The thin GL binding over {@link Colors#getGlComponents}: it normalizes
 * the colour and issues the {@code glColor4f} call so a draw routine names a
 * single {@link Color} (a literal, a palette entry, or a live faction colour)
 * instead of unpacking four channel floats at the call site.
 *
 * <p>Lives apart from {@link Colors} because it touches the GL context: the
 * colour maths stays pure and unit-tested there, while this passthrough to
 * {@code GL11} is exercised in-engine, not in tests.
 */
public final class GlColor {
    // Channel positions in the [r, g, b, a] array Colors#getGlComponents returns,
    // named so the glColor4f call reads by channel rather than bare index.
    private static final int RED = 0;
    private static final int GREEN = 1;
    private static final int BLUE = 2;
    private static final int ALPHA = 3;

    private GlColor() {
    }

    /**
     * Sets the GL draw colour to {@code color}, scaling its alpha by
     * {@code alphaMult} (e.g. a viewport/map fade). Must be called with a
     * current GL context, like any other immediate-mode GL call.
     *
     * @param color     the colour to set; its own alpha is honoured
     * @param alphaMult extra alpha scale; 1 keeps the colour's own alpha
     */
    public static void set(Color color, float alphaMult) {
        var rgba = Colors.getGlComponents(color, alphaMult);
        GL11.glColor4f(rgba[RED], rgba[GREEN], rgba[BLUE], rgba[ALPHA]);
    }
}
