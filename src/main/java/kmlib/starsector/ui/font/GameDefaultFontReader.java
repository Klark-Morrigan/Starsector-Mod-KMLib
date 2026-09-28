package kmlib.starsector.ui.font;

import com.fs.starfarer.api.Global;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * Reads the face the game itself draws its paragraph text in - the {@code defaultFont} its settings name
 * - as the face a text walks to once its own face's family is spent. The game's own choice rather than one
 * written down here, so the face a text falls to is whatever vanilla text on the same install is drawn in.
 *
 * <p>Read the way vanilla's own {@code Fonts.DEFAULT_SMALL} is: through the settings' {@code getString}.
 * Any mod can set the key through its own settings, which the game merges, and so can a core overwrite of
 * {@code starsector-core}; either way it names whatever file that source chose. A file
 * {@link StarsectorFont} names reads as that face, and any other as a {@link DeclaredFontAtlas} at its
 * path - a language pack pointing the setting at an atlas holding its script is exactly the face a text
 * the enum's atlases cannot draw needs. A setting naming nothing reads as vanilla's own face.
 *
 * <p>This is not the last face a text can have: that is {@link FaceResolver#LAST_RESORT_FONT}, named in
 * KMLib, so a setting naming a broken or missing file is passed over rather than answered.
 */
public final class GameDefaultFontReader {

    private static final Logger LOG = Global.getLogger(GameDefaultFontReader.class);

    // The settings key the game names its paragraph face under.
    private static final String DEFAULT_FONT_KEY = "defaultFont";

    // Reads only; never instantiated.
    private GameDefaultFontReader() {
    }

    /**
     * @return the face the game's settings name as its default: the enum's face where it names one, the
     *         declared file otherwise, and vanilla's where the setting names nothing
     */
    public static FontAtlas readDefaultFont() {

        return resolveDefaultFont(
            Global.getSettings().getString(DEFAULT_FONT_KEY),
            GameDefaultFontReader::readInstalledSmoothing);
    }

    /**
     * The face a {@code defaultFont} setting names, by the rule {@link #readDefaultFont()} applies to the
     * running game's - for a reader that has the setting's value, and the declared file, from somewhere
     * else, such as an install read from outside the game.
     *
     * @param defaultFontPath         the setting's value, such as {@code graphics/fonts/insignia15LTaa.fnt}
     * @param declaredSmoothingReader reads the smoothing of a file the enum does not name, given its path
     * @return the face the value names
     */
    public static FontAtlas resolveDefaultFont(
            String defaultFontPath,
            Function<String, AtlasSmoothing> declaredSmoothingReader) {

        // The last resort is the face vanilla's settings name, so a setting naming nothing reads as vanilla's.
        if (defaultFontPath == null || defaultFontPath.isBlank()) {
            return FaceResolver.LAST_RESORT_FONT;
        }

        var knownFont = StarsectorFont.findFontByPath(defaultFontPath);
        if (knownFont.isPresent()) {
            return knownFont.get();
        }

        return new DeclaredFontAtlas(defaultFontPath, declaredSmoothingReader.apply(defaultFontPath));
    }

    // The smoothing a declared file's descriptor states, read the way the game opens any of its files. A file
    // that cannot be read will not load either, so every walk passes over it whatever is answered here;
    // antialiased is answered, being what every face but a deliberate pixel face is, and the miss is
    // logged, since a setting naming an unreadable file is worth a line.
    private static AtlasSmoothing readInstalledSmoothing(String defaultFontPath) {

        try (var descriptor = new BufferedReader(new InputStreamReader(
                Global.getSettings().openStream(defaultFontPath),
                StandardCharsets.ISO_8859_1))) {

            return AtlasSmoothing.resolveFromInfoLine(descriptor.readLine());

        } catch (IOException | RuntimeException exception) {

            LOG.warn("The game's defaultFont '"
                    + defaultFontPath
                    + "' could not be read; taking it as antialiased",
                exception);

            return AtlasSmoothing.SMOOTHED;
        }
    }
}
