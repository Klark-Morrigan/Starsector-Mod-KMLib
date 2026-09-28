package kmlib.starsector.ui.font;

/**
 * Reads the line height of the atlas the install holds for a face - its descriptor's
 * {@code lineHeight}, the one size its glyphs draw at 1:1, because the font loader scales every
 * request against it.
 *
 * <p>A port rather than a number on {@link StarsectorFont}, because the number is the install's and
 * not the face's: a core localisation replaces several of the game's atlases with larger ones under
 * the same basenames, and each of its editions moves the line height differently. Only the loaded
 * face knows which atlas it is, and loading one needs a running game, so a style composed away from
 * one is handed the reading rather than performing it. {@link LazyFontLineHeightReader} is the
 * LazyLib-backed adapter.
 */
@FunctionalInterface
public interface FaceLineHeightReader {

    /**
     * The line height of the atlas installed at {@code atlas}'s path, in the units a text size is stated
     * in.
     *
     * @param atlas the face whose installed atlas is read
     * @return the atlas's line height, or zero when the face cannot load
     */
    double readLineHeight(FontAtlas atlas);
}
