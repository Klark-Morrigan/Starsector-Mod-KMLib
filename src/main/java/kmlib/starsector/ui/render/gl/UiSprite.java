package kmlib.starsector.ui.render.gl;

import com.fs.starfarer.api.graphics.SpriteAPI;

import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * Draws a {@link SpriteAPI} into a rectangle in screen/UI coordinates, composited over what is
 * already drawn behind it by a 0..1 opacity. The textured-quad counterpart to {@link UiFill} (which
 * fills a flat colour): where {@code UiFill} paints a solid rectangle, this paints an image into
 * one, so a KM UI widget can show a small icon - a faction crest on the political-map picker - beside
 * its text.
 *
 * <p>It sizes the sprite to the target rectangle, resets any tint to white (so the image draws in its
 * own colours), and scales its alpha by {@code opacity}, then renders it from the rectangle's
 * lower-left corner - the UI origin - so the icon lands exactly in the box the widget laid out.
 * Standard {@code GL_SRC_ALPHA} / {@code GL_ONE_MINUS_SRC_ALPHA} compositing, so opacity 0 is fully
 * transparent and 1 fully opaque, matching {@link UiFill} so an icon fades with the panel around it.
 *
 * <p>Raw GL passthrough with a live texture bound by the sprite; exercised in-engine like the other
 * draw helpers rather than in unit tests. The caller restores any GL state it depends on, as the
 * sprite render leaves the texture enable and blend it set in place.
 */
public final class UiSprite {
    private UiSprite() {
    }

    /**
     * Draws {@code sprite} into the rectangle at {@code (x, y)}, sized to {@code width} x
     * {@code height} and composited over the existing pixels by {@code alpha}. Must run with a current
     * GL context, like any immediate-mode GL call.
     *
     * @param sprite the image to draw; its size, tint, and alpha are set here before rendering
     * @param x      left edge, in UI coordinates
     * @param y      bottom edge, in UI coordinates (UI origin is bottom-left)
     * @param width  the width to scale the sprite to
     * @param height the height to scale the sprite to
     * @param alpha  overall opacity, 0..1, composited over what is behind
     */
    public static void renderQuad(SpriteAPI sprite, float x, float y, float width, float height,
            float alpha) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        sprite.setSize(width, height);
        sprite.setColor(Color.WHITE);
        sprite.setAlphaMult(alpha);
        sprite.render(x, y);
    }
}
