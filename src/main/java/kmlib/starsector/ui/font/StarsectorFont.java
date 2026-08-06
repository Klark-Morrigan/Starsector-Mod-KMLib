package kmlib.starsector.ui.font;

/**
 * The {@code graphics/fonts} atlases KM UI text draws in, one value per atlas. It is the single
 * door to the game's faces, the way {@code StarsectorUiColour} is to its colours: a face is named
 * rather than spelled, so a mistyped basename is a compile error instead of text that silently
 * fails to draw at runtime, and the set of faces the mods actually use is answerable in one place
 * rather than by grepping string literals across two repositories.
 *
 * <p>Each value carries the size its atlas was rasterised at, because a bitmap face has exactly one
 * resolution it is crisp at - glyphs land 1:1 at that size and are scaled at any other. A caller
 * with no size of its own therefore names the native one rather than repeating a number the atlas
 * already states. The role a face plays is deliberately not encoded in the value name: this enum
 * says which atlases exist, and the styles built over it say which role draws in which.
 */
public enum StarsectorFont {

    /**
     * The game's {@code defaultFont} (from {@code settings.json}), which carries vanilla's
     * paragraph text.
     */
    VANILLA_INSIGNIA_15("insignia15LTaa", 15),

    /**
     * Vanilla's title and section-heading face, distinctly wider and blockier than the body face.
     */
    VANILLA_ORBITRON_20AA("orbitron20aa", 20),

    /**
     * The narrow, small Orbitron the game sets its tooltip key hints in - the "Press F1 for more
     * info" line at the foot of a vanilla box. Condensed rather than merely small, so a line of it
     * stays under the width of the content it sits beneath.
     */
    VANILLA_ORBITRON_12_CONDENSED("orbitron12condensed", 12),

    /**
     * The highest-resolution antialiased atlas the game ships, and so the only one that stays clean
     * when text is magnified far past its native size.
     */
    VANILLA_INSIGNIA_42("insignia42LTaa", 42);

    // The game's bitmap fonts all live under graphics/fonts with a .fnt extension, so a basename
    // resolves to a loadable path by wrapping. Held here rather than at the loader, so the one
    // place that knows a face's basename is also the one place that knows its path.
    private static final String FONT_DIR = "graphics/fonts/";
    private static final String FONT_EXTENSION = ".fnt";

    private final String basename;
    private final int nativeSize;

    StarsectorFont(String basename, int nativeSize) {
        this.basename = basename;
        this.nativeSize = nativeSize;
    }

    /**
     * @return the size the atlas was rasterised at (its descriptor's {@code size=}), the one size
     *         its glyphs draw at 1:1
     */
    public int getNativeSize() {
        return nativeSize;
    }

    /**
     * @return the loadable {@code .fnt} path under {@code graphics/fonts}, the form both LazyLib's
     *         font loader and vanilla's {@code setParaFont} / {@code setTitleFont} take
     */
    public String resolvePath() {
        return FONT_DIR + basename + FONT_EXTENSION;
    }
}
