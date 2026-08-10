package kmlib.starsector.ui.widgets.tabs.style;

import kmlib.starsector.ui.colour.AccentColours;
import kmlib.starsector.ui.colour.StarsectorUiColour;
import kmlib.starsector.ui.widgets.tabs.VanillaTabStrip;

import java.awt.Color;

/**
 * The paint a {@link VanillaTabStrip} wears: the accent its chrome is ruled in, the settled look of each
 * {@link TabLookState}, and the lift each {@link TabWashState} raises a tab by. Kept as a record so a
 * consumer can override any one of them, with a factory per chrome supplying the live vanilla values -
 * {@link #createMapTabPalette} for the map's strip and {@link #createRaisedButtonPalette} for the intel
 * screen's buttons - so a row follows a restyled install and never receives a null shade from an
 * early-boot accessor.
 *
 * <p>The two factories differ in where their shades come from, because the engine's own two chromes do.
 * A vanilla tab is painted from the button roles in settings and takes no faction tint at all, so the map
 * factory reads those roles through {@link StarsectorUiColour} itself; a vanilla button is built from a
 * whole three-colour accent and takes nothing else, so the button factory is handed one. What each
 * factory cannot pick is which accent that is: which palette a row answers to is a question about the
 * panel it belongs to, and only that panel can answer it.
 *
 * <p>Three flavours sit here for the reason they are separate types: a look is a shade a tab settles on, a
 * momentary state a brief lift over whichever look it has settled on, and a {@link TabHover} what the
 * pointer does to whichever look a tab is wearing - which the two chromes disagree about, one meeting at a
 * shade and the other lighting from where it stands, so it is a rule the palette states rather than a
 * shade every palette must have.
 *
 * <p>Substrate-independent, like the rest of this package: colour values and unit fractions, with nothing
 * GL about them, so the whole description travels inside the {@link TabStyle} the layout is measured
 * against and the renderer paints from.
 *
 * <p>The bound key's gold is not here. It reads the same on every tab whatever is happening to it, so it
 * sits with the rest of a key's presentation on {@link HotkeyStyle} rather than among values that answer
 * to a tab's state.
 *
 * <p>Nor is a lift of its own for a bound key's press. That blink carries its tab as far into the
 * {@link #hover} rule as the pointer would and no further, so it is spent on the look channel and needs no
 * wash to name.
 *
 * @param chromeAccent the colour of the dividers and the baseline grounding the row
 * @param unselected   the resting look of a tab the panel is not showing
 * @param selected     the look of the tab whose content the panel is showing
 * @param hover        what the pointer does to a tab of either kind - and what a bound key's blink
 *                     carries its tab into
 * @param clicked      the peak lift a click raises its tab by
 */
public record TabPalette(
    Color chromeAccent,
    TabLook unselected,
    TabLook selected,
    TabHover hover,
    TabWash clicked) {

    // How far a pressed tab lifts past the shade it was already wearing. Modest, because it is measured
    // along the same glow axis the fills are and so reads against them rather than across them: the press
    // has only to stand above the pointed-at tab it is necessarily already showing, not to reach a shade
    // the strip has no other use for.
    private static final float PRESS_GLOW_LIFT = 0.25f;

    // A press that shows nothing, for a chrome copying an engine control that takes no pressed state at
    // all. Stated as a depth of zero rather than as an absent wash: the lift is still resolved and still
    // decays, so the moments the panel hangs on a running pulse - the press sound among them - are the
    // same under either chrome.
    private static final float NO_PRESS_LIFT = 0f;

    /**
     * The live vanilla map-tab paint: the caller's accent for the chrome, the three map-tab fills and
     * labels the engine's own tabs settle on, and the lift a press raises one by. Everything vanilla
     * here resolves through {@link StarsectorUiColour} on each call, so it tracks a live palette change.
     *
     * <p>The fills come from {@link VanillaTabFills}, worked out from the two settings colours a vanilla
     * tab is painted with rather than sampled off one - so a restyled install moves this strip exactly as
     * it moves the tabs above it. Every shade here answers to settings and not to the player faction,
     * because the engine's own tabs take no faction colour at all; only the chrome accent the caller hands
     * in is free to.
     *
     * <p>The three fills are one shade at the engine's three glow amounts, not three shades: resting
     * unlit, the shown tab at {@link VanillaTabFills#SELECTED_GLOW}, and the tab under the pointer at the
     * full {@link VanillaTabFills#POINTED_GLOW}. The pointer's being the brighter of the two lit amounts
     * is what lets a strip mark the shown tab by fill alone - a pointed-at tab outshines it rather than
     * matching it, so the two never read alike.
     *
     * <p>The labels do not follow those amounts at all. The engine parts a resting tab's text from a lit
     * one's by naming a second colour rather than by brightening the first: the {@code buttonText} blue
     * every button's text is while the tab is untouched, and the {@code standardTextColor} grey the rest
     * of the interface reads in once it is shown or pointed at. Both lit states take that grey, their
     * fills already standing at different glows, so the fill is what tells them apart.
     *
     * <p>That is a measurement rather than a derivation, and it replaced two rules that computed the label
     * off the fill's glow. Brightening a colour already at full blue can only push it sideways into cyan,
     * which no amount tuned into such a rule escapes; solving a lit vanilla tab's sampled label for the
     * glyph's coverage gives irreconcilable answers against the blue and one consistent answer against the
     * grey.
     *
     * <p>Every fill here is opaque, so a tab painted from this palette stays a surface throughout: a lift
     * blends RGB alone, and a fade between two of these looks interpolates an alpha that is 255 at both
     * ends.
     *
     * @param chromeAccent the shade the row's dividers and baseline are ruled in, the panel's own to
     *                     choose - see the note on this type
     * @return the vanilla map-tab palette
     */
    public static TabPalette createMapTabPalette(Color chromeAccent) {

        var restingLabel = StarsectorUiColour.VANILLA_BUTTON_TEXT.resolve();

        // What a lit tab reads its label in. A second role rather than the resting one brightened: the
        // engine parts them by colour, not by amount - an untouched tab wears the blue every button's text
        // is, and one it is showing wears the grey the rest of the interface reads in. Measured off the
        // engine's own Sector/System tabs, whose lit label samples as this grey through the glyph's
        // coverage and cannot be reconciled with any lifted form of the blue.
        var litLabel = StarsectorUiColour.VANILLA_TEXT.resolve();

        // The engine's own tab paint. The backdrop is black rather than the host's own panel fill, because
        // a tab row stands wherever its panel does - over a body, over the map where a tab has no body at
        // all - and a fill measured against one of those would be wrong in the others. Its label colour is
        // the resting one throughout, that being what the fills' glow is measured off whatever the text
        // above them is doing.
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
                litLabel),
            // One shade both tabs meet at, which is the engine's own strip rule. The two lit states share
            // the label and are told apart by their fills, which already stand at different glows - only
            // the shown tab's label has been measured; the pointed-at one taking the same grey is the
            // smaller claim, and the fill is what marks the difference in either case.
            new TabHover.MeetingShade(new TabLook(
                VanillaTabFills.resolveFillAtGlow(tabPaint, VanillaTabFills.POINTED_GLOW),
                litLabel)),
            // The press lifts along the glow rather than toward white: the engine brightens a tab by adding
            // its own glow colour, so a press aimed at white would be the one shade on the strip travelling
            // in a direction none of the fills do - most visible exactly when the player is looking at it.
            new TabWash(VanillaTabFills.resolveGlowColour(restingLabel), PRESS_GLOW_LIFT));
    }

    /**
     * The vanilla raised-button paint, from the three steps of the accent the engine builds one of its own
     * buttons with. Where the map-tab palette above reads the engine for its shades, this one is handed
     * them: a vanilla button takes its whole look from the faction's three-colour set rather than from the
     * button roles in settings, so which set that is is the panel's question and not this factory's.
     *
     * <p>The three looks are the dark step at the two lit amounts {@link VanillaButtonFills} names, plus an
     * unpainted resting interior. Resting is stated as a fill at zero alpha rather than as a state the
     * chrome skips, so a button not being shown is the chrome's own backing and frame with nothing inside
     * them - and the travel onto either lit state is that one interior fading in.
     *
     * <p>The labels part by colour rather than by amount, as the map tabs' do: an untouched button reads in
     * the accent it was built from, and one being shown or pointed at in the brighter step reserved for
     * marks that must stand against it.
     *
     * <p>A press paints nothing here. The engine's own intel buttons hold no lit shade while the pointer is
     * down - they answer a click with their sound alone - so the click lift is stated at zero strength
     * rather than left for the chrome to opt out of: the pulse still runs, and the panel still sounds it,
     * because what a press does is the palette's to say and not the paint pass's.
     *
     * <p>The whole accent rather than the steps it needs, and that is why {@link AccentColours} is a
     * neutral value: all three arrive here, in an order a caller cannot transpose. Handed over loose they
     * would be three same-typed arguments whose only guard is their names at the call site, and a dark
     * swapped with a bright compiles, paints, and reads as a button lit inside out.
     *
     * @param accent the accent the buttons are built from - its dark step fills and frames them, its base
     *               is what the pointer adds and what an untouched label reads in, and its bright step
     *               labels the one being shown
     * @return the vanilla raised-button palette
     */
    public static TabPalette createRaisedButtonPalette(AccentColours accent) {

        // The backing the chrome lays under every button, which the dark step is composited onto. Black
        // rather than the host's own panel fill for the reason the tab paint above takes black: a button
        // row stands wherever its panel does, and a shade measured against one surface would be wrong over
        // the others.
        var buttonPaint = new VanillaButtonPaint(
            accent.dark(),
            StarsectorUiColour.BLACK.resolve());

        return new TabPalette(
            accent.dark(),
            new TabLook(
                VanillaButtonFills.resolveUnpaintedFill(buttonPaint),
                accent.base()),
            new TabLook(
                VanillaButtonFills.resolveShownFill(buttonPaint),
                accent.bright()),
            // The pointer adds its base step to whatever a button already wears, which is the engine's own
            // button rule and the reason this is a glow rather than a shade: the shown button lights from
            // its lit interior and an unshown one from an unpainted one, so the two never meet under the
            // pointer the way a strip's tabs do. An unshown button rests on an unpainted interior, and the
            // light is what paints it: as much of the accent as the pointer has brought, no more. The
            // accent rather than white - see the fit on VanillaButtonFills.POINTED_GLOW.
            new TabHover.AddedGlow(accent.base(), VanillaButtonFills.POINTED_GLOW),
            // Aimed along the same axis the interiors brighten by, so the day this chrome does want a
            // visible press it lifts toward the shade its lit states already travel to rather than toward
            // one named nowhere.
            new TabWash(accent.base(), NO_PRESS_LIFT));
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
        };
    }

    /**
     * The look a tab wears part-way into being pointed at: the look its selection names, carried however far
     * its fade has run by whichever hover rule this palette answers by. The whole of the hover channel's
     * resolution, so a tab that has not moved reads exactly as its settled look and one fully hovered reads
     * exactly as that rule's destination, with no separate path for either end.
     *
     * <p>Selection is a flag rather than a {@link TabLookState} because being pointed at is the departure
     * here rather than a state a tab could be asked for: a tab is resting or lit, and the hover is how far
     * it has travelled away from that.
     *
     * @param isSelected    whether this is the tab whose content the panel is showing
     * @param hoverFraction how far the tab has travelled into being pointed at, 0 fully off and 1 fully on
     * @return the tab's look at that point
     */
    public TabLook resolveLookAtHoverFraction(boolean isSelected, float hoverFraction) {
        return hover.computeHoveredLook(
            resolveLook(resolveSettledLookState(isSelected)),
            hoverFraction);
    }

    /**
     * The light to lay over a finished tab at that point of its fade - the other half of the hover
     * channel, and empty for a rule that answers the pointer with a shade instead.
     *
     * <p>Selection is not asked for: light is added to whatever the tab turned out to look like, so which
     * shade that was is already spent by the time this lands.
     *
     * @param hoverFraction how far the tab has travelled into being pointed at, 0 fully off and 1 fully on
     * @return the light to add, or {@link TabLight#NONE}
     */
    public TabLight resolveLightAtHoverFraction(float hoverFraction) {
        return hover.computeAddedLight(hoverFraction);
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
