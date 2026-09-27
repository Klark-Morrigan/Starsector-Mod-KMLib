package kmlib.starsector.ui.font;

/**
 * Reads whether the atlas the install holds for a face draws every character of a text as itself,
 * rather than as the fallback glyph a bitmap face substitutes for a character it lacks.
 *
 * <p>A port for the reason {@link FaceLineHeightReader} is one: coverage is the install's and not the
 * face's. No vanilla atlas carries a CJK glyph, a core localisation replaces some of them with atlases
 * that do, and its editions disagree on how many characters each holds - so what renders on one install
 * draws a row of question marks on another, and only the loaded face can say which. Loading one needs
 * a running game. {@link LazyFontGlyphCoverageReader} is the LazyLib-backed adapter.
 */
@FunctionalInterface
public interface GlyphCoverageReader {

    /**
     * Whether {@code font}'s installed atlas draws every character of {@code text} as itself.
     * Whitespace is not asked about: a face draws it as space whether or not its atlas holds a glyph.
     *
     * @param font the face whose installed atlas is read
     * @param text the text the face would draw
     * @return whether no character of the text would fall back; false for a face that cannot load
     */
    boolean coversText(StarsectorFont font, String text);
}
