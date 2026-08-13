package kmlib.starsector.ui.sound;

import java.util.Objects;

/**
 * What a moment sounds like: one interface role and how loudly it plays. The pair a look states for a
 * press, an arrival, or anything else a control answers audibly.
 *
 * <p>The two travel together because a role resolved in one place and a volume resolved in another is a
 * pair a call site can put together wrongly, and do it silently - nothing on screen shows that an arrival
 * sounded at the press's level. Bound into one value, a moment that sounds at all sounds at the volume its
 * own look named for it.
 *
 * <p>The volume scales the engine's configured level for the id rather than replacing it, so
 * {@link #FULL_VOLUME} is vanilla's own balance for that sample and anything below it is a panel
 * quietening itself relative to that. Nothing caps it at full: a look with a reason to push a sample past
 * its configured level is making the same kind of decision as one turning it down, and a cap here would
 * be this end deciding how loud a mod is.
 *
 * <p>Why a KM panel names volumes at all, when vanilla's own balance is already good, is in
 * {@link StarsectorUiSound}.
 *
 * @param sound  the role that sounds
 * @param volume how loudly, as a multiplier over the engine's configured volume for that role
 */
public record UiSoundCue(
    StarsectorUiSound sound,
    float volume) {

    /**
     * The engine's configured level for a role, unscaled - what a control drawn among vanilla's own and
     * meant to sound like one plays at.
     */
    public static final float FULL_VOLUME = 1f;

    /**
     * The quietest a cue can name, and so the level at which a look composing one from a slider stops
     * naming a cue at all: a moment asked for at nothing is still a moment played, which reads as wiring
     * that half worked rather than as something deliberately quiet. Public because that comparison is
     * made wherever a host builds a cue from a level the player set, and two spellings of the floor is
     * two places for it to be read differently.
     *
     * <p>Below it the engine has no meaning for the value, so a negative volume would fail as quietly as
     * a wrong id does - which is why it is also what {@link #requirePlayableVolume} guards against.
     */
    public static final float SILENT_VOLUME = 0f;

    /**
     * Rejects a cue that names no role, since silence is a null cue rather than a cue with nothing in it -
     * a scheme states a quiet moment by naming no cue for it, so a null role here is a look that meant to
     * sound and left out what with. Rejects a negative volume for the same reason a wrong id is worth
     * catching early: the engine simply plays it and no one hears which end went wrong.
     */
    public UiSoundCue {

        Objects.requireNonNull(sound, "sound");
        requirePlayableVolume(volume);
    }

    /**
     * A cue that plays its role at the level the engine already holds for it, for a look that takes
     * vanilla's balance as it stands - the form every moment of a vanilla-looking control wants, since
     * scaling one of those is what makes it stop matching the chrome around it.
     *
     * @param sound the role that sounds
     * @return that role at the engine's own level for it
     */
    public static UiSoundCue createAtFullVolume(StarsectorUiSound sound) {
        return new UiSoundCue(sound, FULL_VOLUME);
    }

    /**
     * The volume rule itself, for a look holding a volume before it has a role to pair it with. A scheme
     * that names one level per kind of arrival carries loose numbers until the moment it answers for, and a
     * bad one caught only when the cue is finally built would throw on the frame the moment first sounded -
     * a fault that reaches the player as a crash mid-hover rather than as a look that refused to compose.
     *
     * <p>Here rather than repeated there so the two ends cannot disagree about what a volume may be.
     *
     * @param volume how loudly, as a multiplier over the engine's configured volume for a role
     * @throws IllegalArgumentException if the volume is below silence
     */
    static void requirePlayableVolume(float volume) {

        if (volume < SILENT_VOLUME) {
            throw new IllegalArgumentException("volume must not be negative");
        }
    }
}
