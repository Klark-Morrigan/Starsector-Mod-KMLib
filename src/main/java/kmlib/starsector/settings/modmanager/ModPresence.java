package kmlib.starsector.settings.modmanager;

import com.fs.starfarer.api.Global;

/**
 * Whether a mod is enabled this run - the read every optional-mod integration stands behind,
 * written once here rather than at each of them.
 *
 * <p>The hop itself is three calls and is not what this exists for. What it exists for is the two
 * states along the way where there is nothing to ask: the game's settings not stood up yet, and
 * settings carrying no mod manager. A gate exists to make a mod optional, so a read taken before
 * the game is fully up has to answer "not installed" rather than throw - an exception there is a
 * crash on the one path whose whole purpose is to prevent one. Each integration writing the hop
 * itself is free to leave that out, and the ones that do fail only on the installs and the moments
 * nobody develops on.
 *
 * <p>The mod id stays with the integration that needs it rather than being gathered here. What a
 * caller shares with every other caller is the manner of asking; which mod is asked about is the
 * one thing that is genuinely theirs, and a register of every third-party id in the series would
 * put an edit to one integration in a file all of them read.
 *
 * <p>Under the game's settings rather than in {@code kmlib.settings}, which is the LunaLib
 * read/write surface: the mod set is the game's own, and is readable on an install carrying no
 * settings library at all. In a package of its own beneath them because {@code ModManagerAPI} is
 * reached only through {@code SettingsAPI.getModManager()} - the nesting is the engine's own - and
 * because the mod set and the common-data folder its parent package reads are two subjects a reader
 * should not have to open a file to tell apart.
 */
public final class ModPresence {

    private ModPresence() {
        // utility class, no instances.
    }

    /**
     * Whether the game reports that mod as enabled.
     *
     * @param modId the mod's own id, as its {@code mod_info.json} declares it; null yields false
     * @return true only where the game is up far enough to answer and answers yes - every other
     *         state reads as an install without the mod, which is the answer that leaves a caller
     *         doing whatever it does without it
     */
    public static boolean isModEnabled(String modId) {

        if (modId == null) {
            return false;
        }

        var settings = Global.getSettings();

        if (settings == null || settings.getModManager() == null) {
            return false;
        }
        return settings.getModManager().isModEnabled(modId);
    }
}
