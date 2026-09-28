package kmlib.testfixtures.starsector.ui.font;

import kmlib.starsector.ui.font.FaceLineHeightReader;
import kmlib.starsector.ui.font.FontAtlas;
import kmlib.starsector.ui.font.StarsectorFont;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link FaceLineHeightReader} answering from a table instead of from loaded atlases, so a style built
 * over native faces can be composed with no running game. Published as a fixture variant so KMLib's and
 * consuming mods' suites stand in for the install through one shared double.
 *
 * <p>{@link #createVanillaLineHeights()} answers what a vanilla install's descriptors state. A suite
 * pinning that a size was read rather than written down wants numbers no constant could have produced,
 * and takes {@link #answeringLineHeight} over the vanilla table for the face it is about.
 */
public final class FaceLineHeightReaderFake implements FaceLineHeightReader {

    // The lineHeight each vanilla descriptor under starsector-core/graphics/fonts states. Restated rather
    // than parsed, a suite having no install to read.
    private static final double VANILLA_INSIGNIA_15_LINE_HEIGHT = 15d;
    private static final double VANILLA_ORBITRON_20AA_LINE_HEIGHT = 20d;
    private static final double VANILLA_ORBITRON_12_CONDENSED_LINE_HEIGHT = 15d;
    private static final double VANILLA_VICTOR_10_LINE_HEIGHT = 9d;
    private static final double VANILLA_INSIGNIA_21_LINE_HEIGHT = 21d;
    private static final double VANILLA_INSIGNIA_25_LINE_HEIGHT = 24d;
    private static final double VANILLA_INSIGNIA_42_LINE_HEIGHT = 42d;

    // What a face missing from the table reads as - the port's own answer for a face that will not load.
    private static final double NO_HEIGHT = 0d;

    private final Map<FontAtlas, Double> lineHeightByAtlas;

    private FaceLineHeightReaderFake(Map<FontAtlas, Double> lineHeightByAtlas) {
        this.lineHeightByAtlas = lineHeightByAtlas;
    }

    /**
     * @return a reader answering each face with the line height its vanilla descriptor states
     */
    public static FaceLineHeightReaderFake createVanillaLineHeights() {

        var lineHeightByAtlas = new HashMap<FontAtlas, Double>();

        lineHeightByAtlas.put(StarsectorFont.VANILLA_INSIGNIA_15, VANILLA_INSIGNIA_15_LINE_HEIGHT);
        lineHeightByAtlas.put(StarsectorFont.VANILLA_ORBITRON_20AA, VANILLA_ORBITRON_20AA_LINE_HEIGHT);
        lineHeightByAtlas.put(
            StarsectorFont.VANILLA_ORBITRON_12_CONDENSED,
            VANILLA_ORBITRON_12_CONDENSED_LINE_HEIGHT);
        lineHeightByAtlas.put(StarsectorFont.VANILLA_VICTOR_10, VANILLA_VICTOR_10_LINE_HEIGHT);
        lineHeightByAtlas.put(StarsectorFont.VANILLA_INSIGNIA_21, VANILLA_INSIGNIA_21_LINE_HEIGHT);
        lineHeightByAtlas.put(StarsectorFont.VANILLA_INSIGNIA_25, VANILLA_INSIGNIA_25_LINE_HEIGHT);
        lineHeightByAtlas.put(StarsectorFont.VANILLA_INSIGNIA_42, VANILLA_INSIGNIA_42_LINE_HEIGHT);

        return new FaceLineHeightReaderFake(lineHeightByAtlas);
    }

    /**
     * Returns a copy of this reader answering {@code atlas} with {@code lineHeight} - an install whose
     * atlas at that path is not vanilla's, or a face the game's settings declare that vanilla does not ship.
     *
     * @param atlas      the face whose installed atlas differs
     * @param lineHeight the line height that atlas states
     * @return an otherwise-identical reader
     */
    public FaceLineHeightReaderFake answeringLineHeight(FontAtlas atlas, double lineHeight) {

        var lineHeightByAtlas = new HashMap<>(this.lineHeightByAtlas);

        lineHeightByAtlas.put(atlas, lineHeight);

        return new FaceLineHeightReaderFake(lineHeightByAtlas);
    }

    @Override
    public double readLineHeight(FontAtlas atlas) {
        return lineHeightByAtlas.getOrDefault(atlas, NO_HEIGHT);
    }
}
