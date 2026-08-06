package kmlib.starsector.ui.sound;

import com.fs.starfarer.api.Global;

/**
 * The live binding: plays through the running game's sound player, so a KM widget's press sounds like the
 * engine's own because it is the engine's own sample, mixed by the engine's own settings.
 *
 * <p>Fails closed. There is no sound player before the game has one - a unit test, and the moments either
 * side of a session - and a UI sound is the least of what a caller in that position is doing, so a missing
 * player is a silent no-op rather than an exception raised out of an input handler. That also keeps the
 * default construction of a widget usable off a live engine, which is what stops every consumer having to
 * inject a player it does not care about.
 */
public final class VanillaUiSoundPlayer implements UiSoundPlayer {

    // The engine scales its configured volume and pitch by these, so an unmodified play is the balance
    // vanilla struck for the id rather than one of ours laid over it.
    private static final float CONFIGURED_PITCH = 1f;
    private static final float CONFIGURED_VOLUME = 1f;

    @Override
    public void playSound(StarsectorUiSound sound) {

        var soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) {
            return;
        }
        soundPlayer.playUISound(sound.getSoundId(), CONFIGURED_PITCH, CONFIGURED_VOLUME);
    }
}
