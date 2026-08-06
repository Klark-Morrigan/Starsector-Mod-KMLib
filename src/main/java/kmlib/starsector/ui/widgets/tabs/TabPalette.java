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
 * <p>Nor is a lift of its own for a bound key's press. That blink travels onto the {@link #hovered} shade
 * rather than past it, so it is spent on the look channel and needs no wash to name.
 *
 * @param chromeAccent the colour of the dividers, the baseline, and the selected tab's underline
 * @param unselected   the resting look of a tab the panel is not showing
 * @param selected     the look of the tab whose content the panel is showing
 * @param hovered      the look the tab under the pointer wears, selected or not - and the shade a bound
 *                     key's blink carries its tab to
 * @param clicked      the peak lift a click raises its tab by
 */
public record TabPalette(
    Color chromeAccent,
    TabLook unselected,
    TabLook selected,
    TabLook hovered,
    TabWash clicked) {

    // How far the selected tab travels toward white when the pointer lands on it. The hovered shade is
    // measured from the selected look because that is the brighter of the two the strip has to reconcile;
    // the resting tab then travels the whole way up to meet it rather than lifting by its own fraction.
    private static final float SELECTED_HOVER_WHITE_WASH = 0.15f;

    // A click reads as a flash rather than a hold, so it peaks well past the hovered shade; a lift no
    // stronger would be invisible on the tab the pointer is necessarily already over.
    private static final float CLICK_WHITE_WASH = 0.5f;

    /**
     * The live vanilla map-tab paint: the player base colour for the chrome accent, the two map-tab fills
     * the engine's own tabs settle on, the button-text colour for a resting label and the bright player
     * colour for the active one, the hovered shade both of them meet at, and the click lift toward white.
     * Everything here resolves through {@link StarsectorUiColour} on each call, so it tracks a live
     * palette change.
     *
     * <p>The fills come from {@link VanillaTabFills}, worked out from the same two settings colours a
     * vanilla tab is painted with rather than sampled off one - so a restyled install moves this strip
     * exactly as it moves the tabs above it. They answer to settings and not to the player faction
     * because the engine's own tabs do not take a faction colour; the accent and the active label around
     * them do, being ours rather than vanilla's.
     *
     * <p>The lifts over these looks blend RGB alone, so a look that starts opaque - as both fills do -
     * stays opaque through every hover, blink, and click.
     *
     * @return the vanilla map-tab palette
     */
    public static TabPalette createMapTabPalette() {

        var white = StarsectorUiColour.WHITE.resolve();
        var fill = StarsectorUiColour.VANILLA_BUTTON_BG_DARK.resolve();
        var restingLabel = StarsectorUiColour.VANILLA_BUTTON_TEXT.resolve();

        // The surface the fills are laid over. Black rather than the host's own panel fill, because a tab
        // row stands wherever its panel does - over a body, over the map where a tab has no body at all -
        // and a fill measured against one of those would be wrong in the others.
        var backdrop = StarsectorUiColour.BLACK.resolve();

        var selected = new TabLook(
            VanillaTabFills.resolveLitFill(fill, restingLabel, backdrop),
            StarsectorUiColour.VANILLA_PLAYER_BRIGHT.resolve());

        return new TabPalette(
            StarsectorUiColour.VANILLA_PLAYER_BASE.resolve(),
            new TabLook(
                VanillaTabFills.resolveRestingFill(fill, backdrop),
                restingLabel),
            selected,
            // Derived from the selected look rather than written down beside it, so the one hovered shade
            // cannot drift from the look it is measured off when either is retuned.
            selected.computeWashedLook(new TabWash(white, SELECTED_HOVER_WHITE_WASH)),
            new TabWash(white, CLICK_WHITE_WASH));
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
     * The look a tab wears part-way onto the hovered shade: the look its selection names, blended toward the
     * hovered role by however far its fade has run. The whole of the hover channel's resolution, so a tab
     * that has not moved reads exactly as its settled look and one fully hovered reads exactly as the hovered
     * role, with no separate path for either end.
     *
     * <p>Selection is a flag rather than a {@link TabLookState} because the hovered state is the destination
     * here rather than a state a tab could be asked for: a tab is resting or lit, and the hover is how far it
     * has travelled away from that.
     *
     * @param isSelected    whether this is the tab whose content the panel is showing
     * @param hoverFraction how far the tab has travelled onto the hovered shade, 0 fully off and 1 fully on
     * @return the tab's look at that point
     */
    public TabLook resolveLookAtHoverFraction(boolean isSelected, float hoverFraction) {
        return resolveLook(resolveSettledLookState(isSelected))
            .computeBlendedLook(resolveLook(TabLookState.HOVERED), hoverFraction);
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
        };
    }

    // The look a tab rests at with no pointer on it, which is the end every hover fade travels from.
    private static TabLookState resolveSettledLookState(boolean isSelected) {
        return isSelected
            ? TabLookState.SELECTED
            : TabLookState.UNSELECTED;
    }
}
