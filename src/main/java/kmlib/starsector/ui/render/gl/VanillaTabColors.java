package kmlib.starsector.ui.render.gl;

import kmlib.starsector.ui.color.StarsectorUiColor;

import java.awt.Color;

/**
 * The palette a {@link kmlib.starsector.ui.widgets.tabs.VanillaTabStrip} paints with, one colour per
 * role. Kept as a record so a
 * consumer can override any shade, with {@link #mapTabs()} supplying the live vanilla map-tab
 * defaults through the {@link StarsectorUiColor} palette, so the strip recolours with the
 * current player faction and never receives a null shade from an early-boot accessor.
 *
 * @param backdrop      the black fill behind every tab, so labels read over the map
 * @param accent        the player-colour wash lighting the selected/hovered tab, plus the
 *                      dividers, baseline, and selected underline
 * @param labelDefault  a resting tab's label colour
 * @param labelSelected the active tab's label colour
 * @param labelHovered  the hovered tab's label colour
 * @param shortcut      the gold the bracketed shortcut key paints in
 */
public record VanillaTabColors(Color backdrop, Color accent, Color labelDefault,
        Color labelSelected, Color labelHovered, Color shortcut) {

    /**
     * The live vanilla map-tab palette: a black backdrop, the player base colour as the accent
     * and hovered label, the bright player colour for the active label, the button-text colour at
     * rest, and the highlight gold for the shortcut. Resolves through {@link StarsectorUiColor} on
     * each call, so it tracks a player-faction recolour (Nex, modded factions).
     *
     * @return the vanilla map-tab colours
     */
    public static VanillaTabColors mapTabs() {
        return new VanillaTabColors(
                StarsectorUiColor.BLACK.resolve(),
                StarsectorUiColor.VANILLA_PLAYER_BASE.resolve(),
                StarsectorUiColor.VANILLA_BUTTON_TEXT.resolve(),
                StarsectorUiColor.VANILLA_PLAYER_BRIGHT.resolve(),
                StarsectorUiColor.VANILLA_PLAYER_BASE.resolve(),
                StarsectorUiColor.VANILLA_HIGHLIGHT_GOLD.resolve());
    }
}
