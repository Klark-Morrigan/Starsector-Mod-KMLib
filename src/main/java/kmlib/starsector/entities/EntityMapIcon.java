package kmlib.starsector.entities;

import java.awt.Color;

/**
 * The glyph the sector map marks an entity with: its sprite path and the colour that sprite is
 * drawn in.
 *
 * <p>The two travel together because neither states the icon on its own. Vanilla authors one
 * shared glyph per family - every planet type points at the same sprite, every star at another -
 * and tells the types apart by the colour alone, so a path without its colour is an icon that
 * says nothing the name did not.
 *
 * @param spritePath the glyph's sprite path, never blank
 * @param iconColour the colour the glyph is drawn in, or null where the spec authored none
 */
public record EntityMapIcon(
    String spritePath,
    Color iconColour) {
}
