package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.colour.StarsectorUiColour;

import java.awt.Color;

/**
 * The palette a {@link VanillaTabStrip} paints with, one colour per role. Kept as a record so a
 * consumer can override any shade, with {@link #mapTabs()} supplying the live vanilla map-tab
 * defaults through the {@link StarsectorUiColour} palette, so the strip recolours with the
 * current player faction and never receives a null shade from an early-boot accessor.
 *
 * <p>Substrate-independent, like the rest of this package: it is a palette of {@link Color} with
 * nothing GL about it, so it travels inside the {@link TabStyle} the layout is measured against and
 * the renderer paints from, rather than sitting on the paint side of the layout/paint split.
 *
 * <p>Every role here is a tab's own - a fill or a label a tab wears differently depending on its state.
 * The bound key's gold is not: it reads the same on every tab, so it sits with the rest of a key's
 * presentation on {@link HotkeyStyle} rather than among these.
 *
 * @param fillDefault  the solid fill of a resting (unselected) tab - the vanilla dark button teal
 * @param fillSelected the solid fill of the active tab - the vanilla selected map-tab steel-blue
 * @param accent       the colour of the dividers, the baseline, and the selected tab's underline
 * @param tabDefault   a resting tab's label colour
 * @param tabSelected  the active tab's label colour
 * @param tabHovered   the hovered tab's label colour
 */
public record VanillaTabColours(
        Color fillDefault,
        Color fillSelected,
        Color accent,
        Color tabDefault,
        Color tabSelected,
        Color tabHovered) {

    /**
     * The live vanilla map-tab palette: the fixed map-tab fills (the dark teal {@code buttonBgDark} at
     * rest, the sampled selected steel-blue when active) matching the map's own Sector/System tabs, the
     * player base colour for the accent, the button-text colour for a resting label, the bright player
     * colour for the active label, and the base player colour for a hovered label. The fills are fixed UI
     * shades (not the player faction) so they match the vanilla tabs even under a modded player faction;
     * the labels and accent stay player-tinted. Resolves through {@link StarsectorUiColour} on each call,
     * so it tracks a live palette change.
     *
     * @return the vanilla map-tab colours
     */
    public static VanillaTabColours mapTabs() {
        return new VanillaTabColours(
            StarsectorUiColour.VANILLA_BUTTON_BG_DARK.resolve(), // Fill: resting.
            StarsectorUiColour.STEEL_BLUE.resolve(), // Fill: selected.
            StarsectorUiColour.VANILLA_PLAYER_BASE.resolve(), // Accent.
            StarsectorUiColour.VANILLA_BUTTON_TEXT.resolve(), // Tab Default.
            StarsectorUiColour.VANILLA_PLAYER_BRIGHT.resolve(), // Tab Selected.
            StarsectorUiColour.VANILLA_PLAYER_BASE.resolve()); // Tab Hovered.
    }
}
