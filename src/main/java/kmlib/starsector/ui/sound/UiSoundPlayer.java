package kmlib.starsector.ui.sound;

/**
 * Where a widget's interface sounds go. A port rather than a direct reach for the game's sound player,
 * because playing a sound is the one thing a panel's input controller does that leaves no trace in its
 * own state: what a press does to a lift can be read back and asserted, and what it does to the speakers
 * cannot. Inverting it is what lets the rules - which moment makes which sound, and which moments make
 * none - be pinned at all.
 *
 * <p>It carries no volume or timing of its own. When a sound plays is the caller's, and how loudly is the
 * engine's config; all that crosses this seam is which role sounded.
 */
public interface UiSoundPlayer {

    /**
     * Plays one interface sound, now.
     *
     * @param sound the role that sounded
     */
    void playSound(StarsectorUiSound sound);

    /**
     * Plays a sound there may not be: the given role if there is one, and nothing at all otherwise. The
     * form a caller wants wherever the role it holds was resolved rather than written down - a lookup
     * that can come back empty, such as a {@link UiSoundScheme}'s answer for a moment it leaves quiet.
     *
     * <p>Here rather than at each caller so an absent sound means the same thing everywhere, and beside
     * {@link #playSound} rather than inside it so no implementation has to think about it. A null
     * crossing the seam would make every implementation responsible for the same guard - and a recording
     * one would either log a sound nothing played or need a guard of its own to avoid it.
     *
     * @param sound the role that sounded, or null for a moment with no sound to it
     */
    default void playSoundIfPresent(StarsectorUiSound sound) {

        if (sound != null) {
            playSound(sound);
        }
    }
}
