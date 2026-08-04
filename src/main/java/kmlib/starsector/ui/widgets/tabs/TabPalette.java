package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.colour.StarsectorUiColour;

import java.awt.Color;

/**
 * The paint a {@link VanillaTabStrip} wears: the accent its chrome is ruled in, the absolute look of each
 * {@link TabBaseState}, and the lift each {@link TabWashState} raises a tab by. Kept as a record so a
 * consumer can override any one of them, with {@link #createMapTabPalette()} supplying the live vanilla
 * map-tab values through the {@link StarsectorUiColour} palette, so a strip recolours with the current
 * player faction and never receives a null shade from an early-boot accessor.
 *
 * <p>Two flavours sit here for the reason they are separate types: a base state is an absolute look a tab
 * holds, a momentary state a relative lift over whichever base it is in. Splitting them is what lets one
 * hover value serve both the resting and the selected tab.
 *
 * <p>Substrate-independent, like the rest of this package: {@link Color} values and unit fractions, with
 * nothing GL about them, so the whole description travels inside the {@link TabStyle} the layout is
 * measured against and the renderer paints from.
 *
 * <p>The bound key's gold is not here. It reads the same on every tab whatever is happening to it, so it
 * sits with the rest of a key's presentation on {@link HotkeyStyle} rather than among values that answer
 * to a tab's state.
 *
 * @param chromeAccent the colour of the dividers, the baseline, and the selected tab's underline
 * @param unselected   the resting look of a tab the panel is not showing
 * @param selected     the look of the tab whose content the panel is showing
 * @param hovered      the lift the tab under the pointer holds
 * @param clicked      the peak lift a click raises its tab by
 * @param hotkeyed     the peak lift a bound key's press raises its tab by
 */
public record TabPalette(
    Color chromeAccent,
    TabBaseLook unselected,
    TabBaseLook selected,
    TabWash hovered,
    TabWash clicked,
    TabWash hotkeyed) {

    // How far a hovered tab lifts toward white - a small constant hold, so the tab under the pointer
    // reads brighter than its resting state the way the vanilla map tabs do.
    private static final float HOVER_WHITE_WASH = 0.15f;

    // A click reads as a flash rather than a hold, so it peaks several times the hover's depth; a lift
    // no stronger than the hover would be invisible on the tab the pointer is necessarily already over.
    private static final float CLICK_WHITE_WASH = 0.5f;

    // A bound key's blink reaches exactly the level a hover holds, so pressing the key for the tab
    // already under the pointer shows nothing: the two compose by the stronger, and the hover wins
    // outright. The no-op falls out of the values rather than out of a rule that checks for it.
    private static final float HOTKEY_WHITE_WASH = HOVER_WHITE_WASH;

    // TODO: no animator drives the click and hotkey lifts yet - a strip lifts only the tab under the
    // pointer until per-tab pulse timing lands, at which point these peaks are what it decays from.

    /**
     * The live vanilla map-tab paint: the player base colour for the chrome accent, the fixed map-tab
     * fills (the dark teal {@code buttonBgDark} at rest, the sampled steel-blue when active) matching the
     * map's own Sector/System tabs, the button-text colour for a resting label and the bright player
     * colour for the active one, and the three interaction lifts, all toward white. The fills are fixed UI
     * shades rather than player-faction ones so they match the vanilla tabs even under a modded player
     * faction; the labels and the accent stay player-tinted. Resolves through {@link StarsectorUiColour}
     * on each call, so it tracks a live palette change.
     *
     * @return the vanilla map-tab palette
     */
    public static TabPalette createMapTabPalette() {

        var white = StarsectorUiColour.WHITE.resolve();
        
        return new TabPalette(
            StarsectorUiColour.VANILLA_PLAYER_BASE.resolve(),
            new TabBaseLook(
                StarsectorUiColour.VANILLA_BUTTON_BG_DARK.resolve(),
                StarsectorUiColour.VANILLA_BUTTON_TEXT.resolve()),
            new TabBaseLook(
                StarsectorUiColour.STEEL_BLUE.resolve(),
                StarsectorUiColour.VANILLA_PLAYER_BRIGHT.resolve()),
            new TabWash(white, HOVER_WHITE_WASH),
            new TabWash(white, CLICK_WHITE_WASH),
            new TabWash(white, HOTKEY_WHITE_WASH));
    }

    /**
     * The absolute look a tab in the given resting state wears.
     *
     * @param baseState the tab's resting state
     * @return that state's fill and label colour
     */
    public TabBaseLook resolveBaseLook(TabBaseState baseState) {
        return switch (baseState) {
            case UNSELECTED -> unselected;
            case SELECTED -> selected;
        };
    }

    /**
     * The lift a tab takes from the given momentary state at its full depth. A caller animating the lift
     * scales it down as the pulse decays; a held state uses it as it stands.
     *
     * @param washState the momentary state being applied
     * @return that state's wash at full depth
     */
    public TabWash resolveWash(TabWashState washState) {
        return switch (washState) {
            case HOVERED -> hovered;
            case CLICKED -> clicked;
            case HOTKEYED -> hotkeyed;
        };
    }
}
