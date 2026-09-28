package kmlib.starsector.ui.font;

import com.fs.starfarer.api.Global;

import org.apache.log4j.Logger;

/**
 * Reads the face the game itself draws its paragraph text in - the {@code defaultFont} its settings name
 * - as the last face any KM text falls back to. The game's own choice rather than one written down here,
 * so KM's last resort is whatever vanilla text on the same install is drawn in.
 *
 * <p>Read the way vanilla's own {@code Fonts.DEFAULT_SMALL} is: through the settings' {@code getString}.
 * No mod can set the key - the game refuses it from a mod's settings - so only a core overwrite of
 * {@code starsector-core} could move it, and then it names whatever file that overwrite chose. KM only
 * draws in faces {@link StarsectorFont} names, so a setting naming a face the enum does not know is read
 * as {@link StarsectorFont#VANILLA_INSIGNIA_15}, the face vanilla names, and logged, since a default KM
 * cannot draw in is no default at all. A caller reads it when it settles its faces, not per frame, so
 * the line comes once per settling.
 */
public final class GameDefaultFontReader {

    private static final Logger LOG = Global.getLogger(GameDefaultFontReader.class);

    // The settings key the game names its paragraph face under.
    private static final String DEFAULT_FONT_KEY = "defaultFont";

    // The face vanilla's settings name, answered where the setting names a face the enum does not know.
    private static final StarsectorFont VANILLA_DEFAULT_FONT = StarsectorFont.VANILLA_INSIGNIA_15;

    // Reads only; never instantiated.
    private GameDefaultFontReader() {
    }

    /**
     * @return the face the game's settings name as its default, or vanilla's where they name a face KM
     *         does not know
     */
    public static StarsectorFont readDefaultFont() {
        return resolveDefaultFont(Global.getSettings().getString(DEFAULT_FONT_KEY));
    }

    /**
     * The face a {@code defaultFont} setting names, by the rule {@link #readDefaultFont()} applies to the
     * running game's - for a reader that has the setting's value from somewhere else, such as an install's
     * settings file read from outside the game.
     *
     * @param defaultFontPath the setting's value, such as {@code graphics/fonts/insignia15LTaa.fnt}
     * @return the face it names, or vanilla's where it names a face KM does not know
     */
    public static StarsectorFont resolveDefaultFont(String defaultFontPath) {
        return StarsectorFont.findFontByPath(defaultFontPath)
            .orElseGet(() -> reportUnknownDefault(defaultFontPath));
    }

    private static StarsectorFont reportUnknownDefault(String defaultFontPath) {

        LOG.warn("The game's defaultFont '" + defaultFontPath + "' is no face KM draws in; falling back to "
            + VANILLA_DEFAULT_FONT.getBasename());

        return VANILLA_DEFAULT_FONT;
    }
}
