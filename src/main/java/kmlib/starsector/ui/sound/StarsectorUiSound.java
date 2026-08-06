package kmlib.starsector.ui.sound;

/**
 * The engine's own interface sounds, named so a widget asks for the sound a vanilla control would make
 * rather than repeating an id string. The sibling of {@code StarsectorUiColour} for audio: the ids are
 * the engine's, declared in {@code data/config/sounds.json}, so a control drawn to look like a vanilla
 * one sounds like one too and follows a restyled install's own samples.
 *
 * <p>Only the roles a KM widget actually makes are listed. An id is a piece of vanilla's content rather
 * than a fact about our widgets, so a role nothing plays would be a guess about what the engine keeps -
 * and a wrong id fails silently, being looked up by name at play time.
 *
 * <p>Volume and pitch are not here. Each id already carries its own in the engine's config - the pressed
 * sound at 0.6 and the mouseover at 0.25 - so a caller naming its own would be overriding a balance
 * vanilla struck across every screen the sample is used on.
 */
public enum StarsectorUiSound {

    /**
     * What a vanilla button or tab makes when a press on it lands. Shared with much of the campaign UI,
     * which is what makes it read as "the interface answered" rather than as one widget's own noise.
     */
    BUTTON_PRESSED("ui_button_pressed"),

    /** What a vanilla button or tab makes as the pointer arrives on it. Quieter, being unasked for. */
    BUTTON_MOUSEOVER("ui_button_mouseover");

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
