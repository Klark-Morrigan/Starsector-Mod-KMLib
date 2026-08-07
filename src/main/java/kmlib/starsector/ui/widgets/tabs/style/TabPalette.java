package kmlib.starsector.ui.widgets.tabs.style;

import kmlib.starsector.ui.colour.StarsectorUiColour;
import kmlib.starsector.ui.widgets.tabs.VanillaTabStrip;

import java.awt.Color;

/**
 * The paint a {@link VanillaTabStrip} wears: the accent its chrome is ruled in, the settled look of each
 * {@link TabLookState}, and the lift each {@link TabWashState} raises a tab by. Kept as a record so a
 * consumer can override any one of them, with {@link #createMapTabPalette} supplying the live vanilla
 * map-tab values through the {@link StarsectorUiColour} palette, so a strip follows a restyled install
 * and never receives a null shade from an early-boot accessor.
 *
 * <p>What it follows is the install's settings, not the player's faction. Every fill and every label is
 * worked out from the two colours the engine paints its own tabs with, which take no faction tint at all.
 * The chrome accent ruling the row is the one value with no vanilla counterpart to copy - a vanilla tab
 * strip has no such rule - so the factory takes it from its caller rather than picking one: which shade a
 * row is ruled in is a question about the panel the row belongs to, and only that panel can answer it.
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
 * @param chromeAccent the colour of the dividers and the baseline grounding the row
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

    // How far a pressed tab lifts past the shade it was already wearing. Modest, because it is measured
    // along the same glow axis the fills are and so reads against them rather than across them: the press
    // has only to stand above the pointed-at tab it is necessarily already showing, not to reach a shade
    // the strip has no other use for.
    private static final float PRESS_GLOW_LIFT = 0.25f;

    /**
     * The live vanilla map-tab paint: the caller's accent for the chrome, the three map-tab fills and
     * labels the engine's own tabs settle on, and the lift a press raises one by. Everything vanilla
     * here resolves through {@link StarsectorUiColour} on each call, so it tracks a live palette change.
     *
     * <p>Both come from {@link VanillaTabFills}, worked out from the same two settings colours a vanilla
     * tab is painted with rather than sampled off one - so a restyled install moves this strip exactly as
     * it moves the tabs above it. They answer to settings and not to the player faction because the
     * engine's own tabs take no faction colour at all.
     *
     * <p>The three fills are one shade at the engine's three glow amounts, not three shades: resting
     * unlit, the shown tab at {@link VanillaTabFills#SELECTED_GLOW}, and the tab under the pointer at the
     * full {@link VanillaTabFills#POINTED_GLOW}. The pointer's being the brighter of the two lit amounts
     * is what lets a strip mark the shown tab by fill alone - a pointed-at tab outshines it rather than
     * matching it, so the two never read alike.
     *
     * <p>The labels are one shade at those same three amounts, for the same reason and by the same rule:
     * the engine lights a tab with one glow pass over the whole of it, so a label is never a colour a
     * state picks but the {@code buttonText} blue lit by however brightly that state stands. That is what
     * makes a resting tab's text read as the raw blue while a shown or pointed-at tab's whitens out - and
     * a state given a colour of its own instead would part the text from the fill under it at exactly the
     * amounts vanilla keeps them together.
     *
     * <p>The lifts over these looks blend RGB alone, so a look that starts opaque - as all three fills do -
     * stays opaque through every hover, blink, and click.
     *
     * @param chromeAccent the shade the row's dividers and baseline are ruled in, the panel's own to
     *                     choose - see the note on this type
     * @return the vanilla map-tab palette
     */
    public static TabPalette createMapTabPalette(Color chromeAccent) {

        var restingLabel = StarsectorUiColour.VANILLA_BUTTON_TEXT.resolve();

        // The engine's own tab paint. The backdrop is black rather than the host's own panel fill, because
        // a tab row stands wherever its panel does - over a body, over the map where a tab has no body at
        // all - and a fill measured against one of those would be wrong in the others.
        var tabPaint = new VanillaTabPaint(
            StarsectorUiColour.VANILLA_BUTTON_BG_DARK.resolve(),
            restingLabel,
            StarsectorUiColour.BLACK.resolve());

        return new TabPalette(
            chromeAccent,
            new TabLook(
                VanillaTabFills.resolveRestingFill(tabPaint),
                restingLabel),
            new TabLook(
                VanillaTabFills.resolveFillAtGlow(tabPaint, VanillaTabFills.SELECTED_GLOW),
                VanillaTabFills.resolveLabelAtGlow(tabPaint, VanillaTabFills.SELECTED_GLOW)),
            new TabLook(
                VanillaTabFills.resolveFillAtGlow(tabPaint, VanillaTabFills.POINTED_GLOW),
                VanillaTabFills.resolveLabelAtGlow(tabPaint, VanillaTabFills.POINTED_GLOW)),
            // The press lifts along the glow rather than toward white: the engine brightens a tab by adding
            // its own glow colour, so a press aimed at white would be the one shade on the strip travelling
            // in a direction none of the fills do - most visible exactly when the player is looking at it.
            new TabWash(VanillaTabFills.resolveGlowColour(restingLabel), PRESS_GLOW_LIFT));
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
