package kmlib.testfixtures.starsector.ui.font;

import kmlib.starsector.ui.font.GlyphCoverageReader;
import kmlib.starsector.ui.font.StarsectorFont;

import java.util.EnumSet;
import java.util.Set;

/**
 * A {@link GlyphCoverageReader} answering from a rule instead of from loaded atlases, so a face can be
 * settled with no running game. Published as a fixture variant so KMLib's and consuming mods' suites
 * state what an install's atlases hold through one shared double.
 *
 * <p>{@link #createLatinOnlyCoverage()} is a vanilla install: every face holds the Latin-1 range and
 * nothing past it. {@link #coveringEveryCharacter} widens one face to every character, which is what a
 * localised install's replaced atlas comes to for the text a suite poses. Whitespace is never asked
 * about, as the port states.
 */
public final class GlyphCoverageReaderFake implements GlyphCoverageReader {

    // The last code point of Latin-1, the range every vanilla atlas draws.
    private static final int LAST_LATIN_1_CODE_POINT = 0xFF;

    private final Set<StarsectorFont> fullyCoveringFonts;

    private GlyphCoverageReaderFake(Set<StarsectorFont> fullyCoveringFonts) {
        this.fullyCoveringFonts = fullyCoveringFonts;
    }

    /**
     * @return a reader under which every face draws Latin-1 and nothing beyond it
     */
    public static GlyphCoverageReaderFake createLatinOnlyCoverage() {
        return new GlyphCoverageReaderFake(EnumSet.noneOf(StarsectorFont.class));
    }

    /**
     * Returns a copy of this reader under which {@code font} draws every character.
     *
     * @param font the face whose installed atlas holds everything a suite poses
     * @return an otherwise-identical reader
     */
    public GlyphCoverageReaderFake coveringEveryCharacter(StarsectorFont font) {

        var fullyCoveringFonts = EnumSet.noneOf(StarsectorFont.class);

        fullyCoveringFonts.addAll(this.fullyCoveringFonts);
        fullyCoveringFonts.add(font);

        return new GlyphCoverageReaderFake(fullyCoveringFonts);
    }

    @Override
    public boolean coversText(StarsectorFont font, String text) {
        return fullyCoveringFonts.contains(font)
            || text.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .allMatch(codePoint -> codePoint <= LAST_LATIN_1_CODE_POINT);
    }
}
