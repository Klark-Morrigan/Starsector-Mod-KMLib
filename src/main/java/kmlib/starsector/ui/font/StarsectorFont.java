package kmlib.starsector.ui.font;

import java.util.Arrays;
import java.util.Optional;

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
 *
 * <p>A cut of a family with a lower-resolution cut below it names that cut, which is where
 * {@link FaceResolver} falls when the installed atlas cannot draw a text: a smaller cut of the same
 * design is the nearest face to the one asked for, and a core localisation replaces the smaller cuts
 * with atlases holding its script while leaving the largest untouched. A face with no such cut names
 * none, and falls straight to the game's own default face.
 *
 * <p>A {@link FontAtlas}, the faces KM chooses among the two kinds it can draw in; the other is the
 * one face the game's own settings declare, which KM reaches without having chosen it.
 */
public enum StarsectorFont implements FontAtlas {

    /**
     * The game's {@code defaultFont} on a vanilla install, which carries vanilla's paragraph text, and
     * the smallest cut of its family.
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
     * The body face's middle cut, between the paragraph face and the larger cut above it.
     */
    VANILLA_INSIGNIA_21("insignia21LTaa", AtlasSmoothing.SMOOTHED, VANILLA_INSIGNIA_15),

    /**
     * The body face's larger cut, and the largest atlas a core localisation replaces with one holding
     * its script. Where a text needs glyphs the high-resolution atlas above lacks, this is the largest
     * face that can still draw it - blockier when stretched, but readable.
     */
    VANILLA_INSIGNIA_25("insignia25LTaa", AtlasSmoothing.SMOOTHED, VANILLA_INSIGNIA_21),

    /**
     * The highest-resolution antialiased atlas the game ships, and so the only one that stays clean
     * when text is magnified far past its native size.
     */
    VANILLA_INSIGNIA_42("insignia42LTaa", AtlasSmoothing.SMOOTHED, VANILLA_INSIGNIA_25);

    // The game's bitmap fonts all live under graphics/fonts with a .fnt extension, so a basename
    // resolves to a loadable path by wrapping. Held here rather than at the loader, so the one
    // place that knows a face's basename is also the one place that knows its path.
    private static final String FONT_DIR = "graphics/fonts/";
    private static final String FONT_EXTENSION = ".fnt";

    private final AtlasSmoothing smoothing;
    private final String basename;

    // The next cut down in this face's family, or null for a face with none. Null rather than an
    // Optional because an enum constructor takes it positionally; the accessor answers the Optional.
    private final StarsectorFont lowerResolutionFont;

    StarsectorFont(String basename, AtlasSmoothing smoothing) {
        this(basename, smoothing, null);
    }

    StarsectorFont(String basename, AtlasSmoothing smoothing, StarsectorFont lowerResolutionFont) {
        this.basename = basename;
        this.smoothing = smoothing;
        this.lowerResolutionFont = lowerResolutionFont;
    }

    /**
     * The face whose loadable path is {@code path}, the form the game's own settings name a face in.
     *
     * @param path a path such as {@code graphics/fonts/insignia15LTaa.fnt}, may be {@code null}
     * @return the face at that path, or empty where the enum names no face there
     */
    public static Optional<StarsectorFont> findFontByPath(String path) {
        return Arrays.stream(values())
            .filter(font -> font.resolvePath().equals(path))
            .findFirst();
    }

    /**
     * The atlas's file name under {@code graphics/fonts}, without its extension - how a log line or a
     * failure names a face.
     *
     * @return the atlas's basename, such as {@code insignia15LTaa}
     */
    public String getBasename() {
        return basename;
    }

    /**
     * @return the next cut down in this face's family - where the face falls when its installed atlas
     *         cannot draw a text - or empty for a face with no smaller cut
     */
    public Optional<StarsectorFont> resolveLowerResolutionFont() {
        return Optional.ofNullable(lowerResolutionFont);
    }

    /**
     * @return whether this atlas's glyphs want interpolating when drawn, as its descriptor's
     *         {@code aa} states - what a draw pass reads to pick the filter it binds the atlas under
     */
    @Override
    public AtlasSmoothing getSmoothing() {
        return smoothing;
    }

    /**
     * @return the loadable {@code .fnt} path under {@code graphics/fonts}, the form both LazyLib's
     *         font loader and vanilla's {@code setParaFont} / {@code setTitleFont} take
     */
    @Override
    public String resolvePath() {
        return FONT_DIR + basename + FONT_EXTENSION;
    }
}
