package kmlib.starsector.settings.modmanager;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;

/**
 * What the game knows about an installed mod beyond whether it is enabled: the name it shows for
 * one, and the version that mod declares.
 *
 * <p>Beside {@link ModPresence} and guarded the same way, for the same reason: the two states along
 * the way where there is nothing to ask - settings not stood up yet, and settings carrying no mod
 * manager - answer "nothing known" rather than throwing. A caller wanting a mod's name is composing
 * a report, and a report that threw while naming who it was about would lose the report.
 *
 * <p>Answers nothing rather than the ID where the mod is unknown. The two are not the same reading:
 * a caller holding an ID already has it, and what it is asking for is the name to show beside it. An
 * ID handed back as though it were a name would render as a mod called {@code kmu}.
 */
public final class InstalledMods {

    private InstalledMods() {
        // utility class, no instances.
    }

    /**
     * The display name the game holds for that mod.
     *
     * @param modId the mod's own ID, as its {@code mod_info.json} declares it; null yields null
     * @return the name, or null where the game is not up far enough to answer or lists no such mod -
     *         which is also the answer for an ID that names no installed mod at all, so a report
     *         composed from one shows the ID it was given and nothing invented around it
     */
    public static String readModName(String modId) {

        var modSpec = readModSpec(modId);

        return modSpec == null ? null : modSpec.getName();
    }

    /**
     * The version that mod declares for itself.
     *
     * <p>The other half of what a compatibility report states a mismatch between. Unlike a renderer
     * patch, a mod publishes its version through the game rather than through a class of its own, so
     * this read holds for every mod without anything being bound to any of them.
     *
     * @param modId the mod's own ID, as its {@code mod_info.json} declares it; null yields null
     * @return the version, or null where the game is not up far enough to answer, lists no such mod,
     *         or holds a spec that states none - all of which a report renders as an unknown version
     *         rather than as a number it invented
     */
    public static String readModVersion(String modId) {

        var modSpec = readModSpec(modId);

        return modSpec == null ? null : modSpec.getVersion();
    }

    // The hop down to one mod's spec, and the three states along the way where there is nothing to
    // ask: no settings, settings carrying no mod manager, and a manager listing no such mod. Shared
    // by both reads because it is the whole of what they have in common - and because a second copy
    // is a second place to leave one of those states out.
    private static ModSpecAPI readModSpec(String modId) {

        if (modId == null) {
            return null;
        }

        var settings = Global.getSettings();

        if (settings == null || settings.getModManager() == null) {
            return null;
        }
        return settings.getModManager().getModSpec(modId);
    }
}
