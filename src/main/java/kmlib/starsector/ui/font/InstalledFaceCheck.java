package kmlib.starsector.ui.font;

import com.fs.starfarer.api.Global;

import org.apache.log4j.Logger;

import java.util.StringJoiner;

/**
 * Loads every face KM text may draw in once, as the game starts, and states in the log what the install
 * holds under each.
 *
 * <p>At start rather than on first draw, so a face the install cannot load is found - and logged once,
 * by {@link LazyFontCache} - before any text settles on it: {@link FaceResolver} then reads it as absent
 * and no walk stops at it. Every face the enum names is loaded, and so is the face the game's settings
 * declare as its default, which a walk may reach without the enum naming it.
 *
 * <p>The line the check leaves is the one reading of which atlases a session drew with. A localised
 * install replaces several of them under the same basenames, and its editions differ in line height, so
 * a report of text drawn at the wrong size cannot be read without it.
 */
public final class InstalledFaceCheck {

    private static final Logger LOG = Global.getLogger(InstalledFaceCheck.class);

    // What a face that will not load is reported as, in place of a line height.
    private static final String UNAVAILABLE_READING = "unavailable";

    // The line height the reader states for a face that will not load.
    private static final double NO_LINE_HEIGHT = 0d;

    // Checks only; never instantiated.
    private InstalledFaceCheck() {
    }

    /**
     * Loads every face and the game's declared default through the shared cache, and logs each one's
     * installed line height, or that it would not load.
     */
    public static void loadEveryFace() {

        var faceReadings = new StringJoiner(", ");

        for (var font : StarsectorFont.values()) {

            faceReadings.add(font.getBasename() + "="
                + formatLineHeightReading(LazyFontLineHeightReader.readLineHeight(font)));
        }
        var declaredDefaultAtlas = GameDefaultFontReader.readDefaultFont();

        LOG.info("Installed font faces loaded; lineHeight by face: " + faceReadings
            + "; defaultFont " + declaredDefaultAtlas.resolvePath() + "="
            + formatLineHeightReading(LazyFontLineHeightReader.readLineHeight(declaredDefaultAtlas)));
    }

    // A line height as the whole number a descriptor states it as, or the word for a face that will not
    // load, since a zero would read as an atlas of no height rather than as no atlas.
    private static String formatLineHeightReading(double lineHeight) {

        return lineHeight > NO_LINE_HEIGHT
            ? Long.toString(Math.round(lineHeight))
            : UNAVAILABLE_READING;
    }
}
