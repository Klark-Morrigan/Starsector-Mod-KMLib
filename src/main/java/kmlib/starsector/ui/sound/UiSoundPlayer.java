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
}
