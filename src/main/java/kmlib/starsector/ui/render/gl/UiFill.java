package kmlib.starsector.ui.render.gl;

import com.fs.starfarer.api.util.Misc;

import kmlib.math.geometry.Rectangle;
import kmlib.opengl.GlColor;
import kmlib.opengl.GlQuads;
import kmlib.opengl.GlTriangles;

import org.lwjgl.opengl.GL11;

/**
 * Fills a convex primitive (a rectangle or a triangle) in screen/UI coordinates, compositing it
 * over whatever is already drawn behind it by a 0..1 opacity. The alpha-blend counterpart to
 * {@link Misc#renderQuadAlpha}, and the single fill primitive every KM UI widget draws through.
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
     * Fills {@code bounds} with {@code paint}, composited over the existing pixels by the paint's
     * alpha: 0 leaves the destination untouched, 1 paints the colour solid. A hidden paint (no
     * colour or non-positive alpha) emits nothing, since the blend would only discard the run. Must
     * run with a current GL context, like any immediate-mode GL call.
     *
     * @param bounds the rectangle to fill, in UI coordinates (UI origin is bottom-left)
     * @param paint  the fill colour and its compositing alpha
     */
    public static void renderQuad(Rectangle bounds, UiElementPaint paint) {
        if (paint.isHidden()) {
            return;
        }
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlColor.set(paint.color(), paint.alpha());
        var left = bounds.x();
        var bottom = bounds.y();
        var right = left + bounds.width();
        var top = bottom + bounds.height();
        GlQuads.fillQuad(new float[] {
                left, bottom,
                left, top,
                right, top,
                right, bottom,
        });
    }

    /**
     * Fills the triangle whose corners are {@code vertices} - a flat {@code [x1, y1, x2, y2, x3, y3]}
     * run in winding order - with {@code paint}, composited over the existing pixels exactly as
     * {@link #renderQuad} composites a rectangle. A hidden paint emits nothing. Must run with a
     * current GL context, like any immediate-mode GL call.
     *
     * @param vertices the three corners, in UI coordinates, in winding order
     * @param paint    the fill colour and its compositing alpha
     */
    public static void renderTriangle(float[] vertices, UiElementPaint paint) {
        if (paint.isHidden()) {
            return;
        }
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlColor.set(paint.color(), paint.alpha());
        GlTriangles.fillTriangle(vertices);
    }
}
