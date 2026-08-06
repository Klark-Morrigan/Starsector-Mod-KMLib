package kmlib.testfixtures.starsector.ui.sound;

import kmlib.starsector.ui.sound.StarsectorUiSound;
import kmlib.starsector.ui.sound.UiSoundPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link UiSoundPlayer} that records what it was asked to play instead of playing it, so the rules about
 * which moment sounds - and, as much to the point, which moments do not - can be asserted without a running
 * engine.
 *
 * <p>It keeps the order and the repeats rather than a set of roles, because both carry a rule: a press and
 * the pointer arriving are the same widget answering twice and must not collapse into one, and a sound
 * played twice for one act is exactly the fault worth catching.
 *
 * <p>Ships in the main jar rather than a test source set, so a consuming mod's own tests can drive a KM
 * widget through this seam without rebuilding the fixture.
 */
public final class UiSoundPlayerFake implements UiSoundPlayer {

    // Every play in the order it arrived. A list rather than a count per role, so a case can assert what
    // sounded, how often, and in what order against one recording.
    private final List<StarsectorUiSound> playedSounds = new ArrayList<>();

    @Override
    public void playSound(StarsectorUiSound sound) {
        playedSounds.add(sound);
    }

    /**
     * @return every sound played since the last {@link #clearPlayedSounds()}, in the order it was asked
     *         for
     */
    public List<StarsectorUiSound> getPlayedSounds() {
        return List.copyOf(playedSounds);
    }

    /**
     * Forgets everything recorded so far, for a case that drives the widget into position and then asserts
     * only what the act under test sounded.
     */
    public void clearPlayedSounds() {
        playedSounds.clear();
    }
}
