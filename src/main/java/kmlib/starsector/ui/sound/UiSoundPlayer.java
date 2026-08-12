package kmlib.starsector.ui.sound;

/**
 * Where a widget's interface sounds go. A port rather than a direct reach for the game's sound player,
 * because playing a sound is the one thing a panel's input controller does that leaves no trace in its
 * own state: what a press does to a lift can be read back and asserted, and what it does to the speakers
 * cannot. Inverting it is what lets the rules - which moment makes which sound, and which moments make
 * none - be pinned at all.
 *
 * <p>What crosses the seam is a {@link UiSoundCue}: which role sounded and how loudly. Neither is this
 * end's to choose - a cue arrives already resolved, from the look that named it - and neither is
 * <em>when</em>, which stays the caller's. Why a volume crosses at all rather than being left to the
 * engine's per-id balance is in {@link StarsectorUiSound}.
 */
public interface UiSoundPlayer {

    /**
     * Plays one interface sound, now, at the volume its cue names.
     *
     * @param cue the role that sounded and how loudly
     */
    void playCue(UiSoundCue cue);

    /**
     * Plays a cue there may not be: the given cue if there is one, and nothing at all otherwise. The form
     * a caller wants wherever the cue it holds was resolved rather than written down - a lookup that can
     * come back empty, such as a {@link UiSoundScheme}'s answer for a moment it leaves quiet.
     *
     * <p>Here rather than at each caller so an absent cue means the same thing everywhere, and beside
     * {@link #playCue} rather than inside it so no implementation has to think about it. A null crossing
     * the seam would make every implementation responsible for the same guard - and a recording one would
     * either log a sound nothing played or need a guard of its own to avoid it.
     *
     * @param cue the role that sounded and how loudly, or null for a moment with no sound to it
     */
    default void playCueIfPresent(UiSoundCue cue) {

        if (cue != null) {
            playCue(cue);
        }
    }
}
