package kmlib.testfixtures.starsector.settings;

import kmlib.starsector.settings.CommonDataStore;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * A common-data folder that really holds what is written into it, for a subject that keeps a
 * preference across sessions.
 *
 * <p>Backed by a map rather than by stubbed answers, for the reason the sector-memory fake is: a
 * stubbed read states what a reader would find, which suits a suite that only reads, while a suite
 * exercising a writer has to give the write somewhere to land and then read it back the way the
 * game would - so a file written under one name and read under another fails here rather than
 * passing on two stubs that agree.
 *
 * <p>Both failures the live store answers with are posable, because failing open is the contract
 * rather than an accident of it: {@link #refuseReads()} poses a file that will not open, and
 * {@link #refuseWrites()} a folder that will not take one. Neither empties what is stored, since
 * what is being posed is a store that cannot be reached and not one that has been wiped.
 */
public final class CommonDataStoreFake implements CommonDataStore {

    // What the folder holds, and how often each file has been written. The count is kept beside
    // the content because writing the same content again leaves no trace in the map.
    private final Map<String, JSONObject> storedFiles = new HashMap<>();
    private final Map<String, Integer> writeCounts = new HashMap<>();

    private boolean isReadable = true;
    private boolean isWritable = true;

    @Override
    public JSONObject readJsonFile(String fileName) {
        return isReadable ? storedFiles.get(fileName) : null;
    }

    @Override
    public boolean writeJsonFile(String fileName, JSONObject content) {

        if (!isWritable) {
            return false;
        }
        writeCounts.merge(fileName, 1, Integer::sum);
        storedFiles.put(fileName, content);

        return true;
    }

    /**
     * Seeds a file as though an earlier session had written it. Counted as no write, standing for
     * what was there before the case began.
     *
     * @param fileName the common-data file's name
     * @param content  what the folder holds under it
     */
    public void storeFile(String fileName, JSONObject content) {
        storedFiles.put(fileName, content);
    }

    /**
     * @param fileName the common-data file's name
     * @return what the folder holds under it, or null where it holds nothing
     */
    public JSONObject readStoredFile(String fileName) {
        return storedFiles.get(fileName);
    }

    /**
     * @param fileName the common-data file's name
     * @return whether the folder holds anything under it
     */
    public boolean hasStoredFile(String fileName) {
        return storedFiles.containsKey(fileName);
    }

    /**
     * @param fileName the common-data file's name
     * @return how many times it has been written, so a case can show a write was skipped rather
     *         than only that the content ended up unchanged
     */
    public int countWritesTo(String fileName) {
        return writeCounts.getOrDefault(fileName, 0);
    }

    /** Poses a folder whose files will not open, which the live store answers with null. */
    public void refuseReads() {
        isReadable = false;
    }

    /** Poses a folder that will not take a write, which the live store reports rather than throws. */
    public void refuseWrites() {
        isWritable = false;
    }
}
