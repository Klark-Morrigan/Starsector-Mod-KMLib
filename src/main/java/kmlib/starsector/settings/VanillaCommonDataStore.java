package kmlib.starsector.settings;

import com.fs.starfarer.api.Global;

import kmlib.logging.SessionWarning;

import org.apache.log4j.Logger;
import org.json.JSONObject;

/**
 * The live binding of {@link CommonDataStore}: the game's own common-data folder, reached through
 * the three {@code SettingsAPI} calls vanilla keeps the simulator's UI arrangement in.
 *
 * <p>The existence check is made before the read rather than left to the read's own missing-file
 * tolerance, because the two absences are worth telling apart in the log: a file that was never
 * written is the ordinary state of a fresh install, while a file that is there and will not open
 * is something the player may want to know about.
 *
 * <p>Two warnings rather than one, since the read failing and the write failing cost the player
 * different things - the first loses a choice already made, the second loses the one just made -
 * and a session that hit both would otherwise report only whichever came first.
 */
public final class VanillaCommonDataStore implements CommonDataStore {

    private static final Logger LOG = Global.getLogger(VanillaCommonDataStore.class);

    private final SessionWarning readWarning = new SessionWarning(LOG);
    private final SessionWarning writeWarning = new SessionWarning(LOG);

    @Override
    public JSONObject readJsonFile(String fileName) {

        var settings = Global.getSettings();
        if (settings == null) {
            // Before the game is up there is no common data to reach; the caller reads that as
            // "nothing stored", which is what a fresh install answers anyway.
            return null;
        }
        try {
            if (!settings.fileExistsInCommon(fileName)) {
                return null;
            }
            return settings.readJSONFromCommon(fileName, true);

        } catch (Exception readFailed) {
            // Broad on purpose: the call throws two checked kinds and the disk beneath it can
            // raise anything, while every one of them costs the caller the same thing - an answer
            // it must go on without. Letting one out would instead take the caller's whole feature
            // down over a file it only meant to consult.
            readWarning.warnOnce(
                "Could not read the common-data file '" + fileName + "'; whatever it holds will "
                    + "be ignored for this session and may be overwritten by the next write.",
                readFailed);
            return null;
        }
    }

    @Override
    public boolean writeJsonFile(String fileName, JSONObject content) {

        var settings = Global.getSettings();
        if (settings == null) {
            return false;
        }
        try {
            settings.writeJSONToCommon(fileName, content, true);
            return true;

        } catch (Exception writeFailed) {
            writeWarning.warnOnce(
                "Could not write the common-data file '" + fileName + "'; choices stored in it "
                    + "will hold for this session and be gone at the next start.",
                writeFailed);
            return false;
        }
    }
}
