package kmlib.starsector.ui.render.gl;

import com.fs.starfarer.api.graphics.SpriteAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.graphics.StarsectorSprites;

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
     * Loads the image at {@code spritePath} and draws it into {@code bounds}, or draws nothing when the
     * asset does not resolve. The form a widget reaches for when it holds a path rather than a loaded
     * sprite - which is every widget whose content model carries images, since a content model names an
     * asset and never holds a texture.
     *
     * <p>A missing asset is skipped rather than reported, because the alternative is worse in the place
     * this is called from: a draw pass runs every frame and cannot report anything a player would see
     * only once, and a widget that abandoned its whole line over one bad crest would hide the text that
     * still reads. The layout reserved the room from the content either way, so the words around the
     * gap stay where they were laid.
     *
     * @param spritePath the image's {@code graphics} texture path
     * @param bounds     the rectangle to draw into, in UI coordinates (UI origin is bottom-left)
     * @param alpha      overall opacity, 0..1, composited over what is behind
     */
    public static void renderImage(String spritePath, Rectangle bounds, float alpha) {
        var sprite = StarsectorSprites.loadSprite(spritePath);
        if (sprite == null) {
            return;
        }
        renderQuad(sprite, bounds, alpha);
    }

    /**
     * Draws {@code sprite} into {@code bounds}, sized to the rectangle and composited over the
     * existing pixels by {@code alpha}. For a caller that already holds a loaded sprite; one holding a
     * path reaches for {@link #renderImage}. Must run with a current GL context, like any
     * immediate-mode GL call.
     *
     * @param sprite the image to draw; its size, tint, and alpha are set here before rendering
     * @param bounds the rectangle to draw into, in UI coordinates (UI origin is bottom-left)
     * @param alpha  overall opacity, 0..1, composited over what is behind
     */
    public static void renderQuad(SpriteAPI sprite, Rectangle bounds, float alpha) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        sprite.setSize(bounds.width(), bounds.height());
        sprite.setColor(Color.WHITE);
        sprite.setAlphaMult(alpha);
        sprite.render(bounds.x(), bounds.y());
    }
}
