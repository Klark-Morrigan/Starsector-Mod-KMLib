package kmlib.starsector.settings.modmanager;

import com.fs.starfarer.api.Global;

/**
 * What the game knows about an installed mod beyond whether it is enabled - today, the name it shows
 * for one.
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

        if (modId == null) {
            return null;
        }

        var settings = Global.getSettings();

        if (settings == null || settings.getModManager() == null) {
            return null;
        }
        var modSpec = settings.getModManager().getModSpec(modId);

        return modSpec == null ? null : modSpec.getName();
    }
}
