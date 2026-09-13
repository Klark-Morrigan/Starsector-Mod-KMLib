package kmlib.starsector.ui.sound;

import com.fs.starfarer.api.Global;

/**
 * The live binding: plays through the running game's sound player, so a KM widget's press sounds like the
 * engine's own because it is the engine's own sample, mixed by the engine's own settings and scaled by
 * whatever volume the cue named over them.
 *
 * <p>Fails closed. There is no sound player before the game has one - the moments either side of a
 * session, and anywhere the engine is not up - and a UI sound is the least of what a caller in that
 * position is doing, so a missing player is a silent no-op rather than an exception raised out of an
 * input handler. That also keeps the default construction of a widget usable off a live engine, which
 * is what stops every consumer having to inject a player it does not care about.
 */
public final class VanillaUiSoundPlayer implements UiSoundPlayer {

    // The engine scales the ID's configured pitch by this, so a KM sound is the sample vanilla recorded
    // rather than one bent by us. Only the volume is ours to scale, and that arrives with the cue.
    private static final float CONFIGURED_PITCH = 1f;

    @Override
    public void playCue(UiSoundCue cue) {

        var soundPlayer = Global.getSoundPlayer();
        if (soundPlayer == null) {
            return;
        }
        // Multiplied against the ID's own configured volume by the engine, so what the cue names is a
        // scale over vanilla's balance for that sample rather than an absolute level laid over it - which
        // is what keeps a restyled install's own mix intact under a quietened panel.
        soundPlayer.playUISound(
            cue.sound().getSoundId(),
            CONFIGURED_PITCH,
            cue.volume());
    }
}
