package kmlib.starsector.ui.render;

import com.fs.starfarer.api.util.Misc;

import kmlib.opengl.GlColor;
import kmlib.opengl.GlQuads;

import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * Fills a rectangle in screen/UI coordinates, compositing it over whatever is already drawn
 * behind it by a 0..1 opacity. The alpha-blend counterpart to {@link Misc#renderQuadAlpha},
 * and the single fill primitive every KM UI widget draws through.
 *
 * <p>Why not {@code Misc.renderQuadAlpha}: that helper blends with {@code GL_SRC_ALPHA} /
 * {@code GL_ZERO}, which discards the destination and writes {@code colour * alpha}. Over live
 * content a black fill then stays solid black at every opacity - it can never reveal what is
 * behind it, so a panel backdrop meant to fade with an opacity setting instead reads as an
 * opaque block. This uses standard {@code GL_SRC_ALPHA} / {@code GL_ONE_MINUS_SRC_ALPHA}
 * compositing, so opacity 0 is fully transparent (the backdrop vanishes and the map shows
 * through) and 1 fully opaque.
 *
 * <p>Raw GL passthrough (over {@link GlColor} for the colour and {@link GlQuads} for the
 * vertices), exercised in-engine like the other draw helpers. It owns only the state that makes
 * the fill composite - texture off, standard alpha blend - and leaves the quad emission to the
 * shared {@link GlQuads#fillQuad}.
 */
public final class UiFill {
    private UiFill() {
    }

    /**
     * Fills the rectangle at {@code (x, y)} with {@code color}, composited over the existing
     * pixels by {@code alpha}: 0 leaves the destination untouched, 1 paints the colour solid.
     * Must run with a current GL context, like any immediate-mode GL call.
     *
     * @param x      left edge, in UI coordinates
     * @param y      bottom edge, in UI coordinates (UI origin is bottom-left)
     * @param width  rectangle width
     * @param height rectangle height
     * @param color  fill colour; its own alpha is honoured and further scaled by {@code alpha}
     * @param alpha  overall opacity, 0..1, composited over what is behind
     */
    public static void renderQuad(float x, float y, float width, float height, Color color,
            float alpha) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlColor.set(color, alpha);
        GlQuads.fillQuad(new float[] {
                x, y,
                x, y + height,
                x + width, y + height,
                x + width, y,
        });
    }
}
