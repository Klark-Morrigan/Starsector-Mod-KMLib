package kmlib.opengl;

import kmlib.colour.Colours;

import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * Sets the current OpenGL draw colour from an AWT {@link Color}.
 *
 * <p>The thin GL binding over {@link Colours#normaliseChannel}: it normalises
 * the colour and issues the {@code glColor4f} call so a draw routine names a
 * single {@link Color} (a literal, a palette entry, or a live faction colour)
 * instead of unpacking four channel floats at the call site.
 *
 * <p>Lives apart from {@link Colours} because it touches the GL context: the
 * colour maths stays pure there, while this passthrough to {@code GL11} runs
 * only in-engine.
 */
public final class GlColour {

    private GlColour() {
    }

    /**
     * Sets the GL draw colour to {@code colour}, scaling its alpha by
     * {@code alphaMult} (e.g. a viewport/map fade). Must be called with a
     * current GL context, like any other immediate-mode GL call.
     *
     * <p>Normalises channel by channel rather than through
     * {@link Colours#getGlComponents}: a renderer sets a colour per band or per
     * layer every frame, and the array that method answers would be garbage at
     * that rate.
     *
     * @param colour    the colour to set; its own alpha is honoured
     * @param alphaMult extra alpha scale; 1 keeps the colour's own alpha
     */
    public static void set(Color colour, float alphaMult) {
        GL11.glColor4f(
            Colours.normaliseChannel(colour.getRed()),
            Colours.normaliseChannel(colour.getGreen()),
            Colours.normaliseChannel(colour.getBlue()),
            Colours.normaliseChannel(colour.getAlpha()) * alphaMult);
    }
}
