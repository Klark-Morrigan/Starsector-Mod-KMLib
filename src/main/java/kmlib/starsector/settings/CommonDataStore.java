package kmlib.starsector.settings;

import org.json.JSONObject;

/**
 * Where a preference about the interface is kept: a JSON file in the game's common data, which is
 * per user and per install rather than per save, so a choice made once holds for every campaign
 * the player loads afterwards.
 *
 * <p>A port rather than a static reach, for the reason every port here is one: the read and the
 * write go through {@code Global} to a disk the game owns, so a caller naming the reach directly
 * could only ever run inside a running game. Named as a role, what keeps the file is the caller's
 * to choose.
 *
 * <p>Both halves fail open. A common-data file is one the player may not have, may have emptied,
 * or may have hand-edited into something unreadable, and none of those is worth an exception to a
 * caller whose subject is a preference: a read that cannot be made answers nothing, and the caller
 * carries on with whatever it does without one. A write that cannot be made is reported rather
 * than thrown, so a caller may say so once and go on.
 */
public interface CommonDataStore {

    /**
     * @param fileName the common-data file's name, which is one namespace across every installed
     *                 mod - so a caller prefixes it with its own mod id
     * @return what the file holds, or null where there is no such file, where it cannot be read,
     *         or where the game is not up far enough to have common data at all
     */
    JSONObject readJsonFile(String fileName);

    /**
     * Writes {@code content} into the named file, replacing whatever it held.
     *
     * @param fileName the common-data file's name, prefixed by the caller's own mod id
     * @param content  what the file is to hold
     * @return whether the write landed, so a caller can say once that a preference is not being
     *         kept rather than leaving the player to find out across a restart
     */
    boolean writeJsonFile(String fileName, JSONObject content);
}
