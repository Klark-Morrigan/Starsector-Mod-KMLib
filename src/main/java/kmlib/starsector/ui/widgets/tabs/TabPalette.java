package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.colour.StarsectorUiColour;

import java.awt.Color;

/**
 * The paint a {@link VanillaTabStrip} wears: the accent its chrome is ruled in, the settled look of each
 * {@link TabLookState}, and the lift each {@link TabWashState} raises a tab by. Kept as a record so a
 * consumer can override any one of them, with {@link #createMapTabPalette()} supplying the live vanilla
 * map-tab values through the {@link StarsectorUiColour} palette, so a strip recolours with the current
 * player faction and never receives a null shade from an early-boot accessor.
 *
 * <p>Two flavours sit here for the reason they are separate types: a look is a shade a tab settles on, a
 * momentary state a brief lift over whichever look it has settled on. Naming the hovered shade outright
 * rather than deriving it per tab is what lets the resting and the selected tab meet under the pointer.
 *
 * <p>Substrate-independent, like the rest of this package: colour values and unit fractions, with nothing
 * GL about them, so the whole description travels inside the {@link TabStyle} the layout is measured
 * against and the renderer paints from.
 *
 * <p>The bound key's gold is not here. It reads the same on every tab whatever is happening to it, so it
 * sits with the rest of a key's presentation on {@link HotkeyStyle} rather than among values that answer
 * to a tab's state.
 *
 * @param chromeAccent the colour of the dividers, the baseline, and the selected tab's underline
 * @param unselected   the resting look of a tab the panel is not showing
 * @param selected     the look of the tab whose content the panel is showing
 * @param hovered      the look the tab under the pointer wears, selected or not
 * @param clicked      the peak lift a click raises its tab by
 * @param hotkeyed     the peak lift a bound key's press raises its tab by
 */
public record TabPalette(
    Color chromeAccent,
    TabLook unselected,
    TabLook selected,
    TabLook hovered,
    TabWash clicked,
    TabWash hotkeyed) {

    // How far the selected tab travels toward white when the pointer lands on it. The hovered shade is
    // measured from the selected look because that is the brighter of the two the strip has to reconcile;
    // the resting tab then travels the whole way up to meet it rather than lifting by its own fraction.
    private static final float SELECTED_HOVER_WHITE_WASH = 0.15f;

    // A click reads as a flash rather than a hold, so it peaks well past the hovered shade; a lift no
    // stronger would be invisible on the tab the pointer is necessarily already over.
    private static final float CLICK_WHITE_WASH = 0.5f;

    // A bound key's blink is the quieter of the two pulses - it confirms a keypress rather than marking a
    // pointer landing on the tab.
    private static final float HOTKEY_WHITE_WASH = 0.15f;

    // TODO: no animator drives the click and hotkey lifts yet - a strip paints its looks and nothing
    // more until per-tab pulse timing lands, at which point these peaks are what it decays from. The
    // hotkey blink's "no louder than a hover" rule needs restating against the hovered look then, since
    // the hovered shade is no longer a wash depth two pulses can be compared with.

    /**
     * The live vanilla map-tab paint: the player base colour for the chrome accent, the fixed map-tab
     * fills (the dark teal {@code buttonBgDark} at rest, the sampled steel-blue when active) matching the
     * map's own Sector/System tabs, the button-text colour for a resting label and the bright player
     * colour for the active one, the hovered shade both of them meet at, and the two pulse lifts toward
     * white. The fills are fixed UI shades rather than player-faction ones so they match the vanilla tabs
     * even under a modded player faction; the labels and the accent stay player-tinted. Resolves through
     * {@link StarsectorUiColour} on each call, so it tracks a live palette change.
     *
     * @return the vanilla map-tab palette
     */
    public static TabPalette createMapTabPalette() {

        var white = StarsectorUiColour.WHITE.resolve();

        var selected = new TabLook(
            StarsectorUiColour.STEEL_BLUE.resolve(),
            StarsectorUiColour.VANILLA_PLAYER_BRIGHT.resolve());

        return new TabPalette(
            StarsectorUiColour.VANILLA_PLAYER_BASE.resolve(),
            new TabLook(
                StarsectorUiColour.VANILLA_BUTTON_BG_DARK.resolve(),
                StarsectorUiColour.VANILLA_BUTTON_TEXT.resolve()),
            selected,
            // Derived from the selected look rather than written down beside it, so the one hovered shade
            // cannot drift from the look it is measured off when either is retuned.
            selected.computeWashedLook(new TabWash(white, SELECTED_HOVER_WHITE_WASH)),
            new TabWash(white, CLICK_WHITE_WASH),
            new TabWash(white, HOTKEY_WHITE_WASH));
    }

    /**
     * The settled shade a tab in the given look state wears.
     *
     * @param lookState which of the three looks the tab is showing
     * @return that state's fill and label colour
     */
    public TabLook resolveLook(TabLookState lookState) {
        return switch (lookState) {
            case UNSELECTED -> unselected;
            case SELECTED -> selected;
            case HOVERED -> hovered;
        };
    }

    /**
     * The lift a tab takes from the given momentary state at its full depth. A caller animating the lift
     * scales it down as the pulse decays.
     *
     * @param washState the momentary state being applied
     * @return that state's wash at full depth
     */
    public TabWash resolveWash(TabWashState washState) {
        return switch (washState) {
            case CLICKED -> clicked;
            case HOTKEYED -> hotkeyed;
        };
    }
}
