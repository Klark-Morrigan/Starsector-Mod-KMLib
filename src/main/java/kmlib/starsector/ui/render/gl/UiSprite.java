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
 * <p>It sizes the sprite to the target rectangle, multiplies it by the tint the caller states (white
 * where it states none, so the image draws in its own colours), and scales its alpha by
 * {@code opacity}, then renders it from the rectangle's lower-left corner - the UI origin - so the icon
 * lands exactly in the box the widget laid out. The tint is taken rather than fixed because some assets
 * are one shared glyph per family whose colour is declared beside the path, so drawing every caller's
 * image untinted would flatten a whole family into one shade.
 * Standard {@code GL_SRC_ALPHA} / {@code GL_ONE_MINUS_SRC_ALPHA} compositing, so opacity 0 is fully
 * transparent and 1 fully opaque, matching {@link UiFill} so an icon fades with the panel around it.
 *
 * <p>Raw GL passthrough with a live texture bound by the sprite; exercised in-engine like the other
 * draw helpers rather than in unit tests. The caller restores any GL state it depends on, as the
 * sprite render leaves the texture enable and blend it set in place.
 */
public final class UiSprite {

    // What an untinted image is multiplied by: white leaves every channel as the texture authored it,
    // so a caller stating no tint and one stating white draw the same pixels.
    private static final Color AS_AUTHORED_TINT = Color.WHITE;

    private UiSprite() {
    }

    /**
     * Loads the image at {@code spritePath} and draws it into {@code bounds} in its own colours, or
     * draws nothing when the asset does not resolve. The form a widget reaches for when it holds a path
     * rather than a loaded sprite - which is every widget whose content model carries images, since a
     * content model names an asset and never holds a texture - and whose asset states no colour of its
     * own, which is every image whose colours are in its own pixels.
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
        renderImage(spritePath, bounds, alpha, AS_AUTHORED_TINT);
    }

    /**
     * The same draw with the image multiplied by {@code tintColour}. For a caller whose asset states a
     * colour beside its path - a map glyph shared across a family of types and told apart only by the
     * colour each declares - so the shade drawn is the one the asset was authored with rather than one
     * the draw picked.
     *
     * @param spritePath the image's {@code graphics} texture path
     * @param bounds     the rectangle to draw into, in UI coordinates (UI origin is bottom-left)
     * @param alpha      overall opacity, 0..1, composited over what is behind
     * @param tintColour the colour the texture is multiplied by, or null to draw it as authored
     */
    public static void renderImage(
            String spritePath,
            Rectangle bounds,
            float alpha,
            Color tintColour) {

        var sprite = StarsectorSprites.loadSprite(spritePath);
        if (sprite == null) {
            return;
        }
        renderQuad(sprite, bounds, alpha, tintColour);
    }

    /**
     * Draws {@code sprite} into {@code bounds}, sized to the rectangle, multiplied by
     * {@code tintColour}, and composited over the existing pixels by {@code alpha}. For a caller that
     * already holds a loaded sprite; one holding a path reaches for {@link #renderImage}. Must run with
     * a current GL context, like any immediate-mode GL call.
     *
     * <p>A null tint resolves to white, which is the no-op multiply an untinted image already drew
     * through - so "no colour stated" and "drawn as authored" are the same pixels rather than two paths.
     *
     * @param sprite     the image to draw; its size, tint, and alpha are set here before rendering
     * @param bounds     the rectangle to draw into, in UI coordinates (UI origin is bottom-left)
     * @param alpha      overall opacity, 0..1, composited over what is behind
     * @param tintColour the colour the texture is multiplied by, or null to draw it as authored
     */
    public static void renderQuad(
            SpriteAPI sprite,
            Rectangle bounds,
            float alpha,
            Color tintColour) {

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        sprite.setSize(bounds.width(), bounds.height());
        sprite.setColor(tintColour == null ? AS_AUTHORED_TINT : tintColour);
        sprite.setAlphaMult(alpha);
        sprite.render(bounds.x(), bounds.y());
    }
}
