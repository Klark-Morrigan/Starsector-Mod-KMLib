package kmlib.testfixtures.starsector.ui.sound;

import kmlib.starsector.ui.sound.StarsectorUiSound;
import kmlib.starsector.ui.sound.UiSoundCue;
import kmlib.starsector.ui.sound.UiSoundPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link UiSoundPlayer} that records what it was asked to play instead of playing it, so the rules about
 * which moment sounds - and, as much to the point, which moments do not - can be asserted without a running
 * engine.
 *
 * <p>It keeps the order and the repeats rather than a set of cues, because both carry a rule: a press and
 * the pointer arriving are the same widget answering twice and must not collapse into one, and a sound
 * played twice for one act is exactly the fault worth catching.
 *
 * <p>Whole cues are recorded rather than roles, so a case can assert how loudly a moment sounded and not
 * only that it did. Which volume answers which kind of moment is a rule of the same order as which role
 * does, and one nothing on screen would show had gone wrong.
 *
 * <p>Ships in the main jar rather than a test source set, so a consuming mod's own tests can drive a KM
 * widget through this seam without rebuilding the fixture.
 */
public final class UiSoundPlayerFake implements UiSoundPlayer {

    // Every play in the order it arrived. A list rather than a count per cue, so a case can assert what
    // sounded, how often, and in what order against one recording.
    private final List<UiSoundCue> playedCues = new ArrayList<>();

    @Override
    public void playCue(UiSoundCue cue) {
        playedCues.add(cue);
    }

    /**
     * @return every cue played since the last {@link #clearPlayedCues()}, in the order it was asked for
     */
    public List<UiSoundCue> getPlayedCues() {
        return List.copyOf(playedCues);
    }

    /**
     * The same recording read as roles alone, for a case about which moment sounded rather than about how
     * loudly. A projection of {@link #getPlayedCues()} rather than a second recording, so the two cannot
     * disagree - offered because most cases are about the moment, and spelling a volume into each of them
     * would state a rule they are not testing.
     *
     * @return the role of every cue played since the last {@link #clearPlayedCues()}, in order
     */
    public List<StarsectorUiSound> getPlayedSounds() {
        return playedCues.stream()
            .map(UiSoundCue::sound)
            .toList();
    }

    /**
     * Forgets everything recorded so far, for a case that drives the widget into position and then asserts
     * only what the act under test sounded.
     */
    public void clearPlayedCues() {
        playedCues.clear();
    }
}
