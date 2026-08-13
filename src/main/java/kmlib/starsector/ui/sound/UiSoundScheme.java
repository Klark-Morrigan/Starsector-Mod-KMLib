package kmlib.starsector.ui.sound;

/**
 * What each moment a control answers sounds like: the press it confirms, and the pointer arriving on it.
 * The middle of this package - {@link StarsectorUiSound} names the roles, {@link UiSoundCue} binds a role
 * to a volume, this says which cue belongs to which moment, and {@link UiSoundPlayer} is where they go.
 *
 * <p>A value the look carries rather than a cue each controller names in its own code, because a sound is
 * a property of a look exactly as a fill is - it is part of how a panel presents itself, and a panel
 * presenting itself differently may well answer differently. That covers how loud as much as what: a
 * volume is a component of the look for the same reason a fill colour is, so it comes in from the host
 * with the rest of the scheme rather than being fixed by the library.
 *
 * <p>Naming the cues in one value is also the only way a panel can be quietened at all. A controller that
 * named its own would have to be visited, control by control, to change or silence a scheme; a value
 * handed in is changed where the look is composed.
 *
 * <p>A null cue is a silent moment, so silence is something a look states rather than a call somebody
 * forgot to write. That is the difference between a control deliberately quiet under the pointer and one
 * whose mouseover was never wired: the first is written down where the scheme is, the second is nowhere.
 * It is also what a host wanting silence states, rather than a cue at zero volume - a sound played at
 * nothing is still a sound played, and reads as wiring that half worked.
 *
 * <p>The arrival is the one moment that answers at more than one level, so it is held as a role and a
 * volume per {@link PointerArrivalTarget} rather than as a cue. One sample throughout and one level per
 * kind is what keeps those levels a balance rather than three unrelated sounds; which kinds there are, and
 * why the axis is kinds rather than widgets, the enum states. Callers take
 * {@link #resolvePointerArrivalCueFor} and never the role alone, the pair being what a moment is owed.
 *
 * <p>The moments are a control's, for all that the name says only "UI": a press and the pointer arriving
 * are what a thing the player aims at and clicks does, and a scheme naming them describes a row of tabs,
 * a collapse handle, or a checkbox alike. The name stays broad because the seam is - what a moment is
 * called here should not have to change the day something that is not quite a control wants one.
 *
 * <p>Only the moments a KM control actually answers are named. The engine keeps a sound for a press that
 * lands on a control too disabled to take it, and nothing here has a disabled state to press yet - so
 * that role is left to whatever grows one, on the same rule {@link StarsectorUiSound} follows about not
 * guessing at ids nothing plays.
 *
 * @param pressCue                          what a control makes when a press on it lands, or null to press
 *                                          silently
 * @param pointerArrivalSound               what a control makes as the pointer arrives on it, whatever kind
 *                                          of thing was reached, or null to stay silent under the pointer
 * @param panelChromeArrivalVolume          how loudly an arrival on the panel's own furniture sounds
 * @param singleOptionControlArrivalVolume  how loudly an arrival on a control with one answer to give
 *                                          sounds
 * @param listedItemArrivalVolume           how loudly an arrival on one of many alike sounds
 */
public record UiSoundScheme(
    UiSoundCue pressCue,
    StarsectorUiSound pointerArrivalSound,
    float panelChromeArrivalVolume,
    float singleOptionControlArrivalVolume,
    float listedItemArrivalVolume) {

    // The balance a KM panel takes when its host states none: vanilla's own arrival level halved for
    // anything the player aims at, and halved again for the items a sweep crosses several of on its way
    // somewhere else. What makes them right is the ratio rather than any one of them - a column of listed
    // rows answering as loudly as the tab above it is what reads as chatter.
    private static final float DEFAULT_PANEL_CHROME_ARRIVAL_VOLUME = 0.5f;
    private static final float DEFAULT_SINGLE_OPTION_CONTROL_ARRIVAL_VOLUME = 0.5f;
    private static final float DEFAULT_LISTED_ITEM_ARRIVAL_VOLUME = 0.25f;

    /**
     * Rejects a volume the engine has no meaning for at the point the look is composed, rather than leaving
     * it to the cue built from it - which is built on the frame the moment first sounds, where a throw is a
     * crash mid-hover instead of a look that refused to compose.
     */
    public UiSoundScheme {

        UiSoundCue.requirePlayableVolume(panelChromeArrivalVolume);
        UiSoundCue.requirePlayableVolume(singleOptionControlArrivalVolume);
        UiSoundCue.requirePlayableVolume(listedItemArrivalVolume);
    }

    /**
     * A scheme naming the two moments and taking the library's own arrival balance - the form a host wants
     * until it has volumes of its own to state, which in practice means until it has a player looking at a
     * settings screen. Shipping defaults rather than demanding numbers keeps a consuming mod from having to
     * invent a balance before it has an opinion about one; what it gets by saying nothing is the balance a
     * KM sidebar was tuned at.
     *
     * @param pressCue            what a control makes when a press on it lands, or null to press silently
     * @param pointerArrivalSound what a control makes as the pointer arrives on it, or null to stay silent
     *                            under the pointer
     */
    public UiSoundScheme(UiSoundCue pressCue, StarsectorUiSound pointerArrivalSound) {
        this(
            pressCue,
            pointerArrivalSound,
            DEFAULT_PANEL_CHROME_ARRIVAL_VOLUME,
            DEFAULT_SINGLE_OPTION_CONTROL_ARRIVAL_VOLUME,
            DEFAULT_LISTED_ITEM_ARRIVAL_VOLUME);
    }

    /**
     * The engine's own roles, for a control drawn to look like a vanilla one: a vanilla button's press and a
     * vanilla button's mouseover. The default a host wants unless it has a reason not to - a KM control that
     * looks like the chrome around it and answers with different samples reads as a foreign widget however
     * closely it is painted.
     *
     * <p>Vanilla's roles at a KM panel's levels, though, and deliberately so. The press keeps the engine's
     * own balance, being one act the player asked for; the arrivals take the library's defaults, because
     * what vanilla mixed its mouseover for is a screen with a handful of hit targets on it and not a column
     * of them. Matching the engine's chrome means sounding like it under one pointer sweep, which is what
     * the levels answer for and the ids cannot.
     *
     * @return the scheme a vanilla-looking control sounds by
     */
    public static UiSoundScheme createVanillaSoundScheme() {
        return new UiSoundScheme(
            UiSoundCue.createAtFullVolume(StarsectorUiSound.BUTTON_PRESSED),
            StarsectorUiSound.BUTTON_MOUSEOVER);
    }

    /**
     * A scheme that names no sound at all, for a panel meant to be seen and not heard - a dense row of
     * controls where every arrival would chatter, or a screen whose own chrome is silent.
     *
     * @return a scheme every moment of which is quiet
     */
    public static UiSoundScheme createSilentSoundScheme() {
        return new UiSoundScheme(null, null);
    }

    /**
     * What the pointer reaching the given kind of thing sounds like, role and level together. One accessor
     * over the kinds rather than a cue apiece, so a caller answers an arrival by saying what was arrived at
     * - which is the only question it can answer - and the look decides the rest.
     *
     * @param arrivalTarget what kind of thing the pointer reached
     * @return the role and volume that arrival sounds at, or null for a look that stays silent under the
     *         pointer
     */
    public UiSoundCue resolvePointerArrivalCueFor(PointerArrivalTarget arrivalTarget) {
        return pointerArrivalSound == null
            ? null
            : new UiSoundCue(pointerArrivalSound, resolveArrivalVolumeFor(arrivalTarget));
    }

    // Which of the three levels a kind answers at. A switch rather than a map, so a kind added to the enum
    // stops compiling here rather than resolving to a volume nobody chose for it.
    private float resolveArrivalVolumeFor(PointerArrivalTarget arrivalTarget) {
        return switch (arrivalTarget) {
            case PANEL_CHROME -> panelChromeArrivalVolume;
            case SINGLE_OPTION_CONTROL -> singleOptionControlArrivalVolume;
            case LISTED_ITEM -> listedItemArrivalVolume;
        };
    }
}
