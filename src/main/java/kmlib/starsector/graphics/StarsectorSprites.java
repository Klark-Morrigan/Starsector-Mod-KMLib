package kmlib.starsector.graphics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;

/**
 * Defensive single entry point for fetching a sprite by texture path
 * from Starsector's {@link com.fs.starfarer.api.SettingsAPI#getSprite(String)}.
 *
 * <p>The settings call throws when the path is not registered in any
 * loaded mod's texture graph, which would otherwise abort a whole
 * render or layout pass over an icon grid the moment one entry points
 * at a missing asset. Collapsing that failure to {@code null} lets a
 * caller route a bad icon to a skipped draw with a single null check
 * instead of guarding every call site with its own try / catch.
 */
public final class StarsectorSprites {

    private StarsectorSprites() {
    }

    /**
     * Returns the sprite registered under {@code path}, or {@code null}
     * when the settings lookup throws because the path is unknown.
     */
    public static SpriteAPI loadSprite(String path) {
        try {
            return Global.getSettings().getSprite(path);
        } catch (RuntimeException exception) {
            return null;
        }
    }

}
