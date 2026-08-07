package kmlib.starsector.ui.sound;

/**
 * Which interface sound each moment a control answers makes: the press it confirms, and the pointer
 * arriving on it. The middle of the three types this package holds - {@link StarsectorUiSound} names the
 * roles, this says which role belongs to which moment, and {@link UiSoundPlayer} is where they go.
 *
 * <p>A value the look carries rather than a role each controller names in its own code, because a sound
 * is a property of a look exactly as a fill is - it is part of how a panel presents itself, and a panel
 * presenting itself differently may well answer differently.
 *
 * <p>Naming the roles in one value is also the only way a panel can be quietened at all. A controller
 * that named its own would have to be visited, control by control, to change or silence a scheme; a
 * value handed in is changed where the look is composed.
 *
 * <p>A null role is a silent moment, so silence is something a look states rather than a call somebody
 * forgot to write. That is the difference between a control deliberately quiet under the pointer and one
 * whose mouseover was never wired: the first is written down where the scheme is, the second is nowhere.
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
 * @param pressSound          what a control makes when a press on it lands, or null to press silently
 * @param pointerArrivalSound what a control makes as the pointer arrives on it, or null to stay silent
 *                            under the pointer
 */
public record UiSoundScheme(
    StarsectorUiSound pressSound,
    StarsectorUiSound pointerArrivalSound) {

    /**
     * The engine's own scheme, for a control drawn to look like a vanilla one: a vanilla button's press
     * and a vanilla button's mouseover. The default a host wants unless it has a reason not to - a KM
     * control that looks like the chrome around it and answers differently reads as a foreign widget
     * however closely it is painted.
     *
     * @return the scheme a vanilla-looking control sounds by
     */
    public static UiSoundScheme createVanillaSoundScheme() {
        return new UiSoundScheme(
            StarsectorUiSound.BUTTON_PRESSED,
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
}
