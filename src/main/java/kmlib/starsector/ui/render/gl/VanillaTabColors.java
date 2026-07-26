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
 * @param fillDefault  the solid fill of a resting (unselected) tab - the vanilla dark button teal
 * @param fillSelected the solid fill of the active tab - the vanilla bright button cyan
 * @param accent       the colour of the dividers, the baseline, and the selected tab's underline
 * @param tabDefault   a resting tab's label colour
 * @param tabSelected  the active tab's label colour
 * @param tabHovered   the hovered tab's label colour
 * @param hotkey       the gold the shortcut key paints in, apart from its label-coloured delimiters
 */
public record VanillaTabColors(
        Color fillDefault,
        Color fillSelected,
        Color accent,
        Color tabDefault,
        Color tabSelected,
        Color tabHovered,
        Color hotkey) {

    /**
     * The live vanilla map-tab palette: the fixed button fills (dark teal at rest, bright cyan when
     * active) matching the map's own Sector/System tabs, the player base colour for the accent, the
     * button-text colour for a resting label, the bright player colour for the active label, the base
     * player colour for a hovered label, and the highlight gold for the shortcut. The fills come from
     * the engine's shared button palette (not the player faction) so they match the vanilla tabs even
     * under a modded player faction; the labels and accent stay player-tinted. Resolves through
     * {@link StarsectorUiColor} on each call, so it tracks a live palette change.
     *
     * @return the vanilla map-tab colours
     */
    public static VanillaTabColors mapTabs() {
        return new VanillaTabColors(
                StarsectorUiColor.VANILLA_BUTTON_BG_DARK.resolve(), // Fill: resting.
                StarsectorUiColor.VANILLA_BUTTON_BG.resolve(), // Fill: selected.
                StarsectorUiColor.VANILLA_PLAYER_BASE.resolve(), // Accent.
                StarsectorUiColor.VANILLA_BUTTON_TEXT.resolve(), // Tab Default.
                StarsectorUiColor.VANILLA_PLAYER_BRIGHT.resolve(), // Tab Selected.
                StarsectorUiColor.VANILLA_PLAYER_BASE.resolve(), // Tab Hovered.
                StarsectorUiColor.VANILLA_HIGHLIGHT_GOLD.resolve()); // Hotkey.
    }
}
