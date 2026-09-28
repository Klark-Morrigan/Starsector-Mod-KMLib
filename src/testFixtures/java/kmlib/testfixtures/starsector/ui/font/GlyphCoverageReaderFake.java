package kmlib.testfixtures.starsector.ui.font;

import kmlib.starsector.ui.font.FontAtlas;
import kmlib.starsector.ui.font.GlyphCoverageReader;

import java.util.HashSet;
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

    private final Set<FontAtlas> fullyCoveringAtlases;

    private GlyphCoverageReaderFake(Set<FontAtlas> fullyCoveringAtlases) {
        this.fullyCoveringAtlases = fullyCoveringAtlases;
    }

    /**
     * @return a reader under which every face draws Latin-1 and nothing beyond it
     */
    public static GlyphCoverageReaderFake createLatinOnlyCoverage() {
        return new GlyphCoverageReaderFake(Set.of());
    }

    /**
     * Returns a copy of this reader under which {@code atlas} draws every character.
     *
     * @param atlas the face whose installed atlas holds everything a suite poses
     * @return an otherwise-identical reader
     */
    public GlyphCoverageReaderFake coveringEveryCharacter(FontAtlas atlas) {

        var fullyCoveringAtlases = new HashSet<>(this.fullyCoveringAtlases);

        fullyCoveringAtlases.add(atlas);

        return new GlyphCoverageReaderFake(fullyCoveringAtlases);
    }

    @Override
    public boolean coversText(FontAtlas atlas, String text) {
        return fullyCoveringAtlases.contains(atlas)
            || text.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .allMatch(codePoint -> codePoint <= LAST_LATIN_1_CODE_POINT);
    }
}
