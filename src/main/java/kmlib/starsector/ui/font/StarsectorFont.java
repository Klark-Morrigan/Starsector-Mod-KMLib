package kmlib.starsector.ui.font;

/**
 * The {@code graphics/fonts} atlases KM UI text draws in, one value per atlas. It is the single
 * door to the game's faces, the way {@code StarsectorUiColour} is to its colours: a face is named
 * rather than spelled, so a mistyped basename is a compile error instead of text that silently
 * fails to draw at runtime, and the set of faces the mods actually use is answerable in one place
 * rather than by grepping string literals across two repositories.
 *
 * <p>Each value carries the size its atlas draws 1:1 at, because a bitmap face has exactly one
 * resolution it is crisp at - glyphs land pixel-for-pixel at that size and are resampled at any
 * other. A caller with no size of its own therefore names the native one rather than repeating a
 * number the atlas already states. The role a face plays is deliberately not encoded in the value
 * name: this enum says which atlases exist, and the styles built over it say which role draws in
 * which.
 *
 * <p>That size is the descriptor's {@code lineHeight}, not its nominal {@code size=}: the font
 * loader scales a face by the size asked for over the atlas's line height, so the line height is
 * what a request has to name to come out at 1:1. The two agree on every antialiased atlas here and
 * part on the pixel faces, which state the character height they were matched at as a negative
 * {@code size=} - so the distinction only shows itself where it matters most, a pixel face being
 * the one kind of atlas a fractional scale visibly damages.
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
     *
     * <p>TODO: its atlas draws 1:1 at 15, the line height its descriptor states; the 12 here is the
     * nominal size its filename carries, so text asking for the native size renders at four-fifths
     * scale. Being an antialiased face it resamples cleanly and the shortfall has gone unnoticed,
     * which is why correcting it is a deliberate resize of the footnote text rather than a fix
     * folded into this one.
     */
    VANILLA_ORBITRON_12_CONDENSED("orbitron12condensed", 12),

    /**
     * The pixel face vanilla sets its compact chrome in - the map-toggle buttons above the intel
     * screen's visor - so a KM row drawn to sit among those buttons is lettered the way they are.
     *
     * <p>Its atlas draws capitals whatever case a caller writes: every glyph sits on the same 5x5
     * cell with lowercase included and no descenders, so a mixed-case label needs no upper-casing
     * pass and the width it is measured at is the width it draws at.
     *
     * <p>Nine, against the {@code size=-10} its name and its descriptor both carry: the atlas states
     * a line height of 9, and that is what the loader scales against. Asked for at 10 it comes out
     * at ten-ninths - which on a face of single-pixel strokes is a row of glyphs landing between
     * pixels rather than a slightly larger row of them.
     */
    VANILLA_VICTOR_10("victor10", 9),

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
