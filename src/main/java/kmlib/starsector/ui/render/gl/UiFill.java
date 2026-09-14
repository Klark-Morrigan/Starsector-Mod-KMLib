package kmlib.starsector.ui.render.gl;

import com.fs.starfarer.api.util.Misc;

import kmlib.math.geometry.Rectangle;
import kmlib.opengl.GlBlendMode;
import kmlib.opengl.GlColour;
import kmlib.opengl.GlQuads;
import kmlib.opengl.GlTriangles;

import org.lwjgl.opengl.GL11;

import java.util.List;

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
 * <p>Raw GL passthrough (over {@link GlColour} for the colour and {@link GlQuads} for the
 * vertices), run only in-engine like the other draw helpers. It owns only the state that makes
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
        beginUntexturedPass(GlBlendMode.ALPHA);
        GlColour.set(paint.colour(), paint.alpha());
        renderQuadVertices(bounds);
    }

    /**
     * Fills every rectangle in {@code bounds} with one {@code paint}, each composited exactly as
     * {@link #renderQuad} composites a single one. A hidden paint or an empty run emits nothing. Must run
     * with a current GL context, like any immediate-mode GL call.
     *
     * <p>For a run of rectangles that are one element rather than several - the blocks a withheld name
     * draws as, a banded stroke, a dashed rule. The pipeline is set once for the whole run instead of once
     * per rectangle, which is the difference between an element costing one state change and costing as
     * many as it has parts; and the caller states its colour once, where a loop outside would restate it
     * per part and could be given two.
     *
     * @param bounds the rectangles to fill, in UI coordinates (UI origin is bottom-left)
     * @param paint  the fill colour and its compositing alpha, shared by all of them
     */
    public static void renderQuads(List<Rectangle> bounds, UiElementPaint paint) {
        if (paint.isHidden()) {
            return;
        }
        beginUntexturedPass(GlBlendMode.ALPHA);
        GlColour.set(paint.colour(), paint.alpha());

        for (var quadBounds : bounds) {
            renderQuadVertices(quadBounds);
        }
    }

    /**
     * Fills {@code bounds} with {@code paint} added to the pixels already there rather than composited
     * over them: the paint's alpha scales how much of its colour is added, and nothing is ever darkened.
     * A hidden paint emits nothing. Must run with a current GL context, like any immediate-mode GL call.
     *
     * <p>For light, as against surface. A composited quad interpolates toward its own colour, so what it
     * lands on depends on what was already there - over a transparent surface it arrives diluted, and over
     * a bright one it can darken. Added, the same pass lifts every pixel it covers by the same amount,
     * which is what an element standing on live content needs when it must brighten by a fixed step
     * whatever is showing behind it.
     *
     * @param bounds the rectangle to light, in UI coordinates (UI origin is bottom-left)
     * @param paint  the light's colour and how much of it to add
     */
    public static void renderAdditiveQuad(Rectangle bounds, UiElementPaint paint) {
        if (paint.isHidden()) {
            return;
        }
        beginUntexturedPass(GlBlendMode.ADDITIVE);
        GlColour.set(paint.colour(), paint.alpha());
        renderQuadVertices(bounds);
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
        beginUntexturedPass(GlBlendMode.ALPHA);
        GlColour.set(paint.colour(), paint.alpha());
        GlTriangles.fillTriangle(vertices);
    }

    // What every fill here draws under: no texture, blending on, and the mode this one composites by.
    // Set per call rather than for a whole pass because these are primitives a widget scatters through a
    // bracketed draw, mixing modes as it goes - the save that puts the pipeline back is the bracket's, and
    // a mode chosen per pass would be a mode the next primitive silently inherited.
    private static void beginUntexturedPass(GlBlendMode blendMode) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        blendMode.applyBlendFunction();
    }

    // The four corners of a UI box, wound for the shared quad emitter. Held apart from the passes above
    // because they differ in how the colour lands and not in where the quad is, so a change to one cannot
    // move the other.
    private static void renderQuadVertices(Rectangle bounds) {

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
}
