package kmlib.starsector.factions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import kmlib.starsector.settings.modmanager.ModSource;
import kmlib.text.KmlibStrings;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Which mod each faction was declared by.
 *
 * <p>A faction cannot be asked: neither it nor its spec carries where it came from. The loader
 * reads {@code data/world/factions/factions.csv}, opens each {@code .faction} file the rows name,
 * takes the ID out of that file and keeps nothing else. The one place the answer survives is the
 * spreadsheet, which is why this reads game data rather than the sector.
 *
 * <p>What makes the read possible is {@code fs_rowSource}, a column the game's own CSV merger
 * writes onto every row it emits, holding the folder that row was read from joined to the file
 * path. Reading the merged sheet back therefore says which install folder declared each faction,
 * and matching that folder against the enabled mods turns it into a mod's own name. The base game
 * is the one source with no folder to report, since it loads from the working directory rather than
 * from a named one, and the join stringifies that absence rather than leaving the column empty.
 *
 * <p>The ID comes out of the {@code .faction} file rather than off its name. The two agree on
 * nearly every file, but the loader reads the file, and a listing that disagreed with the loader
 * about which faction a row declares would be wrong in exactly the case worth running it for.
 *
 * <p>What this answers is who DECLARED a faction, not who supplies its contents now: a mod shipping
 * its own copy of a {@code .faction} path another already declared changes what the file says,
 * while the declaring row stays whichever one won the merge. Telling the two apart would mean
 * opening every faction file once per enabled mod, which is a read of a different size than this
 * one.
 */
public final class FactionSourceMods {

    private static final Logger LOG = Global.getLogger(FactionSourceMods.class);

    private static final String BASE_GAME_SOURCE_NAME = "vanilla";
    private static final String FACTIONS_CSV_PATH = "data/world/factions/factions.csv";
    private static final String FACTION_FILE_COLUMN = "faction";
    private static final String FACTION_ID_KEY = "id";

    // What a row reports for a folder it has none of: the merger joins the folder in with string
    // concatenation, which spells an absent one out rather than dropping it.
    private static final String MISSING_DIRECTORY = "null";
    private static final String PATH_SEPARATOR = "/";
    private static final String ROW_SOURCE_COLUMN = "fs_rowSource";
    private static final String WINDOWS_PATH_SEPARATOR = "\\";

    private FactionSourceMods() {
        // utility class, no instances.
    }

    /**
     * Every faction the game's data declares, keyed by the ID the loader builds it under.
     *
     * @return the source per faction ID; empty where the game is not up far enough to hold data
     *         and where the spreadsheet will not open, both of which leave a caller with nothing
     *         to say rather than with a wrong answer
     */
    public static Map<String, ModSource> readSourcesByFactionId() {

        var settings = Global.getSettings();

        if (settings == null) {
            return Map.of();
        }

        JSONArray declarationRows;

        try {
            declarationRows =
                settings.getMergedSpreadsheetData(FACTION_FILE_COLUMN, FACTIONS_CSV_PATH);

        } catch (Exception readFailed) {
            // Broad on purpose: the call throws two checked kinds and the disk beneath it can raise
            // anything, while every one of them costs the caller the same thing - an answer it must
            // go on without. The whole listing is worth less without this, but it is not worth
            // taking the listing down over.
            LOG.warn("Could not read '" + FACTIONS_CSV_PATH + "'; factions will be listed without "
                + "the mod that declared them.", readFailed);
            return Map.of();
        }

        var modsByDirectory = readModsByDirectory(settings);
        var sources = new HashMap<String, ModSource>();

        for (var rowIndex = 0; rowIndex < declarationRows.length(); rowIndex++) {

            var declarationRow = declarationRows.optJSONObject(rowIndex);

            if (declarationRow == null) {
                continue;
            }
            var factionId = readDeclaredFactionIdOf(settings, declarationRow);

            if (factionId != null) {
                sources.put(factionId, readSourceOf(declarationRow, modsByDirectory));
            }
        }
        return sources;
    }

    // The source a row was read from: the mod installed in that folder where the mod manager lists
    // one, the base game where the row reports no folder at all, and the folder itself otherwise -
    // a source the manager does not account for is still better named by the folder it sits in
    // than left unnamed, even though there is no mod ID to name beside it.
    private static ModSource readSourceOf(
            JSONObject declarationRow,
            Map<String, ModSource> modsByDirectory) {

        var sourceDirectory = readSourceDirectoryOf(declarationRow);

        if (sourceDirectory.isEmpty() || MISSING_DIRECTORY.equals(sourceDirectory)) {
            return new ModSource(BASE_GAME_SOURCE_NAME, null);
        }
        return modsByDirectory.getOrDefault(
            sourceDirectory,
            new ModSource(sourceDirectory, null));
    }

    // The ID the row's faction file declares, or null where the row names no file or the file will
    // not open. A file that will not open here did not become a faction either - the loader reads
    // it the same way, from the same merge - so such a row has no faction to attribute rather than
    // a faction whose attribution was lost.
    private static String readDeclaredFactionIdOf(SettingsAPI settings, JSONObject declarationRow) {

        var factionFilePath = declarationRow.optString(FACTION_FILE_COLUMN, "");

        if (factionFilePath.isEmpty()) {
            return null;
        }

        try {
            var factionFile = settings.getMergedJSON(factionFilePath);
            var declaredId = factionFile == null
                ? ""
                : factionFile.optString(FACTION_ID_KEY, "");

            return declaredId.isEmpty()
                ? null
                : declaredId;

        } catch (Exception readFailed) {
            // Below a warning: one unreadable faction file costs a single row its name, and an
            // install carrying a few would otherwise bury the one line worth reading.
            LOG.debug("Could not read the faction file '" + factionFilePath + "'; whatever it "
                + "declares will be listed without the mod that declared it.", readFailed);
            return null;
        }
    }

    // The last segment of a path, which is the one part a row's source and a mod's install folder
    // are bound to spell the same way. The rest is the install root, which differs per machine and
    // is written with whichever separators the platform handed the loader.
    private static String readLeafDirectoryOf(String directoryPath) {

        if (directoryPath == null) {
            return "";
        }
        var normalised = directoryPath.replace(WINDOWS_PATH_SEPARATOR, PATH_SEPARATOR);

        while (normalised.endsWith(PATH_SEPARATOR)) {
            normalised = normalised.substring(0, normalised.length() - PATH_SEPARATOR.length());
        }
        return normalised.substring(normalised.lastIndexOf(PATH_SEPARATOR) + 1);
    }

    // The enabled mods keyed by the folder each is installed in. Keyed by folder rather than by ID
    // because the folder is all a row reports, and a mod's ID is not what its folder is called.
    private static Map<String, ModSource> readModsByDirectory(SettingsAPI settings) {

        var modManager = settings.getModManager();

        if (modManager == null) {
            // Settings that are up without a mod manager on them is the half-built state a read
            // taken while the game is still coming up meets; every source then keeps its folder
            // name, which places it well enough to be worth printing.
            return Map.of();
        }
        var mods = new HashMap<String, ModSource>();

        for (var mod : modManager.getEnabledModsCopy()) {

            var modDirectory = readLeafDirectoryOf(mod.getDirName());

            if (modDirectory.isEmpty()) {
                continue;
            }
            var modName = KmlibStrings.hasText(mod.getName())
                ? mod.getName()
                : modDirectory;

            mods.put(modDirectory, new ModSource(modName, mod.getId()));
        }
        return mods;
    }

    // The folder a row was read from: its source is that folder joined to the file path, so cutting
    // the path off leaves it. A row carrying no source at all reads as one with no folder, which is
    // how the base game's own rows arrive anyway.
    private static String readSourceDirectoryOf(JSONObject declarationRow) {

        var rowSource = declarationRow.optString(ROW_SOURCE_COLUMN, "");
        var csvPathAt = rowSource.lastIndexOf(FACTIONS_CSV_PATH);

        if (csvPathAt < 0) {
            return "";
        }
        return readLeafDirectoryOf(rowSource.substring(0, csvPathAt));
    }
}
