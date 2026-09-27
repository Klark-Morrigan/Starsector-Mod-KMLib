package kmlib.starsector.ui.font;

/**
 * The {@code graphics/fonts} atlases KM UI text draws in, one value per atlas. It is the single
 * door to the game's faces, the way {@code StarsectorUiColour} is to its colours: a face is named
 * rather than spelled, so a mistyped basename is a compile error instead of text that silently
 * fails to draw at runtime, and the set of faces the mods actually use is answerable in one place
 * rather than by grepping string literals across two repositories.
 *
 * <p>A value names an atlas and does not state its size. A bitmap face has exactly one size it is
 * crisp at - its descriptor's {@code lineHeight}, which the font loader scales every request
 * against - but that number belongs to whichever atlas the install holds under the basename, and
 * installs disagree: a core localisation overwriting {@code starsector-core} replaces several of
 * these files with larger atlases under the same names, and each of its editions moves the line
 * height differently. So the size is read off the loaded face through {@link FaceLineHeightReader}
 * and never written down here, where it could only be right for one install. The role a face plays
 * is deliberately not encoded in the value name either: this enum says which atlases exist, and the
 * styles built over it say which role draws in which.
 *
 * <p>Each value does carry whether its atlas wants interpolating. Drawing at the native size is only
 * half of what a pixel face needs: landed pixel-for-pixel but drawn through an interpolating filter,
 * every one of its single-pixel strokes is an edge and loses part of itself, so the text arrives
 * dimmer than the colour it was set in. That flag holds across every install known to replace these
 * files, which keep each face's antialiasing as vanilla ships it.
 */
public enum StarsectorFont {

    /**
     * The game's {@code defaultFont} (from {@code settings.json}), which carries vanilla's
     * paragraph text.
     */
    VANILLA_INSIGNIA_15("insignia15LTaa", AtlasSmoothing.SMOOTHED),

    /**
     * Vanilla's title and section-heading face, distinctly wider and blockier than the body face.
     */
    VANILLA_ORBITRON_20AA("orbitron20aa", AtlasSmoothing.SMOOTHED),

    /**
     * The narrow Orbitron the game sets its sector-map Sector/System tabs in, and its tooltip key
     * hints - the "Press F1 for more info" line at the foot of a vanilla box. Condensed rather than
     * merely small: at the same height as the title face it draws a visibly narrower glyph, which is
     * what lets a fixed-width tab hold a label the wider face would overrun.
     *
     * <p>The twelve in its name is not the size it draws at: it is the character height the face was
     * matched at, which the descriptor carries as {@code size=-12} beside a line height of 15 on a
     * vanilla install.
     *
     * <p>Hard-edged despite being an Orbitron: its atlas is {@code aa=1}, so it carries no soft edge
     * of its own and interpolating it costs every stroke part of itself. It reads as a smooth face
     * because the family is a smooth one and because its {@code smooth=1} flag says so, and neither
     * is the test.
     */
    VANILLA_ORBITRON_12_CONDENSED("orbitron12condensed", AtlasSmoothing.PIXEL_EXACT),

    /**
     * The pixel face vanilla sets its compact chrome in - the map-toggle buttons above the intel
     * screen's visor - so a KM row drawn to sit among those buttons is lettered the way they are.
     *
     * <p>Its atlas draws capitals whatever case a caller writes: every glyph sits on the same 5x5
     * cell with lowercase included and no descenders, so a mixed-case label needs no upper-casing
     * pass and the width it is measured at is the width it draws at.
     *
     * <p>The ten in its name is not the size it draws at either: a vanilla descriptor carries
     * {@code size=-10} beside a line height of 9. Asked for at 10 there, it comes out at
     * ten-ninths - which on a face of single-pixel strokes is a row of glyphs landing between pixels
     * rather than a slightly larger row of them.
     */
    VANILLA_VICTOR_10("victor10", AtlasSmoothing.PIXEL_EXACT),

    /**
     * The body face's larger cut, and the largest atlas a core localisation replaces with one holding
     * its script. Where a text needs glyphs the high-resolution atlas below lacks, this is the largest
     * face that can still draw it - blockier when stretched, but readable.
     */
    VANILLA_INSIGNIA_25("insignia25LTaa", AtlasSmoothing.SMOOTHED),

    /**
     * The highest-resolution antialiased atlas the game ships, and so the only one that stays clean
     * when text is magnified far past its native size.
     */
    VANILLA_INSIGNIA_42("insignia42LTaa", AtlasSmoothing.SMOOTHED);

    // The game's bitmap fonts all live under graphics/fonts with a .fnt extension, so a basename
    // resolves to a loadable path by wrapping. Held here rather than at the loader, so the one
    // place that knows a face's basename is also the one place that knows its path.
    private static final String FONT_DIR = "graphics/fonts/";
    private static final String FONT_EXTENSION = ".fnt";

    private final AtlasSmoothing smoothing;
    private final String basename;

    StarsectorFont(String basename, AtlasSmoothing smoothing) {
        this.basename = basename;
        this.smoothing = smoothing;
    }

    /**
     * The atlas's file name under {@code graphics/fonts}, without its extension - the one spelling of a
     * face a player sees, since a settings Radio offering faces lists them by it. An identifier rather
     * than a caption, so it is the same in every locale and a stored choice survives a locale switch.
     *
     * @return the atlas's basename, such as {@code insignia15LTaa}
     */
    public String getBasename() {
        return basename;
    }

    /**
     * @return whether this atlas's glyphs want interpolating when drawn, as its descriptor's
     *         {@code smooth} states - what a draw pass reads to pick the filter it binds the atlas
     *         under
     */
    public AtlasSmoothing getSmoothing() {
        return smoothing;
    }

    /**
     * @return the loadable {@code .fnt} path under {@code graphics/fonts}, the form both LazyLib's
     *         font loader and vanilla's {@code setParaFont} / {@code setTitleFont} take
     */
    public String resolvePath() {
        return FONT_DIR + basename + FONT_EXTENSION;
    }
}
