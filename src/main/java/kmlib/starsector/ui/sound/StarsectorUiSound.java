package kmlib.starsector.ui.sound;

/**
 * The engine's own interface sounds, named so a widget asks for the sound a vanilla control would make
 * rather than repeating an id string. The sibling of {@code StarsectorUiColour} for audio: the ids are
 * the engine's, declared in {@code data/config/sounds.json}, so a control drawn to look like a vanilla
 * one sounds like one too and follows a restyled install's own samples.
 *
 * <p>Only the roles KM code actually plays are listed. An id is a piece of vanilla's content rather
 * than a fact about our widgets, so a role nothing plays would be a guess about what the engine keeps -
 * and a wrong id fails silently, being looked up by name at play time.
 *
 * <p>"Widget" is the common case and not the boundary: what a role names is a moment the interface
 * answers, and an overlay drawn onto the sector map answers moments no control has - so a role is listed
 * here on something playing it rather than on a control playing it.
 *
 * <p>Volume is not here, but it is no longer nowhere. Each id carries its own in the engine's config -
 * the pressed sound at 0.6 and the mouseover at 0.25 - and that balance is still exactly right for one
 * control drawn among vanilla's own, which is why nothing here overrides it. It stops being right for a
 * KM panel: a strip of tabs over a column of checkboxes over a list of rows puts more hit targets under
 * one sweep of the pointer than any vanilla screen does, and at vanilla's mouseover level that sweep
 * chatters. So how loud a role plays is named per moment, in the {@link UiSoundCue} a look carries, and
 * scaled over the engine's balance rather than replacing it.
 *
 * <p>Pitch stays vanilla's entirely. Nothing here has a reason to bend a sample, and an id played at a
 * pitch it was not recorded at stops being the engine's sound in the only way that matters.
 */
public enum StarsectorUiSound {

    /**
     * What a vanilla button or tab makes when a press on it lands. Shared with much of the campaign UI,
     * which is what makes it read as "the interface answered" rather than as one widget's own noise.
     */
    BUTTON_PRESSED("ui_button_pressed"),

    /** What a vanilla button or tab makes as the pointer arrives on it. Quieter, being unasked for. */
    BUTTON_MOUSEOVER("ui_button_mouseover"),

    /**
     * What a vanilla scrolling readout makes as its content moves. The one role here that answers
     * something other than a hit target: the player turned a wheel and content slid, which is why it is
     * neither of the button moments above however much a list looks like a column of them.
     *
     * <p>The engine keeps several ids over the one sample and this is the one named for scrolling, so it
     * is the one a list takes. A wrong id fails silently, being looked up by name at play time, which is
     * why the choice between them is written down here rather than left to whoever plays it.
     */
    LIST_SCROLLED("ui_number_scrolling"),

    /**
     * What a vanilla readout makes as it types a character out: a short tick with none of a button's
     * body to it. The role for a moment that is not a hit target being reached - a cell of the sector
     * map arriving under the cursor - where a button's mouseover would claim a control the surface
     * underneath does not have.
     *
     * <p>The one id here whose name has to be looked up rather than guessed. {@code ui_type.ogg} is the
     * file, and the ids the engine keeps over it are this one, {@link #LIST_SCROLLED}'s
     * {@code ui_number_scrolling}, and {@code ui_typer_buzz} at half pitch - so the sample a caller
     * means and the id it must ask for do not share a name, and the plausible spelling is not a sound.
     */
    TEXT_TYPED("ui_typer_type");

    // The engine's own id for this sound, looked up in its sound config at play time.
    private final String soundId;

    StarsectorUiSound(String soundId) {
        this.soundId = soundId;
    }

    /**
     * @return the engine's id for this sound, for whatever hands it to the game's sound player
     */
    public String getSoundId() {
        return soundId;
    }
}
