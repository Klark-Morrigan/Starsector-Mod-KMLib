package kmlib.starsector.ui.font;

/**
 * A bitmap font atlas text can be drawn in: where its descriptor is, and whether its glyphs want
 * interpolating. What the loader, the measurers and the draw passes need of a face, and nothing about
 * where the face came from.
 *
 * <p>Sealed over the two ways KM comes to draw in an atlas. {@link StarsectorFont} is a face KM names
 * itself - the only way KM code chooses one. {@link DeclaredFontAtlas} is the one face KM does not choose:
 * whatever file the game's {@code defaultFont} setting names, which any mod can point at a face of its
 * own, such as a language pack's atlas holding its script. Kept apart so the enum stays the door for
 * every face KM picks, while a text walking down to the game's own face can still reach one the enum has
 * never heard of.
 */
public sealed interface FontAtlas permits StarsectorFont, DeclaredFontAtlas {

    /**
     * @return the loadable {@code .fnt} path, the form both LazyLib's font loader and vanilla's
     *         {@code setParaFont} / {@code setTitleFont} take
     */
    String resolvePath();

    /**
     * @return whether this atlas's glyphs want interpolating when drawn - what a draw pass reads to pick
     *         the filter it binds the atlas under
     */
    AtlasSmoothing getSmoothing();
}
